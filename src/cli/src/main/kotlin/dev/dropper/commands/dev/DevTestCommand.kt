package dev.dropper.commands.dev

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.util.ConfigReader
import dev.dropper.commands.util.GradleRunner
import dev.dropper.util.Logger
import java.io.File

/**
 * Run tests in development environment
 */
class DevTestCommand : CliktCommand(
    name = "test",
    help = "Run tests for your mod"
) {
    private val version by option("--version", "-v", help = "Minecraft version (e.g., 1.20.1)")
    private val loader by option("--loader", "-l", help = "Mod loader (fabric, forge, neoforge)")
    private val clean by option("--clean", "-c", help = "Clean before running tests").flag(default = false)

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configReader = ConfigReader(projectDir)

        // Check if project is initialized
        val projectInfo = configReader.readProjectInfo()
        if (projectInfo == null) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            Logger.info("Run 'dropper init' to initialize a new project")
            return
        }

        Logger.info("Detecting project configuration...")

        if (projectInfo.versions.isEmpty()) {
            Logger.error("No Minecraft versions configured")
            Logger.info("Run 'dropper create version <version>' to add a version")
            return
        }

        // Determine version to use
        val selectedVersion = version ?: projectInfo.versions.first().minecraftVersion
        val versionInfo = projectInfo.versions.find { it.minecraftVersion == selectedVersion }

        if (versionInfo == null) {
            Logger.error("Version $selectedVersion not found")
            Logger.info("Available versions: ${projectInfo.versions.joinToString(", ") { it.minecraftVersion }}")
            return
        }

        // Determine loader to use
        if (versionInfo.loaders.isEmpty()) {
            Logger.error("No loaders configured for version $selectedVersion")
            return
        }

        val selectedLoader = loader ?: versionInfo.loaders.first()
        if (!versionInfo.loaders.contains(selectedLoader)) {
            Logger.error("Loader $selectedLoader not available for version $selectedVersion")
            Logger.info("Available loaders: ${versionInfo.loaders.joinToString(", ")}")
            return
        }

        // Check if version-loader combination exists
        if (!configReader.versionLoaderExists(selectedVersion, selectedLoader)) {
            Logger.error("Version-loader combination $selectedVersion-$selectedLoader does not exist")
            return
        }

        // Check Gradle wrapper
        val gradleRunner = GradleRunner(projectDir)
        if (!gradleRunner.hasGradleWrapper()) {
            Logger.error("Gradle wrapper not found. Run 'gradle wrapper' first.")
            return
        }

        Logger.info("Running tests for $selectedVersion-$selectedLoader...")
        val versionGradle = configReader.versionToGradleFormat(selectedVersion)
        Logger.info("Executing: ./gradlew :${versionGradle}-${selectedLoader}:test")
        println()

        // Execute Gradle test task
        val exitCode = gradleRunner.executeTask(
            version = versionGradle,
            loader = selectedLoader,
            task = "test",
            cleanFirst = clean
        )

        println()
        if (exitCode == 0) {
            Logger.success("All tests passed!")
        } else {
            Logger.error("Tests failed (exit code $exitCode)")
        }
    }
}
