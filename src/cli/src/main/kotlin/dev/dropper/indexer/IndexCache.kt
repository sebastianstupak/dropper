package dev.dropper.indexer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime

/**
 * Cache for component indexes to improve performance
 */
object IndexCache {

    private val mapper = jacksonObjectMapper()
    private const val CACHE_FILE_NAME = ".dropper/cache/index.json"

    data class CacheEntry(
        val timestamp: Long,
        val components: Map<String, List<ComponentInfo>>
    )

    /**
     * Get cached index if valid
     */
    fun get(projectDir: File): Map<String, List<ComponentInfo>>? {
        val cacheFile = File(projectDir, CACHE_FILE_NAME)
        if (!cacheFile.exists()) return null

        try {
            val cacheEntry: CacheEntry = mapper.readValue(cacheFile.readText())

            // Check if cache is still valid (no file modifications since cache creation)
            if (isCacheValid(projectDir, cacheEntry.timestamp)) {
                return cacheEntry.components
            }
        } catch (e: Exception) {
            // Invalid cache, delete it
            cacheFile.delete()
        }

        return null
    }

    /**
     * Save index to cache
     */
    fun save(projectDir: File, components: Map<String, List<ComponentInfo>>) {
        val cacheFile = File(projectDir, CACHE_FILE_NAME)
        cacheFile.parentFile.mkdirs()

        val cacheEntry = CacheEntry(
            timestamp = System.currentTimeMillis(),
            components = components
        )

        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(cacheFile, cacheEntry)
        } catch (e: Exception) {
            // Silently fail cache write
        }
    }

    /**
     * Invalidate cache
     */
    fun invalidate(projectDir: File) {
        val cacheFile = File(projectDir, CACHE_FILE_NAME)
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
    }

    /**
     * Check if cache is still valid by comparing modification times
     */
    private fun isCacheValid(projectDir: File, cacheTimestamp: Long): Boolean {
        val dirsToCheck = listOf(
            "shared/common/src/main/java",
            "shared/fabric/src/main/java",
            "shared/forge/src/main/java",
            "shared/neoforge/src/main/java",
            "versions/shared"
        )

        for (dir in dirsToCheck) {
            val directory = File(projectDir, dir)
            if (!directory.exists()) continue

            if (hasNewerFiles(directory, cacheTimestamp)) {
                return false
            }
        }

        return true
    }

    /**
     * Recursively check if any files are newer than the cache
     */
    private fun hasNewerFiles(directory: File, cacheTimestamp: Long): Boolean {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (hasNewerFiles(file, cacheTimestamp)) return true
            } else {
                val lastModified = Files.getLastModifiedTime(file.toPath()).toMillis()
                if (lastModified > cacheTimestamp) return true
            }
        }
        return false
    }
}
