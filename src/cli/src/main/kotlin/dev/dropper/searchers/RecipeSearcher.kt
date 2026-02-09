package dev.dropper.searchers

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File

/**
 * Search for recipes in the project
 */
class RecipeSearcher : Searcher() {

    fun search(projectDir: File, query: String, showDetails: Boolean, limit: Int): List<RecipeSearchResult> {
        val results = mutableListOf<RecipeSearchResult>()

        // Search in versions/shared/*/data/*/recipe or recipes/
        val versionsShared = File(projectDir, "versions/shared")
        if (versionsShared.exists()) {
            versionsShared.listFiles()?.forEach { assetPack ->
                val dataDir = File(assetPack, "data")
                if (dataDir.exists()) {
                    dataDir.listFiles()?.forEach { modIdDir ->
                        // Check both "recipe" and "recipes" directories
                        listOf("recipe", "recipes").forEach { recipeDirName ->
                            val recipeDir = File(modIdDir, recipeDirName)
                            if (recipeDir.exists()) {
                                findRecipes(projectDir, recipeDir, query, showDetails, results)
                            }
                        }
                    }
                }
            }
        }

        // Sort by score and limit
        return results
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun findRecipes(
        projectDir: File,
        directory: File,
        query: String,
        showDetails: Boolean,
        results: MutableList<RecipeSearchResult>
    ) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                findRecipes(projectDir, file, query, showDetails, results)
            } else if (file.extension == "json") {
                if (file.nameWithoutExtension.contains(query, ignoreCase = true) ||
                    (showDetails && fileContentMatches(file, query))) {

                    val (type, details) = if (showDetails) {
                        extractRecipeDetails(file)
                    } else {
                        Pair(extractRecipeType(file), null)
                    }

                    results.add(
                        RecipeSearchResult(
                            name = file.nameWithoutExtension,
                            type = type,
                            file = file,
                            details = details,
                            score = if (file.nameWithoutExtension.equals(query, ignoreCase = true)) 1.0 else 0.7
                        )
                    )
                }
            }
        }
    }

    private fun fileContentMatches(file: File, query: String): Boolean {
        return try {
            file.readText().contains(query, ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    private fun extractRecipeType(file: File): String {
        return try {
            val content = file.readText()
            Regex("\"type\":\\s*\"([^\"]+)\"").find(content)?.groupValues?.get(1) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun extractRecipeDetails(file: File): Pair<String, String> {
        return try {
            val mapper = ObjectMapper()
            val content = file.readText()
            val json = mapper.readTree(content)

            val type = json.get("type")?.asText() ?: "unknown"
            val result = json.get("result")?.get("id")?.asText() ?:
                         json.get("result")?.get("item")?.asText() ?:
                         json.get("output")?.get("item")?.asText() ?: "unknown"

            Pair(type, "Produces: $result")
        } catch (e: Exception) {
            Pair("unknown", null) as Pair<String, String>
        }
    }
}
