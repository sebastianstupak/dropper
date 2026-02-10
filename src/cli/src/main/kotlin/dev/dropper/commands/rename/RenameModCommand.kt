package dev.dropper.commands.rename

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.DropperCommand
import dev.dropper.renamers.*
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to rename the entire mod (change mod ID)
 */
class RenameModCommand : DropperCommand(
    name = "mod",
    help = "Rename the entire mod (change mod ID and update all references)"
) {
    private val oldId by argument(help = "Current mod ID")
    private val newId by argument(help = "New mod ID")
    private val dryRun by option("--dry-run", help = "Preview changes without applying").flag()
    private val force by option("--force", "-f", help = "Skip confirmation prompt").flag()

    override fun run() {
        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        // Verify the config contains the old mod ID
        val currentModId = extractModId(configFile)
        if (currentModId == null) {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        Logger.info("Renaming mod: $oldId -> $newId")

        // Create rename context
        val context = RenameContext(
            projectDir = projectDir,
            modId = oldId,
            packageName = "com.${sanitize(oldId)}",
            oldName = oldId,
            newName = newId,
            componentType = ComponentType.MOD,
            dryRun = dryRun,
            force = force
        )

        // Create renamer and executor
        val renamer = ModRenamer()
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
            Logger.error("No files found for mod ID '$oldId'")
            return
        }

        Logger.info("Found ${discoveredFiles.size} files to update")

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
            echo("  - Update ${discoveredFiles.size} files")
            echo("  - Modify references in ${references.size} files")
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
                Logger.success("Successfully renamed mod: $oldId -> $newId")
                Logger.info("  - ${discoveredFiles.size} files updated")
                Logger.info("  - ${operations.size} operations performed")
            }
        } else {
            Logger.error("Rename failed: ${result.message}")
            result.errors.forEach { Logger.error("  - $it") }
        }
    }

    /**
     * Sanitize mod ID for package names (remove hyphens and underscores).
     */
    private fun sanitize(modId: String): String {
        return modId.replace("-", "").replace("_", "")
    }
}
