package dev.dropper.commands.rename

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.DropperCommand
import dev.dropper.renamers.*
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to rename an item and update all references
 */
class RenameItemCommand : DropperCommand(
    name = "item",
    help = "Rename an item and update all references"
) {
    private val oldName by argument(help = "Current item name in snake_case")
    private val newName by argument(help = "New item name in snake_case")
    private val dryRun by option("--dry-run", help = "Preview changes without applying").flag()
    private val force by option("--force", "-f", help = "Skip confirmation prompt").flag()
    private val version by option("--version", "-v", help = "Rename in specific version only")

    override fun run() {
        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        // Read config
        val modId = extractModId(configFile)
        val packageName = extractPackageName(configFile)

        if (modId == null || packageName == null) {
            Logger.error("Could not read config.yml")
            return
        }

        // Create rename context
        val context = RenameContext(
            projectDir = projectDir,
            modId = modId,
            packageName = packageName,
            oldName = oldName,
            newName = newName,
            componentType = ComponentType.ITEM,
            version = version,
            dryRun = dryRun,
            force = force
        )

        Logger.info("Renaming item: $oldName -> $newName")

        // Create renamer and executor
        val renamer = ItemRenamer()
        val executor = RenameExecutor()

        // Check conflicts
        val conflicts = renamer.checkConflicts(context)
        if (conflicts.isNotEmpty()) {
            Logger.error("Conflicts detected:")
            conflicts.forEach { Logger.error("  - $it") }
            return
        }

        // Discover files
        val discoveredFiles = renamer.discover(context)
        if (discoveredFiles.isEmpty()) {
            Logger.error("Item '$oldName' not found")
            return
        }

        Logger.info("Found ${discoveredFiles.size} files to rename")

        // Find references
        val references = renamer.findReferences(context, discoveredFiles)
        Logger.info("Found references in ${references.size} files")

        // Plan rename
        val operations = renamer.planRename(context)
        Logger.info("Planned ${operations.size} operations")

        // Confirm if not force
        if (!force && !dryRun) {
            echo("")
            echo("This will:")
            echo("  - Rename ${discoveredFiles.size} files")
            echo("  - Update references in ${references.size} files")
            echo("  - Perform ${operations.size} total operations")
            echo("")
            echo("Continue? (y/N) ", trailingNewline = false)
            val input = readLine()
            if (input?.lowercase() != "y") {
                Logger.info("Cancelled")
                return
            }
        }

        // Execute
        val result = executor.execute(operations, dryRun)

        if (result.success) {
            if (dryRun) {
                Logger.success("Dry run completed. No changes made.")
            } else {
                Logger.success("Successfully renamed item: $oldName -> $newName")
                Logger.info("  - ${discoveredFiles.size} files renamed")
                Logger.info("  - ${references.size} files updated")
            }
        } else {
            Logger.error("Rename failed: ${result.message}")
            result.errors.forEach { Logger.error("  - $it") }
        }
    }

    private fun extractModId(configFile: File): String? {
        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
    }

    private fun extractPackageName(configFile: File): String? {
        val modId = extractModId(configFile)
        return modId?.let { "com.$it" }
    }
}
