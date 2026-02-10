package dev.dropper.renamers

import java.io.File

/**
 * Base interface for component renamers
 */
interface ComponentRenamer {
    /**
     * Discover all files and references related to the component
     */
    fun discover(context: RenameContext): List<File>

    /**
     * Find all references to the component in code and assets
     */
    fun findReferences(context: RenameContext, discoveredFiles: List<File>): Map<File, List<String>>

    /**
     * Check if the new name would cause conflicts
     */
    fun checkConflicts(context: RenameContext): List<String>

    /**
     * Plan all rename operations
     */
    fun planRename(context: RenameContext): List<RenameOperation>

    /**
     * Validate that the rename was successful
     */
    fun validate(context: RenameContext): Boolean

    /**
     * Get component-specific file patterns
     */
    fun getFilePatterns(context: RenameContext): List<FilePattern>
}

/**
 * Represents a file pattern to search for
 */
data class FilePattern(
    val pattern: String,
    val type: FileType,
    val loader: String? = null
)

enum class FileType {
    JAVA_CLASS,
    JSON_ASSET,
    JSON_DATA,
    DIRECTORY,
    TEXTURE,
    MODEL
}

/**
 * Helper functions for all renamers
 */
object RenamerUtil {
    /**
     * Convert snake_case to PascalCase class name
     */
    fun toClassName(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { word -> word.replaceFirstChar { it.uppercase() } }
    }

    /**
     * Convert PascalCase to snake_case
     */
    fun toSnakeCase(pascalCase: String): String {
        return pascalCase
            .replace(Regex("([A-Z])"), "_$1")
            .lowercase()
            .removePrefix("_")
    }

    /**
     * Find all files matching a pattern
     */
    fun findFiles(dir: File, pattern: String): List<File> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .toRegex()

        val results = mutableListOf<File>()
        dir.walkTopDown().forEach { file ->
            if (file.isFile && regex.matches(file.name)) {
                results.add(file)
            }
        }
        return results
    }

    /**
     * Find all files containing a specific string
     */
    fun findFilesContaining(dir: File, searchString: String, extensions: List<String>): List<File> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val results = mutableListOf<File>()
        dir.walkTopDown().forEach { file ->
            if (file.isFile && extensions.any { file.name.endsWith(it) }) {
                try {
                    val content = file.readText()
                    if (content.contains(searchString)) {
                        results.add(file)
                    }
                } catch (e: Exception) {
                    // Skip files that can't be read
                }
            }
        }
        return results
    }

    /**
     * Replace all occurrences of a string in a file
     */
    fun replaceInFile(file: File, oldString: String, newString: String): Boolean {
        try {
            val content = file.readText()
            if (content.contains(oldString)) {
                val newContent = content.replace(oldString, newString)
                file.writeText(newContent)
                return true
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    /**
     * Get package path from package name
     */
    fun getPackagePath(packageName: String): String {
        return packageName.replace(".", "/")
    }

    /**
     * Get the proper class name suffix for a loader.
     * Maps loader names to their PascalCase class name suffixes.
     * For example: "neoforge" -> "NeoForge", "fabric" -> "Fabric"
     */
    fun loaderClassName(loader: String): String {
        return when (loader) {
            "neoforge" -> "NeoForge"
            else -> loader.replaceFirstChar { it.uppercase() }
        }
    }
}
