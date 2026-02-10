package dev.dropper.indexer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Indexes recipes in a Dropper project
 */
class RecipeIndexer : ComponentIndexer {

    private val mapper = jacksonObjectMapper()

    override fun getComponentType(): String = "recipe"

    override fun index(projectDir: File): List<ComponentInfo> {
        val recipes = mutableListOf<ComponentInfo>()

        // Read mod ID from config.yml
        val modId = extractModId(projectDir) ?: return emptyList()

        // Scan recipe directories
        val recipeDir = File(projectDir, "versions/shared/v1/data/$modId/recipe")
        if (!recipeDir.exists()) return emptyList()

        recipeDir.listFiles { file -> file.isFile && file.extension == "json" }?.forEach { file ->
            val recipeName = file.nameWithoutExtension

            try {
                val recipeData: Map<String, Any> = mapper.readValue(file.readText())
                val recipeType = recipeData["type"]?.toString() ?: "unknown"

                recipes.add(
                    ComponentInfo(
                        name = recipeName,
                        type = "recipe",
                        loaders = detectLoaders(projectDir),
                        versions = detectVersions(projectDir),
                        metadata = mapOf(
                            "recipeType" to recipeType,
                            "shapeless" to (recipeType == "minecraft:crafting_shapeless"),
                            "shaped" to (recipeType == "minecraft:crafting_shaped"),
                            "smelting" to (recipeType.contains("smelting")),
                            "result" to extractResult(recipeData)
                        )
                    )
                )
            } catch (e: Exception) {
                // Skip invalid recipes
            }
        }

        return recipes.sortedBy { it.name }
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

    @Suppress("UNCHECKED_CAST")
    private fun extractResult(recipeData: Map<String, Any>): String {
        val result = recipeData["result"] ?: return "unknown"

        return when (result) {
            is String -> result
            is Map<*, *> -> {
                val resultMap = result as Map<String, Any>
                resultMap["id"]?.toString() ?: resultMap["item"]?.toString() ?: "unknown"
            }
            else -> "unknown"
        }
    }
}
