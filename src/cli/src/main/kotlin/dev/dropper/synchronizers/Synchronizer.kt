package dev.dropper.synchronizers

import java.io.File

/**
 * Base interface for synchronizing assets and data between versions and asset packs
 */
interface Synchronizer {
    /**
     * Synchronize assets/data from source to target
     *
     * @param projectDir The project root directory
     * @param source Source version or asset pack (e.g., "1.20.1", "v1")
     * @param target Target version or asset pack (e.g., "1.21.1", "v2")
     * @param options Sync options (dry-run, force, bidirectional, exclude patterns)
     * @return Sync result with details about copied, skipped, and conflicted files
     */
    fun sync(
        projectDir: File,
        source: String,
        target: String,
        options: SyncOptions
    ): SyncResult
}

/**
 * Options for sync operations
 */
data class SyncOptions(
    val dryRun: Boolean = false,
    val force: Boolean = false,
    val bidirectional: Boolean = false,
    val excludePatterns: List<String> = emptyList()
)

/**
 * Result of a sync operation
 */
data class SyncResult(
    val copied: List<FileChange> = emptyList(),
    val skipped: List<FileChange> = emptyList(),
    val conflicts: List<FileChange> = emptyList(),
    val merged: List<FileChange> = emptyList(),
    val success: Boolean = true,
    val message: String = ""
) {
    fun totalChanges(): Int = copied.size + merged.size
    fun hasConflicts(): Boolean = conflicts.isNotEmpty()
}

/**
 * Represents a file change during sync
 */
data class FileChange(
    val sourcePath: String,
    val targetPath: String,
    val changeType: ChangeType,
    val reason: String = ""
)

/**
 * Type of change
 */
enum class ChangeType {
    COPY,      // File copied as-is
    MERGE,     // File merged (e.g., lang files)
    SKIP,      // File skipped (already up-to-date or excluded)
    CONFLICT   // File has conflicting changes
}
