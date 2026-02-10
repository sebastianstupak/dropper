package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.choice
import dev.dropper.util.Logger
import java.io.File

/**
 * Generate code and assets for items, blocks, entities
 */
class GenerateCommand : CliktCommand(
    name = "generate",
    help = "Generate code and assets (item, block, entity)"
) {
    private val type by argument(
        name = "TYPE",
        help = "Type to generate (item, block, entity)"
    ).choice("item", "block", "entity")

    private val name by argument(
        name = "NAME",
        help = "Name of the item/block/entity (snake_case)"
    )

    override fun run() {
        val projectDir = File(".")

        // Validate we're in a Dropper project
        if (!File(projectDir, "config.yml").exists()) {
            Logger.error("Not in a Dropper project directory (config.yml not found)")
            throw IllegalStateException("Run this command from a Dropper project root")
        }

        // Read config to get mod ID and package
        val configContent = File(projectDir, "config.yml").readText()
        val modId = configContent.lines()
            .find { it.trim().startsWith("id:") }
            ?.substringAfter("id:")
            ?.trim()
            ?: throw IllegalStateException("Could not find mod ID in config.yml")

        val packageName = "com." + modId.replace("-", "").replace("_", "")

        when (type) {
            "item" -> {
                Logger.info("Generating item via create command: $name")
                val command = CreateItemCommand()
                command.parse(arrayOf(name))
            }
            "block" -> {
                Logger.info("Generating block via create command: $name")
                val command = CreateBlockCommand()
                command.parse(arrayOf(name))
            }
            "entity" -> {
                Logger.info("Generating entity via create command: $name")
                val command = CreateEntityCommand()
                command.parse(arrayOf(name))
            }
        }
    }
}
