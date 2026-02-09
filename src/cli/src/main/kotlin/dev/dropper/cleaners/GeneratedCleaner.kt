package dev.dropper.cleaners

import dev.dropper.util.Logger
import java.io.File

/**
 * Clean generated metadata files
 */
class GeneratedCleaner : Cleaner() {

    override fun clean(projectDir: File, dryRun: Boolean, force: Boolean): CleanReport? {
        val generatedDirs = listOf(
            File(projectDir, "build/generated"),
            File(projectDir, "build/tmp")
        )

        val existingDirs = generatedDirs.filter { it.exists() }

        if (existingDirs.isEmpty()) {
            Logger.info("No generated files to clean")
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

        if (!confirmDeletion("Delete ${existingDirs.size} generated directories (${totalSize / 1024 / 1024} MB)?", force)) {
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
