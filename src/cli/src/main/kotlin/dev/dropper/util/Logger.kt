package dev.dropper.util

/**
 * Enhanced console logger with structured output
 */
object Logger {
    private var verboseMode = false
    private var progressActive = false

    /**
     * Enable verbose/debug logging
     */
    fun setVerbose(enabled: Boolean) {
        verboseMode = enabled
    }

    /**
     * Debug messages (only shown in verbose mode)
     */
    fun debug(message: String) {
        if (verboseMode) {
            println("[DEBUG] $message")
        }
    }

    /**
     * Informational messages
     */
    fun info(message: String) {
        clearProgress()
        println("ℹ $message")
    }

    /**
     * Success messages
     */
    fun success(message: String) {
        clearProgress()
        println("✓ $message")
    }

    /**
     * Error messages (written to stderr)
     */
    fun error(message: String) {
        clearProgress()
        System.err.println("✗ $message")
    }

    /**
     * Warning messages
     */
    fun warn(message: String) {
        clearProgress()
        println("⚠ $message")
    }

    /**
     * Show a spinner/progress indicator
     */
    fun progress(message: String) {
        clearProgress()
        print("⏳ $message...")
        progressActive = true
    }

    /**
     * Complete current progress indicator
     */
    fun progressComplete(success: Boolean = true) {
        if (progressActive) {
            println(if (success) " done" else " failed")
            progressActive = false
        }
    }

    /**
     * Clear progress indicator without completion
     */
    private fun clearProgress() {
        if (progressActive) {
            println()
            progressActive = false
        }
    }

    /**
     * Execute operation with spinner
     */
    inline fun <T> withProgress(message: String, block: () -> T): T {
        progress(message)
        return try {
            val result = block()
            progressComplete(true)
            result
        } catch (e: Exception) {
            progressComplete(false)
            throw e
        }
    }

    /**
     * Print a separator line
     */
    fun separator() {
        println("─".repeat(60))
    }

    /**
     * Print a section header
     */
    fun section(title: String) {
        println()
        println("═══ $title ═══")
        println()
    }

    /**
     * Print progress with count
     */
    fun progressCount(current: Int, total: Int, message: String) {
        clearProgress()
        println("[$current/$total] $message")
    }
}
