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
    private val freshClone by option("--fresh-clone", help = "Clean as if for a fresh clone (remove all build artifacts but keep source)").flag()
    private val backup by option("--backup", "-b", help = "Create backup before cleaning").flag()

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

        // Create backups if requested
        if (backup) {
            val buildDir = File(projectDir, "build")
            val gradleDir = File(projectDir, ".gradle")
            val buildCleaner = BuildCleaner()
            if (buildDir.exists()) {
                buildCleaner.backupDirectory(projectDir, buildDir)
            }
            if (gradleDir.exists()) {
                buildCleaner.backupDirectory(projectDir, gradleDir)
            }
        }

        if (dryRun) {
            Logger.info("Dry run - would clean:")

            val buildDir = File(projectDir, "build")
            if (buildDir.exists()) {
                val buildCleaner = BuildCleaner()
                val size = buildCleaner.dirSize(buildDir)
                val count = buildCleaner.dirFileCount(buildDir)
                println("  Build: $count files (${size / 1024 / 1024} MB)")
                totalFiles += count
                totalBytes += size
            }

            val gradleDir = File(projectDir, ".gradle")
            if (gradleDir.exists()) {
                val cacheCleaner = CacheCleaner()
                val size = cacheCleaner.dirSize(gradleDir)
                val count = cacheCleaner.dirFileCount(gradleDir)
                println("  Cache: $count files (${size / 1024 / 1024} MB)")
                totalFiles += count
                totalBytes += size
            }

            println("  Total: $totalFiles files (${totalBytes / 1024 / 1024} MB)")
            return
        }

        // Clean build
        val buildCleaner = BuildCleaner()
        val buildResult = buildCleaner.clean(projectDir, dryRun, force = true)
        if (buildResult != null) {
            totalFiles += buildResult.filesDeleted
            totalBytes += buildResult.bytesFreed
        }

        // Clean cache
        val cacheCleaner = CacheCleaner()
        val cacheResult = cacheCleaner.clean(projectDir, dryRun, force = true)
        if (cacheResult != null) {
            totalFiles += cacheResult.filesDeleted
            totalBytes += cacheResult.bytesFreed
        }

        // Clean generated
        val generatedCleaner = GeneratedCleaner()
        val generatedResult = generatedCleaner.clean(projectDir, dryRun, force = true)
        if (generatedResult != null) {
            totalFiles += generatedResult.filesDeleted
            totalBytes += generatedResult.bytesFreed
        }

        // For fresh-clone, also clean additional artifacts
        if (freshClone) {
            Logger.info("Fresh clone cleanup - removing additional artifacts...")
            val additionalDirs = listOf(".idea", ".eclipse", "out", "bin", ".settings")
            for (dirName in additionalDirs) {
                val dir = File(projectDir, dirName)
                if (dir.exists()) {
                    val count = dir.walkTopDown().filter { it.isFile }.count()
                    val size = dir.walkTopDown().filter { it.isFile }.map { it.length() }.sum()
                    dir.deleteRecursively()
                    totalFiles += count
                    totalBytes += size
                }
            }
        }

        Logger.success("Complete cleanup finished!")
        println("  Total files deleted: $totalFiles")
        println("  Total space freed: ${totalBytes / 1024 / 1024} MB")
    }
}
