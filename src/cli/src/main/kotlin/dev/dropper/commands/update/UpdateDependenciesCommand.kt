package dev.dropper.commands.update

import com.github.ajalt.clikt.core.CliktCommand
import dev.dropper.updaters.DependencyUpdater
import dev.dropper.util.Logger
import java.io.File

/**
 * Update all dependencies
 */
class UpdateDependenciesCommand : CliktCommand(
    name = "dependencies",
    help = "Update all dependencies to latest versions"
) {
    override fun run() {
        val projectDir = File(".").absoluteFile

        if (!File(projectDir, "config.yml").exists()) {
            Logger.error("Not a Dropper project (config.yml not found)")
            return
        }

        try {
            val updater = DependencyUpdater()
            updater.updateAllDependencies(projectDir)
        } catch (e: Exception) {
            Logger.error("Failed to update dependencies: ${e.message}")
        }
    }
}
