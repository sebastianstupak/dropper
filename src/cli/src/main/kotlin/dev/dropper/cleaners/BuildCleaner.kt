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

    /**
     * Clean with filtering options (preserve, exclude, only, min-size, version, loader)
     */
    fun cleanFiltered(
        projectDir: File,
        dryRun: Boolean,
        force: Boolean,
        preserve: String? = null,
        exclude: String? = null,
        only: String? = null,
        minSizeBytes: Long? = null,
        version: String? = null,
        loader: String? = null
    ): CleanReport? {
        val buildDir = File(projectDir, "build")

        if (!buildDir.exists()) {
            Logger.info("No build artifacts to clean")
            return CleanReport(0, 0)
        }

        if (!confirmDeletion("Clean build artifacts?", force)) {
            Logger.info("Cleanup cancelled")
            return null
        }

        // If --only is specified, only delete files matching the pattern
        if (only != null) {
            return deleteMatchingFiles(buildDir, only)
        }

        // Use filtered deletion with preserve/exclude/min-size
        return deleteDirectoryFiltered(
            dir = buildDir,
            preserve = preserve,
            exclude = exclude,
            minSizeBytes = minSizeBytes
        )
    }

    /**
     * Create a backup of the given directory.
     */
    fun backupDirectory(projectDir: File, targetDir: File) {
        createBackup(projectDir, targetDir)
    }

    /**
     * Get the total size of a directory.
     */
    fun dirSize(directory: File): Long {
        return calculateSize(directory)
    }

    /**
     * Get the total file count of a directory.
     */
    fun dirFileCount(directory: File): Int {
        return countFiles(directory)
    }

    /**
     * Parse a human-readable size string to bytes.
     */
    fun parseSizeString(sizeStr: String): Long {
        return parseSize(sizeStr)
    }
}
