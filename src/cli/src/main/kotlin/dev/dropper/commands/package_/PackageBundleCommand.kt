package dev.dropper.commands.package_

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import dev.dropper.commands.util.ConfigReader
import dev.dropper.packagers.BundlePackager
import dev.dropper.packagers.PackageOptions
import dev.dropper.util.Logger
import java.io.File

/**
 * Package all versions and loaders into a bundle
 */
class PackageBundleCommand : CliktCommand(
    name = "bundle",
    help = "Bundle all versions and loaders into a single ZIP"
) {
    private val output by option("--output", "-o", help = "Output directory")
        .default("build/packages")

    private val includeSources by option("--include-sources", help = "Include source JARs")
        .flag(default = false)

    private val includeJavadoc by option("--include-javadoc", help = "Include javadoc JARs")
        .flag(default = false)

    private val versions by option("--versions", "-v", help = "Specific versions to include (comma-separated)")
        .split(",")

    private val loaders by option("--loaders", "-l", help = "Specific loaders to include (comma-separated)")
        .split(",")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        // Read project info
        val configReader = ConfigReader(projectDir)
        val projectInfo = configReader.readProjectInfo()

        if (projectInfo == null) {
            Logger.error("Failed to read project configuration")
            return
        }

        // Read additional metadata from config.yml
        val configText = configFile.readText()
        val description = extractYamlValue(configText, "description")
        val author = extractYamlValue(configText, "author")
        val license = extractYamlValue(configText, "license")
        val version = extractYamlValue(configText, "version") ?: "1.0.0"

        val options = PackageOptions(
            includeSources = includeSources,
            includeJavadoc = includeJavadoc,
            versions = versions ?: emptyList(),
            loaders = loaders ?: emptyList(),
            modId = projectInfo.modId,
            modName = projectInfo.modName,
            modVersion = version,
            description = description,
            author = author,
            license = license
        )

        val outputDir = File(output)
        val packager = BundlePackager()

        try {
            val packageFile = packager.pack(projectDir, outputDir, options)
            Logger.success("Package created successfully!")
            Logger.info("Location: ${packageFile.absolutePath}")
        } catch (e: Exception) {
            Logger.error("Failed to create package: ${e.message}")
        }
    }

    private fun extractYamlValue(yamlText: String, key: String): String? {
        val pattern = """^\s*$key:\s*["']?([^"'\n]+)["']?\s*$""".toRegex(RegexOption.MULTILINE)
        val match = pattern.find(yamlText)
        return match?.groupValues?.get(1)?.trim()
    }
}
