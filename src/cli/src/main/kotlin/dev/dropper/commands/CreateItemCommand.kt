package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to create a new item in the mod
 * Generates registration code, assets (model, texture), and optional recipe
 */
class CreateItemCommand : CliktCommand(
    name = "item",
    help = "Create a new item with registration code and assets"
) {
    private val name by argument(help = "Item name in snake_case (e.g., ruby_sword)")
    private val type by option("--type", "-t", help = "Item type (basic, tool, food)").default("basic")
    private val recipe by option("--recipe", "-r", help = "Generate recipe").default("true")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

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

        Logger.info("Creating item: $name")

        // Generate item registration code
        generateItemRegistration(projectDir, name, modId, type)

        // Generate assets
        generateItemAssets(projectDir, name, modId)

        // Generate recipe if requested
        if (recipe == "true") {
            generateItemRecipe(projectDir, name, modId)
        }

        Logger.success("Item '$name' created successfully!")
        Logger.info("Next steps:")
        Logger.info("  1. Add texture: versions/shared/v1/assets/$modId/textures/item/$name.png")
        Logger.info("  2. Customize recipe: versions/shared/v1/data/$modId/recipe/$name.json")
        Logger.info("  3. Register in ModItems class initialization")
    }

    private fun extractModId(configFile: File): String? {
        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
    }

    private fun generateItemRegistration(projectDir: File, itemName: String, modId: String, type: String) {
        val className = toClassName(itemName)
        val packageName = "com.$modId.items"

        val content = when (type) {
            "tool" -> generateToolItem(className, itemName)
            "food" -> generateFoodItem(className, itemName)
            else -> generateBasicItem(className, itemName)
        }

        val itemFile = File(projectDir, "shared/common/src/main/java/com/$modId/items/$className.java")
        FileUtil.writeText(itemFile, content)

        Logger.info("  ✓ Created registration: shared/common/src/main/java/com/$modId/items/$className.java")
    }

    private fun generateBasicItem(className: String, itemName: String): String {
        return """
            package com.$modId.items;

            /**
             * Custom item: $className
             *
             * Registration pattern for multi-loader compatibility:
             * - Fabric: Use FabricItemSettings or Item.Settings
             * - Forge/NeoForge: Use Item.Properties
             *
             * This base class provides the shared logic.
             * Loader-specific registration happens in platform code.
             */
            public class $className {
                public static final String ID = "$itemName";

                // TODO: Implement item logic
                // Example:
                // public static final Item INSTANCE = new Item(new Item.Settings());
                //
                // For tools:
                // public static final Item INSTANCE = new SwordItem(
                //     ToolMaterial.IRON, new Item.Settings().attributeModifiers(...)
                // );
                //
                // For food:
                // public static final Item INSTANCE = new Item(
                //     new Item.Settings().food(new FoodComponent.Builder()
                //         .nutrition(4).saturationModifier(0.3f).build())
                // );
            }
        """.trimIndent()
    }

    private fun generateToolItem(className: String, itemName: String): String {
        return """
            package com.$modId.items;

            /**
             * Tool item: $className
             *
             * TODO: Extend appropriate tool class (SwordItem, AxeItem, etc.)
             * and implement tool-specific logic.
             */
            public class $className {
                public static final String ID = "$itemName";

                // Example for sword:
                // public static final Item INSTANCE = new SwordItem(
                //     CustomToolMaterial.INSTANCE,
                //     new Item.Settings().attributeModifiers(
                //         SwordItem.createAttributeModifiers(
                //             CustomToolMaterial.INSTANCE, 3, -2.4f
                //         )
                //     )
                // );
            }
        """.trimIndent()
    }

    private fun generateFoodItem(className: String, itemName: String): String {
        return """
            package com.$modId.items;

            /**
             * Food item: $className
             */
            public class $className {
                public static final String ID = "$itemName";

                // Example:
                // public static final Item INSTANCE = new Item(
                //     new Item.Settings().food(new FoodComponent.Builder()
                //         .nutrition(4)
                //         .saturationModifier(0.3f)
                //         .alwaysEdible()
                //         .build())
                // );
            }
        """.trimIndent()
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

    private fun generateItemRecipe(projectDir: File, itemName: String, modId: String) {
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

        val recipeFile = File(projectDir, "versions/shared/v1/data/$modId/recipe/$itemName.json")
        FileUtil.writeText(recipeFile, recipeContent)

        Logger.info("  ✓ Created recipe: versions/shared/v1/data/$modId/recipe/$itemName.json")
    }

    private fun toClassName(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { it.capitalize() }
    }
}
