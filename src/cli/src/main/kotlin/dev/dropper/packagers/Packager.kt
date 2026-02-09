package dev.dropper.packagers

import java.io.File

/**
 * Interface for platform-specific packagers
 */
interface Packager {
    /**
     * Package the mod for distribution
     * @param projectDir The project root directory
     * @param outputDir The output directory for packages
     * @param options Packaging options
     * @return The created package file
     */
    fun pack(projectDir: File, outputDir: File, options: PackageOptions): File
}

/**
 * Options for packaging
 */
data class PackageOptions(
    val includeSources: Boolean = false,
    val includeJavadoc: Boolean = false,
    val versions: List<String> = emptyList(), // Empty means all
    val loaders: List<String> = emptyList(),  // Empty means all
    val modId: String,
    val modName: String,
    val modVersion: String,
    val description: String?,
    val author: String?,
    val license: String?
)
