package dev.dropper.importers

import java.io.File

/**
 * Interface for importing existing mod projects into Dropper structure
 */
interface Importer {
    /**
     * Import an existing mod project
     * @param source Source mod directory
     * @param target Target Dropper project directory
     */
    fun import(source: File, target: File)

    /**
     * Analyze the source project and extract metadata
     */
    fun analyze(source: File): ImportResult
}

/**
 * Result of importing a mod project
 */
data class ImportResult(
    val modId: String,
    val modName: String,
    val version: String,
    val description: String,
    val author: String,
    val license: String,
    val minecraftVersion: String,
    val loader: String,
    val dependencies: List<Dependency> = emptyList(),
    val sourceFiles: List<SourceFile> = emptyList(),
    val resourceFiles: List<ResourceFile> = emptyList()
)

/**
 * Represents a dependency found in the source project
 */
data class Dependency(
    val name: String,
    val version: String,
    val scope: String = "implementation"
)

/**
 * Represents a source file to be mapped
 */
data class SourceFile(
    val relativePath: String,
    val packageName: String,
    val className: String
)

/**
 * Represents a resource file (asset/data)
 */
data class ResourceFile(
    val relativePath: String,
    val type: ResourceType
)

enum class ResourceType {
    ASSET,
    DATA,
    METADATA
}
