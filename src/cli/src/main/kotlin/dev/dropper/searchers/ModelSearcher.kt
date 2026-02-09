package dev.dropper.searchers

import java.io.File

/**
 * Search for models in the project
 */
class ModelSearcher : Searcher() {

    fun search(projectDir: File, query: String, preview: Boolean, limit: Int): List<ModelSearchResult> {
        val results = mutableListOf<ModelSearchResult>()

        // Search in versions/shared/*/assets/*/models/
        val versionsShared = File(projectDir, "versions/shared")
        if (versionsShared.exists()) {
            versionsShared.listFiles()?.forEach { assetPack ->
                val assetsDir = File(assetPack, "assets")
                if (assetsDir.exists()) {
                    assetsDir.listFiles()?.forEach { modIdDir ->
                        val modelsDir = File(modIdDir, "models")
                        if (modelsDir.exists()) {
                            findModels(projectDir, modelsDir, query, preview, results)
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

    private fun findModels(
        projectDir: File,
        directory: File,
        query: String,
        preview: Boolean,
        results: MutableList<ModelSearchResult>
    ) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                findModels(projectDir, file, query, preview, results)
            } else if (file.extension == "json") {
                if (file.nameWithoutExtension.contains(query, ignoreCase = true)) {
                    val previewText = if (preview) {
                        try {
                            val content = file.readText()
                            val parent = Regex("\"parent\":\\s*\"([^\"]+)\"").find(content)?.groupValues?.get(1)
                            parent ?: "No parent"
                        } catch (e: Exception) {
                            "Error reading file"
                        }
                    } else {
                        null
                    }

                    results.add(
                        ModelSearchResult(
                            file = file,
                            relativePath = getRelativePath(projectDir, file),
                            preview = previewText,
                            score = if (file.nameWithoutExtension.equals(query, ignoreCase = true)) 1.0 else 0.7
                        )
                    )
                }
            }
        }
    }
}
