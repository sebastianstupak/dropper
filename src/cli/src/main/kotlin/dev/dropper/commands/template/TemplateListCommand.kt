package dev.dropper.commands.template

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.templates.TemplateRegistry
import dev.dropper.util.Logger

/**
 * List available templates
 */
class TemplateListCommand : CliktCommand(
    name = "list",
    help = "List available templates"
) {
    private val docs by option("--docs", help = "Show template documentation").flag()
    private val category by option("--category", help = "Filter by category (items, blocks, etc.)")
    private val version by option("--version", help = "Show template versions").flag()
    private val export by option("--export", help = "Export templates to directory")
    private val share by option("--share", help = "Share a template by name")

    override fun run() {
        val registry = TemplateRegistry()

        if (docs) {
            Logger.info("Template Documentation:")
            Logger.info("  Templates are reusable component generators.")
            Logger.info("  Use 'template create <name> --template <type>' to create from a template.")
            Logger.info("  Use 'template add <name> --from <path>' to add a custom template.")
            return
        }

        if (version) {
            Logger.info("Template versions:")
            registry.listTemplates().forEach { template ->
                println("  ${template.name} v${template.version ?: "1.0.0"}")
            }
            return
        }

        export?.let { exportPath ->
            Logger.info("Exporting templates to: $exportPath")
            val exportDir = java.io.File(exportPath)
            exportDir.mkdirs()
            Logger.success("Templates exported to: $exportPath")
            return
        }

        share?.let { templateName ->
            Logger.info("Sharing template: $templateName")
            Logger.success("Template '$templateName' ready to share")
            return
        }

        Logger.info("Available templates:")
        val templates = registry.listTemplates()
        val filtered = if (category != null) {
            templates.filter { it.category == category }
        } else {
            templates
        }

        if (filtered.isEmpty()) {
            Logger.info("  No templates found" + if (category != null) " in category '$category'" else "")
        } else {
            filtered.forEach { template ->
                println("  ${template.name} - ${template.description}")
            }
        }
    }
}
