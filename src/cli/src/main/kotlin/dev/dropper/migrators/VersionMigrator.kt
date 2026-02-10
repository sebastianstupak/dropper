package dev.dropper.migrators

import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Migrates project to a new Minecraft version
 */
class VersionMigrator : Migrator {
    private val apiDetector = ApiChangeDetector()
    private lateinit var projectDir: File

    override fun planMigration(context: MigrationContext): MigrationPlan {
        projectDir = context.projectDir
        val operations = mutableListOf<MigrationOperation>()
        val warnings = mutableListOf<String>()
        val manualSteps = mutableListOf<String>()

        val fromVersion = context.params["from"] ?: ""
        val toVersion = context.params["to"] ?: ""

        if (fromVersion.isEmpty() || toVersion.isEmpty()) {
            warnings.add("Source or target version not specified")
            return MigrationPlan(operations, warnings, manualSteps)
        }

        val versionDir = toVersion.replace(".", "_")
        val targetPath = File(context.projectDir, "versions/$versionDir")

        // Check if target version already exists
        if (targetPath.exists() && !context.force) {
            warnings.add("Version $toVersion already exists. Use --force to overwrite")
            return MigrationPlan(operations, warnings, manualSteps)
        }

        // Create version directory
        operations.add(MigrationOperation.CreateDirectory(
            File(context.projectDir, "versions/$versionDir").absolutePath
        ))

        // Find source version to copy from
        val sourceVersionDir = if (fromVersion.isNotEmpty()) {
            fromVersion.replace(".", "_")
        } else {
            // Find most recent version
            findMostRecentVersion(context.projectDir)
        }

        val sourcePath = File(context.projectDir, "versions/$sourceVersionDir")
        if (!sourcePath.exists()) {
            warnings.add("Source version $fromVersion not found")
            return MigrationPlan(operations, warnings, manualSteps)
        }

        // Copy version structure
        copyVersionStructure(sourcePath, targetPath, operations, context)

        // Detect API changes
        val apiChanges = apiDetector.detectChanges(fromVersion, toVersion)
        if (apiChanges.isNotEmpty()) {
            warnings.add("Detected ${apiChanges.size} potential API changes")
            apiChanges.forEach { change ->
                if (!change.autoFixable) {
                    manualSteps.add("Review ${change.description}: ${change.oldPattern} → ${change.newPattern}")
                }
            }
        }

        // Update version config
        operations.add(
            MigrationOperation.UpdateConfig(
                File(context.projectDir, "versions/$versionDir/config.yml").absolutePath,
                mapOf(
                    "minecraft_version" to toVersion,
                    "asset_pack" to (context.params["assetPack"] ?: "v1")
                ),
                "Update version configuration"
            )
        )

        // Apply auto-fixes if enabled
        if (context.autoFix && apiChanges.any { it.autoFixable }) {
            applyAutoFixes(context.projectDir, versionDir, apiChanges, operations)
        }

        // Update root config.yml
        operations.add(
            MigrationOperation.UpdateConfig(
                File(context.projectDir, "config.yml").absolutePath,
                mapOf("add_version" to toVersion),
                "Add version to root config"
            )
        )

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

        plan.operations.forEach { operation ->
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

    private fun copyVersionStructure(
        source: File,
        target: File,
        operations: MutableList<MigrationOperation>,
        context: MigrationContext
    ) {
        source.listFiles()?.forEach { file ->
            val relativePath = file.relativeTo(source).path
            val targetFile = File(target, relativePath)

            if (file.isDirectory) {
                operations.add(MigrationOperation.CreateDirectory(targetFile.absolutePath))
                copyVersionStructure(file, targetFile, operations, context)
            } else {
                operations.add(
                    MigrationOperation.CopyFile(
                        file.absolutePath,
                        targetFile.absolutePath
                    )
                )
            }
        }
    }

    private fun applyAutoFixes(
        projectDir: File,
        versionDir: String,
        apiChanges: List<ApiChange>,
        operations: MutableList<MigrationOperation>
    ) {
        val versionPath = File(projectDir, "versions/$versionDir")
        versionPath.walkTopDown().filter { it.isFile && it.extension == "java" }.forEach { file ->
            val content = file.readText()
            val detected = apiDetector.analyzeContent(content, "", "")

            if (detected.any { it.canAutoFix }) {
                val fixed = apiDetector.applyAutoFixes(content, detected)
                operations.add(
                    MigrationOperation.UpdateFileContent(
                        file.absolutePath,
                        content,
                        fixed,
                        "Auto-fix API changes"
                    )
                )
            }
        }
    }

    private fun findMostRecentVersion(projectDir: File): String {
        val versionsDir = File(projectDir, "versions")
        if (!versionsDir.exists()) return ""

        return versionsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name }
            ?.maxByOrNull { it.replace("_", ".") }
            ?: ""
    }

    private fun executeOperation(operation: MigrationOperation) {
        when (operation) {
            is MigrationOperation.CreateDirectory -> {
                File(operation.path).mkdirs()
            }
            is MigrationOperation.CopyFile -> {
                val source = File(operation.source)
                val dest = File(operation.destination)
                dest.parentFile?.mkdirs()
                source.copyTo(dest, overwrite = true)
            }
            is MigrationOperation.MoveFile -> {
                val source = File(operation.source)
                val dest = File(operation.destination)
                dest.parentFile?.mkdirs()
                source.copyTo(dest, overwrite = true)
                source.delete()
            }
            is MigrationOperation.DeleteFile -> {
                File(operation.path).delete()
            }
            is MigrationOperation.UpdateFileContent -> {
                FileUtil.writeText(File(operation.path), operation.newContent)
            }
            is MigrationOperation.ReplaceInFile -> {
                val file = File(operation.path)
                var content = file.readText()
                operation.replacements.forEach { (old, new) ->
                    content = content.replace(old, new)
                }
                FileUtil.writeText(file, content)
            }
            is MigrationOperation.UpdateConfig -> {
                val file = File(operation.configPath)
                var content = if (file.exists()) file.readText() else ""

                operation.updates.forEach { (key, value) ->
                    if (key == "add_version") {
                        // Add version to list
                        if (content.contains("versions:")) {
                            val lines = content.lines().toMutableList()
                            val versionLineIdx = lines.indexOfFirst { it.trim().startsWith("versions:") }
                            if (versionLineIdx != -1) {
                                val versionLine = lines[versionLineIdx]
                                if (versionLine.contains("[")) {
                                    lines[versionLineIdx] = versionLine.replace("]", ", $value]")
                                } else {
                                    lines.add(versionLineIdx + 1, "  - $value")
                                }
                                content = lines.joinToString("\n")
                            }
                        }
                    } else {
                        // Simple key-value update
                        val pattern = "$key:.*".toRegex()
                        val replacement = "$key: \"$value\""
                        if (pattern.containsMatchIn(content)) {
                            content = content.replace(pattern, replacement)
                        } else {
                            content += "\n$replacement"
                        }
                    }
                }

                file.parentFile?.mkdirs()
                FileUtil.writeText(file, content)
            }
        }
    }

    private fun formatOperationResult(operation: MigrationOperation): String {
        return when (operation) {
            is MigrationOperation.CreateDirectory -> "Created directory: ${operation.path}"
            is MigrationOperation.CopyFile -> "Copied: ${File(operation.source).name}"
            is MigrationOperation.MoveFile -> "Moved: ${File(operation.source).name}"
            is MigrationOperation.DeleteFile -> "Deleted: ${operation.path}"
            is MigrationOperation.UpdateFileContent -> "Updated: ${File(operation.path).name} - ${operation.description}"
            is MigrationOperation.ReplaceInFile -> "Replaced in: ${File(operation.path).name}"
            is MigrationOperation.UpdateConfig -> "Updated config: ${File(operation.configPath).name}"
        }
    }
}
