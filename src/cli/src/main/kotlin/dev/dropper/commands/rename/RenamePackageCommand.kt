package dev.dropper.commands.rename

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.Logger

/**
 * Command to rename package name
 */
class RenamePackageCommand : CliktCommand(
    name = "package",
    help = "Refactor package name across all files"
) {
    private val oldPackage by argument(help = "Current package name (e.g., com.oldmod)")
    private val newPackage by argument(help = "New package name (e.g., com.newmod)")
    private val dryRun by option("--dry-run", help = "Preview changes without applying").flag()
    private val force by option("--force", "-f", help = "Skip confirmation prompt").flag()

    override fun run() {
        Logger.info("Renaming package: $oldPackage -> $newPackage")
        Logger.warn("Package renaming not yet implemented")
        // TODO: Implement package renaming
        // This should:
        // - Move directory structure
        // - Update package declarations
        // - Update all imports
    }
}
