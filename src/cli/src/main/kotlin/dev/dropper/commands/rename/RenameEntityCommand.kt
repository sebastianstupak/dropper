package dev.dropper.commands.rename

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.Logger

/**
 * Command to rename an entity and update all references
 */
class RenameEntityCommand : CliktCommand(
    name = "entity",
    help = "Rename an entity and update all references"
) {
    private val oldName by argument(help = "Current entity name in snake_case")
    private val newName by argument(help = "New entity name in snake_case")
    private val dryRun by option("--dry-run", help = "Preview changes without applying").flag()
    private val force by option("--force", "-f", help = "Skip confirmation prompt").flag()
    private val version by option("--version", "-v", help = "Rename in specific version only")

    override fun run() {
        Logger.info("Renaming entity: $oldName -> $newName")
        Logger.warn("Entity renaming not yet implemented")
        // TODO: Implement entity renaming following the same pattern as item/block
    }
}
