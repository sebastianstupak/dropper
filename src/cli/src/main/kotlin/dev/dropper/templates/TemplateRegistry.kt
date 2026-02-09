package dev.dropper.templates

import dev.dropper.util.Logger
import java.io.File

/**
 * Registry for all available templates
 */
class TemplateRegistry {

    private val builtInTemplates = listOf(
        Template("armor-set", "Complete armor set (helmet, chestplate, leggings, boots)", ArmorSetTemplate()),
        Template("tool-set", "Complete tool set (sword, axe, pickaxe, shovel, hoe)", ToolSetTemplate()),
        Template("ore-set", "Ore block + ingot + smelting recipe", OreSetTemplate()),
        Template("wood-set", "Complete wood type (log, planks, stairs, slabs, etc.)", WoodSetTemplate()),
        Template("dimension", "Custom dimension with biome and structure", DimensionTemplate())
    )

    private val customTemplates = mutableListOf<Template>()

    fun listTemplates(): List<Template> {
        return builtInTemplates + customTemplates
    }

    fun createFromTemplate(projectDir: File, templateName: String, name: String, material: String?): Boolean {
        val template = (builtInTemplates + customTemplates).find { it.name == templateName }

        if (template == null) {
            Logger.error("Template not found: $templateName")
            return false
        }

        return template.generator.generate(projectDir, name, material)
    }

    fun addCustomTemplate(templateDir: File): Boolean {
        // Verify template structure
        val configFile = File(templateDir, "template.yml")
        if (!configFile.exists()) {
            Logger.error("Template must contain template.yml")
            return false
        }

        // Parse template config
        val name = templateDir.name
        val description = "Custom template: $name"

        customTemplates.add(
            Template(name, description, CustomTemplate(templateDir))
        )

        Logger.info("Added custom template: $name")
        return true
    }
}
