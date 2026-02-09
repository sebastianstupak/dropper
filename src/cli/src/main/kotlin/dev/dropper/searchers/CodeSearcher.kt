package dev.dropper.searchers

import java.io.File

/**
 * Search in Java source code
 */
class CodeSearcher : Searcher() {

    fun search(projectDir: File, query: String, useRegex: Boolean, limit: Int): List<CodeSearchResult> {
        val results = mutableListOf<CodeSearchResult>()

        // Search in shared/*/src/
        val sharedDir = File(projectDir, "shared")
        if (sharedDir.exists()) {
            sharedDir.listFiles()?.forEach { loaderDir ->
                val srcDir = File(loaderDir, "src")
                if (srcDir.exists()) {
                    searchInDirectory(projectDir, srcDir, query, useRegex, results)
                }
            }
        }

        // Sort by score and limit
        return results.take(limit)
    }

    private fun searchInDirectory(
        projectDir: File,
        directory: File,
        query: String,
        useRegex: Boolean,
        results: MutableList<CodeSearchResult>
    ) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                searchInDirectory(projectDir, file, query, useRegex, results)
            } else if (file.extension == "java" || file.extension == "kt") {
                searchInFile(projectDir, file, query, useRegex, results)
            }
        }
    }

    private fun searchInFile(
        projectDir: File,
        file: File,
        query: String,
        useRegex: Boolean,
        results: MutableList<CodeSearchResult>
    ) {
        try {
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                val matches = if (useRegex) {
                    try {
                        Regex(query).containsMatchIn(line)
                    } catch (e: Exception) {
                        line.contains(query, ignoreCase = true)
                    }
                } else {
                    line.contains(query, ignoreCase = true)
                }

                if (matches) {
                    results.add(
                        CodeSearchResult(
                            file = getRelativePath(projectDir, file),
                            lineNumber = index + 1,
                            line = line,
                            score = 1.0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Skip files that can't be read
        }
    }
}
