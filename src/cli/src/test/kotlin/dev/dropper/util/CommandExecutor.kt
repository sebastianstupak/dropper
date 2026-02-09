package dev.dropper.util

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Utility to execute commands in a specific directory without modifying the JVM's user.dir
 * This is safe for Windows and prevents test executor crashes
 */
object CommandExecutor {

    /**
     * Execute a Gradle command in a specific directory
     * This runs the command as a subprocess, so it's safe on all platforms
     */
    fun executeGradle(
        workingDir: File,
        vararg args: String,
        timeout: Long = 300
    ): ExecutionResult {
        val gradle = if (System.getProperty("os.name").lowercase().contains("windows")) {
            "gradlew.bat"
        } else {
            "./gradlew"
        }

        return executeCommand(workingDir, gradle, *args, timeout = timeout)
    }

    /**
     * Execute a command in a specific directory
     */
    fun executeCommand(
        workingDir: File,
        command: String,
        vararg args: String,
        timeout: Long = 60
    ): ExecutionResult {
        val processBuilder = ProcessBuilder(command, *args)
            .directory(workingDir)
            .redirectErrorStream(true)

        val process = processBuilder.start()
        val output = process.inputStream.bufferedReader().readText()

        val completed = process.waitFor(timeout, TimeUnit.SECONDS)

        return if (completed) {
            ExecutionResult(
                exitCode = process.exitValue(),
                output = output,
                timedOut = false
            )
        } else {
            process.destroyForcibly()
            ExecutionResult(
                exitCode = -1,
                output = output + "\n\n[TIMED OUT after ${timeout}s]",
                timedOut = true
            )
        }
    }
}

/**
 * Result of command execution
 */
data class ExecutionResult(
    val exitCode: Int,
    val output: String,
    val timedOut: Boolean
) {
    val success: Boolean get() = exitCode == 0 && !timedOut
}
