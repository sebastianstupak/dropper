package dev.dropper.templates

import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.CreateRecipeCommand
import dev.dropper.util.Logger
import java.io.File

/**
 * Template for creating ore + ingot + recipes
 */
class OreSetTemplate : TemplateGenerator {

    override fun generate(projectDir: File, name: String, material: String?): Boolean {
        val mat = material ?: name
        Logger.info("Generating ore set for: $mat")

        // Create ore block
        try {
            Logger.info("  Creating ${mat}_ore block...")
            val oreCommand = CreateBlockCommand()
            oreCommand.parse(arrayOf("${mat}_ore", "--type", "ore"))

            // Create ingot item
            Logger.info("  Creating ${mat}_ingot item...")
            val ingotCommand = CreateItemCommand()
            ingotCommand.parse(arrayOf("${mat}_ingot", "--type", "basic", "--recipe", "false"))

            // Create smelting recipe
            Logger.info("  Creating smelting recipe...")
            val recipeCommand = CreateRecipeCommand()
            recipeCommand.parse(arrayOf("${mat}_ingot_from_smelting", "--type", "smelting"))

            Logger.success("Ore set created successfully!")
            return true
        } catch (e: Exception) {
            Logger.error("Failed to create ore set: ${e.message}")
            return false
        }
    }
}
