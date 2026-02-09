package dev.dropper.commands.template

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.templates.TemplateRegistry
import dev.dropper.util.Logger
import java.io.File

/**
 * Create components from template
 */
class TemplateCreateCommand : CliktCommand(
    name = "create",
    help = "Create components from template"
) {
    private val templateName by argument(help = "Template name")
    private val name by option("--name", "-n", help = "Component name")
    private val material by option("--material", "-m", help = "Material name for sets")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        if (name == null) {
            Logger.error("--name is required")
            return
        }

        Logger.info("Creating from template: $templateName...")

        val registry = TemplateRegistry()
        val success = registry.createFromTemplate(projectDir, templateName, name!!, material)

        if (success) {
            Logger.success("Template '$templateName' created successfully!")
        } else {
            Logger.error("Failed to create from template")
        }
    }
}
