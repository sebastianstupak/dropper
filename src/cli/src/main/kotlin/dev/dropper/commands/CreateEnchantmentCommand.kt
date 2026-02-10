package dev.dropper.commands

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import dev.dropper.util.StringUtil
import dev.dropper.util.ValidationUtil
import java.io.File

/**
 * Command to create a new enchantment in the mod
 * Generates registration code for all loaders and lang entries
 */
class CreateEnchantmentCommand : DropperCommand(
    name = "enchantment",
    help = "Create a new enchantment with registration code and lang entries"
) {
    private val name by argument(help = "Enchantment name in snake_case (e.g., fire_aspect)")
    private val maxLevel by option("--max-level", help = "Maximum enchantment level (default: 1)").default("1")
    private val rarity by option("--rarity", help = "Enchantment rarity: common, uncommon, rare, very_rare (default: common)").default("common")
    private val category by option("--category", help = "Enchantment category: armor, armor_feet, armor_legs, armor_chest, armor_head, weapon, digger, fishing_rod, trident, breakable, bow, wearable, crossbow, vanishable (default: breakable)").default("breakable")
    private val treasure by option("--treasure", help = "Treasure-only enchantment (not available in enchanting table)").flag(default = false)

    override fun run() {
        // Validate enchantment name
        val nameValidation = ValidationUtil.validateName(name, "enchantment")
        if (!nameValidation.isValid) {
            ValidationUtil.exitWithError(nameValidation)
            return
        }

        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        // Read mod ID from config
        val modId = extractModId(configFile)
        if (modId == null) {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        // Sanitize mod ID for package names (remove hyphens and underscores)
        val sanitizedModId = FileUtil.sanitizeModId(modId)

        // Validate inputs
        val maxLevelInt = maxLevel.toIntOrNull()
        if (maxLevelInt == null || maxLevelInt < 1) {
            Logger.error("Invalid max level: $maxLevel (must be a positive integer)")
            return
        }

        if (!isValidRarity(rarity)) {
            Logger.error("Invalid rarity: $rarity (must be: common, uncommon, rare, very_rare)")
            return
        }

        if (!isValidCategory(category)) {
            Logger.error("Invalid category: $category")
            Logger.error("Valid categories: armor, armor_feet, armor_legs, armor_chest, armor_head, weapon, digger, fishing_rod, trident, breakable, bow, wearable, crossbow, vanishable")
            return
        }

        Logger.info("Creating enchantment: $name")

        // Generate common enchantment code (shared across all loaders)
        generateEnchantmentClass(projectDir, name, modId, sanitizedModId, maxLevelInt, treasure)

        // Generate enchantment data JSON
        generateEnchantmentJson(projectDir, name, modId, maxLevelInt, rarity, category)

        // Generate lang entries
        generateLangEntries(projectDir, name, modId)

        Logger.success("Enchantment '$name' created successfully!")
        Logger.info("Next steps:")
        Logger.info("  1. Customize enchantment constants in shared/common/src/main/java/com/$sanitizedModId/enchantments/${toClassName(name)}.java")
        Logger.info("  2. Customize enchantment JSON in versions/shared/v1/data/$modId/enchantment/$name.json")
        Logger.info("  3. Update lang entry in versions/shared/v1/assets/$modId/lang/en_us.json")
        Logger.info("  4. Build with: dropper build")
    }

    private fun isValidRarity(rarity: String): Boolean {
        return rarity in listOf("common", "uncommon", "rare", "very_rare")
    }

    private fun isValidCategory(category: String): Boolean {
        return category in listOf(
            "armor", "armor_feet", "armor_legs", "armor_chest", "armor_head",
            "weapon", "digger", "fishing_rod", "trident", "breakable",
            "bow", "wearable", "crossbow", "vanishable"
        )
    }

    private fun generateEnchantmentClass(
        projectDir: File,
        enchantmentName: String,
        modId: String,
        sanitizedModId: String,
        maxLevel: Int,
        treasure: Boolean
    ) {
        val className = toClassName(enchantmentName)
        val packageName = "com.$sanitizedModId.enchantments"

        val content = """
            package $packageName;

            /**
             * Enchantment definition: $className
             *
             * Holds configuration constants for the custom enchantment.
             * In modern Minecraft (1.20+), enchantments are data-driven.
             * The actual enchantment is defined in:
             *   data/$modId/enchantment/$enchantmentName.json
             *
             * Loader-specific registration is handled via platform code.
             */
            public class $className {
                public static final String ID = "$enchantmentName";

                // Enchantment configuration
                public static final int MAX_LEVEL = $maxLevel;
                public static final boolean IS_TREASURE = $treasure;
                public static final boolean IS_TRADEABLE = ${!treasure};
                public static final boolean IS_DISCOVERABLE = ${!treasure};

                /**
                 * Minimum XP cost for this enchantment at the given level.
                 */
                public static int getMinCost(int level) {
                    return 1 + (level - 1) * 10;
                }

                /**
                 * Maximum XP cost for this enchantment at the given level.
                 */
                public static int getMaxCost(int level) {
                    return getMinCost(level) + 50;
                }
            }
        """.trimIndent()

        val enchantmentFile = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/enchantments/$className.java")
        FileUtil.writeText(enchantmentFile, content)

        Logger.info("  ✓ Created enchantment: shared/common/src/main/java/com/$sanitizedModId/enchantments/$className.java")
    }

    private fun rarityToWeight(rarity: String): Int {
        return when (rarity) {
            "common" -> 10
            "uncommon" -> 5
            "rare" -> 2
            "very_rare" -> 1
            else -> 10
        }
    }

    private fun generateEnchantmentJson(
        projectDir: File,
        enchantmentName: String,
        modId: String,
        maxLevel: Int,
        rarity: String,
        category: String
    ) {
        val weight = rarityToWeight(rarity)
        val slots = when (category) {
            "armor" -> """["head", "chest", "legs", "feet"]"""
            "armor_feet" -> """["feet"]"""
            "armor_legs" -> """["legs"]"""
            "armor_chest" -> """["chest"]"""
            "armor_head" -> """["head"]"""
            "weapon" -> """["mainhand"]"""
            "digger" -> """["mainhand"]"""
            "fishing_rod" -> """["mainhand"]"""
            "trident" -> """["mainhand"]"""
            "bow" -> """["mainhand"]"""
            "crossbow" -> """["mainhand"]"""
            else -> """["any"]"""
        }
        val supportedItems = when (category) {
            "armor", "armor_feet", "armor_legs", "armor_chest", "armor_head" -> "#minecraft:enchantable/armor"
            "weapon" -> "#minecraft:enchantable/weapon"
            "digger" -> "#minecraft:enchantable/mining"
            "fishing_rod" -> "#minecraft:enchantable/fishing"
            "trident" -> "#minecraft:enchantable/trident"
            "bow" -> "#minecraft:enchantable/bow"
            "crossbow" -> "#minecraft:enchantable/crossbow"
            else -> "#minecraft:enchantable/durability"
        }

        val content = """
            {
              "supported_items": "$supportedItems",
              "weight": $weight,
              "max_level": $maxLevel,
              "min_cost": {
                "base": 1,
                "per_level_above_first": 10
              },
              "max_cost": {
                "base": 51,
                "per_level_above_first": 10
              },
              "slots": $slots
            }
        """.trimIndent()

        val jsonFile = File(projectDir, "versions/shared/v1/data/$modId/enchantment/$enchantmentName.json")
        FileUtil.writeText(jsonFile, content)

        Logger.info("  ✓ Created enchantment JSON: versions/shared/v1/data/$modId/enchantment/$enchantmentName.json")
    }

    private fun generateLangEntries(projectDir: File, enchantmentName: String, modId: String) {
        val langFile = File(projectDir, "versions/shared/v1/assets/$modId/lang/en_us.json")
        val displayName = enchantmentName.split("_").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }

        // Create or update lang file
        val existingContent = if (langFile.exists()) {
            langFile.readText()
        } else {
            "{}"
        }

        // Parse JSON and add enchantment entry
        val langKey = "enchantment.$modId.$enchantmentName"
        val updatedContent = addJsonEntry(existingContent, langKey, displayName)

        FileUtil.writeText(langFile, updatedContent)

        Logger.info("  ✓ Added lang entry: versions/shared/v1/assets/$modId/lang/en_us.json")
        Logger.info("    → $langKey: $displayName")
    }

    private fun addJsonEntry(json: String, key: String, value: String): String {
        // Simple JSON manipulation - parse and add entry
        val trimmed = json.trim()
        if (trimmed == "{}") {
            return """
                {
                  "$key": "$value"
                }
            """.trimIndent()
        }

        // Remove closing brace, add entry, add closing brace
        val withoutClosing = trimmed.trimEnd().trimEnd('}')
        val needsComma = withoutClosing.trim().isNotEmpty() && !withoutClosing.trim().endsWith("{")

        return if (needsComma) {
            """
                $withoutClosing,
                  "$key": "$value"
                }
            """.trimIndent()
        } else {
            """
                $withoutClosing
                  "$key": "$value"
                }
            """.trimIndent()
        }
    }

    private fun toClassName(snakeCase: String): String = StringUtil.toClassName(snakeCase)
}
