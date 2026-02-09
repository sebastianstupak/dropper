package dev.dropper.cleaners

import java.io.File

/**
 * Base class for cleaners
 */
abstract class Cleaner {

    /**
     * Clean operation
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

        print("$message (y/N): ")
        val response = readLine()?.lowercase()
        return response == "y" || response == "yes"
    }
}

/**
 * Report of cleanup operation
 */
data class CleanReport(
    val filesDeleted: Int,
    val bytesFreed: Long
)
