package dev.dropper.commands.rename

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.renamers.*
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to rename a block and update all references
 */
class RenameBlockCommand : CliktCommand(
    name = "block",
    help = "Rename a block and update all references"
) {
    private val oldName by argument(help = "Current block name in snake_case")
    private val newName by argument(help = "New block name in snake_case")
    private val dryRun by option("--dry-run", help = "Preview changes without applying").flag()
    private val force by option("--force", "-f", help = "Skip confirmation prompt").flag()
    private val version by option("--version", "-v", help = "Rename in specific version only")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val modId = extractModId(configFile)
        val packageName = extractPackageName(configFile)

        if (modId == null || packageName == null) {
            Logger.error("Could not read config.yml")
            return
        }

        val context = RenameContext(
            projectDir = projectDir,
            modId = modId,
            packageName = packageName,
            oldName = oldName,
            newName = newName,
            componentType = ComponentType.BLOCK,
            version = version,
            dryRun = dryRun,
            force = force
        )

        Logger.info("Renaming block: $oldName -> $newName")

        val renamer = BlockRenamer()
        val executor = RenameExecutor()

        // Check conflicts
        val conflicts = renamer.checkConflicts(context)
        if (conflicts.isNotEmpty()) {
            Logger.error("Conflicts detected:")
            conflicts.forEach { Logger.error("  - $it") }
            return
        }

        // Discover and plan
        val discoveredFiles = renamer.discover(context)
        if (discoveredFiles.isEmpty()) {
            Logger.error("Block '$oldName' not found")
            return
        }

        Logger.info("Found ${discoveredFiles.size} files to rename")

        val references = renamer.findReferences(context, discoveredFiles)
        Logger.info("Found references in ${references.size} files")

        val operations = renamer.planRename(context)
        Logger.info("Planned ${operations.size} operations")

        // Confirm
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
                Logger.success("Successfully renamed block: $oldName -> $newName")
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
