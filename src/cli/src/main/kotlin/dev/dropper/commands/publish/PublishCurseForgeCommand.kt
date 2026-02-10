package dev.dropper.commands.publish

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.publishers.*
import dev.dropper.util.Logger
import java.io.File

/**
 * Publish to CurseForge
 */
class PublishCurseForgeCommand : CliktCommand(
    name = "curseforge",
    help = "Publish mod to CurseForge"
) {
    private val version by option("--version", "-v", help = "Release version")
    private val changelog by option("--changelog", "-c", help = "Changelog file path")
    private val autoChangelog by option("--auto-changelog", help = "Generate changelog from git").flag(default = false)
    private val gameVersions by option("--game-versions", "-g", help = "Minecraft versions (comma-separated)")
    private val loaders by option("--loaders", "-l", help = "Mod loaders (comma-separated)")
    private val releaseType by option("--release-type", "-r", help = "Release type (alpha, beta, release)").default("release")
    private val dryRun by option("--dry-run", help = "Preview without publishing").flag(default = false)

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))

        try {
            // Load publish config
            val publishHelper = PublishHelper(projectDir)
            val configFile = publishHelper.loadConfigFile()

            if (configFile.curseforge == null) {
                Logger.error("CurseForge configuration not found in .dropper/publish-config.yml")
                return
            }

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

            // Publish
            val publisher = CurseForgePublisher()
            val errors = publisher.validate(config)
            if (errors.isNotEmpty()) {
                Logger.error("Validation errors:")
                errors.forEach { Logger.error("  - $it") }
                return
            }

            Logger.info("Publishing to CurseForge...")
            val result = publisher.publish(config, jarFiles)

            if (result.success) {
                Logger.success(result.message)
                result.url?.let { Logger.info("URL: $it") }
            } else {
                Logger.error(result.message)
            }

        } catch (e: Exception) {
            Logger.error("Failed to publish: ${e.message}")
            Logger.debug("Stack trace: ${e.stackTraceToString()}")
        }
    }
}
