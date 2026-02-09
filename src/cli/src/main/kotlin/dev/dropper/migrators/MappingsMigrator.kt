package dev.dropper.migrators

import dev.dropper.util.FileUtil
import java.io.File

/**
 * Updates mappings versions in build files
 * Note: Full remapping would require MCP/Yarn tools integration
 */
class MappingsMigrator : Migrator {
    override fun planMigration(context: MigrationContext): MigrationPlan {
        val operations = mutableListOf<MigrationOperation>()
        val warnings = mutableListOf<String>()
        val manualSteps = mutableListOf<String>()

        val mappingsVersion = context.params["mappingsVersion"]

        if (mappingsVersion == null) {
            warnings.add("Mappings version not specified")
            return MigrationPlan(operations, warnings, manualSteps)
        }

        // Find all build files that reference mappings
        findBuildFiles(context.projectDir).forEach { buildFile ->
            val content = buildFile.readText()
            val replacements = mutableListOf<Pair<String, String>>()

            // Update Yarn mappings (Fabric)
            val yarnPattern = "net\\.fabricmc:yarn:([^\"]+)".toRegex()
            yarnPattern.findAll(content).forEach { match ->
                val oldVersion = match.groupValues[1]
                replacements.add(
                    "net.fabricmc:yarn:$oldVersion" to "net.fabricmc:yarn:$mappingsVersion"
                )
            }

            // Update Parchment mappings (Forge/NeoForge)
            val parchmentPattern = "parchmentmc/([^\"]+)".toRegex()
            parchmentPattern.findAll(content).forEach { match ->
                val oldVersion = match.groupValues[1]
                replacements.add(
                    "parchmentmc/$oldVersion" to "parchmentmc/$mappingsVersion"
                )
            }

            if (replacements.isNotEmpty()) {
                operations.add(
                    MigrationOperation.ReplaceInFile(
                        buildFile.absolutePath,
                        replacements,
                        "Update mappings version"
                    )
                )
            }
        }

        if (operations.isEmpty()) {
            warnings.add("No build files found with mappings references")
        }

        manualSteps.add("Refresh Gradle project after mappings update")
        manualSteps.add("Verify code still compiles with new mappings")
        manualSteps.add("Review any deprecation warnings")

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

    private fun findBuildFiles(dir: File): List<File> {
        val buildFiles = mutableListOf<File>()

        dir.walkTopDown().forEach { file ->
            if (file.name == "build.gradle.kts" || file.name == "build.gradle") {
                buildFiles.add(file)
            }
        }

        return buildFiles
    }

    private fun executeOperation(operation: MigrationOperation) {
        when (operation) {
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
            is MigrationOperation.ReplaceInFile -> "Updated mappings in: ${File(operation.path).name}"
            else -> "Executed operation"
        }
    }
}
