package dev.dropper.templates

import dev.dropper.commands.CreateBlockCommand
import dev.dropper.util.Logger
import java.io.File

/**
 * Template for creating a complete wood type
 */
class WoodSetTemplate : TemplateGenerator {

    override fun generate(projectDir: File, name: String, material: String?): Boolean {
        val mat = material ?: name
        Logger.info("Generating wood set for: $mat")

        val variants = listOf("log", "planks", "stairs", "slab", "fence", "fence_gate", "door", "trapdoor")

        variants.forEach { variant ->
            val blockName = "${mat}_$variant"
            Logger.info("  Creating $variant...")

            try {
                val command = CreateBlockCommand()
                val type = when (variant) {
                    "stairs" -> "stairs"
                    "slab" -> "slab"
                    else -> "basic"
                }
                command.parse(arrayOf(blockName, "--type", type))
            } catch (e: Exception) {
                Logger.error("Failed to create $variant: ${e.message}")
                return false
            }
        }

        Logger.success("Wood set created: ${variants.size} blocks")
        return true
    }
}
