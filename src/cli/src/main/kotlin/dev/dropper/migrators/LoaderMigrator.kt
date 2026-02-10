package dev.dropper.migrators

import dev.dropper.util.FileUtil
import java.io.File

/**
 * Migrates project to support additional mod loaders
 */
class LoaderMigrator : Migrator {
    override fun planMigration(context: MigrationContext): MigrationPlan {
        val operations = mutableListOf<MigrationOperation>()
        val warnings = mutableListOf<String>()
        val manualSteps = mutableListOf<String>()

        val loader = context.params["loader"] ?: ""
        val version = context.params["version"]

        if (loader.isEmpty()) {
            warnings.add("Loader not specified")
            return MigrationPlan(operations, warnings, manualSteps)
        }

        if (!listOf("fabric", "forge", "neoforge").contains(loader)) {
            warnings.add("Unknown loader: $loader. Supported: fabric, forge, neoforge")
            return MigrationPlan(operations, warnings, manualSteps)
        }

        // Find all version directories
        val versionsDir = File(context.projectDir, "versions")
        if (!versionsDir.exists()) {
            warnings.add("No versions directory found")
            return MigrationPlan(operations, warnings, manualSteps)
        }

        val versionDirs = if (version != null && version.isNotEmpty()) {
            listOf(version.replace(".", "_"))
        } else {
            versionsDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
        }

        versionDirs.forEach { versionDir ->
            val versionPath = File(versionsDir, versionDir)
            val loaderPath = File(versionPath, loader)

            // Check if loader already exists with full structure
            val srcMainJava = File(loaderPath, "src/main/java")
            if (loaderPath.exists() && srcMainJava.exists() && !context.force) {
                warnings.add("Loader $loader already exists in $versionDir. Use --force to overwrite")
                return@forEach
            }

            // Create loader directory structure
            operations.add(MigrationOperation.CreateDirectory(
                File(context.projectDir, "versions/$versionDir/$loader").absolutePath
            ))
            operations.add(MigrationOperation.CreateDirectory(
                File(context.projectDir, "versions/$versionDir/$loader/src/main/java").absolutePath
            ))
            operations.add(MigrationOperation.CreateDirectory(
                File(context.projectDir, "versions/$versionDir/$loader/src/main/resources").absolutePath
            ))

            // Generate loader-specific registration code
            val packagePath = context.packageName.replace(".", "/")
            val mainClassContent = generateLoaderMainClass(loader, context.modId, context.packageName)

            operations.add(
                MigrationOperation.UpdateFileContent(
                    File(context.projectDir, "versions/$versionDir/$loader/src/main/java/$packagePath/${context.modId.capitalize()}${loader.capitalize()}.java").absolutePath,
                    "",
                    mainClassContent,
                    "Generate $loader main class"
                )
            )

            // Copy common code references
            manualSteps.add("Implement loader-specific initialization in $versionDir/$loader")
            manualSteps.add("Update build configuration to include $loader")

            // Update version config
            operations.add(
                MigrationOperation.UpdateConfig(
                    File(context.projectDir, "versions/$versionDir/config.yml").absolutePath,
                    mapOf("add_loader" to loader),
                    "Add $loader to version config"
                )
            )
        }

        // Update root config to include loader
        operations.add(
            MigrationOperation.UpdateConfig(
                File(context.projectDir, "config.yml").absolutePath,
                mapOf("add_loader" to loader),
                "Add $loader to root config"
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

    private fun generateLoaderMainClass(loader: String, modId: String, packageName: String): String {
        val className = "${modId.capitalize()}${loader.capitalize()}"

        return when (loader) {
            "fabric" -> """
                package $packageName;

                import net.fabricmc.api.ModInitializer;
                import $packageName.${modId.capitalize()}Mod;

                public class $className implements ModInitializer {
                    @Override
                    public void onInitialize() {
                        ${modId.capitalize()}Mod.init();
                    }
                }
            """.trimIndent()

            "forge" -> """
                package $packageName;

                import net.minecraftforge.fml.common.Mod;
                import $packageName.${modId.capitalize()}Mod;

                @Mod("$modId")
                public class $className {
                    public $className() {
                        ${modId.capitalize()}Mod.init();
                    }
                }
            """.trimIndent()

            "neoforge" -> """
                package $packageName;

                import net.neoforged.fml.common.Mod;
                import $packageName.${modId.capitalize()}Mod;

                @Mod("$modId")
                public class $className {
                    public $className() {
                        ${modId.capitalize()}Mod.init();
                    }
                }
            """.trimIndent()

            else -> ""
        }
    }

    private fun executeOperation(operation: MigrationOperation) {
        when (operation) {
            is MigrationOperation.CreateDirectory -> {
                File(operation.path).mkdirs()
            }
            is MigrationOperation.UpdateFileContent -> {
                val file = File(operation.path)
                file.parentFile?.mkdirs()
                FileUtil.writeText(file, operation.newContent)
            }
            is MigrationOperation.UpdateConfig -> {
                val file = File(operation.configPath)
                var content = if (file.exists()) file.readText() else ""

                operation.updates.forEach { (key, value) ->
                    if (key == "add_loader") {
                        if (content.contains("loaders:")) {
                            val lines = content.lines().toMutableList()
                            val loaderLineIdx = lines.indexOfFirst { it.trim().startsWith("loaders:") }
                            if (loaderLineIdx != -1) {
                                val loaderLine = lines[loaderLineIdx]
                                if (loaderLine.contains("[")) {
                                    lines[loaderLineIdx] = loaderLine.replace("]", ", $value]")
                                } else {
                                    lines.add(loaderLineIdx + 1, "  - $value")
                                }
                                content = lines.joinToString("\n")
                            }
                        }
                    }
                }

                file.parentFile?.mkdirs()
                FileUtil.writeText(file, content)
            }
            else -> {
                // Handle other operations
            }
        }
    }

    private fun formatOperationResult(operation: MigrationOperation): String {
        return when (operation) {
            is MigrationOperation.CreateDirectory -> "Created directory: ${operation.path}"
            is MigrationOperation.UpdateFileContent -> "Created: ${File(operation.path).name}"
            is MigrationOperation.UpdateConfig -> "Updated config: ${File(operation.configPath).name}"
            else -> "Executed operation"
        }
    }

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
