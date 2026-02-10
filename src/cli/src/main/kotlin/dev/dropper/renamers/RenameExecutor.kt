package dev.dropper.renamers

import dev.dropper.util.Logger
import java.io.File

/**
 * Executes rename operations with transaction-like atomicity
 */
class RenameExecutor {

    private val backups = mutableMapOf<File, String>()

    /**
     * Execute a list of rename operations
     * If any operation fails, rollback all changes
     */
    fun execute(operations: List<RenameOperation>, dryRun: Boolean = false): RenameResult {
        if (dryRun) {
            return previewOperations(operations)
        }

        backups.clear()
        val errors = mutableListOf<String>()

        try {
            operations.forEach { operation ->
                when (operation) {
                    is RenameOperation.FileRename -> executeFileRename(operation)
                    is RenameOperation.ContentReplace -> executeContentReplace(operation)
                    is RenameOperation.FileDelete -> executeFileDelete(operation)
                    is RenameOperation.FileCreate -> executeFileCreate(operation)
                }
            }

            return RenameResult(
                operations = operations,
                success = true,
                message = "Successfully renamed ${operations.size} items"
            )
        } catch (e: Exception) {
            errors.add(e.message ?: "Unknown error")
            rollback()
            return RenameResult(
                operations = operations,
                success = false,
                message = "Rename failed, rolled back all changes",
                errors = errors
            )
        } finally {
            backups.clear()
        }
    }

    private fun executeFileRename(operation: RenameOperation.FileRename) {
        if (!operation.oldPath.exists()) {
            throw IllegalStateException("File does not exist: ${operation.oldPath}")
        }

        // Check if this is a case-only rename (same path, different case)
        val isCaseOnlyRename = operation.oldPath.absolutePath.equals(
            operation.newPath.absolutePath, ignoreCase = true
        ) && operation.oldPath.absolutePath != operation.newPath.absolutePath

        if (operation.newPath.exists() && !isCaseOnlyRename) {
            throw IllegalStateException("Target file already exists: ${operation.newPath}")
        }

        // Create parent directories
        operation.newPath.parentFile?.mkdirs()

        // Backup if it's a file
        if (operation.oldPath.isFile) {
            backups[operation.oldPath] = operation.oldPath.readText()
        }

        // For case-only renames on case-insensitive file systems (Windows),
        // use a two-step rename via a temporary file
        if (isCaseOnlyRename) {
            val tempFile = File(operation.oldPath.parentFile, operation.oldPath.name + ".tmp_rename")
            val step1 = operation.oldPath.renameTo(tempFile)
            if (!step1) {
                throw IllegalStateException("Failed to rename ${operation.oldPath} to temp file")
            }
            val step2 = tempFile.renameTo(operation.newPath)
            if (!step2) {
                // Try to restore
                tempFile.renameTo(operation.oldPath)
                throw IllegalStateException("Failed to rename temp file to ${operation.newPath}")
            }
        } else {
            // Standard rename
            val success = operation.oldPath.renameTo(operation.newPath)
            if (!success) {
                throw IllegalStateException("Failed to rename ${operation.oldPath} to ${operation.newPath}")
            }
        }
    }

    private fun executeContentReplace(operation: RenameOperation.ContentReplace) {
        if (!operation.file.exists()) {
            throw IllegalStateException("File does not exist: ${operation.file}")
        }

        // Backup
        backups[operation.file] = operation.file.readText()

        // Replace
        val content = operation.file.readText()
        val newContent = content.replace(operation.oldContent, operation.newContent)
        operation.file.writeText(newContent)
    }

    private fun executeFileDelete(operation: RenameOperation.FileDelete) {
        if (!operation.file.exists()) {
            return // Already deleted
        }

        // Backup
        if (operation.file.isFile) {
            backups[operation.file] = operation.file.readText()
        }

        // Delete
        val success = operation.file.delete()
        if (!success) {
            throw IllegalStateException("Failed to delete ${operation.file}")
        }
    }

    private fun executeFileCreate(operation: RenameOperation.FileCreate) {
        if (operation.file.exists()) {
            throw IllegalStateException("File already exists: ${operation.file}")
        }

        // Create parent directories
        operation.file.parentFile?.mkdirs()

        // Create
        operation.file.writeText(operation.content)
    }

    private fun rollback() {
        Logger.warn("Rolling back changes...")
        backups.forEach { (file, content) ->
            try {
                file.parentFile?.mkdirs()
                file.writeText(content)
            } catch (e: Exception) {
                Logger.error("Failed to rollback ${file}: ${e.message}")
            }
        }
    }

    private fun previewOperations(operations: List<RenameOperation>): RenameResult {
        Logger.info("Preview of changes (dry run):")
        Logger.info("")

        operations.forEachIndexed { index, operation ->
            when (operation) {
                is RenameOperation.FileRename -> {
                    Logger.info("${index + 1}. Rename file:")
                    Logger.info("   From: ${operation.oldPath.path}")
                    Logger.info("   To:   ${operation.newPath.path}")
                }
                is RenameOperation.ContentReplace -> {
                    Logger.info("${index + 1}. Update content in ${operation.file.name}:")
                    Logger.info("   ${operation.description}")
                    Logger.info("   Replace: ${operation.oldContent.take(50)}...")
                    Logger.info("   With:    ${operation.newContent.take(50)}...")
                }
                is RenameOperation.FileDelete -> {
                    Logger.info("${index + 1}. Delete file:")
                    Logger.info("   ${operation.file.path}")
                }
                is RenameOperation.FileCreate -> {
                    Logger.info("${index + 1}. Create file:")
                    Logger.info("   ${operation.file.path}")
                }
            }
            Logger.info("")
        }

        return RenameResult(
            operations = operations,
            success = true,
            message = "Dry run completed. ${operations.size} operations planned."
        )
    }
}
