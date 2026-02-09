package dev.dropper.publishers

import dev.dropper.util.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Publisher for GitHub Releases
 * API docs: https://docs.github.com/en/rest/releases/releases
 */
class GitHubPublisher(
    private val httpClient: HttpClient = OkHttpClientImpl()
) : Publisher {

    private val baseUrl = "https://api.github.com"
    private val json = Json { ignoreUnknownKeys = true }

    override fun publish(config: PublishConfig, jarFiles: List<File>): PublishResult {
        val githubConfig = config.github
            ?: return PublishResult(false, "GitHub configuration not provided")

        val (owner, repo) = parseRepository(githubConfig.repository)
            ?: return PublishResult(false, "Invalid repository format. Expected: owner/repo")

        if (config.dryRun) {
            Logger.info("[DRY RUN] Would publish to GitHub:")
            Logger.info("  Repository: ${githubConfig.repository}")
            Logger.info("  Version: ${config.version}")
            Logger.info("  Tag: v${config.version}")
            Logger.info("  Release Type: ${config.releaseType.value}")
            Logger.info("  Create Tag: ${githubConfig.createTag}")
            Logger.info("  Files: ${jarFiles.map { it.name }}")
            return PublishResult(true, "Dry run successful", "https://github.com/$owner/$repo/releases/tag/v${config.version}")
        }

        try {
            // Create release
            val releaseData = createReleasePayload(config)
            val createResponse = httpClient.post(
                url = "$baseUrl/repos/$owner/$repo/releases",
                headers = mapOf(
                    "Authorization" to "Bearer ${githubConfig.apiToken}",
                    "Accept" to "application/vnd.github+json",
                    "User-Agent" to "Dropper/1.0.0"
                ),
                body = json.encodeToString(releaseData)
            )

            if (!createResponse.success) {
                return PublishResult(
                    false,
                    "Failed to create GitHub release: ${createResponse.body}",
                    error = RuntimeException("HTTP ${createResponse.code}")
                )
            }

            val release = json.decodeFromString<GitHubReleaseResponse>(createResponse.body)

            // Upload assets
            jarFiles.forEach { jarFile ->
                val uploadResponse = httpClient.postMultipart(
                    url = "${release.uploadUrl.substringBefore("{?")}?name=${jarFile.name}",
                    headers = mapOf(
                        "Authorization" to "Bearer ${githubConfig.apiToken}",
                        "Accept" to "application/vnd.github+json",
                        "Content-Type" to "application/octet-stream",
                        "User-Agent" to "Dropper/1.0.0"
                    ),
                    parts = mapOf("file" to jarFile)
                )

                if (!uploadResponse.success) {
                    Logger.warn("Failed to upload ${jarFile.name}: ${uploadResponse.body}")
                }
            }

            Logger.success("Published to GitHub: ${release.htmlUrl}")
            return PublishResult(true, "Successfully published to GitHub", release.htmlUrl)

        } catch (e: Exception) {
            return PublishResult(false, "Error publishing to GitHub: ${e.message}", error = e)
        }
    }

    override fun validate(config: PublishConfig): List<String> {
        val errors = mutableListOf<String>()

        if (config.github == null) {
            errors.add("GitHub configuration is missing")
            return errors
        }

        if (config.github.repository.isBlank()) {
            errors.add("GitHub repository is required")
        } else {
            val parsed = parseRepository(config.github.repository)
            if (parsed == null) {
                errors.add("Invalid repository format. Expected: owner/repo")
            }
        }

        if (config.github.apiToken.isBlank()) {
            errors.add("GitHub API token is required")
        }

        if (config.version.isBlank()) {
            errors.add("Version is required")
        }

        return errors
    }

    override fun platformName(): String = "GitHub"

    private fun parseRepository(repository: String): Pair<String, String>? {
        val parts = repository.split("/")
        return if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            parts[0] to parts[1]
        } else {
            null
        }
    }

    private fun createReleasePayload(config: PublishConfig): GitHubReleaseData {
        val prerelease = config.releaseType != ReleaseType.RELEASE

        return GitHubReleaseData(
            tagName = "v${config.version}",
            name = "v${config.version}",
            body = config.changelog.ifBlank { "Release ${config.version}" },
            draft = false,
            prerelease = prerelease
        )
    }

    @Serializable
    private data class GitHubReleaseData(
        val tagName: String,
        val name: String,
        val body: String,
        val draft: Boolean,
        val prerelease: Boolean
    )

    @Serializable
    private data class GitHubReleaseResponse(
        val id: Long,
        val htmlUrl: String,
        val uploadUrl: String
    )
}
