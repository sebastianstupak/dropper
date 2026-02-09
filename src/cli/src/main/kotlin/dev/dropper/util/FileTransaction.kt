package dev.dropper.util

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Transaction-based file operations for atomic multi-file changes
 *
 * Usage:
 * ```
 * FileTransaction().use { tx ->
 *     tx.writeFile(file1, content1)
 *     tx.writeFile(file2, content2)
 *     tx.deleteFile(file3)
 *     tx.commit()
 * }
 * ```
 */
class FileTransaction : AutoCloseable {

    private val operations = mutableListOf<Operation>()
    private val backups = mutableMapOf<File, File>()
    private var committed = false
    private var rolledBack = false

    /**
     * Sealed class representing different file operations
     */
    private sealed class Operation {
        abstract fun execute()
        abstract fun rollback()

        data class Write(
            val file: File,
            val content: String,
            val backup: File?
        ) : Operation() {
            override fun execute() {
                file.parentFile?.mkdirs()
                file.writeText(content)
            }

            override fun rollback() {
                if (backup != null && backup.exists()) {
                    Files.copy(backup.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } else if (backup == null) {
                    // File didn't exist before, delete it
                    file.delete()
                }
            }
        }

        data class Delete(
            val file: File,
            val backup: File
        ) : Operation() {
            override fun execute() {
                if (file.exists()) {
                    file.delete()
                }
            }

            override fun rollback() {
                if (backup.exists()) {
                    file.parentFile?.mkdirs()
                    Files.copy(backup.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }

        data class Copy(
            val source: File,
            val dest: File,
            val backup: File?
        ) : Operation() {
            override fun execute() {
                dest.parentFile?.mkdirs()
                Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            override fun rollback() {
                if (backup != null && backup.exists()) {
                    Files.copy(backup.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                } else if (backup == null) {
                    dest.delete()
                }
            }
        }

        data class Move(
            val source: File,
            val dest: File,
            val backup: File?
        ) : Operation() {
            override fun execute() {
                dest.parentFile?.mkdirs()
                Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            override fun rollback() {
                if (backup != null && backup.exists()) {
                    // Restore dest from backup
                    Files.copy(backup.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    // Restore source (it was moved)
                    if (dest.exists()) {
                        Files.move(dest.toPath(), source.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                } else {
                    // Move back
                    if (dest.exists()) {
                        Files.move(dest.toPath(), source.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }
            }
        }
    }

    /**
     * Write content to a file
     */
    fun writeFile(file: File, content: String) {
        ensureNotCommitted()
        val backup = createBackup(file)
        operations.add(Operation.Write(file, content, backup))
    }

    /**
     * Delete a file
     */
    fun deleteFile(file: File) {
        ensureNotCommitted()
        val backup = createBackup(file)
        if (backup != null) {
            operations.add(Operation.Delete(file, backup))
        }
    }

    /**
     * Copy a file
     */
    fun copyFile(source: File, dest: File) {
        ensureNotCommitted()
        val backup = createBackup(dest)
        operations.add(Operation.Copy(source, dest, backup))
    }

    /**
     * Move a file
     */
    fun moveFile(source: File, dest: File) {
        ensureNotCommitted()
        val backup = createBackup(dest)
        operations.add(Operation.Move(source, dest, backup))
    }

    /**
     * Create backup of file if it exists
     */
    private fun createBackup(file: File): File? {
        if (!file.exists()) {
            return null
        }

        // Use existing backup if already created
        if (file in backups) {
            return backups[file]
        }

        val backupDir = File(System.getProperty("java.io.tmpdir"), "dropper-backup-${System.currentTimeMillis()}")
        backupDir.mkdirs()

        val backup = File(backupDir, "${file.name}.backup")
        Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING)
        backups[file] = backup

        return backup
    }

    /**
     * Commit all operations atomically
     */
    fun commit() {
        ensureNotCommitted()

        try {
            // Execute all operations
            for (operation in operations) {
                operation.execute()
            }
            committed = true

            // Clean up backups on success
            cleanupBackups()
        } catch (e: Exception) {
            Logger.error("Transaction failed: ${e.message}")
            Logger.info("Rolling back changes...")
            rollback()
            throw e
        }
    }

    /**
     * Rollback all operations
     */
    fun rollback() {
        if (rolledBack) {
            return
        }

        Logger.debug("Rolling back ${operations.size} operations")

        // Rollback in reverse order
        for (operation in operations.reversed()) {
            try {
                operation.rollback()
            } catch (e: Exception) {
                Logger.warn("Rollback failed for operation: ${e.message}")
                // Continue rolling back other operations
            }
        }

        rolledBack = true
        cleanupBackups()
    }

    /**
     * Clean up backup files
     */
    private fun cleanupBackups() {
        for (backup in backups.values) {
            try {
                backup.delete()
            } catch (e: Exception) {
                Logger.debug("Failed to cleanup backup: ${backup.absolutePath}")
            }
        }

        // Try to remove backup directory
        backups.values.firstOrNull()?.parentFile?.let { dir ->
            try {
                if (dir.listFiles()?.isEmpty() == true) {
                    dir.delete()
                }
            } catch (e: Exception) {
                Logger.debug("Failed to cleanup backup directory: ${dir.absolutePath}")
            }
        }

        backups.clear()
    }

    private fun ensureNotCommitted() {
        if (committed) {
            throw IllegalStateException("Transaction already committed")
        }
        if (rolledBack) {
            throw IllegalStateException("Transaction already rolled back")
        }
    }

    /**
     * Auto-cleanup on close
     */
    override fun close() {
        if (!committed && !rolledBack) {
            Logger.warn("Transaction was not committed, rolling back")
            rollback()
        }
    }

    companion object {
        /**
         * Execute multiple file operations atomically
         */
        inline fun atomic(block: FileTransaction.() -> Unit) {
            FileTransaction().use { tx ->
                tx.block()
                tx.commit()
            }
        }
    }
}
