package dev.dropper.commands.dev

import com.github.ajalt.clikt.core.CliktCommand
import dev.dropper.util.Logger

/**
 * Hot reload changes (basic implementation)
 */
class DevReloadCommand : CliktCommand(
    name = "reload",
    help = "Hot reload mod changes (currently shows workaround instructions)"
) {
    override fun run() {
        Logger.info("Hot reload is not yet fully implemented.")
        println()
        Logger.info("To see your changes, restart the game with: dropper dev run")
        println()
        Logger.info("Tip: For faster iteration:")
        println("  1. Keep Minecraft running")
        println("  2. Make code changes")
        println("  3. Rebuild: ./gradlew :<version>-<loader>:build")
        println("  4. Use in-game /reload command (for data/resource changes only)")
        println()
        Logger.warn("Note: The /reload command only reloads datapacks and resource packs.")
        Logger.warn("Code changes require a full restart of the game.")
        println()
        Logger.info("Future versions of Dropper will support hot code replacement via JVM debug interface.")
    }
}
