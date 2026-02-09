package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand

/**
 * Parent command for exporting project artifacts
 */
class ExportCommand : CliktCommand(
    name = "export",
    help = "Export datapacks, resource packs, or assets"
) {
    override fun run() = Unit
}
