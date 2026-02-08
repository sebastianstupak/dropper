package dev.dropper

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.BuildCommand
import dev.dropper.commands.CreateCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.GenerateCommand
import dev.dropper.commands.InitCommand

/**
 * Main CLI class for Dropper - Multi-loader Minecraft mod development tool
 */
class DropperCLI : CliktCommand(
    name = "dropper",
    help = "Multi-loader Minecraft mod development tool",
    printHelpOnEmptyArgs = true
) {
    private val version by option("--version", help = "Show version").flag()

    override fun run() {
        if (version) {
            echo(getVersion())
        } else {
            echo("Dropper - Multi-Loader Minecraft Mod Tool")
            echo("Run 'dropper --help' for available commands")
        }
    }

    companion object {
        fun getVersion(): String = "1.0.0"
    }
}

fun main(args: Array<String>) = DropperCLI()
    .subcommands(
        InitCommand(),
        CreateCommand().subcommands(
            CreateItemCommand(),
            CreateBlockCommand()
        ),
        BuildCommand(),
        GenerateCommand()
    )
    .main(args)
