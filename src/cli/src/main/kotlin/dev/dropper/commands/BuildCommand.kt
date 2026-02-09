package dev.dropper.commands

import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to build the mod for all or specific versions/loaders
 */
class BuildCommand : DropperCommand(
    name = "build",
    help = "Build mod JARs for all versions and loaders"
) {
    private val all by option("--all", "-a", help = "Build all versions and loaders").flag(default = false)
    private val version by option("--version", "-v", help = "Build specific MC version (e.g., 1.20.1)")
    private val loader by option("--loader", "-l", help = "Build specific loader (fabric, forge, neoforge)")
    private val clean by option("--clean", "-c", help = "Clean before building").flag(default = false)

    override fun run() {
        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        Logger.info("Building mod...")

        // Determine what to build
        val buildTargets = when {
            version != null && loader != null -> {
                Logger.info("Building ${version}-${loader}...")
                listOf("${version!!.replace(".", "_")}-${loader}")
            }
            version != null -> {
                Logger.info("Building all loaders for version ${version}...")
                val v = version!!.replace(".", "_")
                listOf("${v}-fabric", "${v}-forge", "${v}-neoforge")
            }
            loader != null -> {
                Logger.info("Building ${loader} for all versions...")
                // TODO: Read versions from config
                emptyList()
            }
            all -> {
                Logger.info("Building all versions and loaders...")
                emptyList() // Empty means build everything
            }
            else -> {
                Logger.info("Building all versions and loaders...")
                emptyList()
            }
        }

        // Execute Gradle build
        val gradlewCmd = if (System.getProperty("os.name").lowercase().contains("windows")) {
            File(projectDir, "gradlew.bat").absolutePath
        } else {
            File(projectDir, "gradlew").absolutePath
        }

        if (!File(gradlewCmd).exists()) {
            Logger.error("Gradle wrapper not found. Run 'gradle wrapper' first.")
            return
        }

        val buildCmd = mutableListOf(gradlewCmd)
        if (clean) {
            buildCmd.add("clean")
        }

        if (buildTargets.isEmpty()) {
            buildCmd.add("build")
        } else {
            buildTargets.forEach { target ->
                buildCmd.add(":${target}:build")
            }
        }

        val process = ProcessBuilder(buildCmd)
            .directory(projectDir)
            .inheritIO()
            .start()

        val exitCode = process.waitFor()

        if (exitCode == 0) {
            Logger.success("Build completed successfully!")
            Logger.info("JARs are in: build/<version>/<loader>.jar")
        } else {
            Logger.error("Build failed with exit code $exitCode")
        }
    }
}
