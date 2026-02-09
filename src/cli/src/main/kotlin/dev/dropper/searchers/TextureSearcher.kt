package dev.dropper.searchers

import java.io.File

/**
 * Search for textures in the project
 */
class TextureSearcher : Searcher() {

    fun search(projectDir: File, query: String, fuzzy: Boolean, limit: Int): List<FileSearchResult> {
        val results = mutableListOf<FileSearchResult>()
        val textureExtensions = listOf(".png", ".jpg", ".jpeg", ".gif")

        // Search in versions/shared/*/assets/*/textures/
        val versionsShared = File(projectDir, "versions/shared")
        if (versionsShared.exists()) {
            versionsShared.listFiles()?.forEach { assetPack ->
                val texturesDir = File(assetPack, "assets")
                if (texturesDir.exists()) {
                    findTextures(projectDir, texturesDir, query, fuzzy, textureExtensions, results)
                }
            }
        }

        // Sort by score and limit
        return results
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun findTextures(
        projectDir: File,
        directory: File,
        query: String,
        fuzzy: Boolean,
        extensions: List<String>,
        results: MutableList<FileSearchResult>
    ) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                findTextures(projectDir, file, query, fuzzy, extensions, results)
            } else if (extensions.any { file.name.endsWith(it, ignoreCase = true) }) {
                val score = if (fuzzy) {
                    fuzzyMatch(query, file.nameWithoutExtension)
                } else {
                    if (file.name.contains(query, ignoreCase = true)) 1.0 else 0.0
                }

                if (score > 0) {
                    results.add(
                        FileSearchResult(
                            file = file,
                            relativePath = getRelativePath(projectDir, file),
                            score = score,
                            size = file.length()
                        )
                    )
                }
            }
        }
    }
}
