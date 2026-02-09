package dev.dropper.templates

import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Custom user-provided template
 */
class CustomTemplate(private val templateDir: File) : TemplateGenerator {

    override fun generate(projectDir: File, name: String, material: String?): Boolean {
        Logger.info("Generating from custom template...")

        try {
            // Copy template files with variable substitution
            copyTemplateFiles(templateDir, projectDir, name, material)
            Logger.success("Custom template generated successfully!")
            return true
        } catch (e: Exception) {
            Logger.error("Failed to generate from custom template: ${e.message}")
            return false
        }
    }

    private fun copyTemplateFiles(source: File, target: File, name: String, material: String?) {
        source.listFiles()?.forEach { file ->
            if (file.name == "template.yml") {
                // Skip template config
                return@forEach
            }

            if (file.isDirectory) {
                val targetDir = File(target, file.name)
                copyTemplateFiles(file, targetDir, name, material)
            } else {
                val content = file.readText()
                    .replace("{{name}}", name)
                    .replace("{{material}}", material ?: name)

                val targetFile = File(target, file.name)
                FileUtil.writeText(targetFile, content)
            }
        }
    }
}
