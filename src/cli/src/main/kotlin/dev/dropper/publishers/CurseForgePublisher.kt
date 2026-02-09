package dev.dropper.publishers

import dev.dropper.util.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Publisher for CurseForge platform
 * API docs: https://docs.curseforge.com/
 */
class CurseForgePublisher(
    private val httpClient: HttpClient = OkHttpClientImpl()
) : Publisher {

    private val baseUrl = "https://minecraft.curseforge.com/api"
    private val json = Json { ignoreUnknownKeys = true }

    // CurseForge game version mappings (simplified)
    private val gameVersionMap = mapOf(
        "1.20.1" to 9990,
        "1.20.2" to 9991,
        "1.20.3" to 9992,
        "1.20.4" to 9993,
        "1.21" to 10000,
        "1.21.1" to 10001
    )

    override fun publish(config: PublishConfig, jarFiles: List<File>): PublishResult {
        val curseForgeConfig = config.curseforge
            ?: return PublishResult(false, "CurseForge configuration not provided")

        if (config.dryRun) {
            Logger.info("[DRY RUN] Would publish to CurseForge:")
            Logger.info("  Project: ${curseForgeConfig.projectId}")
            Logger.info("  Version: ${config.version}")
            Logger.info("  Release Type: ${config.releaseType.value}")
            Logger.info("  Game Versions: ${config.gameVersions.joinToString(", ")}")
            Logger.info("  Loaders: ${config.loaders.joinToString(", ")}")
            Logger.info("  Files: ${jarFiles.map { it.name }}")
            return PublishResult(true, "Dry run successful", "https://www.curseforge.com/minecraft/mc-mods/${curseForgeConfig.projectId}")
        }

        try {
            // Upload each file
            jarFiles.forEach { jarFile ->
                val metadata = createUploadMetadata(config, jarFile)
                val response = httpClient.postMultipart(
                    url = "$baseUrl/projects/${curseForgeConfig.projectId}/upload-file",
                    headers = mapOf(
                        "X-Api-Token" to curseForgeConfig.apiToken,
                        "User-Agent" to "Dropper/1.0.0"
                    ),
                    parts = mapOf(
                        "metadata" to json.encodeToString(metadata),
                        "file" to jarFile
                    )
                )

                if (!response.success) {
                    return PublishResult(
                        false,
                        "Failed to upload ${jarFile.name} to CurseForge: ${response.body}",
                        error = RuntimeException("HTTP ${response.code}")
                    )
                }
            }

            val url = "https://www.curseforge.com/minecraft/mc-mods/${curseForgeConfig.projectId}"
            Logger.success("Published to CurseForge: $url")
            return PublishResult(true, "Successfully published to CurseForge", url)

        } catch (e: Exception) {
            return PublishResult(false, "Error publishing to CurseForge: ${e.message}", error = e)
        }
    }

    override fun validate(config: PublishConfig): List<String> {
        val errors = mutableListOf<String>()

        if (config.curseforge == null) {
            errors.add("CurseForge configuration is missing")
            return errors
        }

        if (config.curseforge.projectId <= 0) {
            errors.add("CurseForge project ID is required")
        }

        if (config.curseforge.apiToken.isBlank()) {
            errors.add("CurseForge API token is required")
        }

        if (config.version.isBlank()) {
            errors.add("Version is required")
        }

        if (config.gameVersions.isEmpty()) {
            errors.add("At least one game version is required")
        }

        // Validate game versions are supported
        config.gameVersions.forEach { version ->
            if (version !in gameVersionMap) {
                errors.add("Unsupported game version: $version")
            }
        }

        return errors
    }

    override fun platformName(): String = "CurseForge"

    private fun createUploadMetadata(config: PublishConfig, jarFile: File): CurseForgeUploadMetadata {
        val releaseType = when (config.releaseType) {
            ReleaseType.ALPHA -> "alpha"
            ReleaseType.BETA -> "beta"
            ReleaseType.RELEASE -> "release"
        }

        val gameVersionIds = config.gameVersions.mapNotNull { gameVersionMap[it] }

        // Add loader-specific version IDs
        val loaderIds = config.loaders.mapNotNull { loader ->
            when (loader.lowercase()) {
                "fabric" -> 7499 // Fabric
                "forge" -> 7498  // Forge
                "neoforge" -> 8000 // NeoForge (example ID)
                else -> null
            }
        }

        return CurseForgeUploadMetadata(
            changelog = config.changelog.ifBlank { "Release ${config.version}" },
            changelogType = "markdown",
            displayName = jarFile.name,
            gameVersions = gameVersionIds + loaderIds,
            releaseType = releaseType,
            relations = CurseForgeRelations(
                projects = config.dependencies.map { dep ->
                    CurseForgeRelation(
                        slug = dep.id,
                        type = when (dep.type) {
                            DependencyType.REQUIRED -> "requiredDependency"
                            DependencyType.OPTIONAL -> "optionalDependency"
                            DependencyType.INCOMPATIBLE -> "incompatible"
                            DependencyType.EMBEDDED -> "embeddedLibrary"
                        }
                    )
                }
            )
        )
    }

    @Serializable
    private data class CurseForgeUploadMetadata(
        val changelog: String,
        val changelogType: String,
        val displayName: String,
        val gameVersions: List<Int>,
        val releaseType: String,
        val relations: CurseForgeRelations
    )

    @Serializable
    private data class CurseForgeRelations(
        val projects: List<CurseForgeRelation>
    )

    @Serializable
    private data class CurseForgeRelation(
        val slug: String,
        val type: String
    )
}
