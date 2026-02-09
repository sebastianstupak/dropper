package dev.dropper.commands.rename

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.Logger

/**
 * Command to rename the entire mod (change mod ID)
 */
class RenameModCommand : CliktCommand(
    name = "mod",
    help = "Rename the entire mod (change mod ID and update all references)"
) {
    private val oldId by argument(help = "Current mod ID")
    private val newId by argument(help = "New mod ID")
    private val dryRun by option("--dry-run", help = "Preview changes without applying").flag()
    private val force by option("--force", "-f", help = "Skip confirmation prompt").flag()

    override fun run() {
        Logger.info("Renaming mod: $oldId -> $newId")
        Logger.warn("Mod renaming not yet implemented")
        // TODO: Implement mod renaming
        // This should:
        // - Update config.yml
        // - Rename all package directories
        // - Update all resource locations
        // - Update all namespaces
    }
}
