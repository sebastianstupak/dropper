package dev.dropper.commands.template

import com.github.ajalt.clikt.core.CliktCommand
import dev.dropper.templates.TemplateRegistry
import dev.dropper.util.Logger

/**
 * List available templates
 */
class TemplateListCommand : CliktCommand(
    name = "list",
    help = "List available templates"
) {
    override fun run() {
        Logger.info("Available templates:")
        val registry = TemplateRegistry()
        registry.listTemplates().forEach { template ->
            println("  ${template.name} - ${template.description}")
        }
    }
}
