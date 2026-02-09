package dev.dropper.commands.update

import com.github.ajalt.clikt.core.CliktCommand
import dev.dropper.updaters.LoaderUpdater
import dev.dropper.util.Logger
import java.io.File

/**
 * Update all mod loaders
 */
class UpdateLoadersCommand : CliktCommand(
    name = "loaders",
    help = "Update all mod loaders to latest versions"
) {
    override fun run() {
        val projectDir = File(".").absoluteFile

        if (!File(projectDir, "config.yml").exists()) {
            Logger.error("Not a Dropper project (config.yml not found)")
            return
        }

        try {
            val updater = LoaderUpdater()
            updater.updateAllLoaders(projectDir)
        } catch (e: Exception) {
            Logger.error("Failed to update loaders: ${e.message}")
        }
    }
}
