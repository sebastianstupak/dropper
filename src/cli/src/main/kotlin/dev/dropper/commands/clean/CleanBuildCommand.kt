package dev.dropper.commands.clean

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.cleaners.BuildCleaner
import dev.dropper.util.Logger
import java.io.File

/**
 * Clean build artifacts
 */
class CleanBuildCommand : CliktCommand(
    name = "build",
    help = "Clean build artifacts"
) {
    private val dryRun by option("--dry-run", "-d", help = "Preview what would be deleted").flag()
    private val force by option("--force", "-f", help = "Skip confirmation").flag()
    private val backup by option("--backup", "-b", help = "Create backup before cleaning").flag()
    private val only by option("--only", help = "Only clean files matching this pattern")
    private val preserve by option("--preserve", help = "Preserve files matching this pattern")
    private val exclude by option("--exclude", help = "Exclude files matching this pattern")
    private val verify by option("--verify", help = "Verify cleanup after deletion").flag()
    private val minSize by option("--min-size", help = "Only clean files larger than this size (e.g. 5M, 100K)")
    private val version by option("--version", "-v", help = "Clean build artifacts for a specific Minecraft version")
    private val loader by option("--loader", "-l", help = "Clean build artifacts for a specific mod loader")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val buildDir = File(projectDir, "build")

        // Create backup if requested
        if (backup && buildDir.exists()) {
            val cleaner = BuildCleaner()
            cleaner.backupDirectory(projectDir, buildDir)
        }

        if (dryRun) {
            Logger.info("Dry run - would clean build artifacts:")
            if (buildDir.exists()) {
                val cleaner = BuildCleaner()
                val size = cleaner.dirSize(buildDir)
                val count = cleaner.dirFileCount(buildDir)
                println("  ${buildDir.absolutePath} ($count files, ${size / 1024 / 1024} MB)")
            } else {
                Logger.info("No build artifacts to clean")
            }
            return
        }

        val cleaner = BuildCleaner()
        val minSizeBytes = if (minSize != null) cleaner.parseSizeString(minSize!!) else null

        val result = cleaner.cleanFiltered(
            projectDir = projectDir,
            dryRun = false,
            force = force,
            preserve = preserve,
            exclude = exclude,
            only = only,
            minSizeBytes = minSizeBytes,
            version = version,
            loader = loader
        )

        if (result != null) {
            if (verify) {
                val remaining = if (buildDir.exists()) buildDir.walkTopDown().filter { it.isFile }.count() else 0
                Logger.info("Verification: $remaining files remaining in build directory")
            }
            Logger.success("Build cleanup complete!")
            println("  Files deleted: ${result.filesDeleted}")
            println("  Space freed: ${result.bytesFreed / 1024 / 1024} MB")
        }
    }
}
