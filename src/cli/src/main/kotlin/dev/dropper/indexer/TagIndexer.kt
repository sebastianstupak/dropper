package dev.dropper.indexer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Indexes tags in a Dropper project
 */
class TagIndexer : ComponentIndexer {

    private val mapper = jacksonObjectMapper()

    override fun getComponentType(): String = "tag"

    override fun index(projectDir: File): List<ComponentInfo> {
        val tags = mutableListOf<ComponentInfo>()

        // Read mod ID from config.yml
        val modId = extractModId(projectDir) ?: return emptyList()

        // Scan tag directories
        val tagTypes = listOf("blocks", "items", "entity_types", "fluids", "biomes")

        for (tagType in tagTypes) {
            val tagDir = File(projectDir, "versions/shared/v1/data/$modId/tags/$tagType")
            if (!tagDir.exists()) continue

            tagDir.listFiles { file -> file.isFile && file.extension == "json" }?.forEach { file ->
                val tagName = file.nameWithoutExtension

                try {
                    val tagData: Map<String, Any> = mapper.readValue(file.readText())
                    @Suppress("UNCHECKED_CAST")
                    val values = (tagData["values"] as? List<String>) ?: emptyList()

                    tags.add(
                        ComponentInfo(
                            name = tagName,
                            type = "tag",
                            loaders = detectLoaders(projectDir),
                            versions = detectVersions(projectDir),
                            metadata = mapOf(
                                "tagType" to tagType,
                                "valueCount" to values.size,
                                "values" to values
                            )
                        )
                    )
                } catch (e: Exception) {
                    // Skip invalid tags
                }
            }
        }

        return tags.sortedBy { it.name }
    }

    private fun extractModId(projectDir: File): String? {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) return null

        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9_-]+)").find(content)?.groupValues?.get(1)
    }

    private fun detectLoaders(projectDir: File): List<String> {
        val loaders = mutableListOf<String>()

        if (File(projectDir, "shared/fabric/src/main/java").exists()) {
            loaders.add("fabric")
        }
        if (File(projectDir, "shared/forge/src/main/java").exists()) {
            loaders.add("forge")
        }
        if (File(projectDir, "shared/neoforge/src/main/java").exists()) {
            loaders.add("neoforge")
        }

        return loaders
    }

    private fun detectVersions(projectDir: File): List<String> {
        val versionsDir = File(projectDir, "versions")
        if (!versionsDir.exists()) return emptyList()

        return versionsDir.listFiles { file -> file.isDirectory && file.name.matches(Regex("\\d+_\\d+_\\d+")) }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }
}
