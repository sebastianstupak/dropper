package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand

/**
 * Parent command for updating project dependencies
 */
class UpdateCommand : CliktCommand(
    name = "update",
    help = "Update Minecraft, loaders, and dependencies"
) {
    override fun run() = Unit
}
