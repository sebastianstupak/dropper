package dev.dropper.synchronizers

import java.io.File

/**
 * Handles conflict resolution during sync
 */
object ConflictResolver {

    /**
     * Resolve conflicts between source and target files
     *
     * @param conflicts List of conflicting files
     * @param force If true, always use source (overwrite)
     * @return Resolution strategy for each conflict
     */
    fun resolve(
        conflicts: List<File>,
        sourceDir: File,
        targetDir: File,
        force: Boolean
    ): Map<File, ConflictResolution> {
        return conflicts.associateWith { sourceFile ->
            if (force) {
                ConflictResolution.USE_SOURCE
            } else {
                // In non-interactive mode, keep target by default (preserve existing work)
                ConflictResolution.KEEP_TARGET
            }
        }
    }

    /**
     * Merge two lang files, combining all keys
     *
     * @param sourceFile Source lang file
     * @param targetFile Target lang file
     * @return Merged JSON content
     */
    fun mergeLangFiles(sourceFile: File, targetFile: File): String {
        val sourceLines = sourceFile.readLines()
        val targetLines = targetFile.readLines()

        // Parse JSON manually (simple key-value parser)
        val sourceEntries = parseLangJson(sourceLines)
        val targetEntries = parseLangJson(targetLines)

        // Merge: target takes precedence for existing keys
        val merged = sourceEntries.toMutableMap()
        merged.putAll(targetEntries)

        // Format back to JSON
        return formatLangJson(merged)
    }

    /**
     * Parse lang JSON into map
     */
    private fun parseLangJson(lines: List<String>): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("\"") && trimmed.contains(":")) {
                val parts = trimmed.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim().removeSurrounding("\"")
                    val value = parts[1].trim().removeSuffix(",").trim().removeSurrounding("\"")
                    entries[key] = value
                }
            }
        }
        return entries
    }

    /**
     * Format map back to lang JSON
     */
    private fun formatLangJson(entries: Map<String, String>): String {
        val lines = mutableListOf<String>()
        lines.add("{")
        entries.entries.forEachIndexed { index, (key, value) ->
            val comma = if (index < entries.size - 1) "," else ""
            lines.add("  \"$key\": \"$value\"$comma")
        }
        lines.add("}")
        return lines.joinToString("\n")
    }
}

/**
 * Conflict resolution strategy
 */
enum class ConflictResolution {
    USE_SOURCE,    // Overwrite with source
    KEEP_TARGET,   // Keep target (skip)
    MERGE          // Merge both (for lang files)
}
