package dev.dropper.commands.publish

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.publishers.*
import dev.dropper.util.Logger
import java.io.File

/**
 * Publish to all configured platforms
 */
class PublishAllCommand : CliktCommand(
    name = "all",
    help = "Publish mod to all configured platforms"
) {
    private val version by option("--version", "-v", help = "Release version")
    private val changelog by option("--changelog", "-c", help = "Changelog file path")
    private val autoChangelog by option("--auto-changelog", help = "Generate changelog from git").flag(default = false)
    private val gameVersions by option("--game-versions", "-g", help = "Minecraft versions (comma-separated)")
    private val loaders by option("--loaders", "-l", help = "Mod loaders (comma-separated)")
    private val releaseType by option("--release-type", "-r", help = "Release type (alpha, beta, release)").default("release")
    private val dryRun by option("--dry-run", help = "Preview without publishing").flag(default = false)
    private val continueOnError by option("--continue-on-error", help = "Continue if one platform fails").flag(default = false)

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))

        try {
            // Load publish config
            val publishHelper = PublishHelper(projectDir)
            val configFile = publishHelper.loadConfigFile()

            // Build publish config
            val config = publishHelper.buildPublishConfig(
                version = version,
                changelog = changelog,
                autoChangelog = autoChangelog,
                gameVersions = gameVersions,
                loaders = loaders,
                releaseType = releaseType,
                dryRun = dryRun,
                configFile = configFile
            )

            // Find JAR files
            val jarFiles = publishHelper.findJarFiles()
            if (jarFiles.isEmpty()) {
                Logger.error("No JAR files found. Run 'dropper build' first.")
                return
            }

            // Determine which platforms to publish to
            val publishers = mutableListOf<Pair<Publisher, String>>()

            if (configFile.modrinth != null) {
                publishers.add(ModrinthPublisher() to "Modrinth")
            }

            if (configFile.curseforge != null) {
                publishers.add(CurseForgePublisher() to "CurseForge")
            }

            if (configFile.github != null) {
                publishers.add(GitHubPublisher() to "GitHub")
            }

            if (publishers.isEmpty()) {
                Logger.error("No platforms configured. Add configurations to .dropper/publish-config.yml")
                return
            }

            Logger.info("Publishing to ${publishers.size} platform(s)...")

            val results = mutableMapOf<String, PublishResult>()

            // Publish to each platform
            publishers.forEach { (publisher, platformName) ->
                Logger.info("\n--- Publishing to $platformName ---")

                val errors = publisher.validate(config)
                if (errors.isNotEmpty()) {
                    Logger.warn("Validation errors for $platformName:")
                    errors.forEach { Logger.warn("  - $it") }
                    results[platformName] = PublishResult(false, "Validation failed")

                    if (!continueOnError) {
                        Logger.error("Stopping due to validation errors. Use --continue-on-error to continue.")
                        return
                    }
                    return@forEach
                }

                val result = publisher.publish(config, jarFiles)
                results[platformName] = result

                if (result.success) {
                    Logger.success("${platformName}: ${result.message}")
                    result.url?.let { Logger.info("URL: $it") }
                } else {
                    Logger.error("${platformName}: ${result.message}")
                    if (!continueOnError) {
                        Logger.error("Stopping due to error. Use --continue-on-error to continue.")
                        return
                    }
                }
            }

            // Summary
            Logger.info("\n--- Publishing Summary ---")
            val successful = results.count { it.value.success }
            val failed = results.count { !it.value.success }

            Logger.info("Successful: $successful/${results.size}")
            if (failed > 0) {
                Logger.warn("Failed: $failed/${results.size}")
            }

            results.forEach { (platform, result) ->
                val status = if (result.success) "✓" else "✗"
                Logger.info("  $status $platform")
            }

        } catch (e: Exception) {
            Logger.error("Failed to publish: ${e.message}")
            e.printStackTrace()
        }
    }
}
