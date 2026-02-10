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
    private val temp by option("--temp", help = "Clean temporary files (*.tmp, *.temp)").flag()
    private val logs by option("--logs", help = "Clean log files (*.log)").flag()
    private val ide by option("--ide", help = "Clean IDE metadata (.idea, .eclipse, *.iml)").flag()
    private val osSpecific by option("--os-specific", help = "Clean OS-specific files (.DS_Store, Thumbs.db)").flag()

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        var totalFiles = 0
        var totalBytes = 0L

        // Handle specific file type cleaning options
        if (temp) {
            Logger.info("Cleaning temporary files...")
            val result = cleanFilesByExtensions(projectDir, listOf("*.tmp", "*.temp"))
            totalFiles += result.first
            totalBytes += result.second
        }

        if (logs) {
            Logger.info("Cleaning log files...")
            val result = cleanFilesByExtensions(projectDir, listOf("*.log"))
            totalFiles += result.first
            totalBytes += result.second
        }

        if (ide) {
            Logger.info("Cleaning IDE metadata...")
            val ideDirs = listOf(".idea", ".eclipse", ".settings", ".vscode")
            val ideFiles = listOf("*.iml", "*.ipr", "*.iws")

            for (dirName in ideDirs) {
                val dir = File(projectDir, dirName)
                if (dir.exists()) {
                    val count = dir.walkTopDown().filter { it.isFile }.count()
                    val size = dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                    if (!dryRun) {
                        dir.deleteRecursively()
                    }
                    totalFiles += count
                    totalBytes += size
                }
            }

            for (pattern in ideFiles) {
                val ext = pattern.removePrefix("*")
                projectDir.walkTopDown()
                    .filter { it.isFile && it.name.endsWith(ext) }
                    .forEach { file ->
                        totalFiles++
                        totalBytes += file.length()
                        if (!dryRun) {
                            file.delete()
                        }
                    }
            }
        }

        if (osSpecific) {
            Logger.info("Cleaning OS-specific files...")
            val osFiles = listOf(".DS_Store", "Thumbs.db", "Desktop.ini", "._*")

            projectDir.walkTopDown()
                .filter { it.isFile && (it.name == ".DS_Store" || it.name == "Thumbs.db" || it.name == "Desktop.ini" || it.name.startsWith("._")) }
                .forEach { file ->
                    totalFiles++
                    totalBytes += file.length()
                    if (!dryRun) {
                        file.delete()
                    }
                }
        }

        // If no specific option was given, do the default generated files cleanup
        if (!temp && !logs && !ide && !osSpecific) {
            val cleaner = GeneratedCleaner()
            val result = cleaner.clean(projectDir, dryRun, force)

            if (result != null) {
                Logger.success("Generated files cleanup complete!")
                println("  Files deleted: ${result.filesDeleted}")
                println("  Space freed: ${result.bytesFreed / 1024 / 1024} MB")
            }
            return
        }

        if (dryRun) {
            Logger.info("Dry run - would delete $totalFiles files (${totalBytes / 1024 / 1024} MB)")
        } else {
            Logger.success("Cleanup complete!")
            println("  Files deleted: $totalFiles")
            println("  Space freed: ${totalBytes / 1024 / 1024} MB")
        }
    }

    private fun cleanFilesByExtensions(projectDir: File, patterns: List<String>): Pair<Int, Long> {
        var count = 0
        var bytes = 0L

        for (pattern in patterns) {
            val ext = pattern.removePrefix("*")
            projectDir.walkTopDown()
                .filter { it.isFile && it.name.endsWith(ext) }
                .forEach { file ->
                    count++
                    bytes += file.length()
                    if (!dryRun) {
                        file.delete()
                    }
                }
        }

        return Pair(count, bytes)
    }
}
