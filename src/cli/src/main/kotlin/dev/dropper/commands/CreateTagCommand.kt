package dev.dropper.commands

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to create a new tag in the mod
 * Generates tag JSON files for blocks, items, entity_types, or fluids
 */
class CreateTagCommand : DropperCommand(
    name = "tag",
    help = "Create a new tag with JSON definition"
) {
    private val name by argument(help = "Tag name (e.g., mineable/pickaxe, ores/iron)")
    private val type by option("--type", "-t", help = "Tag type: block, item, entity_type, fluid").required()
    private val values by option("--values", "-v", help = "Comma-separated list of resource locations to include (supports # prefix for tag references)")
    private val replace by option("--replace", "-r", help = "Replace existing tags (default: false)").flag(default = false)

    override fun run() {
        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        // Validate tag type
        val validTypes = listOf("block", "item", "entity_type", "fluid")
        if (type !in validTypes) {
            Logger.error("Invalid tag type: $type. Valid types are: ${validTypes.joinToString(", ")}")
            return
        }

        // Read mod ID from config
        val modId = extractModId(configFile)
        if (modId == null) {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        Logger.info("Creating $type tag: $name")

        // Parse namespace and path from tag name
        val (namespace, tagPath) = parseTagName(name, modId)

        // Parse values
        val tagValues = parseValues(values)

        // Generate tag JSON
        generateTag(projectDir, namespace, tagPath, type, tagValues, replace, modId)

        Logger.success("Tag '$namespace:$tagPath' created successfully!")
        Logger.info("Location: versions/shared/v1/data/$namespace/tags/$type/$tagPath.json")
        Logger.info("Next steps:")
        Logger.info("  1. Add more values to the tag if needed")
        Logger.info("  2. Reference in other tags with #$namespace:$tagPath")
        Logger.info("  3. Build with: dropper build")
    }

    /**
     * Parse tag name into namespace and path
     * Examples:
     *   "mineable/pickaxe" -> ("minecraft", "mineable/pickaxe")
     *   "minecraft:mineable/pickaxe" -> ("minecraft", "mineable/pickaxe")
     *   "forge:ores" -> ("forge", "ores")
     *   "custom_tag" -> (modId, "custom_tag")
     */
    private fun parseTagName(tagName: String, modId: String): Pair<String, String> {
        return if (tagName.contains(":")) {
            val parts = tagName.split(":", limit = 2)
            parts[0] to parts[1]
        } else if (tagName.startsWith("mineable/") || tagName.startsWith("logs")) {
            // Common vanilla tags default to minecraft namespace
            "minecraft" to tagName
        } else if (tagName.startsWith("ores/") || tagName.startsWith("ingots/") || tagName.startsWith("storage_blocks/")) {
            // Common forge tags default to forge namespace
            "forge" to tagName
        } else {
            // Default to mod namespace
            modId to tagName
        }
    }

    /**
     * Parse comma-separated values into list
     * Supports both regular entries (modid:path) and tag references (#modid:path)
     */
    private fun parseValues(valuesString: String?): List<String> {
        if (valuesString.isNullOrBlank()) {
            return emptyList()
        }

        return valuesString.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    /**
     * Generate tag JSON file
     */
    private fun generateTag(
        projectDir: File,
        namespace: String,
        tagPath: String,
        tagType: String,
        values: List<String>,
        replace: Boolean,
        modId: String
    ) {
        val content = buildString {
            appendLine("{")
            if (replace) {
                appendLine("  \"replace\": true,")
            }
            if (values.isEmpty()) {
                appendLine("  \"values\": []")
            } else {
                appendLine("  \"values\": [")
                values.forEachIndexed { index, value ->
                    val quotedValue = "\"$value\""
                    if (index < values.size - 1) {
                        appendLine("    $quotedValue,")
                    } else {
                        appendLine("    $quotedValue")
                    }
                }
                appendLine("  ]")
            }
            append("}")
        }

        val tagFile = File(projectDir, "versions/shared/v1/data/$namespace/tags/$tagType/$tagPath.json")
        FileUtil.writeText(tagFile, content)

        Logger.info("  ✓ Created tag: versions/shared/v1/data/$namespace/tags/$tagType/$tagPath.json")
    }
}
