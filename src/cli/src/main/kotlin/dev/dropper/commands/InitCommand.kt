package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.util.Logger
import java.io.File

/**
 * Initialize a new multi-loader Minecraft mod project
 */
class InitCommand : CliktCommand(
    name = "init",
    help = "Initialize a new multi-loader Minecraft mod project"
) {
    private val projectName by argument(
        name = "PROJECT_NAME",
        help = "Name of the project directory to create"
    )

    private val modName by option("--name", help = "Mod display name")
        .prompt("Mod name")

    private val modId by option("--id", help = "Mod ID (lowercase, no spaces)")

    private val author by option("--author", help = "Mod author")
        .prompt("Author")

    private val description by option("--description", help = "Mod description")
        .prompt("Description")

    private val license by option("--license", help = "License")
        .default("MIT")

    private val versions by option("--versions", help = "Minecraft versions (comma-separated)")
        .prompt("Minecraft versions (comma-separated, e.g., 1.20.1,1.21.1)")

    private val loaders by option("--loaders", help = "Mod loaders (comma-separated)")
        .default("fabric,forge,neoforge")

    override fun run() {
        val projectDir = File(projectName)

        // Validate project directory doesn't exist
        if (projectDir.exists()) {
            Logger.error("Directory '$projectName' already exists")
            throw IllegalStateException("Project directory already exists")
        }

        // Generate mod ID from project name if not provided
        val finalModId = modId ?: projectName.lowercase()
            .replace(" ", "-")
            .replace("_", "-")

        // Parse versions and loaders
        val versionList = versions.split(",").map { it.trim() }
        val loaderList = loaders.split(",").map { it.trim() }

        // Create config
        val config = ModConfig(
            id = finalModId,
            name = modName,
            description = description,
            author = author,
            license = license,
            minecraftVersions = versionList,
            loaders = loaderList
        )

        // Generate project
        val generator = ProjectGenerator()
        generator.generate(projectDir, config)
    }
}
