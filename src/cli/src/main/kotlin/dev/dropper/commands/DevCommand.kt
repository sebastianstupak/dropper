package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.dropper.commands.dev.*

/**
 * Parent command for development workflow commands
 */
class DevCommand : CliktCommand(
    name = "dev",
    help = "Development workflow commands for running and testing your mod"
) {
    override fun run() = Unit
}
