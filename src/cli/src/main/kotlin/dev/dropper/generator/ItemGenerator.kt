package dev.dropper.generator

import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Generates item code and assets
 */
class ItemGenerator {

    fun generate(projectDir: File, itemName: String, packageName: String, modId: String) {
        Logger.info("Generating item: $itemName")

        val className = itemName.split("_")
            .joinToString("") { word -> word.replaceFirstChar { it.uppercase() } }

        // Generate Java class
        generateItemClass(projectDir, className, packageName, modId)

        // Generate item model
        generateItemModel(projectDir, itemName, modId)

        // Generate texture placeholder
        generateTexturePlaceholder(projectDir, itemName, modId)

        // Generate recipe
        generateRecipe(projectDir, itemName, modId)

        Logger.success("Item '$itemName' generated successfully")
        Logger.info("Add texture at: versions/shared/v1/assets/$modId/textures/item/$itemName.png")
    }

    private fun generateItemClass(projectDir: File, className: String, packageName: String, modId: String) {
        val itemName = className.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()

        val content = """
            package $packageName.items;

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

        // Create proper package directory structure for IntelliJ
        val packagePath = packageName.replace(".", "/")
        val file = File(projectDir, "shared/common/src/main/java/$packagePath/items/$className.java")
        FileUtil.writeText(file, content)
    }

    private fun generateItemModel(projectDir: File, itemName: String, modId: String) {
        val content = """
            {
              "parent": "item/generated",
              "textures": {
                "layer0": "$modId:item/$itemName"
              }
            }
        """.trimIndent()

        val file = File(projectDir, "versions/shared/v1/assets/$modId/models/item/$itemName.json")
        FileUtil.writeText(file, content)
    }

    private fun generateTexturePlaceholder(projectDir: File, itemName: String, modId: String) {
        // Create a simple placeholder file
        val file = File(projectDir, "versions/shared/v1/assets/$modId/textures/item/$itemName.png")
        file.parentFile?.mkdirs()
        file.createNewFile()
        // Note: In a real implementation, we would generate a 16x16 PNG
    }

    private fun generateRecipe(projectDir: File, itemName: String, modId: String) {
        val content = """
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

        val file = File(projectDir, "versions/shared/v1/data/$modId/recipe/$itemName.json")
        FileUtil.writeText(file, content)
    }
}
