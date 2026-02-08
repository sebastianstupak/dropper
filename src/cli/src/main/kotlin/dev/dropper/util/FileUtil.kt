package dev.dropper.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories

/**
 * Safe file operation utilities
 */
object FileUtil {

    /**
     * Create directory and all parent directories
     */
    fun createDirectories(path: Path) {
        path.createDirectories()
    }

    /**
     * Create directory from File
     */
    fun createDirectories(file: File) {
        file.mkdirs()
    }

    /**
     * Write text to file, creating parent directories if needed
     */
    fun writeText(file: File, content: String) {
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    /**
     * Copy directory recursively
     */
    fun copyDirectory(source: File, target: File) {
        if (!source.exists()) {
            throw IllegalArgumentException("Source directory does not exist: ${source.absolutePath}")
        }

        if (source.isDirectory) {
            target.mkdirs()
            source.listFiles()?.forEach { child ->
                val targetChild = File(target, child.name)
                if (child.isDirectory) {
                    copyDirectory(child, targetChild)
                } else {
                    child.copyTo(targetChild, overwrite = true)
                }
            }
        } else {
            source.copyTo(target, overwrite = true)
        }
    }

    /**
     * Validate that a path is safe (prevent directory traversal)
     */
    fun isSafePath(basePath: File, targetPath: File): Boolean {
        val base = basePath.canonicalPath
        val target = targetPath.canonicalPath
        return target.startsWith(base)
    }
}
