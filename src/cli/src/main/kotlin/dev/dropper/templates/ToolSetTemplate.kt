package dev.dropper.templates

import dev.dropper.commands.CreateItemCommand
import dev.dropper.util.Logger
import java.io.File

/**
 * Template for creating a complete tool set
 */
class ToolSetTemplate : TemplateGenerator {

    override fun generate(projectDir: File, name: String, material: String?): Boolean {
        val mat = material ?: name
        Logger.info("Generating tool set for: $mat")

        val tools = listOf("sword", "axe", "pickaxe", "shovel", "hoe")

        tools.forEach { tool ->
            val itemName = "${mat}_$tool"
            Logger.info("  Creating $tool...")

            try {
                val command = CreateItemCommand()
                command.parse(arrayOf(itemName, "--type", "tool", "--recipe", "true"))
            } catch (e: Exception) {
                Logger.error("Failed to create $tool: ${e.message}")
                return false
            }
        }

        Logger.success("Tool set created: ${tools.size} tools")
        return true
    }
}
