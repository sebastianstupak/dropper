package dev.dropper.commands.remove

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.DropperCommand
import dev.dropper.removers.EntityRemover
import dev.dropper.removers.RemovalOptions
import dev.dropper.util.Logger

class RemoveEntityCommand : DropperCommand(
    name = "entity",
    help = "Remove an entity and all associated files"
) {
    private val name by argument(help = "Entity name in snake_case")
    private val dryRun by option("--dry-run", help = "Preview what would be deleted").flag()
    private val force by option("--force", "-f", help = "Skip confirmation and ignore dependencies").flag()
    private val keepAssets by option("--keep-assets", help = "Remove code but keep textures/models").flag()

    override fun run() {
        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val modId = extractModId(configFile) ?: run {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        val options = RemovalOptions(dryRun = dryRun, force = force, keepAssets = keepAssets)

        if (!force && !dryRun) {
            Logger.warn("This will remove entity '$name' and all associated files")
            print("Continue? (y/n): ")
            if (readlnOrNull()?.lowercase() !in listOf("y", "yes")) {
                Logger.info("Cancelled")
                return
            }
        }

        val result = EntityRemover().remove(projectDir, name, modId, options)

        if (result.success) {
            Logger.success(if (dryRun) "DRY RUN: Would remove ${result.filesRemoved.size} file(s)"
                          else "Successfully removed entity '$name'")
        } else {
            result.errors.forEach { Logger.error(it) }
        }
        result.warnings.forEach { Logger.warn(it) }
    }

}
