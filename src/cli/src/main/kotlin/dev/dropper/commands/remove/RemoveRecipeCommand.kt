package dev.dropper.commands.remove

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.DropperCommand
import dev.dropper.removers.RecipeRemover
import dev.dropper.removers.RemovalOptions
import dev.dropper.util.Logger

class RemoveRecipeCommand : DropperCommand(
    name = "recipe",
    help = "Remove a recipe"
) {
    private val name by argument(help = "Recipe name in snake_case")
    private val dryRun by option("--dry-run", help = "Preview what would be deleted").flag()
    private val force by option("--force", "-f", help = "Skip confirmation").flag()

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

        val options = RemovalOptions(dryRun = dryRun, force = force)

        if (!force && !dryRun) {
            Logger.warn("This will remove recipe '$name'")
            print("Continue? (y/n): ")
            if (readlnOrNull()?.lowercase() !in listOf("y", "yes")) {
                Logger.info("Cancelled")
                return
            }
        }

        val result = RecipeRemover().remove(projectDir, name, modId, options)

        if (result.success) {
            Logger.success(if (dryRun) "DRY RUN: Would remove ${result.filesRemoved.size} file(s)"
                          else "Successfully removed recipe '$name'")
        } else {
            result.errors.forEach { Logger.error(it) }
        }
    }

}
