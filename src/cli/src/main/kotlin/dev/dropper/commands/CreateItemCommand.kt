package dev.dropper.commands

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import dev.dropper.util.MinecraftVersions
import dev.dropper.util.StringUtil
import dev.dropper.util.ValidationUtil
import java.io.File

/**
 * Command to create a new item in the mod
 * Generates registration code, assets (model, texture), and optional recipe
 */
class CreateItemCommand : DropperCommand(
    name = "item",
    help = "Create a new item with registration code and assets"
) {
    private val name by argument(help = "Item name in snake_case (e.g., ruby_sword)")
    private val type by option("--type", "-t", help = "Item type (basic, tool, food)").default("basic")
    private val recipe by option("--recipe", "-r", help = "Generate recipe").default("true")

    override fun run() {
        // Validate item name
        val nameValidation = ValidationUtil.validateName(name, "Item name")
        if (!nameValidation.isValid) {
            ValidationUtil.exitWithError(nameValidation)
            return
        }

        // Validate we're in a Dropper project
        val projectValidation = ValidationUtil.validateDropperProject(projectDir)
        if (!projectValidation.isValid) {
            ValidationUtil.exitWithError(projectValidation)
            return
        }

        val configFile = getConfigFile()

        // Read mod ID from config
        val modId = extractModId(configFile)
        if (modId == null) {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        // Read minecraft versions from config for version-aware generation
        val minecraftVersions = extractMinecraftVersions(configFile)
        val primaryVersion = minecraftVersions.firstOrNull() ?: "1.20.1"

        // Sanitize mod ID for package names (remove hyphens and underscores)
        val sanitizedModId = FileUtil.sanitizeModId(modId)

        // Check for duplicates
        val duplicateCheck = ValidationUtil.checkDuplicate(
            projectDir,
            "item",
            name,
            listOf("shared/common/src/main/java", "shared/fabric/src/main/java", "shared/forge/src/main/java", "shared/neoforge/src/main/java")
        )
        if (!duplicateCheck.isValid) {
            ValidationUtil.exitWithError(duplicateCheck)
            Logger.warn("Item was not created to avoid overwriting existing files")
            return
        }

        Logger.info("Creating item: $name")

        // Generate common item class (shared across all loaders)
        generateItemRegistration(projectDir, name, modId, sanitizedModId, type, primaryVersion)

        // Generate/update registry class using Architectury DeferredRegister
        generateOrUpdateModItems(projectDir, name, modId, sanitizedModId, type)

        // Update main mod class init() to call ModItems.init()
        updateMainModInit(projectDir, sanitizedModId, "ModItems")

        // Generate assets
        generateItemAssets(projectDir, name, modId)

        // Generate recipe if requested
        val recipeDir = MinecraftVersions.recipeDir(primaryVersion)
        if (recipe == "true") {
            generateItemRecipe(projectDir, name, modId, recipeDir)
        }

        Logger.success("Item '$name' created successfully!")
        Logger.info("Next steps:")
        Logger.info("  1. Add texture: versions/shared/v1/assets/$modId/textures/item/$name.png")
        Logger.info("  2. Customize recipe: versions/shared/v1/data/$modId/$recipeDir/$name.json")
        Logger.info("  3. Build with: dropper build")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun generateItemRegistration(projectDir: File, itemName: String, modId: String, sanitizedModId: String, type: String, mcVersion: String) {
        val className = toClassName(itemName)

        val content = when (type) {
            "tool" -> generateToolItem(className, itemName, sanitizedModId, mcVersion)
            "food" -> generateFoodItem(className, itemName, sanitizedModId, mcVersion)
            else -> generateBasicItem(className, itemName, sanitizedModId)
        }

        val itemFile = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/items/$className.java")
        FileUtil.writeText(itemFile, content)

        Logger.info("  ✓ Created registration: shared/common/src/main/java/com/$sanitizedModId/items/$className.java")
    }

    private fun generateBasicItem(className: String, itemName: String, sanitizedModId: String): String {
        return """
            package com.$sanitizedModId.items;

            import net.minecraft.world.item.Item;

            /**
             * Custom item: $className
             *
             * This base class provides the shared item definition.
             * Loader-specific registration happens in platform code.
             */
            public class $className extends Item {
                public static final String ID = "$itemName";

                public $className(Properties properties) {
                    super(properties);
                }

                public $className() {
                    super(new Properties());
                }
            }
        """.trimIndent()
    }

    private fun generateToolItem(className: String, itemName: String, sanitizedModId: String, mcVersion: String): String {
        // 1.21+ uses SwordItem(Tier, Properties.attributes(...))
        // 1.20.x uses SwordItem(Tier, int, float, Properties)
        val defaultConstructor = if (MinecraftVersions.usesNewSwordConstructor(mcVersion)) {
            """
                public $className() {
                    super(TIER, new Properties()
                            .attributes(SwordItem.createAttributes(TIER, ATTACK_DAMAGE_BONUS, ATTACK_SPEED)));
                }"""
        } else {
            """
                public $className() {
                    super(TIER, ATTACK_DAMAGE_BONUS, ATTACK_SPEED, new Properties());
                }"""
        }

        return """
            package com.$sanitizedModId.items;

            import net.minecraft.world.item.SwordItem;
            import net.minecraft.world.item.Tier;
            import net.minecraft.world.item.Tiers;

            /**
             * Tool item: $className
             *
             * Extends SwordItem with configurable tier and properties.
             * Loader-specific registration happens in platform code.
             */
            public class $className extends SwordItem {
                public static final String ID = "$itemName";
                public static final Tier TIER = Tiers.IRON;
                public static final int ATTACK_DAMAGE_BONUS = 3;
                public static final float ATTACK_SPEED = -2.4f;

                public $className(Tier tier, Properties properties) {
                    super(tier, properties);
                }
            $defaultConstructor
            }
        """.trimIndent()
    }

    @Suppress("UNUSED_PARAMETER")
    private fun generateFoodItem(className: String, itemName: String, sanitizedModId: String, mcVersion: String): String {
        return """
            package com.$sanitizedModId.items;

            import net.minecraft.world.food.FoodProperties;
            import net.minecraft.world.item.Item;

            /**
             * Food item: $className
             *
             * Provides configurable nutrition and saturation.
             * Loader-specific registration happens in platform code.
             */
            public class $className extends Item {
                public static final String ID = "$itemName";

                public static final FoodProperties FOOD_PROPERTIES = new FoodProperties.Builder()
                        .nutrition(4)
                        .saturationModifier(0.3f)
                        .alwaysEdible()
                        .build();

                public $className(Properties properties) {
                    super(properties);
                }

                public $className() {
                    super(new Properties().food(FOOD_PROPERTIES));
                }
            }
        """.trimIndent()
    }

    /**
     * Generate or update the common ModItems registry class using Architectury DeferredRegister.
     * This replaces per-loader registration files with a single cross-platform registry.
     */
    private fun generateOrUpdateModItems(
        projectDir: File,
        itemName: String,
        modId: String,
        sanitizedModId: String,
        type: String
    ) {
        val className = toClassName(itemName)
        val constantName = itemName.uppercase()
        val registryFile = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/registry/ModItems.java")

        if (registryFile.exists()) {
            // Append new item to existing registry
            val existingContent = registryFile.readText()

            val newField = "    public static final RegistrySupplier<Item> $constantName = ITEMS.register(\"$itemName\", () -> new ${className}());"
            val newImport = "import com.$sanitizedModId.items.$className;"

            // Insert field before init() method
            val updatedContent = existingContent
                .replace("    public static void init()", "$newField\n\n    public static void init()")
                .let { content ->
                    // Add import if not present
                    if (!content.contains(newImport)) {
                        content.replace("import net.minecraft.world.item.Item;", "import net.minecraft.world.item.Item;\nimport com.$sanitizedModId.items.$className;")
                    } else content
                }

            FileUtil.writeText(registryFile, updatedContent)
            Logger.info("  ✓ Added $className to registry/ModItems.java")
        } else {
            // Create new registry file
            val content = """
                package com.$sanitizedModId.registry;

                import com.$sanitizedModId.items.$className;
                import dev.architectury.registry.registries.DeferredRegister;
                import dev.architectury.registry.registries.RegistrySupplier;
                import net.minecraft.core.registries.Registries;
                import net.minecraft.world.item.Item;

                public class ModItems {
                    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create("$modId", Registries.ITEM);

                    public static final RegistrySupplier<Item> $constantName = ITEMS.register("$itemName", () -> new ${className}());

                    public static void init() {
                        ITEMS.register();
                    }
                }
            """.trimIndent()

            FileUtil.writeText(registryFile, content)
            Logger.info("  ✓ Created registry/ModItems.java with $className")
        }
    }

    private fun generateItemAssets(projectDir: File, itemName: String, modId: String) {
        // Generate item model JSON
        val modelContent = """
            {
              "parent": "item/generated",
              "textures": {
                "layer0": "$modId:item/$itemName"
              }
            }
        """.trimIndent()

        val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/item/$itemName.json")
        FileUtil.writeText(modelFile, modelContent)

        // Create placeholder texture file
        val textureFile = File(projectDir, "versions/shared/v1/assets/$modId/textures/item/$itemName.png")
        textureFile.parentFile.mkdirs()
        textureFile.createNewFile()

        Logger.info("  ✓ Created model: versions/shared/v1/assets/$modId/models/item/$itemName.json")
        Logger.info("  ✓ Created placeholder texture: versions/shared/v1/assets/$modId/textures/item/$itemName.png")
    }

    private fun generateItemRecipe(projectDir: File, itemName: String, modId: String, recipeDir: String) {
        val recipeContent = """
            {
              "type": "minecraft:crafting_shaped",
              "pattern": [
                "###",
                " S ",
                " S "
              ],
              "key": {
                "#": {
                  "item": "minecraft:iron_ingot"
                },
                "S": {
                  "item": "minecraft:stick"
                }
              },
              "result": {
                "id": "$modId:$itemName"
              }
            }
        """.trimIndent()

        val recipeFile = File(projectDir, "versions/shared/v1/data/$modId/$recipeDir/$itemName.json")
        FileUtil.writeText(recipeFile, recipeContent)

        Logger.info("  ✓ Created recipe: versions/shared/v1/data/$modId/$recipeDir/$itemName.json")
    }

    private fun toClassName(snakeCase: String): String = StringUtil.toClassName(snakeCase)
}
