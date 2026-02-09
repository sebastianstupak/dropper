package dev.dropper.commands.clean

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.cleaners.BuildCleaner
import dev.dropper.cleaners.CacheCleaner
import dev.dropper.cleaners.GeneratedCleaner
import dev.dropper.util.Logger
import java.io.File

/**
 * Clean everything
 */
class CleanAllCommand : CliktCommand(
    name = "all",
    help = "Clean everything (build, cache, generated)"
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

        Logger.info("Cleaning all...")

        var totalFiles = 0
        var totalBytes = 0L

        // Clean build
        val buildCleaner = BuildCleaner()
        val buildResult = buildCleaner.clean(projectDir, dryRun, force)
        if (buildResult != null) {
            totalFiles += buildResult.filesDeleted
            totalBytes += buildResult.bytesFreed
        }

        // Clean cache
        val cacheCleaner = CacheCleaner()
        val cacheResult = cacheCleaner.clean(projectDir, dryRun, force)
        if (cacheResult != null) {
            totalFiles += cacheResult.filesDeleted
            totalBytes += cacheResult.bytesFreed
        }

        // Clean generated
        val generatedCleaner = GeneratedCleaner()
        val generatedResult = generatedCleaner.clean(projectDir, dryRun, force)
        if (generatedResult != null) {
            totalFiles += generatedResult.filesDeleted
            totalBytes += generatedResult.bytesFreed
        }

        Logger.success("Complete cleanup finished!")
        println("  Total files deleted: $totalFiles")
        println("  Total space freed: ${totalBytes / 1024 / 1024} MB")
    }
}
