package dev.dropper.validator

import java.io.File

/**
 * Validates project directory structure
 */
class StructureValidator : Validator {
    override val name = "Structure Validator"

    private val requiredDirs = listOf(
        "shared",
        "shared/common",
        "versions"
    )

    override fun validate(projectDir: File, options: ValidationOptions): ValidationResult {
        val startTime = System.currentTimeMillis()
        val issues = mutableListOf<ValidationIssue>()
        var filesScanned = 0

        // Check required directories
        requiredDirs.forEach { dirPath ->
            val dir = File(projectDir, dirPath)
            if (!dir.exists()) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Missing required directory: $dirPath",
                        projectDir,
                        null,
                        "Create directory: ${dir.absolutePath}"
                    )
                )
            }
        }

        // Validate Java package structure
        val sharedCommonJava = File(projectDir, "shared/common/src/main/java")
        if (sharedCommonJava.exists()) {
            sharedCommonJava.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "java") {
                    filesScanned++
                    issues.addAll(validateJavaFile(file, projectDir))
                }
            }
        }

        // Validate shared loader directories
        val sharedDir = File(projectDir, "shared")
        if (sharedDir.exists()) {
            sharedDir.listFiles()?.forEach { loaderDir ->
                if (loaderDir.isDirectory && loaderDir.name != "common") {
                    val loaderJava = File(loaderDir, "src/main/java")
                    if (loaderJava.exists()) {
                        loaderJava.walkTopDown().forEach { file ->
                            if (file.isFile && file.extension == "java") {
                                filesScanned++
                                issues.addAll(validateJavaFile(file, projectDir))
                            }
                        }
                    }
                }
            }
        }

        // Check for files in wrong locations
        val versionsDir = File(projectDir, "versions")
        if (versionsDir.exists()) {
            versionsDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "java") {
                    issues.add(
                        ValidationIssue(
                            ValidationSeverity.WARNING,
                            "Java file in versions directory (should be in shared/)",
                            file,
                            null,
                            "Move Java files to shared/common or shared/<loader>"
                        )
                    )
                }
            }
        }

        // Validate version directory structure
        if (versionsDir.exists()) {
            versionsDir.listFiles()?.forEach { versionDir ->
                if (versionDir.isDirectory) {
                    // Each version should have assets or data
                    val hasAssets = File(versionDir, "assets").exists() ||
                            versionDir.walkTopDown().any { it.isDirectory && it.name == "assets" }
                    val hasData = File(versionDir, "data").exists() ||
                            versionDir.walkTopDown().any { it.isDirectory && it.name == "data" }

                    if (!hasAssets && !hasData && versionDir.name != "shared") {
                        issues.add(
                            ValidationIssue(
                                ValidationSeverity.WARNING,
                                "Version directory '${versionDir.name}' has no assets or data",
                                versionDir,
                                null,
                                "Add assets/ or data/ subdirectories"
                            )
                        )
                    }
                }
            }
        }

        // Check for invalid directory names
        val invalidChars = Regex("[^a-zA-Z0-9_\\-.]")
        projectDir.walkTopDown().maxDepth(3).forEach { file ->
            if (file.isDirectory && file != projectDir) {
                if (invalidChars.containsMatchIn(file.name)) {
                    issues.add(
                        ValidationIssue(
                            ValidationSeverity.WARNING,
                            "Directory name contains invalid characters: ${file.name}",
                            file,
                            null,
                            "Use only letters, numbers, hyphens, and underscores"
                        )
                    )
                }
            }
        }

        return ValidationResult(name, issues, filesScanned, System.currentTimeMillis() - startTime)
    }

    private fun validateJavaFile(file: File, projectDir: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        try {
            val content = file.readText()
            val lines = content.lines()

            // Find package declaration
            val packageLine = lines.indexOfFirst { it.trim().startsWith("package ") }
            if (packageLine == -1) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Missing package declaration",
                        file,
                        null,
                        "Add package declaration at top of file"
                    )
                )
                return issues
            }

            val packageDeclaration = lines[packageLine].trim()
                .removePrefix("package ")
                .removeSuffix(";")
                .trim()

            // Validate package matches directory structure
            val javaDir = findJavaDir(file) ?: return issues
            val relativePath = file.relativeTo(javaDir).parent?.replace("\\", "/") ?: ""
            val expectedPackage = relativePath.replace("/", ".")

            if (packageDeclaration != expectedPackage) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Package declaration '$packageDeclaration' does not match directory structure",
                        file,
                        packageLine + 1,
                        "Should be: package $expectedPackage;"
                    )
                )
            }

        } catch (e: Exception) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.WARNING,
                    "Failed to validate Java file: ${e.message}",
                    file,
                    null,
                    "Check file encoding and syntax"
                )
            )
        }

        return issues
    }

    private fun findJavaDir(file: File): File? {
        var current = file.parentFile
        while (current != null) {
            if (current.name == "java" && current.parentFile?.name == "main") {
                return current
            }
            current = current.parentFile
        }
        return null
    }

    override fun autoFix(projectDir: File, issues: List<ValidationIssue>): Int {
        var fixed = 0

        issues.forEach { issue ->
            if (issue.message.contains("Package declaration") && issue.suggestion != null) {
                val file = issue.file
                if (file != null && file.exists()) {
                    try {
                        val content = file.readText()
                        val lines = content.lines().toMutableList()
                        val packageLine = lines.indexOfFirst { it.trim().startsWith("package ") }

                        if (packageLine != -1 && issue.suggestion.startsWith("Should be: ")) {
                            val correctPackage = issue.suggestion.removePrefix("Should be: ")
                            lines[packageLine] = correctPackage
                            file.writeText(lines.joinToString("\n"))
                            fixed++
                        }
                    } catch (e: Exception) {
                        // Skip on error
                    }
                }
            }
        }

        return fixed
    }
}
