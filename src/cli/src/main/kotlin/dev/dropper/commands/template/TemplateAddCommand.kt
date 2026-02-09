package dev.dropper.commands.template

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import dev.dropper.templates.TemplateRegistry
import dev.dropper.util.Logger
import java.io.File

/**
 * Add custom template
 */
class TemplateAddCommand : CliktCommand(
    name = "add",
    help = "Add custom template"
) {
    private val path by argument(help = "Path to template directory")

    override fun run() {
        val templateDir = File(path)

        if (!templateDir.exists()) {
            Logger.error("Template directory not found: $path")
            return
        }

        Logger.info("Adding custom template...")

        val registry = TemplateRegistry()
        val success = registry.addCustomTemplate(templateDir)

        if (success) {
            Logger.success("Custom template added successfully!")
        } else {
            Logger.error("Failed to add custom template")
        }
    }
}
