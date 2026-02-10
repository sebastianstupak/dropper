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
        return Regex("id:\\s*([a-z0-9_-]+)").find(content)?.groupValues?.get(1)
    }

    /**
     * Extract mod name from config.yml
     */
    fun extractModName(configFile: File): String? {
        val content = configFile.readText()
        return Regex("name:\\s*\"?([^\"\n]+)\"?").find(content)?.groupValues?.get(1)
    }

    /**
     * Extract description from config.yml
     */
    fun extractDescription(configFile: File): String? {
        val content = configFile.readText()
        return Regex("description:\\s*\"?([^\"\n]+)\"?").find(content)?.groupValues?.get(1)
    }

    /**
     * Resolve output directory relative to project directory if it is not absolute
     */
    fun resolveOutputDir(projectDir: File, outputDir: File): File {
        return if (outputDir.isAbsolute) outputDir else File(projectDir, outputDir.path)
    }

    /**
     * Get default pack format for a Minecraft version
     */
    fun getDefaultPackFormat(mcVersion: String): Int {
        return when {
            mcVersion.startsWith("1.21") -> 34
            mcVersion.startsWith("1.20.5") || mcVersion.startsWith("1.20.6") -> 32
            mcVersion.startsWith("1.20.3") || mcVersion.startsWith("1.20.4") -> 22
            mcVersion.startsWith("1.20.2") -> 18
            mcVersion.startsWith("1.20.1") || mcVersion.startsWith("1.20") -> 15
            mcVersion.startsWith("1.19.4") -> 13
            mcVersion.startsWith("1.19") -> 10
            else -> 15
        }
    }

    /**
     * Get default resource pack format for a Minecraft version
     */
    fun getDefaultResourcePackFormat(mcVersion: String): Int {
        return when {
            mcVersion.startsWith("1.21") -> 34
            mcVersion.startsWith("1.20.5") || mcVersion.startsWith("1.20.6") -> 32
            mcVersion.startsWith("1.20.3") || mcVersion.startsWith("1.20.4") -> 22
            mcVersion.startsWith("1.20.2") -> 18
            mcVersion.startsWith("1.20.1") || mcVersion.startsWith("1.20") -> 15
            mcVersion.startsWith("1.19.4") -> 13
            mcVersion.startsWith("1.19") -> 10
            else -> 15
        }
    }
}
