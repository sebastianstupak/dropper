package dev.dropper.commands

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
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
        generateEnchantmentClass(projectDir, name, modId, sanitizedModId, maxLevelInt, rarity, category, treasure)

        // Generate loader-specific registration
        generateFabricRegistration(projectDir, name, modId, sanitizedModId)
        generateForgeRegistration(projectDir, name, modId, sanitizedModId)
        generateNeoForgeRegistration(projectDir, name, modId, sanitizedModId)

        // Generate lang entries
        generateLangEntries(projectDir, name, modId)

        Logger.success("Enchantment '$name' created successfully!")
        Logger.info("Next steps:")
        Logger.info("  1. Customize enchantment logic in shared/common/src/main/java/com/$sanitizedModId/enchantments/${toClassName(name)}.java")
        Logger.info("  2. Update lang entry in versions/shared/v1/assets/$modId/lang/en_us.json")
        Logger.info("  3. Build with: dropper build")
    }

    private fun extractModId(configFile: File): String? {
        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
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
        rarity: String,
        category: String,
        treasure: Boolean
    ) {
        val className = toClassName(enchantmentName)
        val packageName = "com.$sanitizedModId.enchantments"

        val content = """
            package $packageName;

            /**
             * Custom enchantment: $className
             *
             * Registration pattern for multi-loader compatibility:
             * - Fabric: Use Enchantment class with custom properties
             * - Forge/NeoForge: Use Enchantment class with properties
             *
             * This base class provides the shared logic.
             * Loader-specific registration happens in platform code.
             */
            public class $className {
                public static final String ID = "$enchantmentName";

                // Enchantment configuration
                public static final int MAX_LEVEL = $maxLevel;
                public static final Rarity RARITY = Rarity.${rarity.uppercase()};
                public static final Category CATEGORY = Category.${category.uppercase()};
                public static final boolean IS_TREASURE = $treasure;
                public static final boolean IS_CURSE = false;
                public static final boolean IS_TRADEABLE = ${!treasure};
                public static final boolean IS_DISCOVERABLE = ${!treasure};

                // TODO: Implement enchantment logic
                // Example for Fabric:
                // public static final Enchantment INSTANCE = new Enchantment(
                //     Enchantment.definition(
                //         RegistryKey.of(RegistryKeys.ITEM, new Identifier("$modId", ID)),
                //         EnchantmentTarget.$category,
                //         RARITY,
                //         MAX_LEVEL,
                //         Enchantment.constantCost(1),
                //         Enchantment.constantCost(50),
                //         1
                //     )
                // ) {
                //     @Override
                //     public boolean canApplyAtEnchantingTable(ItemStack stack) {
                //         return !IS_TREASURE && super.canApplyAtEnchantingTable(stack);
                //     }
                // };
                //
                // Example for Forge/NeoForge:
                // public static final Enchantment INSTANCE = new Enchantment(
                //     RARITY,
                //     EnchantmentCategory.$category,
                //     EquipmentSlot.values()
                // ) {
                //     @Override
                //     public int getMaxLevel() {
                //         return MAX_LEVEL;
                //     }
                //
                //     @Override
                //     public boolean isTreasureOnly() {
                //         return IS_TREASURE;
                //     }
                //
                //     @Override
                //     public boolean isTradeable() {
                //         return IS_TRADEABLE;
                //     }
                //
                //     @Override
                //     public boolean isDiscoverable() {
                //         return IS_DISCOVERABLE;
                //     }
                // };

                /**
                 * Enchantment rarity levels
                 * Maps to Minecraft's enchantment rarity system
                 */
                public enum Rarity {
                    COMMON,
                    UNCOMMON,
                    RARE,
                    VERY_RARE
                }

                /**
                 * Enchantment categories (what items can be enchanted)
                 * Maps to Minecraft's enchantment target system
                 */
                public enum Category {
                    ARMOR,
                    ARMOR_FEET,
                    ARMOR_LEGS,
                    ARMOR_CHEST,
                    ARMOR_HEAD,
                    WEAPON,
                    DIGGER,
                    FISHING_ROD,
                    TRIDENT,
                    BREAKABLE,
                    BOW,
                    WEARABLE,
                    CROSSBOW,
                    VANISHABLE
                }
            }
        """.trimIndent()

        val enchantmentFile = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/enchantments/$className.java")
        FileUtil.writeText(enchantmentFile, content)

        Logger.info("  ✓ Created enchantment: shared/common/src/main/java/com/$sanitizedModId/enchantments/$className.java")
    }

    private fun generateFabricRegistration(projectDir: File, enchantmentName: String, modId: String, sanitizedModId: String) {
        val className = toClassName(enchantmentName)
        val content = """
            package com.$sanitizedModId.platform.fabric;

            import com.$sanitizedModId.enchantments.$className;
            import net.minecraft.enchantment.Enchantment;
            import net.minecraft.registry.Registries;
            import net.minecraft.registry.Registry;
            import net.minecraft.util.Identifier;

            /**
             * Fabric-specific enchantment registration for $className
             */
            public class ${className}Fabric {
                public static void register() {
                    // Example Fabric registration:
                    // Registry.register(
                    //     Registries.ENCHANTMENT,
                    //     Identifier.of("$modId", $className.ID),
                    //     $className.INSTANCE
                    // );
                }
            }
        """.trimIndent()

        val file = File(projectDir, "shared/fabric/src/main/java/com/$sanitizedModId/platform/fabric/${className}Fabric.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created Fabric registration: shared/fabric/src/main/java/com/$sanitizedModId/platform/fabric/${className}Fabric.java")
    }

    private fun generateForgeRegistration(projectDir: File, enchantmentName: String, modId: String, sanitizedModId: String) {
        val className = toClassName(enchantmentName)
        val content = """
            package com.$sanitizedModId.platform.forge;

            import com.$sanitizedModId.enchantments.$className;
            import net.minecraft.world.item.enchantment.Enchantment;
            import net.minecraftforge.registries.DeferredRegister;
            import net.minecraftforge.registries.ForgeRegistries;
            import net.minecraftforge.registries.RegistryObject;

            /**
             * Forge-specific enchantment registration for $className
             */
            public class ${className}Forge {
                // Example Forge registration:
                // public static final DeferredRegister<Enchantment> ENCHANTMENTS =
                //     DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, "$modId");
                //
                // public static final RegistryObject<Enchantment> ${enchantmentName.uppercase()} =
                //     ENCHANTMENTS.register($className.ID, () -> $className.INSTANCE);
            }
        """.trimIndent()

        val file = File(projectDir, "shared/forge/src/main/java/com/$sanitizedModId/platform/forge/${className}Forge.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created Forge registration: shared/forge/src/main/java/com/$sanitizedModId/platform/forge/${className}Forge.java")
    }

    private fun generateNeoForgeRegistration(projectDir: File, enchantmentName: String, modId: String, sanitizedModId: String) {
        val className = toClassName(enchantmentName)
        val content = """
            package com.$sanitizedModId.platform.neoforge;

            import com.$sanitizedModId.enchantments.$className;
            import net.minecraft.core.registries.Registries;
            import net.minecraft.world.item.enchantment.Enchantment;
            import net.neoforged.neoforge.registries.DeferredRegister;
            import net.neoforged.neoforge.registries.DeferredHolder;

            /**
             * NeoForge-specific enchantment registration for $className
             */
            public class ${className}NeoForge {
                // Example NeoForge registration:
                // public static final DeferredRegister<Enchantment> ENCHANTMENTS =
                //     DeferredRegister.create(Registries.ENCHANTMENT, "$modId");
                //
                // public static final DeferredHolder<Enchantment, Enchantment> ${enchantmentName.uppercase()} =
                //     ENCHANTMENTS.register($className.ID, () -> $className.INSTANCE);
            }
        """.trimIndent()

        val file = File(projectDir, "shared/neoforge/src/main/java/com/$sanitizedModId/platform/neoforge/${className}NeoForge.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created NeoForge registration: shared/neoforge/src/main/java/com/$sanitizedModId/platform/neoforge/${className}NeoForge.java")
    }

    private fun generateLangEntries(projectDir: File, enchantmentName: String, modId: String) {
        val langFile = File(projectDir, "versions/shared/v1/assets/$modId/lang/en_us.json")
        val className = toClassName(enchantmentName)
        val displayName = enchantmentName.split("_").joinToString(" ") { it.capitalize() }

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

    private fun toClassName(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { it.capitalize() }
    }
}
