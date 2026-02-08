package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to create a new block in the mod
 * Generates registration code, block states, models, and textures
 */
class CreateBlockCommand : CliktCommand(
    name = "block",
    help = "Create a new block with registration code, blockstates, and assets"
) {
    private val name by argument(help = "Block name in snake_case (e.g., ruby_ore)")
    private val type by option("--type", "-t", help = "Block type (basic, ore, pillar)").default("basic")
    private val dropsSelf by option("--drops-self", "-d", help = "Block drops itself").default("true")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val modId = extractModId(configFile)
        if (modId == null) {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        Logger.info("Creating block: $name")

        // Generate block registration code
        generateBlockRegistration(projectDir, name, modId, type)

        // Generate blockstates
        generateBlockState(projectDir, name, modId, type)

        // Generate block models
        generateBlockModels(projectDir, name, modId, type)

        // Generate item model (for inventory)
        generateBlockItemModel(projectDir, name, modId)

        // Generate loot table if drops self
        if (dropsSelf == "true") {
            generateLootTable(projectDir, name, modId)
        }

        Logger.success("Block '$name' created successfully!")
        Logger.info("Next steps:")
        Logger.info("  1. Add texture: versions/shared/v1/assets/$modId/textures/block/$name.png")
        Logger.info("  2. Customize blockstate: versions/shared/v1/assets/$modId/blockstates/$name.json")
        Logger.info("  3. Register in ModBlocks class initialization")
    }

    private fun extractModId(configFile: File): String? {
        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
    }

    private fun generateBlockRegistration(projectDir: File, blockName: String, modId: String, type: String) {
        val className = toClassName(blockName)
        val content = """
            package com.$modId.blocks;

            /**
             * Custom block: $className
             *
             * Registration pattern for multi-loader compatibility:
             * - Fabric: Use FabricBlockSettings or AbstractBlock.Settings
             * - Forge/NeoForge: Use BlockBehaviour.Properties
             *
             * This base class provides the shared logic.
             * Loader-specific registration happens in platform code.
             */
            public class $className {
                public static final String ID = "$blockName";

                // TODO: Implement block logic
                // Example for basic block:
                // public static final Block INSTANCE = new Block(
                //     AbstractBlock.Settings.create()
                //         .strength(3.0f, 3.0f)
                //         .sounds(BlockSoundGroup.STONE)
                // );
                //
                // For ore blocks:
                // public static final Block INSTANCE = new Block(
                //     AbstractBlock.Settings.create()
                //         .strength(3.0f, 3.0f)
                //         .requiresTool()
                //         .sounds(BlockSoundGroup.STONE)
                // );
            }
        """.trimIndent()

        val blockFile = File(projectDir, "shared/common/src/main/java/com/$modId/blocks/$className.java")
        FileUtil.writeText(blockFile, content)

        Logger.info("  ✓ Created registration: shared/common/src/main/java/com/$modId/blocks/$className.java")
    }

    private fun generateBlockState(projectDir: File, blockName: String, modId: String, type: String) {
        val content = when (type) {
            "pillar" -> """
                {
                  "variants": {
                    "axis=x": {
                      "model": "$modId:block/$blockName",
                      "x": 90,
                      "y": 90
                    },
                    "axis=y": {
                      "model": "$modId:block/$blockName"
                    },
                    "axis=z": {
                      "model": "$modId:block/$blockName",
                      "x": 90
                    }
                  }
                }
            """.trimIndent()
            else -> """
                {
                  "variants": {
                    "": {
                      "model": "$modId:block/$blockName"
                    }
                  }
                }
            """.trimIndent()
        }

        val blockStateFile = File(projectDir, "versions/shared/v1/assets/$modId/blockstates/$blockName.json")
        FileUtil.writeText(blockStateFile, content)

        Logger.info("  ✓ Created blockstate: versions/shared/v1/assets/$modId/blockstates/$blockName.json")
    }

    private fun generateBlockModels(projectDir: File, blockName: String, modId: String, type: String) {
        val content = when (type) {
            "pillar" -> """
                {
                  "parent": "block/cube_column",
                  "textures": {
                    "end": "$modId:block/${blockName}_top",
                    "side": "$modId:block/$blockName"
                  }
                }
            """.trimIndent()
            else -> """
                {
                  "parent": "block/cube_all",
                  "textures": {
                    "all": "$modId:block/$blockName"
                  }
                }
            """.trimIndent()
        }

        val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/block/$blockName.json")
        FileUtil.writeText(modelFile, content)

        Logger.info("  ✓ Created block model: versions/shared/v1/assets/$modId/models/block/$blockName.json")

        // Create texture placeholder
        val textureFile = File(projectDir, "versions/shared/v1/assets/$modId/textures/block/$blockName.png")
        textureFile.parentFile.mkdirs()
        textureFile.createNewFile()

        Logger.info("  ✓ Created placeholder texture: versions/shared/v1/assets/$modId/textures/block/$blockName.png")
    }

    private fun generateBlockItemModel(projectDir: File, blockName: String, modId: String) {
        val content = """
            {
              "parent": "$modId:block/$blockName"
            }
        """.trimIndent()

        val itemModelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/item/$blockName.json")
        FileUtil.writeText(itemModelFile, content)

        Logger.info("  ✓ Created item model: versions/shared/v1/assets/$modId/models/item/$blockName.json")
    }

    private fun generateLootTable(projectDir: File, blockName: String, modId: String) {
        val content = """
            {
              "type": "minecraft:block",
              "pools": [
                {
                  "rolls": 1,
                  "entries": [
                    {
                      "type": "minecraft:item",
                      "name": "$modId:$blockName"
                    }
                  ],
                  "conditions": [
                    {
                      "condition": "minecraft:survives_explosion"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val lootTableFile = File(projectDir, "versions/shared/v1/data/$modId/loot_table/blocks/$blockName.json")
        FileUtil.writeText(lootTableFile, content)

        Logger.info("  ✓ Created loot table: versions/shared/v1/data/$modId/loot_table/blocks/$blockName.json")
    }

    private fun toClassName(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { it.capitalize() }
    }
}
