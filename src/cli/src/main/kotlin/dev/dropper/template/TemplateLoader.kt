package dev.dropper.template

import java.io.File

/**
 * Loads templates from embedded resources
 */
object TemplateLoader {

    /**
     * Load a template from resources
     * @param path Path relative to /templates/ (e.g., "project/config.yml.mustache")
     * @return Template content as string
     * @throws TemplateNotFoundException if template doesn't exist
     */
    fun load(path: String): String {
        val fullPath = "/templates/$path"
        val stream = javaClass.getResourceAsStream(fullPath)
            ?: throw TemplateNotFoundException("Template not found: $path (tried: $fullPath)")

        return stream.bufferedReader().use { it.readText() }
    }

    /**
     * Check if a template exists
     */
    fun exists(path: String): Boolean {
        val fullPath = "/templates/$path"
        return javaClass.getResource(fullPath) != null
    }

    /**
     * Copy an entire directory from resources to target directory
     * This is GraalVM-compatible by listing resources explicitly
     *
     * @param resourcePath Path in resources (e.g., "build-logic")
     * @param targetDir Target directory on filesystem
     */
    fun copyDirectory(resourcePath: String, targetDir: File) {
        // For GraalVM compatibility, we need to know the file structure ahead of time
        // This will be populated with the actual structure from the ruby-sword example
        targetDir.mkdirs()

        // TODO: Implement directory copying with resource listing
        // For now, we'll use individual file copying in the generator
    }
}

class TemplateNotFoundException(message: String) : Exception(message)
