package dev.dropper.commands.remove

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.removers.TagRemover
import dev.dropper.removers.RemovalOptions
import dev.dropper.util.Logger
import java.io.File

class RemoveTagCommand : CliktCommand(
    name = "tag",
    help = "Remove a tag"
) {
    private val name by argument(help = "Tag name in snake_case")
    private val dryRun by option("--dry-run", help = "Preview what would be deleted").flag()
    private val force by option("--force", "-f", help = "Skip confirmation").flag()

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val modId = extractModId(configFile) ?: run {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        val options = RemovalOptions(dryRun = dryRun, force = force)

        if (!force && !dryRun) {
            Logger.warn("This will remove tag '$name'")
            print("Continue? (y/n): ")
            if (readlnOrNull()?.lowercase() !in listOf("y", "yes")) {
                Logger.info("Cancelled")
                return
            }
        }

        val result = TagRemover().remove(projectDir, name, modId, options)

        if (result.success) {
            Logger.success(if (dryRun) "DRY RUN: Would remove ${result.filesRemoved.size} file(s)"
                          else "Successfully removed tag '$name'")
        } else {
            result.errors.forEach { Logger.error(it) }
        }
    }

    private fun extractModId(configFile: File): String? =
        Regex("id:\\s*([a-z0-9-]+)").find(configFile.readText())?.groupValues?.get(1)
}
