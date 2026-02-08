package dev.dropper.template

import com.github.mustachejava.DefaultMustacheFactory
import java.io.StringReader
import java.io.StringWriter

/**
 * Renders Mustache templates with context variables
 */
class TemplateEngine {
    private val mustache = DefaultMustacheFactory()

    /**
     * Render a template with the given context
     * @param templatePath Path to template (relative to /templates/)
     * @param context Variables to substitute
     * @return Rendered template string
     */
    fun render(templatePath: String, context: Map<String, Any>): String {
        val templateContent = TemplateLoader.load(templatePath)
        val reader = StringReader(templateContent)
        val compiled = mustache.compile(reader, templatePath)

        val writer = StringWriter()
        compiled.execute(writer, context)
        return writer.toString()
    }

    /**
     * Check if a template exists
     */
    fun exists(templatePath: String): Boolean {
        return TemplateLoader.exists(templatePath)
    }
}
