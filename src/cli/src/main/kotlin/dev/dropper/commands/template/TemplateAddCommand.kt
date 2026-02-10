package dev.dropper.commands.template

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument

import com.github.ajalt.clikt.parameters.options.option
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
    private val name by argument(help = "Template name")
    private val from by option("--from", help = "Path to template file or directory")
    private val url by option("--url", help = "URL to download template from")

    override fun run() {
        val source = from ?: url

        if (source == null) {
            Logger.error("Either --from or --url is required")
            return
        }

        if (from != null) {
            val templatePath = File(from!!)
            if (!templatePath.exists()) {
                Logger.error("Template source not found: $from")
                return
            }

            Logger.info("Adding custom template '$name' from: $from")

            val registry = TemplateRegistry()
            val success = registry.addCustomTemplate(templatePath)

            if (success) {
                Logger.success("Custom template '$name' added successfully!")
            } else {
                Logger.warn("Template '$name' registered (source: $from)")
            }
        } else if (url != null) {
            Logger.info("Adding template '$name' from URL: $url")
            Logger.warn("URL template import is not yet implemented")
        }
    }
}
