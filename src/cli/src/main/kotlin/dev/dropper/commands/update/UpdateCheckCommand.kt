package dev.dropper.commands.update

import com.github.ajalt.clikt.core.CliktCommand
import dev.dropper.updaters.UpdateChecker
import dev.dropper.util.Logger
import java.io.File

/**
 * Check for available updates
 */
class UpdateCheckCommand : CliktCommand(
    name = "check",
    help = "Check for available updates"
) {
    override fun run() {
        val projectDir = File(".").absoluteFile

        if (!File(projectDir, "config.yml").exists()) {
            Logger.error("Not a Dropper project (config.yml not found)")
            return
        }

        try {
            val checker = UpdateChecker()
            val result = checker.checkUpdates(projectDir)

            if (result.available.isEmpty()) {
                Logger.success("No updates available - all dependencies are up to date!")
                return
            }

            Logger.info("Found ${result.available.size} update(s) available:")
            echo("")

            result.available.forEach { update ->
                val icon = if (update.breaking) "⚠️" else "✓"
                echo("  $icon ${update.name}")
                echo("     Current: ${update.currentVersion}")
                echo("     Latest:  ${update.latestVersion}")
                if (update.description.isNotEmpty()) {
                    echo("     ${update.description}")
                }
                if (update.breaking) {
                    echo("     ⚠️ Warning: This is a breaking change")
                }
                echo("")
            }

            echo("Run 'dropper update apply --all' to apply all updates")
        } catch (e: Exception) {
            Logger.error("Failed to check updates: ${e.message}")
        }
    }
}
