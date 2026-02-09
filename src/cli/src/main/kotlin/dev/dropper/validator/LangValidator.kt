package dev.dropper.validator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Validates language files (translations)
 */
class LangValidator : Validator {
    override val name = "Language Validator"

    private val objectMapper = ObjectMapper()

    override fun validate(projectDir: File, options: ValidationOptions): ValidationResult {
        val startTime = System.currentTimeMillis()
        val issues = mutableListOf<ValidationIssue>()
        var filesScanned = 0

        // Collect all items, blocks, entities, enchantments from the project
        val registeredIds = collectRegisteredIds(projectDir)

        // Find all lang files
        val versionsDir = File(projectDir, "versions")
        if (!versionsDir.exists()) {
            return ValidationResult(name, emptyList(), 0, System.currentTimeMillis() - startTime)
        }

        val langFiles = mutableListOf<File>()
        versionsDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "json" && file.path.contains("/lang/")) {
                langFiles.add(file)
            }
        }

        if (langFiles.isEmpty() && registeredIds.isNotEmpty()) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.WARNING,
                    "No language files found but project has ${registeredIds.size} registered items/blocks",
                    versionsDir,
                    null,
                    "Create en_us.json in versions/shared/v1/assets/<modid>/lang/"
                )
            )
            return ValidationResult(name, issues, filesScanned, System.currentTimeMillis() - startTime)
        }

        langFiles.forEach { langFile ->
            filesScanned++

            try {
                @Suppress("UNCHECKED_CAST")
                val translations = objectMapper.readValue<Map<String, String>>(langFile)
                val translationKeys = translations.keys

                // Check for missing translations
                registeredIds.forEach { (type, id) ->
                    val key = when (type) {
                        "item" -> "item.${getModIdFromLangPath(langFile)}.$id"
                        "block" -> "block.${getModIdFromLangPath(langFile)}.$id"
                        "entity" -> "entity.${getModIdFromLangPath(langFile)}.$id"
                        "enchantment" -> "enchantment.${getModIdFromLangPath(langFile)}.$id"
                        else -> null
                    }

                    if (key != null && !translationKeys.contains(key)) {
                        issues.add(
                            ValidationIssue(
                                ValidationSeverity.WARNING,
                                "Missing translation for $type '$id'",
                                langFile,
                                null,
                                "Add key '$key' to ${langFile.name}"
                            )
                        )
                    }
                }

                // Check for duplicate keys (already handled by JSON parser)
                // Check for empty values
                translations.forEach { (key, value) ->
                    if (value.isBlank()) {
                        issues.add(
                            ValidationIssue(
                                ValidationSeverity.WARNING,
                                "Empty translation value for key '$key'",
                                langFile,
                                null,
                                "Add translation text for '$key'"
                            )
                        )
                    }
                }

            } catch (e: Exception) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Invalid JSON: ${e.message}",
                        langFile,
                        null,
                        "Fix JSON syntax errors"
                    )
                )
            }
        }

        return ValidationResult(name, issues, filesScanned, System.currentTimeMillis() - startTime)
    }

    private fun collectRegisteredIds(projectDir: File): Map<String, Set<String>> {
        val items = mutableSetOf<String>()
        val blocks = mutableSetOf<String>()
        val entities = mutableSetOf<String>()
        val enchantments = mutableSetOf<String>()

        // Scan versions directory for models to find items and blocks
        val versionsDir = File(projectDir, "versions")
        if (versionsDir.exists()) {
            // Find item models
            versionsDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "json" && file.path.contains("/models/item/")) {
                    items.add(file.nameWithoutExtension)
                }
            }

            // Find block models
            versionsDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "json" && file.path.contains("/models/block/")) {
                    blocks.add(file.nameWithoutExtension)
                }
            }
        }

        // Scan Java files for entity and enchantment registrations
        val sharedDir = File(projectDir, "shared")
        if (sharedDir.exists()) {
            sharedDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "java") {
                    val content = file.readText()

                    // Look for ID constants
                    val idPattern = Regex("""public\s+static\s+final\s+String\s+ID\s*=\s*"([^"]+)"""")
                    val matches = idPattern.findAll(content)
                    matches.forEach { match ->
                        val id = match.groupValues[1]

                        when {
                            file.path.contains("/entities/") -> entities.add(id)
                            file.path.contains("/enchantments/") -> enchantments.add(id)
                        }
                    }
                }
            }
        }

        return mapOf(
            "item" to items,
            "block" to blocks,
            "entity" to entities,
            "enchantment" to enchantments
        )
    }

    private fun getModIdFromLangPath(file: File): String {
        // Extract mod ID from path like: .../assets/modid/lang/...
        val path = file.path.replace("\\", "/")
        val assetsIndex = path.indexOf("/assets/")
        if (assetsIndex != -1) {
            val afterAssets = path.substring(assetsIndex + 8)
            val nextSlash = afterAssets.indexOf("/")
            if (nextSlash != -1) {
                return afterAssets.substring(0, nextSlash)
            }
        }
        return "unknown"
    }

    override fun autoFix(projectDir: File, issues: List<ValidationIssue>): Int {
        var fixed = 0

        // Group issues by lang file
        val issuesByFile = issues.filter { it.message.startsWith("Missing translation") }
            .groupBy { it.file }

        issuesByFile.forEach { (langFile, fileIssues) ->
            if (langFile != null && langFile.exists()) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val translations = objectMapper.readValue<MutableMap<String, String>>(langFile)

                    fileIssues.forEach { issue ->
                        // Extract key from suggestion
                        val suggestion = issue.suggestion ?: return@forEach
                        val keyMatch = Regex("""Add key '([^']+)'""").find(suggestion)
                        if (keyMatch != null) {
                            val key = keyMatch.groupValues[1]
                            val displayName = key.split(".").last()
                                .split("_")
                                .joinToString(" ") { it.capitalize() }
                            translations[key] = displayName
                            fixed++
                        }
                    }

                    // Write back sorted
                    val sorted = translations.toSortedMap()
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(langFile, sorted)

                } catch (e: Exception) {
                    // Skip on error
                }
            }
        }

        return fixed
    }
}
