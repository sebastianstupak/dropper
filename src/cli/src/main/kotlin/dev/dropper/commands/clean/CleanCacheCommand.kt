package dev.dropper.commands.clean

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.cleaners.CacheCleaner
import dev.dropper.util.Logger
import java.io.File

/**
 * Clean Gradle and Dropper caches
 */
class CleanCacheCommand : CliktCommand(
    name = "cache",
    help = "Clean Gradle and Dropper caches"
) {
    private val dryRun by option("--dry-run", "-d", help = "Preview what would be deleted").flag()
    private val force by option("--force", "-f", help = "Skip confirmation").flag()
    private val killZombies by option("--kill-zombies", help = "Attempt to clean up zombie Gradle daemon processes").flag()

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        if (killZombies) {
            Logger.info("Checking for zombie Gradle daemon processes...")
            // Attempt to stop any running Gradle daemons
            try {
                val gradlew = if (System.getProperty("os.name").lowercase().contains("win")) {
                    File(projectDir, "gradlew.bat")
                } else {
                    File(projectDir, "gradlew")
                }
                if (gradlew.exists()) {
                    Logger.info("Stopping Gradle daemons...")
                    ProcessBuilder(gradlew.absolutePath, "--stop")
                        .directory(projectDir)
                        .start()
                        .waitFor()
                }
            } catch (e: Exception) {
                Logger.warn("Could not stop Gradle daemons: ${e.message}")
            }
            Logger.success("Zombie process cleanup complete")
        }

        val cleaner = CacheCleaner()
        val result = cleaner.clean(projectDir, dryRun, force)

        if (result != null) {
            Logger.success("Cache cleanup complete!")
            println("  Files deleted: ${result.filesDeleted}")
            println("  Space freed: ${result.bytesFreed / 1024 / 1024} MB")
        }
    }
}
