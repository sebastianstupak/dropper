package dev.dropper.publishers

import kotlinx.serialization.Serializable
import java.io.File

/**
 * Configuration for publishing mods
 */
@Serializable
data class PublishConfig(
    val version: String,
    val releaseType: ReleaseType = ReleaseType.RELEASE,
    val changelog: String = "",
    val gameVersions: List<String> = emptyList(),
    val loaders: List<String> = emptyList(),
    val dependencies: List<Dependency> = emptyList(),
    val dryRun: Boolean = false,

    // Platform-specific configs
    val modrinth: ModrinthConfig? = null,
    val curseforge: CurseForgeConfig? = null,
    val github: GitHubConfig? = null
)

/**
 * Release type
 */
@Serializable
enum class ReleaseType(val value: String) {
    ALPHA("alpha"),
    BETA("beta"),
    RELEASE("release");

    companion object {
        fun fromString(value: String): ReleaseType = when (value.lowercase()) {
            "alpha" -> ALPHA
            "beta" -> BETA
            "release" -> RELEASE
            else -> throw IllegalArgumentException("Invalid release type: $value")
        }
    }
}

/**
 * Dependency configuration
 */
@Serializable
data class Dependency(
    val id: String,
    val type: DependencyType,
    val version: String? = null
)

/**
 * Dependency type
 */
@Serializable
enum class DependencyType(val value: String) {
    REQUIRED("required"),
    OPTIONAL("optional"),
    INCOMPATIBLE("incompatible"),
    EMBEDDED("embedded");

    companion object {
        fun fromString(value: String): DependencyType = when (value.lowercase()) {
            "required" -> REQUIRED
            "optional" -> OPTIONAL
            "incompatible" -> INCOMPATIBLE
            "embedded" -> EMBEDDED
            else -> throw IllegalArgumentException("Invalid dependency type: $value")
        }
    }
}

/**
 * Modrinth-specific configuration
 */
@Serializable
data class ModrinthConfig(
    val projectId: String,
    val apiToken: String
)

/**
 * CurseForge-specific configuration
 */
@Serializable
data class CurseForgeConfig(
    val projectId: Long,
    val apiToken: String
)

/**
 * GitHub-specific configuration
 */
@Serializable
data class GitHubConfig(
    val repository: String, // owner/repo
    val apiToken: String,
    val createTag: Boolean = true
)

/**
 * Configuration loaded from .dropper/publish-config.yml
 */
data class PublishConfigFile(
    val modrinth: ModrinthConfigFile? = null,
    val curseforge: CurseForgeConfigFile? = null,
    val github: GitHubConfigFile? = null,
    val defaults: PublishDefaults = PublishDefaults()
)

data class ModrinthConfigFile(
    val projectId: String,
    val apiToken: String
)

data class CurseForgeConfigFile(
    val projectId: Long,
    val apiToken: String
)

data class GitHubConfigFile(
    val repository: String,
    val apiToken: String
)

data class PublishDefaults(
    val releaseType: String = "release",
    val autoChangelog: Boolean = true,
    val gitTag: Boolean = true
)
