package dev.dropper.searchers

import java.io.File

/**
 * Base class for searchers
 */
abstract class Searcher {

    /**
     * Calculate fuzzy match score (0.0 = no match, 1.0 = perfect match)
     */
    protected fun fuzzyMatch(query: String, target: String): Double {
        val lowerQuery = query.lowercase()
        val lowerTarget = target.lowercase()

        // Exact match
        if (lowerQuery == lowerTarget) return 1.0

        // Contains match
        if (lowerTarget.contains(lowerQuery)) {
            return 0.8 + (0.2 * (lowerQuery.length.toDouble() / lowerTarget.length))
        }

        // Levenshtein distance-based match
        val distance = levenshteinDistance(lowerQuery, lowerTarget)
        val maxLen = maxOf(lowerQuery.length, lowerTarget.length)
        val score = 1.0 - (distance.toDouble() / maxLen)

        return if (score > 0.5) score else 0.0
    }

    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Get relative path from project root
     */
    protected fun getRelativePath(projectDir: File, file: File): String {
        return file.absolutePath.removePrefix(projectDir.absolutePath).removePrefix(File.separator)
    }
}

/**
 * Search result for files
 */
data class FileSearchResult(
    val file: File,
    val relativePath: String,
    val score: Double = 1.0,
    val size: Long = 0
)

/**
 * Search result for models
 */
data class ModelSearchResult(
    val file: File,
    val relativePath: String,
    val preview: String? = null,
    val score: Double = 1.0
)

/**
 * Search result for code
 */
data class CodeSearchResult(
    val file: String,
    val lineNumber: Int,
    val line: String,
    val score: Double = 1.0
)

/**
 * Search result for recipes
 */
data class RecipeSearchResult(
    val name: String,
    val type: String,
    val file: File,
    val details: String? = null,
    val score: Double = 1.0
)
