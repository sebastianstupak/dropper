package dev.dropper.commands.util

import dev.dropper.util.Logger
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

/**
 * Executes Gradle tasks with proper configuration and output handling
 */
class GradleRunner(private val projectDir: File) {

    /**
     * Build a Gradle command for executing a task
     */
    fun buildGradleCommand(
        version: String,
        loader: String,
        task: String,
        jvmArgs: List<String> = emptyList(),
        gradleArgs: List<String> = emptyList()
    ): List<String> {
        val gradlewCmd = getGradleWrapperPath()
        val moduleId = "${version.replace(".", "_")}-$loader"
        val taskName = ":$moduleId:$task"

        val command = mutableListOf(gradlewCmd, taskName)

        // Add JVM arguments
        if (jvmArgs.isNotEmpty()) {
            command.add("-D" + "org.gradle.jvmargs=${jvmArgs.joinToString(" ")}")
        }

        // Add Gradle arguments
        command.addAll(gradleArgs)

        return command
    }

    /**
     * Execute a Gradle task and stream output
     */
    fun executeTask(
        version: String,
        loader: String,
        task: String,
        jvmArgs: List<String> = emptyList(),
        gradleArgs: List<String> = emptyList(),
        cleanFirst: Boolean = false
    ): Int {
        val commands = mutableListOf<String>()

        // Add clean if requested
        if (cleanFirst) {
            commands.add(getGradleWrapper())
            commands.add("clean")
        }

        // Build main task command
        val taskCommand = buildGradleCommand(version, loader, task, jvmArgs, gradleArgs)
        commands.addAll(taskCommand)

        // If we have clean, we need to run two separate commands
        if (cleanFirst) {
            // Run clean first
            val cleanProcess = ProcessBuilder(listOf(getGradleWrapper(), "clean"))
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()

            streamOutput(cleanProcess)
            val cleanExit = cleanProcess.waitFor()
            if (cleanExit != 0) {
                return cleanExit
            }

            // Then run main task
            return executeCommandWithOutput(taskCommand)
        } else {
            return executeCommandWithOutput(taskCommand)
        }
    }

    /**
     * Execute a command and stream its output
     */
    private fun executeCommandWithOutput(command: List<String>): Int {
        val process = ProcessBuilder(command)
            .directory(projectDir)
            .redirectErrorStream(true)
            .start()

        streamOutput(process)
        return process.waitFor()
    }

    /**
     * Stream process output to console
     */
    private fun streamOutput(process: Process) {
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?

        while (reader.readLine().also { line = it } != null) {
            println("[Gradle] $line")
        }
    }

    /**
     * Get the Gradle wrapper path (checks existence when validate is true)
     */
    private fun getGradleWrapperPath(validate: Boolean = false): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val wrapperName = if (isWindows) "gradlew.bat" else "gradlew"
        val wrapper = File(projectDir, wrapperName)

        if (validate && !wrapper.exists()) {
            throw IllegalStateException("Gradle wrapper not found at ${wrapper.absolutePath}")
        }

        return wrapper.absolutePath
    }

    /**
     * Get the Gradle wrapper command for the current OS (validates existence)
     */
    private fun getGradleWrapper(): String {
        return getGradleWrapperPath(validate = true)
    }

    /**
     * Check if Gradle wrapper exists
     */
    fun hasGradleWrapper(): Boolean {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val wrapperName = if (isWindows) "gradlew.bat" else "gradlew"
        return File(projectDir, wrapperName).exists()
    }
}
