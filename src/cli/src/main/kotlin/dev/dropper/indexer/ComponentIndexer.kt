package dev.dropper.indexer

import java.io.File

/**
 * Interface for indexing different types of mod components
 */
interface ComponentIndexer {
    /**
     * Scan project and build index of components
     */
    fun index(projectDir: File): List<ComponentInfo>

    /**
     * Get component type name
     */
    fun getComponentType(): String
}

/**
 * Base class for component information
 */
data class ComponentInfo(
    val name: String,
    val type: String,
    val className: String? = null,
    val packageName: String? = null,
    val hasTexture: Boolean = false,
    val hasModel: Boolean = false,
    val hasRecipe: Boolean = false,
    val hasLootTable: Boolean = false,
    val versions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val metadata: Map<String, Any> = emptyMap()
)
