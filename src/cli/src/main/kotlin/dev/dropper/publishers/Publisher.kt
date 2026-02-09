package dev.dropper.publishers

import java.io.File

/**
 * Interface for publishing mods to different platforms
 */
interface Publisher {
    /**
     * Publish mod files to the platform
     * @param config Publishing configuration
     * @param jarFiles List of JAR files to publish
     * @return true if successful, false otherwise
     */
    fun publish(config: PublishConfig, jarFiles: List<File>): PublishResult

    /**
     * Validate configuration before publishing
     * @param config Publishing configuration
     * @return List of validation errors, empty if valid
     */
    fun validate(config: PublishConfig): List<String>

    /**
     * Get platform name
     */
    fun platformName(): String
}

/**
 * Result of a publish operation
 */
data class PublishResult(
    val success: Boolean,
    val message: String,
    val url: String? = null,
    val error: Throwable? = null
)
