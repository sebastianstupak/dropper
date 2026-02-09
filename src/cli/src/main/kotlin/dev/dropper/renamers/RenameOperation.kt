package dev.dropper.renamers

import java.io.File

/**
 * Represents a single atomic rename operation
 */
sealed class RenameOperation {
    /**
     * Rename a file or directory
     */
    data class FileRename(
        val oldPath: File,
        val newPath: File
    ) : RenameOperation()

    /**
     * Replace content in a file
     */
    data class ContentReplace(
        val file: File,
        val oldContent: String,
        val newContent: String,
        val description: String
    ) : RenameOperation()

    /**
     * Delete a file
     */
    data class FileDelete(
        val file: File
    ) : RenameOperation()

    /**
     * Create a new file
     */
    data class FileCreate(
        val file: File,
        val content: String
    ) : RenameOperation()
}

/**
 * Result of a rename operation
 */
data class RenameResult(
    val operations: List<RenameOperation>,
    val success: Boolean,
    val message: String,
    val errors: List<String> = emptyList()
)

/**
 * Context for rename operations
 */
data class RenameContext(
    val projectDir: File,
    val modId: String,
    val packageName: String,
    val oldName: String,
    val newName: String,
    val componentType: ComponentType,
    val version: String? = null,
    val dryRun: Boolean = false,
    val force: Boolean = false
)

/**
 * Types of components that can be renamed
 */
enum class ComponentType {
    ITEM,
    BLOCK,
    ENTITY,
    ENCHANTMENT,
    BIOME,
    MOD,
    PACKAGE
}
