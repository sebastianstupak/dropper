package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.dropper.commands.rename.*

/**
 * Main rename command with subcommands for different component types
 */
class RenameCommand : CliktCommand(
    name = "rename",
    help = "Rename components and update all references"
) {
    override fun run() {
        echo("Use 'dropper rename <component> <old> <new>' to rename components")
        echo("Available components: item, block, entity, enchantment, biome, mod, package")
    }
}

fun createRenameCommand(): CliktCommand {
    return RenameCommand().subcommands(
        RenameItemCommand(),
        RenameBlockCommand(),
        RenameEntityCommand(),
        RenameEnchantmentCommand(),
        RenameBiomeCommand(),
        RenameModCommand(),
        RenamePackageCommand()
    )
}
