package dev.dropper.util

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories

/**
 * Safe file operation utilities with comprehensive error handling
 */
object FileUtil {

    /**
     * Create directory and all parent directories with validation
     */
    fun createDirectories(path: Path) {
        ErrorHandler.safeFileOperation("create directories", path.toString()) {
            // Validate path length
            val lengthCheck = Validators.validatePathLength(path)
            if (!lengthCheck.isValid) {
                Validators.exitWithError(lengthCheck)
            }

            path.createDirectories()
        }
    }

    /**
     * Create directory from File with validation
     */
    fun createDirectories(file: File) {
        ErrorHandler.safeFileOperation("create directories", file.absolutePath) {
            // Validate path
            val nameCheck = Validators.validateFileName(file.name)
            if (!nameCheck.isValid) {
                Validators.exitWithError(nameCheck)
            }

            val success = file.mkdirs()
            if (!success && !file.exists()) {
                throw IOException("Failed to create directories: ${file.absolutePath}")
            }
        }
    }

    /**
     * Write text to file with comprehensive validation and error handling
     */
    fun writeText(file: File, content: String) {
        ErrorHandler.safeFileOperation("write file", file.absolutePath) {
            // Validate filename
            val nameCheck = Validators.validateFileName(file.name)
            if (!nameCheck.isValid) {
                Validators.exitWithError(nameCheck)
            }

            // Validate parent directory writable
            val parent = file.parentFile
            if (parent != null && parent.exists()) {
                val writeCheck = Validators.validateWritable(parent)
                if (!writeCheck.isValid) {
                    Validators.exitWithError(writeCheck)
                }
            }

            // Estimate file size (rough estimate: 2 bytes per char for UTF-16)
            val estimatedSize = content.length * 2L
            val diskCheck = Validators.validateDiskSpace(file.parentFile ?: file, estimatedSize)
            if (!diskCheck.isValid) {
                Validators.exitWithError(diskCheck)
            }

            // Create parent directories
            file.parentFile?.mkdirs()

            // Use atomic write operation
            atomicWriteText(file, content)
        }
    }

    /**
     * Atomic write operation (write to temp file, then move)
     */
    private fun atomicWriteText(file: File, content: String) {
        val tempFile = File.createTempFile("dropper", ".tmp", file.parentFile)
        try {
            tempFile.writeText(content)
            Files.move(
                tempFile.toPath(),
                file.toPath(),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: Exception) {
            // Clean up temp file on failure
            tempFile.delete()
            throw e
        }
    }

    /**
     * Read text from file with error handling
     */
    fun readText(file: File): String {
        return ErrorHandler.safeFileOperation("read file", file.absolutePath) {
            // Validate file exists
            val existsCheck = Validators.validatePathExists(file, "File")
            if (!existsCheck.isValid) {
                Validators.exitWithError(existsCheck)
            }

            file.readText()
        }
    }

    /**
     * Safe read that returns null on error instead of throwing
     */
    fun readTextOrNull(file: File): String? {
        return try {
            if (file.exists() && file.isFile) {
                file.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Logger.debug("Failed to read file: ${file.absolutePath}: ${e.message}")
            null
        }
    }

    /**
     * Copy directory recursively with validation
     */
    fun copyDirectory(source: File, target: File) {
        ErrorHandler.safeFileOperation("copy directory", source.absolutePath) {
            // Validate source exists
            val existsCheck = Validators.validatePathExists(source, "Source directory")
            if (!existsCheck.isValid) {
                Validators.exitWithError(existsCheck)
            }

            // Validate source is directory
            val dirCheck = Validators.validateIsDirectory(source)
            if (!dirCheck.isValid) {
                Validators.exitWithError(dirCheck)
            }

            // Validate target is writable
            if (target.exists()) {
                val writeCheck = Validators.validateWritable(target)
                if (!writeCheck.isValid) {
                    Validators.exitWithError(writeCheck)
                }
            }

            copyDirectoryRecursive(source, target)
        }
    }

    /**
     * Internal recursive copy implementation
     */
    private fun copyDirectoryRecursive(source: File, target: File) {
        if (source.isDirectory) {
            target.mkdirs()
            val children = source.listFiles()
                ?: throw IOException("Unable to list files in: ${source.absolutePath}")

            for (child in children) {
                val targetChild = File(target, child.name)
                if (child.isDirectory) {
                    copyDirectoryRecursive(child, targetChild)
                } else {
                    Files.copy(
                        child.toPath(),
                        targetChild.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
        } else {
            Files.copy(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }

    /**
     * Delete directory recursively with safety checks
     */
    fun deleteDirectory(directory: File) {
        ErrorHandler.safeFileOperation("delete directory", directory.absolutePath) {
            if (!directory.exists()) {
                return@safeFileOperation
            }

            // Safety check: prevent deleting system directories
            val path = directory.canonicalPath
            val dangerousPaths = listOf(
                "/", "C:\\", "D:\\",
                System.getProperty("user.home"),
                "/bin", "/usr", "/etc", "/var", "/sys"
            )

            if (dangerousPaths.any { path.equals(it, ignoreCase = true) }) {
                throw IllegalArgumentException("Refusing to delete system directory: $path")
            }

            deleteRecursively(directory)
        }
    }

    /**
     * Internal recursive delete
     */
    private fun deleteRecursively(file: File) {
        if (file.isDirectory) {
            file.listFiles()?.forEach { child ->
                deleteRecursively(child)
            }
        }
        val deleted = file.delete()
        if (!deleted && file.exists()) {
            throw IOException("Failed to delete: ${file.absolutePath}")
        }
    }

    /**
     * Validate that a path is safe (prevent directory traversal)
     */
    fun isSafePath(basePath: File, targetPath: File): Boolean {
        return try {
            val base = basePath.canonicalPath
            val target = targetPath.canonicalPath
            target.startsWith(base)
        } catch (e: IOException) {
            false
        }
    }

    /**
     * Ensure path is safe or throw
     */
    fun ensureSafePath(basePath: File, targetPath: File) {
        val result = Validators.validateSafePath(basePath, targetPath)
        if (!result.isValid) {
            Validators.exitWithError(result)
        }
    }

    /**
     * Check if file exists and is readable
     */
    fun isReadable(file: File): Boolean {
        return file.exists() && file.canRead()
    }

    /**
     * Check if file/directory is writable
     */
    fun isWritable(file: File): Boolean {
        return if (file.exists()) {
            file.canWrite()
        } else {
            file.parentFile?.canWrite() ?: false
        }
    }

    /**
     * Get file size in bytes
     */
    fun getSize(file: File): Long {
        return if (file.exists() && file.isFile) {
            file.length()
        } else {
            0L
        }
    }

    /**
     * Format bytes to human-readable size
     */
    fun formatSize(bytes: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var size = bytes.toDouble()
        var unitIndex = 0

        while (size >= 1024 && unitIndex < units.size - 1) {
            size /= 1024
            unitIndex++
        }

        return "%.2f %s".format(size, units[unitIndex])
    }

    /**
     * Sanitize a mod ID to a valid Java package name
     * Removes hyphens and underscores which are invalid in package names
     *
     * Examples:
     * - "my_mod" -> "mymod"
     * - "cool-mod" -> "coolmod"
     * - "testmod" -> "testmod"
     */
    fun sanitizeModId(modId: String): String {
        return modId.replace("-", "").replace("_", "")
    }
}

