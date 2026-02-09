package dev.dropper.publishers

import dev.dropper.util.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Publisher for Modrinth platform
 * API docs: https://docs.modrinth.com/api-spec/
 */
class ModrinthPublisher(
    private val httpClient: HttpClient = OkHttpClientImpl()
) : Publisher {

    private val baseUrl = "https://api.modrinth.com/v2"
    private val json = Json { ignoreUnknownKeys = true }

    override fun publish(config: PublishConfig, jarFiles: List<File>): PublishResult {
        val modrinthConfig = config.modrinth
            ?: return PublishResult(false, "Modrinth configuration not provided")

        if (config.dryRun) {
            Logger.info("[DRY RUN] Would publish to Modrinth:")
            Logger.info("  Project: ${modrinthConfig.projectId}")
            Logger.info("  Version: ${config.version}")
            Logger.info("  Release Type: ${config.releaseType.value}")
            Logger.info("  Game Versions: ${config.gameVersions.joinToString(", ")}")
            Logger.info("  Loaders: ${config.loaders.joinToString(", ")}")
            Logger.info("  Files: ${jarFiles.map { it.name }}")
            return PublishResult(true, "Dry run successful", "https://modrinth.com/project/${modrinthConfig.projectId}")
        }

        try {
            // Create version
            val versionData = createVersionPayload(config, modrinthConfig, jarFiles)
            val response = httpClient.postMultipart(
                url = "$baseUrl/version",
                headers = mapOf(
                    "Authorization" to modrinthConfig.apiToken,
                    "User-Agent" to "Dropper/1.0.0"
                ),
                parts = buildMultipartData(versionData, jarFiles)
            )

            if (!response.success) {
                return PublishResult(
                    false,
                    "Failed to publish to Modrinth: ${response.body}",
                    error = RuntimeException("HTTP ${response.code}")
                )
            }

            val versionId = extractVersionId(response.body)
            val url = "https://modrinth.com/mod/${modrinthConfig.projectId}/version/$versionId"

            Logger.success("Published to Modrinth: $url")
            return PublishResult(true, "Successfully published to Modrinth", url)

        } catch (e: Exception) {
            return PublishResult(false, "Error publishing to Modrinth: ${e.message}", error = e)
        }
    }

    override fun validate(config: PublishConfig): List<String> {
        val errors = mutableListOf<String>()

        if (config.modrinth == null) {
            errors.add("Modrinth configuration is missing")
            return errors
        }

        if (config.modrinth.projectId.isBlank()) {
            errors.add("Modrinth project ID is required")
        }

        if (config.modrinth.apiToken.isBlank()) {
            errors.add("Modrinth API token is required")
        }

        if (config.version.isBlank()) {
            errors.add("Version is required")
        }

        if (config.gameVersions.isEmpty()) {
            errors.add("At least one game version is required")
        }

        if (config.loaders.isEmpty()) {
            errors.add("At least one loader is required")
        }

        return errors
    }

    override fun platformName(): String = "Modrinth"

    private fun createVersionPayload(
        config: PublishConfig,
        modrinthConfig: ModrinthConfig,
        jarFiles: List<File>
    ): ModrinthVersionData {
        return ModrinthVersionData(
            projectId = modrinthConfig.projectId,
            versionNumber = config.version,
            versionTitle = "v${config.version}",
            changelog = config.changelog.ifBlank { "Release ${config.version}" },
            dependencies = config.dependencies.map { dep ->
                ModrinthDependency(
                    projectId = dep.id,
                    dependencyType = dep.type.value
                )
            },
            gameVersions = config.gameVersions,
            versionType = config.releaseType.value,
            loaders = config.loaders,
            featured = true,
            fileParts = jarFiles.indices.map { "file$it" }
        )
    }

    private fun buildMultipartData(versionData: ModrinthVersionData, jarFiles: List<File>): Map<String, Any> {
        val parts = mutableMapOf<String, Any>()
        parts["data"] = json.encodeToString(versionData)

        jarFiles.forEachIndexed { index, file ->
            parts["file$index"] = file
        }

        return parts
    }

    private fun extractVersionId(responseBody: String): String {
        return try {
            val response = json.decodeFromString<ModrinthVersionResponse>(responseBody)
            response.id
        } catch (e: Exception) {
            "unknown"
        }
    }

    @Serializable
    private data class ModrinthVersionData(
        val projectId: String,
        val versionNumber: String,
        val versionTitle: String,
        val changelog: String,
        val dependencies: List<ModrinthDependency>,
        val gameVersions: List<String>,
        val versionType: String,
        val loaders: List<String>,
        val featured: Boolean,
        val fileParts: List<String>
    )

    @Serializable
    private data class ModrinthDependency(
        val projectId: String,
        val dependencyType: String
    )

    @Serializable
    private data class ModrinthVersionResponse(
        val id: String
    )
}
