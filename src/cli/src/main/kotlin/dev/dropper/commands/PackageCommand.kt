package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand

/**
 * Parent command for packaging mods for distribution
 */
class PackageCommand : CliktCommand(
    name = "package",
    help = "Package mods for distribution (Modrinth, CurseForge, etc.)"
) {
    override fun run() = Unit
}
