package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.dropper.commands.migrate.*

/**
 * Main migrate command with subcommands for different migration types
 */
class MigrateCommand : CliktCommand(
    name = "migrate",
    help = "Migrate projects to new versions or loaders"
) {
    override fun run() {
        echo("Use 'dropper migrate <type>' to perform migrations")
        echo("Available migrations:")
        echo("  version <VERSION>     - Migrate to new Minecraft version")
        echo("  loader <LOADER>       - Add new loader support")
        echo("  mappings              - Update to latest mappings")
        echo("  refactor <OLD> <NEW>  - Refactor package names")
    }
}

fun createMigrateCommand(): CliktCommand {
    return MigrateCommand().subcommands(
        MigrateVersionCommand(),
        MigrateLoaderCommand(),
        MigrateMappingsCommand(),
        MigrateRefactorCommand()
    )
}
