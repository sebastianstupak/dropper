package dev.dropper.commands.update

import com.github.ajalt.clikt.core.CliktCommand
import dev.dropper.updaters.MinecraftUpdater
import dev.dropper.util.Logger
import java.io.File

/**
 * Update Minecraft version
 */
class UpdateMinecraftCommand : CliktCommand(
    name = "minecraft",
    help = "Update Minecraft to latest version"
) {
    override fun run() {
        val projectDir = File(".").absoluteFile

        if (!File(projectDir, "config.yml").exists()) {
            Logger.error("Not a Dropper project (config.yml not found)")
            return
        }

        try {
            val updater = MinecraftUpdater()
            updater.updateToLatest(projectDir)
        } catch (e: Exception) {
            Logger.error("Failed to update Minecraft: ${e.message}")
        }
    }
}
