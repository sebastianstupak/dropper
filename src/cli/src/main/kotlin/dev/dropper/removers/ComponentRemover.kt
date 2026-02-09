package dev.dropper.removers

import java.io.File

/**
 * Interface for removing mod components safely
 */
interface ComponentRemover {
    /**
     * Find all files related to this component
     */
    fun findRelatedFiles(projectDir: File, componentName: String, modId: String): List<File>

    /**
     * Check for dependencies that reference this component
     */
    fun findDependencies(projectDir: File, componentName: String, modId: String): List<Dependency>

    /**
     * Remove the component and all related files
     */
    fun remove(
        projectDir: File,
        componentName: String,
        modId: String,
        options: RemovalOptions
    ): RemovalResult
}

/**
 * Options for component removal
 */
data class RemovalOptions(
    val dryRun: Boolean = false,
    val force: Boolean = false,
    val keepAssets: Boolean = false,
    val version: String? = null,
    val createBackup: Boolean = true,
    val interactive: Boolean = true
)

/**
 * Result of a removal operation
 */
data class RemovalResult(
    val success: Boolean,
    val filesRemoved: List<File> = emptyList(),
    val directoriesRemoved: List<File> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList()
)

/**
 * Represents a dependency on a component
 */
data class Dependency(
    val file: File,
    val type: DependencyType,
    val description: String
)

enum class DependencyType {
    RECIPE,
    TAG,
    LOOT_TABLE,
    ADVANCEMENT,
    BLOCK_ENTITY,
    CODE_REFERENCE
}
