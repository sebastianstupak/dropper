package dev.dropper.util

import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.file.AccessDeniedException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import javax.net.ssl.SSLException
import kotlin.system.exitProcess

/**
 * Centralized error handling for Dropper CLI
 *
 * Provides consistent error messages with helpful hints for different error types.
 */
object ErrorHandler {

    /**
     * Handle file operation errors with helpful hints
     */
    fun handleFileError(operation: String, path: String, error: Throwable): Nothing {
        when (error) {
            is NoSuchFileException -> {
                Logger.error("File or directory not found: $path")
                Logger.info("Hint: Verify the path exists or run 'dropper init' to create a project")
                exitProcess(1)
            }
            is AccessDeniedException -> {
                Logger.error("Permission denied: $path")
                Logger.info("Hint: Check file permissions or run with appropriate privileges")
                if (System.getProperty("os.name").lowercase().contains("windows")) {
                    Logger.info("On Windows, try running as Administrator")
                } else {
                    Logger.info("On Unix/Linux, check file ownership with 'ls -l' and use 'chmod' if needed")
                }
                exitProcess(1)
            }
            is FileAlreadyExistsException -> {
                Logger.error("File already exists: $path")
                Logger.info("Hint: Use a different name or delete the existing file")
                exitProcess(1)
            }
            is IOException -> {
                Logger.error("I/O error during $operation: ${error.message}")
                Logger.info("Hint: Check disk space, file system integrity, and that no other process is using the file")
                exitProcess(1)
            }
            else -> {
                Logger.error("Unexpected error during $operation: ${error.message}")
                Logger.debug("Stack trace: ${error.stackTraceToString()}")
                exitProcess(1)
            }
        }
    }

    /**
     * Handle network operation errors
     */
    fun handleNetworkError(operation: String, error: Throwable): Nothing {
        when (error) {
            is SocketTimeoutException -> {
                Logger.error("Network request timed out during $operation")
                Logger.info("Hint: Check your internet connection and try again")
                exitProcess(1)
            }
            is UnknownHostException -> {
                Logger.error("Could not resolve host: ${error.message}")
                Logger.info("Hint: Check DNS settings and internet connectivity")
                exitProcess(1)
            }
            is SSLException -> {
                Logger.error("SSL/TLS error: ${error.message}")
                Logger.info("Hint: Verify system certificates are up to date")
                if (System.getProperty("os.name").lowercase().contains("windows")) {
                    Logger.info("On Windows, run Windows Update")
                } else {
                    Logger.info("On Unix/Linux, update ca-certificates package")
                }
                exitProcess(1)
            }
            is IOException -> {
                Logger.error("Network error during $operation: ${error.message}")
                Logger.info("Hint: Check your internet connection and firewall settings")
                exitProcess(1)
            }
            else -> {
                Logger.error("Unexpected network error: ${error.message}")
                Logger.debug("Stack trace: ${error.stackTraceToString()}")
                exitProcess(1)
            }
        }
    }

    /**
     * Handle process execution errors
     */
    fun handleProcessError(command: String, exitCode: Int, error: Throwable? = null): Nothing {
        if (error != null) {
            when (error) {
                is IOException -> {
                    Logger.error("Failed to start process: $command")
                    Logger.info("Hint: Verify the command exists and is in your PATH")
                    exitProcess(1)
                }
                else -> {
                    Logger.error("Unexpected error running command: ${error.message}")
                    Logger.debug("Command: $command")
                    Logger.debug("Stack trace: ${error.stackTraceToString()}")
                    exitProcess(1)
                }
            }
        } else {
            Logger.error("Command failed with exit code $exitCode: $command")
            Logger.info("Hint: Check the error output above for details")
            exitProcess(exitCode)
        }
    }

    /**
     * Handle validation errors
     */
    fun handleValidationError(message: String, suggestion: String? = null): Nothing {
        Logger.error(message)
        if (suggestion != null) {
            Logger.info("Suggestion: $suggestion")
        }
        exitProcess(1)
    }

    /**
     * Handle configuration errors
     */
    fun handleConfigError(message: String, configPath: String): Nothing {
        Logger.error(message)
        Logger.info("Configuration file: $configPath")
        Logger.info("Hint: Check the configuration syntax and required fields")
        exitProcess(1)
    }

    /**
     * Safe execution wrapper for file operations
     */
    inline fun <T> safeFileOperation(operation: String, path: String, block: () -> T): T {
        return try {
            block()
        } catch (e: Throwable) {
            handleFileError(operation, path, e)
        }
    }

    /**
     * Safe execution wrapper for network operations
     */
    inline fun <T> safeNetworkOperation(operation: String, block: () -> T): T {
        return try {
            block()
        } catch (e: Throwable) {
            handleNetworkError(operation, e)
        }
    }

    /**
     * Safe execution wrapper for process operations
     */
    inline fun safeProcessExecution(command: String, block: () -> Int): Int {
        return try {
            val exitCode = block()
            if (exitCode != 0) {
                handleProcessError(command, exitCode)
            }
            exitCode
        } catch (e: Throwable) {
            handleProcessError(command, -1, e)
        }
    }
}
