package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand

/**
 * Parent command for template operations
 */
class TemplateCommand : CliktCommand(
    name = "template",
    help = "Create components from reusable templates"
) {
    override fun run() = Unit
}
