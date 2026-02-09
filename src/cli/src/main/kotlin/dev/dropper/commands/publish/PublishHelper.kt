package dev.dropper.commands.publish

import com.charleskorn.kaml.Yaml
import dev.dropper.config.ModConfig
import dev.dropper.publishers.*
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import java.io.File

/**
 * Helper for publish commands
 */
class PublishHelper(private val projectDir: File) {

    private val yaml = Yaml.default

    /**
     * Load publish config file
     */
    fun loadConfigFile(): PublishConfigFileData {
        val configFile = File(projectDir, ".dropper/publish-config.yml")

        if (!configFile.exists()) {
            Logger.warn("No publish config found at .dropper/publish-config.yml")
            return PublishConfigFileData()
        }

        return try {
            val content = FileUtil.readText(configFile)
            // Substitute environment variables
            val substituted = substituteEnvVars(content)
            yaml.decodeFromString(PublishConfigFileData.serializer(), substituted)
        } catch (e: Exception) {
            Logger.error("Failed to load publish config: ${e.message}")
            PublishConfigFileData()
        }
    }

    /**
     * Load mod config
     */
    fun loadModConfig(): ModConfig? {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) {
            return null
        }

        return try {
            yaml.decodeFromString(ModConfig.serializer(), FileUtil.readText(configFile))
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Build publish config from options and config file
     */
    fun buildPublishConfig(
        version: String?,
        changelog: String?,
        autoChangelog: Boolean,
        gameVersions: String?,
        loaders: String?,
        releaseType: String,
        dryRun: Boolean,
        configFile: PublishConfigFileData
    ): PublishConfig {
        // Get version from options or mod config
        val modConfig = loadModConfig()
        val finalVersion = version ?: modConfig?.version ?: "1.0.0"

        // Get changelog
        val changelogGenerator = ChangelogGenerator()
        val finalChangelog = when {
            changelog != null -> changelogGenerator.loadFromFile(File(changelog))
            autoChangelog || configFile.defaults.autoChangelog -> changelogGenerator.generateFromGit(projectDir)
            else -> ""
        }

        // Get game versions
        val finalGameVersions = gameVersions?.split(",")?.map { it.trim() }
            ?: modConfig?.minecraftVersions
            ?: emptyList()

        // Get loaders
        val finalLoaders = loaders?.split(",")?.map { it.trim() }
            ?: modConfig?.loaders
            ?: emptyList()

        return PublishConfig(
            version = finalVersion,
            releaseType = ReleaseType.fromString(releaseType),
            changelog = finalChangelog,
            gameVersions = finalGameVersions,
            loaders = finalLoaders,
            dryRun = dryRun,
            modrinth = configFile.modrinth?.let { ModrinthConfig(it.projectId, it.apiToken) },
            curseforge = configFile.curseforge?.let { CurseForgeConfig(it.projectId, it.apiToken) },
            github = configFile.github?.let { GitHubConfig(it.repository, it.apiToken, configFile.defaults.gitTag) }
        )
    }

    /**
     * Find built JAR files
     */
    fun findJarFiles(): List<File> {
        val buildDir = File(projectDir, "build")
        if (!buildDir.exists()) {
            return emptyList()
        }

        return buildDir.walkTopDown()
            .filter { it.isFile && it.extension == "jar" && !it.name.contains("sources") && !it.name.contains("javadoc") }
            .toList()
    }

    /**
     * Substitute environment variables in config
     */
    private fun substituteEnvVars(content: String): String {
        val pattern = Regex("""\$\{([^}]+)\}""")
        return pattern.replace(content) { matchResult ->
            val varName = matchResult.groupValues[1]
            System.getenv(varName) ?: matchResult.value
        }
    }
}

/**
 * Serializable version of publish config file
 */
@Serializable
data class PublishConfigFileData(
    val modrinth: ModrinthConfigFileData? = null,
    val curseforge: CurseForgeConfigFileData? = null,
    val github: GitHubConfigFileData? = null,
    val defaults: PublishDefaultsData = PublishDefaultsData()
)

@Serializable
data class ModrinthConfigFileData(
    val projectId: String = "",
    val apiToken: String = ""
)

@Serializable
data class CurseForgeConfigFileData(
    val projectId: Long = 0,
    val apiToken: String = ""
)

@Serializable
data class GitHubConfigFileData(
    val repository: String = "",
    val apiToken: String = ""
)

@Serializable
data class PublishDefaultsData(
    val releaseType: String = "release",
    val autoChangelog: Boolean = true,
    val gitTag: Boolean = true
)
