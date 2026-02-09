package dev.dropper.commands

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.float
import com.github.ajalt.clikt.parameters.types.int
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to create a new recipe in the mod
 * Generates recipe JSON files for various recipe types
 */
class CreateRecipeCommand : DropperCommand(
    name = "recipe",
    help = "Create a new recipe JSON file"
) {
    private val name by argument(help = "Recipe name in snake_case (e.g., iron_sword)")
    private val type by option("--type", "-t", help = "Recipe type: crafting, smelting, blasting, smoking, stonecutting, smithing").default("crafting")
    private val shaped by option("--shaped", help = "Use shaped crafting (only for crafting type)").default("true")
    private val shapeless by option("--shapeless", help = "Use shapeless crafting (only for crafting type)")
    private val experience by option("--experience", "-e", help = "Experience reward (for smelting types)").float().default(0.1f)
    private val cookingTime by option("--cooking-time", "-c", help = "Cooking time in ticks (for smelting types)").int().default(200)

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

        // Determine if shapeless for crafting
        val isShaped = if (shapeless != null) false else shaped == "true"

        Logger.info("Creating recipe: $name (type: $type)")

        // Generate recipe based on type
        when (type.lowercase()) {
            "crafting" -> {
                if (isShaped) {
                    generateShapedCraftingRecipe(projectDir, name, modId)
                } else {
                    generateShapelessCraftingRecipe(projectDir, name, modId)
                }
            }
            "smelting" -> generateSmeltingRecipe(projectDir, name, modId, "minecraft:smelting", experience, cookingTime)
            "blasting" -> generateSmeltingRecipe(projectDir, name, modId, "minecraft:blasting", experience, cookingTime / 2)
            "smoking" -> generateSmeltingRecipe(projectDir, name, modId, "minecraft:smoking", experience, cookingTime / 2)
            "stonecutting" -> generateStonecuttingRecipe(projectDir, name, modId)
            "smithing" -> generateSmithingRecipe(projectDir, name, modId)
            else -> {
                Logger.error("Unknown recipe type: $type")
                Logger.info("Valid types: crafting, smelting, blasting, smoking, stonecutting, smithing")
                return
            }
        }

        Logger.success("Recipe '$name' created successfully!")
        Logger.info("Location: versions/shared/v1/data/$modId/recipe/$name.json")
        Logger.info("Next steps:")
        Logger.info("  1. Edit the recipe file to customize ingredients and results")
        Logger.info("  2. Build with: dropper build")
    }

    private fun extractModId(configFile: File): String? {
        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
    }

    private fun generateShapedCraftingRecipe(projectDir: File, recipeName: String, modId: String) {
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
                "id": "$modId:$recipeName",
                "count": 1
              }
            }
        """.trimIndent()

        val recipeFile = File(projectDir, "versions/shared/v1/data/$modId/recipe/$recipeName.json")
        FileUtil.writeText(recipeFile, recipeContent)

        Logger.info("  ✓ Created shaped crafting recipe: versions/shared/v1/data/$modId/recipe/$recipeName.json")
    }

    private fun generateShapelessCraftingRecipe(projectDir: File, recipeName: String, modId: String) {
        val recipeContent = """
            {
              "type": "minecraft:crafting_shapeless",
              "ingredients": [
                {
                  "item": "minecraft:iron_ingot"
                },
                {
                  "item": "minecraft:stick"
                },
                {
                  "item": "minecraft:stick"
                }
              ],
              "result": {
                "id": "$modId:$recipeName",
                "count": 1
              }
            }
        """.trimIndent()

        val recipeFile = File(projectDir, "versions/shared/v1/data/$modId/recipe/$recipeName.json")
        FileUtil.writeText(recipeFile, recipeContent)

        Logger.info("  ✓ Created shapeless crafting recipe: versions/shared/v1/data/$modId/recipe/$recipeName.json")
    }

    private fun generateSmeltingRecipe(
        projectDir: File,
        recipeName: String,
        modId: String,
        recipeType: String,
        exp: Float,
        time: Int
    ) {
        val recipeContent = """
            {
              "type": "$recipeType",
              "ingredient": {
                "item": "minecraft:iron_ore"
              },
              "result": {
                "id": "$modId:$recipeName"
              },
              "experience": $exp,
              "cookingtime": $time
            }
        """.trimIndent()

        val recipeFile = File(projectDir, "versions/shared/v1/data/$modId/recipe/$recipeName.json")
        FileUtil.writeText(recipeFile, recipeContent)

        Logger.info("  ✓ Created $recipeType recipe: versions/shared/v1/data/$modId/recipe/$recipeName.json")
        Logger.info("    - Experience: $exp")
        Logger.info("    - Cooking time: $time ticks")
    }

    private fun generateStonecuttingRecipe(projectDir: File, recipeName: String, modId: String) {
        val recipeContent = """
            {
              "type": "minecraft:stonecutting",
              "ingredient": {
                "item": "minecraft:stone"
              },
              "result": {
                "id": "$modId:$recipeName",
                "count": 1
              }
            }
        """.trimIndent()

        val recipeFile = File(projectDir, "versions/shared/v1/data/$modId/recipe/$recipeName.json")
        FileUtil.writeText(recipeFile, recipeContent)

        Logger.info("  ✓ Created stonecutting recipe: versions/shared/v1/data/$modId/recipe/$recipeName.json")
    }

    private fun generateSmithingRecipe(projectDir: File, recipeName: String, modId: String) {
        val recipeContent = """
            {
              "type": "minecraft:smithing_transform",
              "template": {
                "item": "minecraft:netherite_upgrade_smithing_template"
              },
              "base": {
                "item": "minecraft:diamond_sword"
              },
              "addition": {
                "item": "minecraft:netherite_ingot"
              },
              "result": {
                "id": "$modId:$recipeName"
              }
            }
        """.trimIndent()

        val recipeFile = File(projectDir, "versions/shared/v1/data/$modId/recipe/$recipeName.json")
        FileUtil.writeText(recipeFile, recipeContent)

        Logger.info("  ✓ Created smithing recipe: versions/shared/v1/data/$modId/recipe/$recipeName.json")
    }
}
