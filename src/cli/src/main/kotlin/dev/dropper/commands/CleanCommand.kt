package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand

/**
 * Parent command for cleaning build artifacts
 */
class CleanCommand : CliktCommand(
    name = "clean",
    help = "Clean build artifacts and caches"
) {
    override fun run() = Unit
}
