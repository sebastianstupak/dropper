package dev.dropper.updaters

import java.io.File

/**
 * Interface for updating Dropper projects
 */
interface Updater {
    /**
     * Check for updates
     */
    fun checkUpdates(projectDir: File): UpdateCheckResult

    /**
     * Apply updates
     */
    fun applyUpdates(projectDir: File, updates: List<Update>)
}

/**
 * Result of checking for updates
 */
data class UpdateCheckResult(
    val available: List<Update>,
    val current: Map<String, String>
)

/**
 * Represents an available update
 */
data class Update(
    val type: UpdateType,
    val name: String,
    val currentVersion: String,
    val latestVersion: String,
    val description: String = "",
    val breaking: Boolean = false
)

enum class UpdateType {
    MINECRAFT,
    FABRIC_LOADER,
    FABRIC_API,
    FORGE,
    NEOFORGE,
    DEPENDENCY
}
