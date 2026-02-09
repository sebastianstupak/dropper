package dev.dropper.cleaners

import dev.dropper.util.Logger
import java.io.File

/**
 * Clean build artifacts
 */
class BuildCleaner : Cleaner() {

    override fun clean(projectDir: File, dryRun: Boolean, force: Boolean): CleanReport? {
        val buildDirs = listOf(
            File(projectDir, "build"),
            File(projectDir, "build-temp"),
            File(projectDir, ".gradle/build-cache")
        )

        val existingDirs = buildDirs.filter { it.exists() }

        if (existingDirs.isEmpty()) {
            Logger.info("No build artifacts to clean")
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

        if (!confirmDeletion("Delete ${existingDirs.size} build directories (${totalSize / 1024 / 1024} MB)?", force)) {
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
