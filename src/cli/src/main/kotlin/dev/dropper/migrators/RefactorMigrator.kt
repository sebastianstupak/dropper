package dev.dropper.migrators

import dev.dropper.util.FileUtil
import java.io.File

/**
 * Refactors package names and moves files accordingly
 */
class RefactorMigrator : Migrator {
    override fun planMigration(context: MigrationContext): MigrationPlan {
        val operations = mutableListOf<MigrationOperation>()
        val warnings = mutableListOf<String>()
        val manualSteps = mutableListOf<String>()

        val oldPackage = context.params["oldPackage"] ?: ""
        val newPackage = context.params["newPackage"] ?: ""

        if (oldPackage.isEmpty() || newPackage.isEmpty()) {
            warnings.add("Old or new package name not specified")
            return MigrationPlan(operations, warnings, manualSteps)
        }

        if (oldPackage == newPackage) {
            warnings.add("Old and new package names are the same")
            return MigrationPlan(operations, warnings, manualSteps)
        }

        // Find all Java files
        val javaFiles = findJavaFiles(context.projectDir)

        javaFiles.forEach { file ->
            val content = file.readText()

            // Check if this file uses the old package
            if (content.contains("package $oldPackage")) {
                // Update package declaration
                val newContent = content.replace(
                    "package $oldPackage",
                    "package $newPackage"
                )

                operations.add(
                    MigrationOperation.UpdateFileContent(
                        file.absolutePath,
                        content,
                        newContent,
                        "Update package declaration"
                    )
                )

                // Calculate new file path
                val oldPath = oldPackage.replace(".", "/")
                val newPath = newPackage.replace(".", "/")
                val currentPath = file.absolutePath

                if (currentPath.contains(oldPath)) {
                    val newFilePath = currentPath.replace(oldPath, newPath)

                    operations.add(
                        MigrationOperation.CreateDirectory(
                            File(newFilePath).parentFile.absolutePath
                        )
                    )

                    operations.add(
                        MigrationOperation.MoveFile(
                            currentPath,
                            newFilePath
                        )
                    )
                }
            }

            // Update imports
            if (content.contains("import $oldPackage")) {
                val newContent = content.replace(
                    "import $oldPackage",
                    "import $newPackage"
                )

                operations.add(
                    MigrationOperation.UpdateFileContent(
                        file.absolutePath,
                        content,
                        newContent,
                        "Update imports"
                    )
                )
            }
        }

        // Update config files
        val configFile = File(context.projectDir, "config.yml")
        if (configFile.exists()) {
            operations.add(
                MigrationOperation.ReplaceInFile(
                    configFile.absolutePath,
                    listOf("package: \"$oldPackage\"" to "package: \"$newPackage\""),
                    "Update config.yml"
                )
            )
        }

        // Clean up old empty directories
        manualSteps.add("Remove empty directories from old package structure")
        manualSteps.add("Update any external references to the old package")
        manualSteps.add("Rebuild project to verify compilation")

        return MigrationPlan(operations, warnings, manualSteps)
    }

    override fun executeMigration(plan: MigrationPlan, dryRun: Boolean): MigrationResult {
        if (dryRun) {
            return MigrationResult(
                success = true,
                operationsExecuted = 0,
                warnings = plan.warnings,
                manualStepsRequired = plan.requiredManualSteps
            )
        }

        val errors = mutableListOf<String>()
        val changes = mutableListOf<String>()
        var executed = 0

        // Group operations to avoid conflicts
        val createOps = plan.operations.filterIsInstance<MigrationOperation.CreateDirectory>()
        val updateOps = plan.operations.filterIsInstance<MigrationOperation.UpdateFileContent>()
        val moveOps = plan.operations.filterIsInstance<MigrationOperation.MoveFile>()
        val replaceOps = plan.operations.filterIsInstance<MigrationOperation.ReplaceInFile>()

        // Execute in order: create dirs -> update content -> move files -> replace
        (createOps + updateOps + moveOps + replaceOps).forEach { operation ->
            try {
                executeOperation(operation)
                executed++
                changes.add(formatOperationResult(operation))
            } catch (e: Exception) {
                errors.add("Failed to execute ${operation.javaClass.simpleName}: ${e.message}")
            }
        }

        return MigrationResult(
            success = errors.isEmpty(),
            operationsExecuted = executed,
            errors = errors,
            warnings = plan.warnings,
            changes = changes,
            manualStepsRequired = plan.requiredManualSteps
        )
    }

    private fun findJavaFiles(dir: File): List<File> {
        val javaFiles = mutableListOf<File>()

        dir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "java") {
                javaFiles.add(file)
            }
        }

        return javaFiles
    }

    private fun executeOperation(operation: MigrationOperation) {
        when (operation) {
            is MigrationOperation.CreateDirectory -> {
                File(operation.path).mkdirs()
            }
            is MigrationOperation.UpdateFileContent -> {
                FileUtil.writeText(File(operation.path), operation.newContent)
            }
            is MigrationOperation.MoveFile -> {
                val source = File(operation.source)
                val dest = File(operation.destination)
                dest.parentFile?.mkdirs()
                source.copyTo(dest, overwrite = true)
                source.delete()
            }
            is MigrationOperation.ReplaceInFile -> {
                val file = File(operation.path)
                var content = file.readText()
                operation.replacements.forEach { (old, new) ->
                    content = content.replace(old, new)
                }
                FileUtil.writeText(file, content)
            }
            else -> {
                // Handle other operations if needed
            }
        }
    }

    private fun formatOperationResult(operation: MigrationOperation): String {
        return when (operation) {
            is MigrationOperation.CreateDirectory -> "Created directory: ${File(operation.path).name}"
            is MigrationOperation.UpdateFileContent -> "Updated: ${File(operation.path).name}"
            is MigrationOperation.MoveFile -> "Moved: ${File(operation.source).name}"
            is MigrationOperation.ReplaceInFile -> "Updated: ${File(operation.path).name}"
            else -> "Executed operation"
        }
    }
}
