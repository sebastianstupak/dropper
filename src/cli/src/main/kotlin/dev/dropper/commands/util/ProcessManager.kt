package dev.dropper.commands.util

import dev.dropper.util.Logger
import java.io.File

/**
 * Manages Minecraft/Gradle process lifecycle
 */
class ProcessManager {

    private val runningProcesses = mutableListOf<Process>()

    init {
        // Register shutdown hook to clean up processes
        Runtime.getRuntime().addShutdownHook(Thread {
            stopAllProcesses()
        })
    }

    /**
     * Start a Gradle task as a background process
     */
    fun startGradleTask(
        projectDir: File,
        command: List<String>
    ): Process {
        val process = ProcessBuilder(command)
            .directory(projectDir)
            .inheritIO()
            .start()

        runningProcesses.add(process)
        return process
    }

    /**
     * Wait for a process to complete
     */
    fun waitForProcess(process: Process): Int {
        return process.waitFor()
    }

    /**
     * Stop a specific process
     */
    fun stopProcess(process: Process) {
        try {
            if (process.isAlive) {
                Logger.info("Stopping process...")
                process.destroy()

                // Wait a bit for graceful shutdown
                Thread.sleep(2000)

                // Force kill if still alive
                if (process.isAlive) {
                    Logger.warn("Process did not stop gracefully, forcing shutdown...")
                    process.destroyForcibly()
                }
            }
            runningProcesses.remove(process)
        } catch (e: Exception) {
            Logger.error("Error stopping process: ${e.message}")
        }
    }

    /**
     * Stop all managed processes
     */
    fun stopAllProcesses() {
        runningProcesses.forEach { process ->
            if (process.isAlive) {
                try {
                    process.destroy()
                    Thread.sleep(1000)
                    if (process.isAlive) {
                        process.destroyForcibly()
                    }
                } catch (e: Exception) {
                    // Ignore errors during shutdown
                }
            }
        }
        runningProcesses.clear()
    }

    /**
     * Check if any processes are running
     */
    fun hasRunningProcesses(): Boolean {
        return runningProcesses.any { it.isAlive }
    }
}
