package dev.dropper.cleaners

import dev.dropper.util.Logger
import java.io.File

/**
 * Clean Gradle and Dropper caches
 */
class CacheCleaner : Cleaner() {

    override fun clean(projectDir: File, dryRun: Boolean, force: Boolean): CleanReport? {
        val cacheDirs = listOf(
            File(projectDir, ".gradle/caches"),
            File(projectDir, ".gradle/daemon"),
            File(projectDir, ".gradle/wrapper/dists")
        )

        val existingDirs = cacheDirs.filter { it.exists() }

        if (existingDirs.isEmpty()) {
            Logger.info("No caches to clean")
            return CleanReport(0, 0)
        }

        // Calculate what will be deleted
        var totalFiles = 0
        var totalSize = 0L

        existingDirs.forEach { dir ->
            totalFiles += countFiles(dir)
            totalSize += calculateSize(dir)
        }

        if (dryRun) {
            Logger.info("Dry run - would delete:")
            existingDirs.forEach { dir ->
                val size = calculateSize(dir)
                println("  ${dir.absolutePath} (${size / 1024 / 1024} MB)")
            }
            return CleanReport(totalFiles, totalSize)
        }

        if (!confirmDeletion("Delete ${existingDirs.size} cache directories (${totalSize / 1024 / 1024} MB)?", force)) {
            Logger.info("Cleanup cancelled")
            return null
        }

        // Delete directories
        existingDirs.forEach { dir ->
            Logger.info("Deleting ${dir.name}...")
            dir.deleteRecursively()
        }

        return CleanReport(totalFiles, totalSize)
    }
}
