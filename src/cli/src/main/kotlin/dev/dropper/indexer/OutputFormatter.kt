package dev.dropper.indexer

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File

/**
 * Interface for formatting component listings
 */
interface OutputFormatter {
    fun format(components: List<ComponentInfo>, type: String): String
}

/**
 * Table output formatter (default)
 */
class TableFormatter : OutputFormatter {
    override fun format(components: List<ComponentInfo>, type: String): String {
        val plural = pluralize(type)

        if (components.isEmpty()) {
            return "No ${plural} found in project. 0 total\n"
        }

        val sb = StringBuilder()
        sb.append("\n")
        sb.append("═".repeat(80))
        sb.append("\n")
        sb.append("  ${plural.uppercase()} (${components.size} total)\n")
        sb.append("═".repeat(80))
        sb.append("\n\n")

        components.forEach { component ->
            sb.append("  ${component.name}\n")

            if (component.className != null) {
                sb.append("    Class: ${component.packageName}.${component.className}\n")
            }

            val features = mutableListOf<String>()
            if (component.hasTexture) features.add("Texture")
            if (component.hasModel) features.add("Model")
            if (component.hasRecipe) features.add("Recipe")
            if (component.hasLootTable) features.add("Loot Table")

            if (features.isNotEmpty()) {
                sb.append("    Features: ${features.joinToString(", ")}\n")
            }

            if (component.loaders.isNotEmpty()) {
                sb.append("    Loaders: ${component.loaders.joinToString(", ")}\n")
            }

            if (component.metadata.isNotEmpty()) {
                component.metadata.forEach { (key, value) ->
                    if (value !is List<*> && value !is Map<*, *>) {
                        sb.append("    ${key.capitalize()}: $value\n")
                    }
                }
            }

            sb.append("\n")
        }

        sb.append("─".repeat(80))
        sb.append("\n")

        return sb.toString()
    }

    private fun pluralize(type: String): String {
        return when {
            type.endsWith("y") && !type.endsWith("ey") -> type.dropLast(1) + "ies"
            type.endsWith("s") || type.endsWith("x") || type.endsWith("z") ||
                type.endsWith("ch") || type.endsWith("sh") -> type + "es"
            else -> type + "s"
        }
    }

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

/**
 * JSON output formatter
 */
class JsonFormatter : OutputFormatter {
    private val mapper = jacksonObjectMapper()

    override fun format(components: List<ComponentInfo>, type: String): String {
        val output = mapOf(
            "type" to type,
            "count" to components.size,
            "components" to components
        )

        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(output)
    }
}

/**
 * CSV output formatter
 */
class CsvFormatter : OutputFormatter {
    override fun format(components: List<ComponentInfo>, type: String): String {
        if (components.isEmpty()) {
            return "name,type,class,package,texture,model,recipe,loot_table,loaders\n"
        }

        val sb = StringBuilder()
        sb.append("name,type,class,package,texture,model,recipe,loot_table,loaders\n")

        components.forEach { component ->
            sb.append("${component.name},")
            sb.append("${component.type},")
            sb.append("${component.className ?: ""},")
            sb.append("${component.packageName ?: ""},")
            sb.append("${component.hasTexture},")
            sb.append("${component.hasModel},")
            sb.append("${component.hasRecipe},")
            sb.append("${component.hasLootTable},")
            sb.append("\"${component.loaders.joinToString(";")}\"\n")
        }

        return sb.toString()
    }
}

/**
 * Tree output formatter
 */
class TreeFormatter : OutputFormatter {
    override fun format(components: List<ComponentInfo>, type: String): String {
        val plural = pluralize(type)

        if (components.isEmpty()) {
            return "No ${plural} found in project. 0 total\n"
        }

        val sb = StringBuilder()
        sb.append("\n${plural.uppercase()}\n")

        // Group by package or category
        val grouped = when (type) {
            "item" -> groupByMetadata(components, "itemType")
            "block" -> groupByMetadata(components, "blockType")
            "entity" -> groupByMetadata(components, "entityType")
            "recipe" -> groupByMetadata(components, "recipeType")
            "tag" -> groupByMetadata(components, "tagType")
            "enchantment" -> groupByMetadata(components, "enchantmentType")
            "biome" -> groupByMetadata(components, "biomeCategory")
            else -> mapOf("all" to components)
        }

        grouped.forEach { (category, items) ->
            sb.append("├─ ${category.capitalize()} (${items.size})\n")
            items.forEachIndexed { index, component ->
                val isLast = index == items.size - 1
                val prefix = if (isLast) "└──" else "├──"
                sb.append("│  $prefix ${component.name}")

                val features = mutableListOf<String>()
                if (component.hasTexture) features.add("T")
                if (component.hasModel) features.add("M")
                if (component.hasRecipe) features.add("R")
                if (component.hasLootTable) features.add("L")

                if (features.isNotEmpty()) {
                    sb.append(" [${features.joinToString("")}]")
                }

                sb.append("\n")
            }
        }

        sb.append("\nLegend: T=Texture, M=Model, R=Recipe, L=Loot Table\n")
        sb.append("Total: ${components.size} ${plural}\n")

        return sb.toString()
    }

    private fun groupByMetadata(components: List<ComponentInfo>, key: String): Map<String, List<ComponentInfo>> {
        return components.groupBy { component ->
            (component.metadata[key] as? String) ?: "unknown"
        }
    }

    private fun pluralize(type: String): String {
        return when {
            type.endsWith("y") && !type.endsWith("ey") -> type.dropLast(1) + "ies"
            type.endsWith("s") || type.endsWith("x") || type.endsWith("z") ||
                type.endsWith("ch") || type.endsWith("sh") -> type + "es"
            else -> type + "s"
        }
    }

    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}

/**
 * Get formatter by name
 */
object FormatterFactory {
    fun getFormatter(format: String): OutputFormatter {
        return when (format.lowercase()) {
            "json" -> JsonFormatter()
            "csv" -> CsvFormatter()
            "tree" -> TreeFormatter()
            else -> TableFormatter()
        }
    }
}
