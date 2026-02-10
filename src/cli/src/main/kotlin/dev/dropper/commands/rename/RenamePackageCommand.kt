package dev.dropper.commands.rename

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.DropperCommand
import dev.dropper.renamers.*
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to rename package name across all files
 */
class RenamePackageCommand : DropperCommand(
    name = "package",
    help = "Refactor package name across all files"
) {
    private val oldPackage by argument(help = "Current package name (e.g., com.oldmod)")
    private val newPackage by argument(help = "New package name (e.g., com.newmod)")
    private val dryRun by option("--dry-run", help = "Preview changes without applying").flag()
    private val force by option("--force", "-f", help = "Skip confirmation prompt").flag()

    override fun run() {
        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        // Read mod ID from config
        val modId = extractModId(configFile)
        if (modId == null) {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        Logger.info("Renaming package: $oldPackage -> $newPackage")

        // Create rename context
        // For package renaming, oldName/newName hold the package names
        val context = RenameContext(
            projectDir = projectDir,
            modId = modId,
            packageName = oldPackage,
            oldName = oldPackage,
            newName = newPackage,
            componentType = ComponentType.PACKAGE,
            dryRun = dryRun,
            force = force
        )

        // Create renamer and executor
        val renamer = PackageRenamer()
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
            Logger.error("No files found for package '$oldPackage'")
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
                Logger.success("Successfully renamed package: $oldPackage -> $newPackage")
                Logger.info("  - ${discoveredFiles.size} files updated")
                Logger.info("  - ${operations.size} operations performed")
            }
        } else {
            Logger.error("Rename failed: ${result.message}")
            result.errors.forEach { Logger.error("  - $it") }
        }
    }

}
