package dev.dropper.synchronizers

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonSyntaxException
import java.io.File

/**
 * Handles conflict resolution during sync
 */
object ConflictResolver {

    private val gson = GsonBuilder().setPrettyPrinting().create()

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
     * Target values always take precedence for existing keys.
     * If no new keys are added, returns the original target content unchanged.
     *
     * @param sourceFile Source lang file
     * @param targetFile Target lang file
     * @return Merged JSON content
     */
    fun mergeLangFiles(sourceFile: File, targetFile: File): String {
        val sourceContent = sourceFile.readText()
        val originalTargetContent = targetFile.readText()

        val sourceEntries = parseLangJson(sourceContent)
        val targetEntries = parseLangJson(originalTargetContent)

        // Merge: target takes precedence for existing keys
        val merged = linkedMapOf<String, String>()
        // Start with target entries to preserve their order
        merged.putAll(targetEntries)
        // Add source entries that are missing from target
        sourceEntries.forEach { (key, value) ->
            if (!merged.containsKey(key)) {
                merged[key] = value
            }
        }

        // If no new keys were added from source, return original target content unchanged
        if (merged.size == targetEntries.size && merged.keys == targetEntries.keys) {
            return originalTargetContent
        }

        // Format back to JSON
        return formatLangJson(merged)
    }

    /**
     * Parse lang JSON into ordered map using Gson for robust handling
     * of escaped characters, unicode, and edge cases.
     */
    private fun parseLangJson(content: String): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        try {
            val jsonObject = JsonParser.parseString(content).asJsonObject
            for ((key, value) in jsonObject.entrySet()) {
                entries[key] = if (value.isJsonPrimitive) value.asString else value.toString()
            }
        } catch (e: JsonSyntaxException) {
            // Fallback: try regex for malformed JSON (best effort)
            val entryPattern = Regex(""""([^"\\]*(?:\\.[^"\\]*)*)"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""")
            entryPattern.findAll(content).forEach { match ->
                entries[match.groupValues[1]] = match.groupValues[2]
            }
        } catch (e: Exception) {
            // Return empty map if parsing completely fails
        }
        return entries
    }

    /**
     * Format map back to lang JSON using Gson for proper escaping
     */
    private fun formatLangJson(entries: Map<String, String>): String {
        val jsonObject = JsonObject()
        entries.forEach { (key, value) ->
            jsonObject.addProperty(key, value)
        }
        return gson.toJson(jsonObject)
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
