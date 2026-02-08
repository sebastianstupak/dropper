package dev.dropper.util

/**
 * Simple console logger
 */
object Logger {
    fun info(message: String) {
        println("ℹ $message")
    }

    fun success(message: String) {
        println("✓ $message")
    }

    fun error(message: String) {
        System.err.println("✗ $message")
    }

    fun warn(message: String) {
        println("⚠ $message")
    }
}
