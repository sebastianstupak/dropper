package dev.dropper.exporters

import java.io.File

/**
 * Base interface for exporters
 */
interface Exporter {
    /**
     * Export operation
     */
    fun export(projectDir: File, packName: String, outputDir: File): File?

    /**
     * Extract mod ID from config.yml
     */
    fun extractModId(configFile: File): String? {
        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-_]+)").find(content)?.groupValues?.get(1)
    }

    /**
     * Extract mod name from config.yml
     */
    fun extractModName(configFile: File): String? {
        val content = configFile.readText()
        return Regex("name:\\s*\"?([^\"\\n]+)\"?").find(content)?.groupValues?.get(1)
    }

    /**
     * Extract description from config.yml
     */
    fun extractDescription(configFile: File): String? {
        val content = configFile.readText()
        return Regex("description:\\s*\"?([^\"\\n]+)\"?").find(content)?.groupValues?.get(1)
    }
}
