package dev.dropper.templates

import dev.dropper.commands.CreateItemCommand
import dev.dropper.util.Logger
import java.io.File

/**
 * Template for creating a complete armor set
 */
class ArmorSetTemplate : TemplateGenerator {

    override fun generate(projectDir: File, name: String, material: String?): Boolean {
        val mat = material ?: name
        Logger.info("Generating armor set for: $mat")

        val pieces = listOf("helmet", "chestplate", "leggings", "boots")

        pieces.forEach { piece ->
            val itemName = "${mat}_$piece"
            Logger.info("  Creating $piece...")

            try {
                val command = CreateItemCommand()
                command.parse(arrayOf(itemName, "--type", "basic", "--recipe", "true"))
            } catch (e: Exception) {
                Logger.error("Failed to create $piece: ${e.message}")
                return false
            }
        }

        Logger.success("Armor set created: ${pieces.size} pieces")
        return true
    }
}
