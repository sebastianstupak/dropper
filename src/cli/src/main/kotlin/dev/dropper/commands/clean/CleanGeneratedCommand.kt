package dev.dropper.commands.clean

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.cleaners.GeneratedCleaner
import dev.dropper.util.Logger
import java.io.File

/**
 * Clean generated files
 */
class CleanGeneratedCommand : CliktCommand(
    name = "generated",
    help = "Clean generated files"
) {
    private val dryRun by option("--dry-run", "-d", help = "Preview what would be deleted").flag()
    private val force by option("--force", "-f", help = "Skip confirmation").flag()

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val cleaner = GeneratedCleaner()
        val result = cleaner.clean(projectDir, dryRun, force)

        if (result != null) {
            Logger.success("Generated files cleanup complete!")
            println("  Files deleted: ${result.filesDeleted}")
            println("  Space freed: ${result.bytesFreed / 1024 / 1024} MB")
        }
    }
}
