package dev.dropper.commands.dev

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.types.int
import dev.dropper.commands.util.ConfigReader
import dev.dropper.commands.util.GradleRunner
import dev.dropper.commands.util.ProcessManager
import dev.dropper.util.Logger
import java.io.File

/**
 * Launch Minecraft in development mode (both client and server available)
 */
class DevRunCommand : CliktCommand(
    name = "run",
    help = "Launch Minecraft in development mode"
) {
    private val version by option("--version", "-v", help = "Minecraft version (e.g., 1.20.1)")
    private val loader by option("--loader", "-l", help = "Mod loader (fabric, forge, neoforge)")
    private val debug by option("--debug", "-d", help = "Enable debug mode with remote debugging").flag(default = false)
    private val port by option("--port", "-p", help = "Debug port").int().default(5005)
    private val clean by option("--clean", "-c", help = "Start with fresh world/data").flag(default = false)
    private val client by option("--client", help = "Launch client (default)").flag(default = true)

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
        Logger.info("Found mod: ${projectInfo.modName}")

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

        Logger.info("Using version: $selectedVersion")

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

        Logger.info("Using loader: $selectedLoader")

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

        // Build JVM arguments for debug mode
        val jvmArgs = if (debug) {
            Logger.info("Debug mode enabled")
            Logger.info("Debug port: $port")
            Logger.info("Connect debugger to: localhost:$port")
            Logger.info("Waiting for debugger to attach...")
            listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:$port")
        } else {
            emptyList()
        }

        // Build Gradle arguments
        val gradleArgs = if (clean) {
            Logger.info("Clean mode: Starting with fresh world/data")
            listOf("--rerun-tasks")
        } else {
            emptyList()
        }

        // Determine task (runClient or runServer)
        val task = if (client) "runClient" else "runClient"
        val mode = if (client) "client" else "client"

        Logger.info("Starting Minecraft $selectedVersion $mode with $selectedLoader loader...")
        val versionGradle = configReader.versionToGradleFormat(selectedVersion)
        Logger.info("Executing: ./gradlew :${versionGradle}-${selectedLoader}:$task")
        println()

        // Execute Gradle task
        val exitCode = gradleRunner.executeTask(
            version = versionGradle,
            loader = selectedLoader,
            task = task,
            jvmArgs = jvmArgs,
            gradleArgs = gradleArgs
        )

        println()
        if (exitCode == 0) {
            Logger.success("Minecraft $mode started successfully!")
        } else {
            Logger.error("Failed to start Minecraft (exit code $exitCode)")
        }
    }
}
