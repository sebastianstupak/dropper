package dev.dropper.commands.util

import dev.dropper.util.ErrorHandler
import dev.dropper.util.Logger
import dev.dropper.util.Validators
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * Executes Gradle tasks with comprehensive error handling and output streaming
 */
class GradleRunner(private val projectDir: File) {

    private var currentProcess: Process? = null
    private val timeout = TimeUnit.MINUTES.toMillis(30) // 30 minute timeout for builds

    init {
        // Register shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(Thread {
            currentProcess?.let { process ->
                if (process.isAlive) {
                    Logger.debug("Stopping Gradle process on shutdown")
                    stopProcess(process)
                }
            }
        })
    }

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
        // Validate loader
        val loaderCheck = Validators.validateLoader(loader)
        if (!loaderCheck.isValid) {
            Validators.exitWithError(loaderCheck)
        }

        val gradlewCmd = getGradleWrapperPath(validate = false)
        val moduleId = "${version.replace(".", "_")}-$loader"
        val taskName = ":$moduleId:$task"

        val command = mutableListOf(gradlewCmd, taskName)

        // Add JVM arguments
        if (jvmArgs.isNotEmpty()) {
            command.add("-Dorg.gradle.jvmargs=${jvmArgs.joinToString(" ")}")
        }

        // Add Gradle arguments
        command.addAll(gradleArgs)

        Logger.debug("Gradle command: ${command.joinToString(" ")}")

        return command
    }

    /**
     * Execute a Gradle task and stream output with timeout
     */
    fun executeTask(
        version: String,
        loader: String,
        task: String,
        jvmArgs: List<String> = emptyList(),
        gradleArgs: List<String> = emptyList(),
        cleanFirst: Boolean = false
    ): Int {
        // Validate Gradle wrapper exists
        if (!hasGradleWrapper()) {
            Logger.error("Gradle wrapper not found in project directory")
            Logger.info("Hint: Run 'gradle wrapper' to generate the wrapper")
            return 1
        }

        // Validate project directory
        val dirCheck = Validators.validateIsDirectory(projectDir)
        if (!dirCheck.isValid) {
            Validators.exitWithError(dirCheck)
        }

        return try {
            if (cleanFirst) {
                Logger.info("Cleaning build artifacts...")
                val cleanExit = executeClean()
                if (cleanExit != 0) {
                    Logger.error("Clean task failed")
                    return cleanExit
                }
            }

            val taskCommand = buildGradleCommand(version, loader, task, jvmArgs, gradleArgs)
            executeCommandWithOutput(taskCommand)
        } catch (e: Exception) {
            ErrorHandler.handleProcessError("gradle $task", -1, e)
        }
    }

    /**
     * Execute clean task
     */
    private fun executeClean(): Int {
        val gradlewCmd = getGradleWrapper()
        return executeCommandWithOutput(listOf(gradlewCmd, "clean"))
    }

    /**
     * Execute a command and stream its output with timeout
     */
    private fun executeCommandWithOutput(command: List<String>): Int {
        Logger.debug("Executing: ${command.joinToString(" ")}")

        val process = try {
            ProcessBuilder(command)
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()
        } catch (e: IOException) {
            Logger.error("Failed to start Gradle process")
            Logger.info("Hint: Verify Gradle wrapper has execute permissions")
            if (!System.getProperty("os.name").lowercase().contains("windows")) {
                Logger.info("Run: chmod +x gradlew")
            }
            throw e
        }

        currentProcess = process

        try {
            // Stream output in background thread
            val outputThread = Thread {
                streamOutput(process)
            }
            outputThread.start()

            // Wait with timeout
            val completed = process.waitFor(timeout, TimeUnit.MILLISECONDS)

            if (!completed) {
                Logger.error("Gradle task timed out after ${timeout / 1000 / 60} minutes")
                stopProcess(process)
                return -1
            }

            outputThread.join(5000) // Wait up to 5 seconds for output thread

            return process.exitValue()
        } finally {
            currentProcess = null
        }
    }

    /**
     * Stream process output to console
     */
    private fun streamOutput(process: Process) {
        try {
            process.inputStream.bufferedReader().use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    println("[Gradle] $line")
                }
            }
        } catch (e: IOException) {
            Logger.debug("Stream closed: ${e.message}")
        }
    }

    /**
     * Stop a running process gracefully
     */
    private fun stopProcess(process: Process) {
        try {
            if (process.isAlive) {
                Logger.debug("Stopping Gradle process...")
                process.destroy()

                // Wait for graceful shutdown
                val terminated = process.waitFor(5, TimeUnit.SECONDS)
                if (!terminated && process.isAlive) {
                    Logger.debug("Force stopping Gradle process...")
                    process.destroyForcibly()
                    process.waitFor(2, TimeUnit.SECONDS)
                }
            }
        } catch (e: Exception) {
            Logger.debug("Error stopping process: ${e.message}")
        }
    }

    /**
     * Get the Gradle wrapper path with validation
     */
    private fun getGradleWrapperPath(validate: Boolean = false): String {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val wrapperName = if (isWindows) "gradlew.bat" else "gradlew"
        val wrapper = File(projectDir, wrapperName)

        if (validate && !wrapper.exists()) {
            Logger.error("Gradle wrapper not found: ${wrapper.absolutePath}")
            Logger.info("Hint: Run 'gradle wrapper' in project root to generate it")
            throw IllegalStateException("Gradle wrapper not found")
        }

        // Check execute permissions on Unix
        if (validate && !isWindows && !wrapper.canExecute()) {
            Logger.error("Gradle wrapper is not executable: ${wrapper.absolutePath}")
            Logger.info("Hint: Run 'chmod +x ${wrapper.name}'")
            throw IllegalStateException("Gradle wrapper not executable")
        }

        return wrapper.absolutePath
    }

    /**
     * Get the Gradle wrapper command (validates existence and permissions)
     */
    private fun getGradleWrapper(): String {
        return getGradleWrapperPath(validate = true)
    }

    /**
     * Check if Gradle wrapper exists and is valid
     */
    fun hasGradleWrapper(): Boolean {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val wrapperName = if (isWindows) "gradlew.bat" else "gradlew"
        val wrapper = File(projectDir, wrapperName)

        if (!wrapper.exists()) {
            return false
        }

        // On Unix, check if executable
        if (!isWindows && !wrapper.canExecute()) {
            Logger.warn("Gradle wrapper exists but is not executable")
            return false
        }

        return true
    }

    /**
     * Execute any Gradle task with arguments
     */
    fun executeCommand(vararg args: String): Int {
        val gradlewCmd = getGradleWrapper()
        val command = listOf(gradlewCmd) + args
        return executeCommandWithOutput(command)
    }

    /**
     * Check if a Gradle task exists
     */
    fun hasTask(taskName: String): Boolean {
        return try {
            val gradlewCmd = getGradleWrapper()
            val process = ProcessBuilder(listOf(gradlewCmd, "tasks", "--all"))
                .directory(projectDir)
                .redirectErrorStream(true)
                .start()

            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.waitFor()

            output.contains(taskName)
        } catch (e: Exception) {
            Logger.debug("Failed to check task existence: ${e.message}")
            false
        }
    }
}

