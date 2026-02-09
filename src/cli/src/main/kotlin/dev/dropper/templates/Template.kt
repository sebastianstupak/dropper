package dev.dropper.templates

import java.io.File

/**
 * Template information
 */
data class Template(
    val name: String,
    val description: String,
    val generator: TemplateGenerator
)

/**
 * Base interface for template generators
 */
interface TemplateGenerator {
    fun generate(projectDir: File, name: String, material: String?): Boolean
}
