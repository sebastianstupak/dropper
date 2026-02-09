package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import dev.dropper.commands.remove.*

/**
 * Parent command for removing mod components
 */
class RemoveCommand : CliktCommand(
    name = "remove",
    help = "Remove mod components (items, blocks, entities, etc.)"
) {
    override fun run() = Unit
}

fun main(args: Array<String>) = RemoveCommand()
    .subcommands(
        RemoveItemCommand(),
        RemoveBlockCommand(),
        RemoveEntityCommand(),
        RemoveRecipeCommand(),
        RemoveEnchantmentCommand(),
        RemoveBiomeCommand(),
        RemoveTagCommand()
    )
    .main(args)
