package dev.dropper.cleaners

import dev.dropper.util.Logger
import java.io.File

/**
 * Base class for cleaners
 */
abstract class Cleaner {

    /**
     * Clean operation with basic options
     */
    abstract fun clean(projectDir: File, dryRun: Boolean, force: Boolean): CleanReport?

    /**
     * Calculate directory size
     */
    protected fun calculateSize(directory: File): Long {
        var size = 0L
        directory.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    /**
     * Count files in directory
     */
    protected fun countFiles(directory: File): Int {
        var count = 0
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                count += countFiles(file)
            } else {
                count++
            }
        }
        return count
    }

    /**
     * Confirm deletion
     */
    protected fun confirmDeletion(message: String, force: Boolean): Boolean {
        if (force) return true

        // In non-interactive mode (no stdin), auto-confirm
        if (System.console() == null) return true

        print("$message (y/N): ")
        val response = readLine()?.lowercase()
        return response == "y" || response == "yes"
    }

    /**
     * Create a backup of the given directory into .dropper/backups/
     */
    protected fun createBackup(projectDir: File, targetDir: File) {
        if (!targetDir.exists()) return

        val backupRoot = File(projectDir, ".dropper/backups")
        backupRoot.mkdirs()

        val backupDir = File(backupRoot, "${targetDir.name}-${System.currentTimeMillis()}")
        Logger.info("Creating backup: ${backupDir.absolutePath}")
        targetDir.copyRecursively(backupDir, overwrite = true)
    }

    /**
     * Delete a directory while respecting preserve and exclude patterns.
     * Files matching preserve or exclude globs are kept.
     */
    protected fun deleteDirectoryFiltered(
        dir: File,
        preserve: String? = null,
        exclude: String? = null,
        minSizeBytes: Long? = null
    ): CleanReport {
        if (!dir.exists()) return CleanReport(0, 0)

        var filesDeleted = 0
        var bytesFreed = 0L

        dir.walkBottomUp().forEach { file ->
            if (file == dir) return@forEach

            // Check preserve pattern
            if (preserve != null && matchesGlob(file, dir, preserve)) {
                return@forEach
            }

            // Check exclude pattern
            if (exclude != null && matchesGlob(file, dir, exclude)) {
                return@forEach
            }

            if (file.isFile) {
                // Check min-size filter
                if (minSizeBytes != null && file.length() < minSizeBytes) {
                    return@forEach
                }

                val size = file.length()
                if (file.delete()) {
                    filesDeleted++
                    bytesFreed += size
                }
            } else if (file.isDirectory) {
                // Only delete empty directories
                if (file.listFiles()?.isEmpty() == true) {
                    file.delete()
                }
            }
        }

        // Try to delete the root dir if now empty
        if (dir.exists() && dir.listFiles()?.isEmpty() == true) {
            dir.delete()
        }

        return CleanReport(filesDeleted, bytesFreed)
    }

    /**
     * Simple glob matching: supports * as wildcard within path segments and **\/ prefix.
     * Matches against relative path from the base directory.
     */
    protected fun matchesGlob(file: File, baseDir: File, pattern: String): Boolean {
        val relativePath = file.relativeTo(baseDir).path.replace("\\", "/")
        val normalizedPattern = pattern.replace("\\", "/")

        // Convert glob to regex
        val regexStr = buildString {
            var i = 0
            while (i < normalizedPattern.length) {
                val c = normalizedPattern[i]
                when {
                    c == '*' && i + 1 < normalizedPattern.length && normalizedPattern[i + 1] == '*' -> {
                        append(".*")
                        i += 2
                        // skip trailing slash
                        if (i < normalizedPattern.length && normalizedPattern[i] == '/') i++
                        continue
                    }
                    c == '*' -> append("[^/]*")
                    c == '?' -> append("[^/]")
                    c == '.' -> append("\\.")
                    else -> append(c)
                }
                i++
            }
        }

        return Regex(regexStr).matches(relativePath)
    }

    /**
     * Parse a human-readable size string (e.g., "5M", "100K", "1G") to bytes.
     */
    protected fun parseSize(sizeStr: String): Long {
        val trimmed = sizeStr.trim().uppercase()
        val number = trimmed.takeWhile { it.isDigit() || it == '.' }.toDoubleOrNull() ?: return 0L
        val suffix = trimmed.dropWhile { it.isDigit() || it == '.' }

        return when (suffix) {
            "K", "KB" -> (number * 1024).toLong()
            "M", "MB" -> (number * 1024 * 1024).toLong()
            "G", "GB" -> (number * 1024 * 1024 * 1024).toLong()
            "B", "" -> number.toLong()
            else -> number.toLong()
        }
    }

    /**
     * Find and delete files matching a glob pattern within the project directory.
     */
    protected fun deleteMatchingFiles(projectDir: File, pattern: String): CleanReport {
        var filesDeleted = 0
        var bytesFreed = 0L

        projectDir.walkTopDown().forEach { file ->
            if (file.isFile && matchesGlob(file, projectDir, pattern)) {
                val size = file.length()
                if (file.delete()) {
                    filesDeleted++
                    bytesFreed += size
                }
            }
        }

        return CleanReport(filesDeleted, bytesFreed)
    }
}

/**
 * Report of cleanup operation
 */
data class CleanReport(
    val filesDeleted: Int,
    val bytesFreed: Long
)
