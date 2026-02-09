package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands

/**
 * Parent command for creating mod content (items, blocks, etc.)
 */
class CreateCommand : CliktCommand(
    name = "create",
    help = "Create new mod content (items, blocks, etc.)"
) {
    override fun run() = Unit
}

// Standalone main function for testing (commented out to avoid conflicts)
// fun main(args: Array<String>) = CreateCommand()
//     .subcommands(
//         CreateItemCommand(),
//         CreateBlockCommand(),
//         CreateEntityCommand(),
//         CreateBiomeCommand(),
//         CreateEnchantmentCommand(),
//         CreateRecipeCommand(),
//         CreateTagCommand()
//     )
//     .main(args)
