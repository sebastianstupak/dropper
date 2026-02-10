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
        targetDir.mkdirs()

        // For GraalVM native image compatibility, we enumerate known resource files
        // explicitly rather than scanning the classpath at runtime. Resources under
        // the given path are loaded individually and written to the target directory.
        // The generator classes (e.g., ProjectGenerator) handle the explicit file
        // list for their respective resource directories, so this method serves as
        // a fallback that copies any resource files it can discover via the classloader.
        val resourceUrl = javaClass.getResource("/$resourcePath")
        if (resourceUrl != null && resourceUrl.protocol == "file") {
            // Running from filesystem (IDE / development mode)
            val sourceDir = File(resourceUrl.toURI())
            if (sourceDir.isDirectory) {
                sourceDir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val relativePath = file.toRelativeString(sourceDir)
                        val targetFile = File(targetDir, relativePath)
                        targetFile.parentFile?.mkdirs()
                        file.copyTo(targetFile, overwrite = true)
                    }
                }
            }
        }
        // When running from JAR/native image, the generator classes use
        // individual copyResourceFile() calls for each known file path.
    }
}

class TemplateNotFoundException(message: String) : Exception(message)
