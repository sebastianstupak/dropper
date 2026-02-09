package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand

/**
 * Parent command for importing mod projects
 */
class ImportCommand : CliktCommand(
    name = "import",
    help = "Import existing mod projects into Dropper structure"
) {
    override fun run() = Unit
}
