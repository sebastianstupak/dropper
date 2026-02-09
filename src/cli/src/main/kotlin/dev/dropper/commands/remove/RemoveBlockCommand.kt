package dev.dropper.commands.remove

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.DropperCommand
import dev.dropper.removers.BlockRemover
import dev.dropper.removers.RemovalOptions
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to remove a block from the mod
 */
class RemoveBlockCommand : DropperCommand(
    name = "block",
    help = "Remove a block and all associated files"
) {
    private val name by argument(help = "Block name in snake_case (e.g., ruby_ore)")
    private val dryRun by option("--dry-run", help = "Preview what would be deleted").flag()
    private val force by option("--force", "-f", help = "Skip confirmation and ignore dependencies").flag()
    private val keepAssets by option("--keep-assets", help = "Remove code but keep textures/models").flag()
    private val version by option("--version", help = "Remove from specific version only")

    override fun run() {
        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val modId = extractModId(configFile)
        if (modId == null) {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        Logger.info("Removing block: $name")

        val options = RemovalOptions(
            dryRun = dryRun,
            force = force,
            keepAssets = keepAssets,
            version = version,
            createBackup = true,
            interactive = !force
        )

        // Show confirmation if not forced
        if (!force && !dryRun) {
            Logger.warn("This will remove block '$name' and all associated files")
            print("Continue? (y/n): ")
            val response = readlnOrNull()?.lowercase()
            if (response != "y" && response != "yes") {
                Logger.info("Cancelled")
                return
            }
        }

        val remover = BlockRemover()
        val result = remover.remove(projectDir, name, modId, options)

        if (result.success) {
            if (dryRun) {
                Logger.success("DRY RUN: Would remove ${result.filesRemoved.size} file(s)")
            } else {
                Logger.success("Successfully removed block '$name'")
                Logger.info("Files removed: ${result.filesRemoved.size}")
                if (result.directoriesRemoved.isNotEmpty()) {
                    Logger.info("Empty directories cleaned: ${result.directoriesRemoved.size}")
                }
            }
        } else {
            result.errors.forEach { error ->
                Logger.error(error)
            }
        }

        result.warnings.forEach { warning ->
            Logger.warn(warning)
        }
    }

    private fun extractModId(configFile: File): String? {
        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
    }
}
