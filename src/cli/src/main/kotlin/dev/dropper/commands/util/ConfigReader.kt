package dev.dropper.commands.util

import java.io.File

/**
 * Reads and parses project configuration files
 */
class ConfigReader(private val projectDir: File) {

    data class ProjectInfo(
        val modId: String,
        val modName: String,
        val versions: List<VersionInfo>
    )

    data class VersionInfo(
        val minecraftVersion: String,
        val loaders: List<String>,
        val assetPack: String?
    )

    /**
     * Read the main config.yml and scan for available versions
     */
    fun readProjectInfo(): ProjectInfo? {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) {
            return null
        }

        try {
            // Parse as generic map for flexibility
            val configText = configFile.readText()

            // Simple YAML parsing without serialization (for flexibility)
            val modId = extractYamlValue(configText, "id") ?: return null
            val modName = extractYamlValue(configText, "name") ?: modId

            // Scan versions directory
            val versionsDir = File(projectDir, "versions")
            val versions = mutableListOf<VersionInfo>()

            if (versionsDir.exists() && versionsDir.isDirectory) {
                versionsDir.listFiles()?.forEach { versionDir ->
                    if (versionDir.isDirectory && !versionDir.name.equals("shared", ignoreCase = true)) {
                        val versionConfigFile = File(versionDir, "config.yml")
                        if (versionConfigFile.exists()) {
                            val versionConfigText = versionConfigFile.readText()
                            val mcVersion = extractYamlValue(versionConfigText, "minecraft_version")
                                ?: versionDir.name.replace("_", ".")
                            val loadersLine = versionConfigText.lines()
                                .find { it.trim().startsWith("loaders:") }
                            val loaders = if (loadersLine != null) {
                                loadersLine.substringAfter("[")
                                    .substringBefore("]")
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                            } else {
                                emptyList()
                            }
                            val assetPack = extractYamlValue(versionConfigText, "asset_pack")

                            versions.add(VersionInfo(mcVersion, loaders, assetPack))
                        }
                    }
                }
            }

            return ProjectInfo(modId, modName, versions)
        } catch (e: Exception) {
            // Return null on parsing errors
            return null
        }
    }

    /**
     * Simple YAML value extractor for basic key-value pairs
     */
    private fun extractYamlValue(yamlText: String, key: String): String? {
        val pattern = """^\s*$key:\s*["']?([^"'\n]+)["']?\s*$""".toRegex(RegexOption.MULTILINE)
        val match = pattern.find(yamlText)
        return match?.groupValues?.get(1)?.trim()
    }

    /**
     * Check if a specific version-loader combination exists
     */
    fun versionLoaderExists(version: String, loader: String): Boolean {
        val versionDir = File(projectDir, "versions/${version.replace(".", "_")}")
        val loaderDir = File(versionDir, loader)
        return versionDir.exists() && loaderDir.exists()
    }

    /**
     * Convert MC version string to Gradle module format (1.20.1 -> 1_20_1)
     */
    fun versionToGradleFormat(version: String): String {
        return version.replace(".", "_")
    }
}
