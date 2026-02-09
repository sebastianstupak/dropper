package dev.dropper.commands.update

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.updaters.UpdateChecker
import dev.dropper.util.Logger
import java.io.File

/**
 * Apply all available updates
 */
class UpdateApplyCommand : CliktCommand(
    name = "apply",
    help = "Apply available updates"
) {
    private val all by option(
        "--all",
        help = "Apply all available updates"
    ).flag()

    override fun run() {
        val projectDir = File(".").absoluteFile

        if (!File(projectDir, "config.yml").exists()) {
            Logger.error("Not a Dropper project (config.yml not found)")
            return
        }

        if (!all) {
            Logger.error("Please specify --all to apply all updates")
            return
        }

        try {
            val checker = UpdateChecker()
            val result = checker.checkUpdates(projectDir)

            if (result.available.isEmpty()) {
                Logger.success("No updates to apply!")
                return
            }

            Logger.info("Applying ${result.available.size} update(s)...")

            result.available.forEach { update ->
                Logger.info("  ${update.name}: ${update.currentVersion} -> ${update.latestVersion}")
            }

            checker.applyUpdates(projectDir, result.available)

            Logger.success("All updates applied successfully!")
            Logger.info("Please test your project after updating")
        } catch (e: Exception) {
            Logger.error("Failed to apply updates: ${e.message}")
        }
    }
}
