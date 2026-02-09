package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import dev.dropper.util.ValidationUtil
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
    private val type by option("--type", "-t", help = "Block type: basic, ore, pillar, slab, stairs, fence, wall, door, trapdoor, button, pressure_plate, crop").default("basic")
    private val dropsSelf by option("--drops-self", "-d", help = "Block drops itself").default("true")
    private val maxAge by option("--max-age", help = "Max age for crops (default: 7)").default("7")

    override fun run() {
        // Validate block name
        val nameValidation = ValidationUtil.validateName(name, "Block name")
        if (!nameValidation.isValid) {
            ValidationUtil.exitWithError(nameValidation)
            return
        }

        // Validate we're in a Dropper project
        val projectDir = File(System.getProperty("user.dir"))
        val projectValidation = ValidationUtil.validateDropperProject(projectDir)
        if (!projectValidation.isValid) {
            ValidationUtil.exitWithError(projectValidation)
            return
        }

        val configFile = File(projectDir, "config.yml")

        val modId = extractModId(configFile)
        if (modId == null) {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        // Sanitize mod ID for package names (remove hyphens and underscores)
        val sanitizedModId = FileUtil.sanitizeModId(modId)

        // Check for duplicates
        val duplicateCheck = ValidationUtil.checkDuplicate(
            projectDir,
            "block",
            name,
            listOf("shared/common/src/main/java", "shared/fabric/src/main/java", "shared/forge/src/main/java", "shared/neoforge/src/main/java")
        )
        if (!duplicateCheck.isValid) {
            ValidationUtil.exitWithError(duplicateCheck)
            Logger.warn("Block was not created to avoid overwriting existing files")
            return
        }

        Logger.info("Creating block: $name")

        // Generate common block code (shared across all loaders)
        generateBlockRegistration(projectDir, name, sanitizedModId, type)

        // Generate loader-specific registration
        generateFabricRegistration(projectDir, name, modId, sanitizedModId)
        generateForgeRegistration(projectDir, name, modId, sanitizedModId)
        generateNeoForgeRegistration(projectDir, name, modId, sanitizedModId)

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
        Logger.info("  3. Build with: dropper build")
    }

    private fun extractModId(configFile: File): String? {
        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
    }

    private fun generateBlockRegistration(projectDir: File, blockName: String, sanitizedModId: String, type: String) {
        val className = toClassName(blockName)
        val content = """
            package com.$sanitizedModId.blocks;

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

        val blockFile = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/blocks/$className.java")
        FileUtil.writeText(blockFile, content)

        Logger.info("  ✓ Created registration: shared/common/src/main/java/com/$sanitizedModId/blocks/$className.java")
    }

    private fun generateBlockState(projectDir: File, blockName: String, modId: String, type: String) {
        val content = when (type) {
            "pillar" -> generatePillarBlockState(blockName, modId)
            "slab" -> generateSlabBlockState(blockName, modId)
            "stairs" -> generateStairsBlockState(blockName, modId)
            "fence" -> generateFenceBlockState(blockName, modId)
            "wall" -> generateWallBlockState(blockName, modId)
            "door" -> generateDoorBlockState(blockName, modId)
            "trapdoor" -> generateTrapdoorBlockState(blockName, modId)
            "button" -> generateButtonBlockState(blockName, modId)
            "pressure_plate" -> generatePressurePlateBlockState(blockName, modId)
            "crop" -> generateCropBlockState(blockName, modId, maxAge.toInt())
            else -> generateBasicBlockState(blockName, modId)
        }

        val blockStateFile = File(projectDir, "versions/shared/v1/assets/$modId/blockstates/$blockName.json")
        FileUtil.writeText(blockStateFile, content)

        Logger.info("  ✓ Created blockstate: versions/shared/v1/assets/$modId/blockstates/$blockName.json")
    }

    private fun generateBlockModels(projectDir: File, blockName: String, modId: String, type: String) {
        when (type) {
            "pillar" -> generatePillarModels(projectDir, blockName, modId)
            "slab" -> generateSlabModels(projectDir, blockName, modId)
            "stairs" -> generateStairsModels(projectDir, blockName, modId)
            "fence" -> generateFenceModels(projectDir, blockName, modId)
            "wall" -> generateWallModels(projectDir, blockName, modId)
            "door" -> generateDoorModels(projectDir, blockName, modId)
            "trapdoor" -> generateTrapdoorModels(projectDir, blockName, modId)
            "button" -> generateButtonModels(projectDir, blockName, modId)
            "pressure_plate" -> generatePressurePlateModels(projectDir, blockName, modId)
            "crop" -> generateCropModels(projectDir, blockName, modId, maxAge.toInt())
            else -> generateBasicModels(projectDir, blockName, modId)
        }
    }

    private fun generateBasicModels(projectDir: File, blockName: String, modId: String) {
        val content = """
            {
              "parent": "block/cube_all",
              "textures": {
                "all": "$modId:block/$blockName"
              }
            }
        """.trimIndent()

        val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/block/$blockName.json")
        FileUtil.writeText(modelFile, content)
        Logger.info("  ✓ Created block model: versions/shared/v1/assets/$modId/models/block/$blockName.json")

        createTexturePlaceholder(projectDir, blockName, modId)
    }

    private fun generatePillarModels(projectDir: File, blockName: String, modId: String) {
        val content = """
            {
              "parent": "block/cube_column",
              "textures": {
                "end": "$modId:block/${blockName}_top",
                "side": "$modId:block/$blockName"
              }
            }
        """.trimIndent()

        val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/block/$blockName.json")
        FileUtil.writeText(modelFile, content)
        Logger.info("  ✓ Created block model: versions/shared/v1/assets/$modId/models/block/$blockName.json")

        createTexturePlaceholder(projectDir, blockName, modId)
        createTexturePlaceholder(projectDir, "${blockName}_top", modId)
    }

    private fun generateSlabModels(projectDir: File, blockName: String, modId: String) {
        // Bottom slab
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/$blockName.json"),
            """
            {
              "parent": "block/slab",
              "textures": {
                "bottom": "$modId:block/$blockName",
                "top": "$modId:block/$blockName",
                "side": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        // Top slab
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_top.json"),
            """
            {
              "parent": "block/slab_top",
              "textures": {
                "bottom": "$modId:block/$blockName",
                "top": "$modId:block/$blockName",
                "side": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        // Double slab
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_double.json"),
            """
            {
              "parent": "block/cube_all",
              "textures": {
                "all": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        Logger.info("  ✓ Created slab models: $blockName, ${blockName}_top, ${blockName}_double")
        createTexturePlaceholder(projectDir, blockName, modId)
    }

    private fun generateStairsModels(projectDir: File, blockName: String, modId: String) {
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/$blockName.json"),
            """
            {
              "parent": "block/stairs",
              "textures": {
                "bottom": "$modId:block/$blockName",
                "top": "$modId:block/$blockName",
                "side": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        Logger.info("  ✓ Created stairs model: $blockName")
        createTexturePlaceholder(projectDir, blockName, modId)
    }

    private fun generateFenceModels(projectDir: File, blockName: String, modId: String) {
        // Post
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_post.json"),
            """
            {
              "parent": "block/fence_post",
              "textures": {
                "texture": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        // Side
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_side.json"),
            """
            {
              "parent": "block/fence_side",
              "textures": {
                "texture": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        Logger.info("  ✓ Created fence models: ${blockName}_post, ${blockName}_side")
        createTexturePlaceholder(projectDir, blockName, modId)
    }

    private fun generateWallModels(projectDir: File, blockName: String, modId: String) {
        // Post
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_post.json"),
            """
            {
              "parent": "block/template_wall_post",
              "textures": {
                "wall": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        // Side
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_side.json"),
            """
            {
              "parent": "block/template_wall_side",
              "textures": {
                "wall": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        Logger.info("  ✓ Created wall models: ${blockName}_post, ${blockName}_side")
        createTexturePlaceholder(projectDir, blockName, modId)
    }

    private fun generateDoorModels(projectDir: File, blockName: String, modId: String) {
        Logger.info("  ⚠ Door models are complex - using Minecraft's door templates")
        Logger.info("  → Create textures: ${blockName}_bottom.png and ${blockName}_top.png")

        createTexturePlaceholder(projectDir, "${blockName}_bottom", modId)
        createTexturePlaceholder(projectDir, "${blockName}_top", modId)
    }

    private fun generateTrapdoorModels(projectDir: File, blockName: String, modId: String) {
        // Bottom
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_bottom.json"),
            """
            {
              "parent": "block/template_orientable_trapdoor_bottom",
              "textures": {
                "texture": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        // Top
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_top.json"),
            """
            {
              "parent": "block/template_orientable_trapdoor_top",
              "textures": {
                "texture": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        // Open
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_open.json"),
            """
            {
              "parent": "block/template_orientable_trapdoor_open",
              "textures": {
                "texture": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        Logger.info("  ✓ Created trapdoor models: ${blockName}_bottom, ${blockName}_top, ${blockName}_open")
        createTexturePlaceholder(projectDir, blockName, modId)
    }

    private fun generateButtonModels(projectDir: File, blockName: String, modId: String) {
        // Normal
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/$blockName.json"),
            """
            {
              "parent": "block/button",
              "textures": {
                "texture": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        // Pressed
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_pressed.json"),
            """
            {
              "parent": "block/button_pressed",
              "textures": {
                "texture": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        Logger.info("  ✓ Created button models: $blockName, ${blockName}_pressed")
        createTexturePlaceholder(projectDir, blockName, modId)
    }

    private fun generatePressurePlateModels(projectDir: File, blockName: String, modId: String) {
        // Normal
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/$blockName.json"),
            """
            {
              "parent": "block/pressure_plate_up",
              "textures": {
                "texture": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        // Down
        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_down.json"),
            """
            {
              "parent": "block/pressure_plate_down",
              "textures": {
                "texture": "$modId:block/$blockName"
              }
            }
            """.trimIndent()
        )

        Logger.info("  ✓ Created pressure plate models: $blockName, ${blockName}_down")
        createTexturePlaceholder(projectDir, blockName, modId)
    }

    private fun generateCropModels(projectDir: File, blockName: String, modId: String, maxAge: Int) {
        for (age in 0..maxAge) {
            FileUtil.writeText(
                File(projectDir, "versions/shared/v1/assets/$modId/models/block/${blockName}_stage$age.json"),
                """
                {
                  "parent": "block/crop",
                  "textures": {
                    "crop": "$modId:block/${blockName}_stage$age"
                  }
                }
                """.trimIndent()
            )

            createTexturePlaceholder(projectDir, "${blockName}_stage$age", modId)
        }

        Logger.info("  ✓ Created crop models: ${blockName}_stage0 through ${blockName}_stage$maxAge")
    }

    private fun createTexturePlaceholder(projectDir: File, textureName: String, modId: String) {
        val textureFile = File(projectDir, "versions/shared/v1/assets/$modId/textures/block/$textureName.png")
        textureFile.parentFile.mkdirs()
        if (!textureFile.exists()) {
            textureFile.createNewFile()
            Logger.info("  ✓ Created placeholder texture: $textureName.png")
        }
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

    private fun generateFabricRegistration(projectDir: File, blockName: String, modId: String, sanitizedModId: String) {
        val className = toClassName(blockName)
        val content = """
            package com.$sanitizedModId.platform.fabric;

            import com.$sanitizedModId.blocks.$className;
            import net.minecraft.block.Block;
            import net.minecraft.item.BlockItem;
            import net.minecraft.item.Item;
            import net.minecraft.registry.Registries;
            import net.minecraft.registry.Registry;
            import net.minecraft.util.Identifier;

            /**
             * Fabric-specific block registration for $className
             */
            public class ${className}Fabric {
                public static void register() {
                    // Example Fabric registration:
                    // Block block = Registry.register(
                    //     Registries.BLOCK,
                    //     Identifier.of("$modId", $className.ID),
                    //     $className.INSTANCE
                    // );
                    //
                    // Registry.register(
                    //     Registries.ITEM,
                    //     Identifier.of("$modId", $className.ID),
                    //     new BlockItem(block, new Item.Settings())
                    // );
                }
            }
        """.trimIndent()

        val file = File(projectDir, "shared/fabric/src/main/java/com/$sanitizedModId/platform/fabric/${className}Fabric.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created Fabric registration: shared/fabric/src/main/java/com/$sanitizedModId/platform/fabric/${className}Fabric.java")
    }

    private fun generateForgeRegistration(projectDir: File, blockName: String, modId: String, sanitizedModId: String) {
        val className = toClassName(blockName)
        val content = """
            package com.$sanitizedModId.platform.forge;

            import com.$sanitizedModId.blocks.$className;
            import net.minecraft.world.item.BlockItem;
            import net.minecraft.world.item.Item;
            import net.minecraft.world.level.block.Block;
            import net.minecraftforge.registries.DeferredRegister;
            import net.minecraftforge.registries.ForgeRegistries;
            import net.minecraftforge.registries.RegistryObject;

            /**
             * Forge-specific block registration for $className
             */
            public class ${className}Forge {
                // Example Forge registration:
                // public static final DeferredRegister<Block> BLOCKS =
                //     DeferredRegister.create(ForgeRegistries.BLOCKS, "$modId");
                //
                // public static final DeferredRegister<Item> ITEMS =
                //     DeferredRegister.create(ForgeRegistries.ITEMS, "$modId");
                //
                // public static final RegistryObject<Block> ${blockName.uppercase()} =
                //     BLOCKS.register($className.ID, () -> $className.INSTANCE);
                //
                // public static final RegistryObject<Item> ${blockName.uppercase()}_ITEM =
                //     ITEMS.register($className.ID, () -> new BlockItem(${blockName.uppercase()}.get(), new Item.Properties()));
            }
        """.trimIndent()

        val file = File(projectDir, "shared/forge/src/main/java/com/$sanitizedModId/platform/forge/${className}Forge.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created Forge registration: shared/forge/src/main/java/com/$sanitizedModId/platform/forge/${className}Forge.java")
    }

    private fun generateNeoForgeRegistration(projectDir: File, blockName: String, modId: String, sanitizedModId: String) {
        val className = toClassName(blockName)
        val content = """
            package com.$sanitizedModId.platform.neoforge;

            import com.$sanitizedModId.blocks.$className;
            import net.minecraft.core.registries.Registries;
            import net.minecraft.world.item.BlockItem;
            import net.minecraft.world.item.Item;
            import net.minecraft.world.level.block.Block;
            import net.neoforged.neoforge.registries.DeferredRegister;
            import net.neoforged.neoforge.registries.DeferredBlock;
            import net.neoforged.neoforge.registries.DeferredItem;

            /**
             * NeoForge-specific block registration for $className
             */
            public class ${className}NeoForge {
                // Example NeoForge registration:
                // public static final DeferredRegister.Blocks BLOCKS =
                //     DeferredRegister.createBlocks("$modId");
                //
                // public static final DeferredRegister.Items ITEMS =
                //     DeferredRegister.createItems("$modId");
                //
                // public static final DeferredBlock<Block> ${blockName.uppercase()} =
                //     BLOCKS.register($className.ID, () -> $className.INSTANCE);
                //
                // public static final DeferredItem<BlockItem> ${blockName.uppercase()}_ITEM =
                //     ITEMS.registerSimpleBlockItem(${blockName.uppercase()});
            }
        """.trimIndent()

        val file = File(projectDir, "shared/neoforge/src/main/java/com/$sanitizedModId/platform/neoforge/${className}NeoForge.java")
        FileUtil.writeText(file, content)

        Logger.info("  ✓ Created NeoForge registration: shared/neoforge/src/main/java/com/$sanitizedModId/platform/neoforge/${className}NeoForge.java")
    }

    private fun toClassName(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { it.capitalize() }
    }

    // ========== Blockstate Generators ==========

    private fun generateBasicBlockState(blockName: String, modId: String): String = """
        {
          "variants": {
            "": {
              "model": "$modId:block/$blockName"
            }
          }
        }
    """.trimIndent()

    private fun generatePillarBlockState(blockName: String, modId: String): String = """
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

    private fun generateSlabBlockState(blockName: String, modId: String): String = """
        {
          "variants": {
            "type=bottom": {
              "model": "$modId:block/${blockName}"
            },
            "type=top": {
              "model": "$modId:block/${blockName}_top"
            },
            "type=double": {
              "model": "$modId:block/${blockName}_double"
            }
          }
        }
    """.trimIndent()

    private fun generateStairsBlockState(blockName: String, modId: String): String = """
        {
          "variants": {
            "facing=east,half=bottom,shape=straight": {
              "model": "$modId:block/$blockName"
            },
            "facing=west,half=bottom,shape=straight": {
              "model": "$modId:block/$blockName",
              "y": 180,
              "uvlock": true
            },
            "facing=south,half=bottom,shape=straight": {
              "model": "$modId:block/$blockName",
              "y": 90,
              "uvlock": true
            },
            "facing=north,half=bottom,shape=straight": {
              "model": "$modId:block/$blockName",
              "y": 270,
              "uvlock": true
            },
            "facing=east,half=top,shape=straight": {
              "model": "$modId:block/$blockName",
              "x": 180,
              "uvlock": true
            },
            "facing=west,half=top,shape=straight": {
              "model": "$modId:block/$blockName",
              "x": 180,
              "y": 180,
              "uvlock": true
            },
            "facing=south,half=top,shape=straight": {
              "model": "$modId:block/$blockName",
              "x": 180,
              "y": 90,
              "uvlock": true
            },
            "facing=north,half=top,shape=straight": {
              "model": "$modId:block/$blockName",
              "x": 180,
              "y": 270,
              "uvlock": true
            }
          }
        }
    """.trimIndent()

    private fun generateFenceBlockState(blockName: String, modId: String): String = """
        {
          "multipart": [
            {
              "apply": {
                "model": "$modId:block/${blockName}_post"
              }
            },
            {
              "when": {
                "north": "true"
              },
              "apply": {
                "model": "$modId:block/${blockName}_side",
                "uvlock": true
              }
            },
            {
              "when": {
                "east": "true"
              },
              "apply": {
                "model": "$modId:block/${blockName}_side",
                "y": 90,
                "uvlock": true
              }
            },
            {
              "when": {
                "south": "true"
              },
              "apply": {
                "model": "$modId:block/${blockName}_side",
                "y": 180,
                "uvlock": true
              }
            },
            {
              "when": {
                "west": "true"
              },
              "apply": {
                "model": "$modId:block/${blockName}_side",
                "y": 270,
                "uvlock": true
              }
            }
          ]
        }
    """.trimIndent()

    private fun generateWallBlockState(blockName: String, modId: String): String = """
        {
          "multipart": [
            {
              "when": {
                "up": "true"
              },
              "apply": {
                "model": "$modId:block/${blockName}_post"
              }
            },
            {
              "when": {
                "north": "low"
              },
              "apply": {
                "model": "$modId:block/${blockName}_side",
                "uvlock": true
              }
            },
            {
              "when": {
                "east": "low"
              },
              "apply": {
                "model": "$modId:block/${blockName}_side",
                "y": 90,
                "uvlock": true
              }
            },
            {
              "when": {
                "south": "low"
              },
              "apply": {
                "model": "$modId:block/${blockName}_side",
                "y": 180,
                "uvlock": true
              }
            },
            {
              "when": {
                "west": "low"
              },
              "apply": {
                "model": "$modId:block/${blockName}_side",
                "y": 270,
                "uvlock": true
              }
            }
          ]
        }
    """.trimIndent()

    private fun generateDoorBlockState(blockName: String, modId: String): String = """
        {
          "variants": {
            "facing=east,half=lower,hinge=left,open=false": {
              "model": "$modId:block/${blockName}_bottom_left"
            },
            "facing=east,half=lower,hinge=left,open=true": {
              "model": "$modId:block/${blockName}_bottom_left_open",
              "y": 90
            },
            "facing=east,half=lower,hinge=right,open=false": {
              "model": "$modId:block/${blockName}_bottom_right"
            },
            "facing=east,half=lower,hinge=right,open=true": {
              "model": "$modId:block/${blockName}_bottom_right_open",
              "y": 270
            },
            "facing=east,half=upper,hinge=left,open=false": {
              "model": "$modId:block/${blockName}_top_left"
            },
            "facing=east,half=upper,hinge=left,open=true": {
              "model": "$modId:block/${blockName}_top_left_open",
              "y": 90
            },
            "facing=east,half=upper,hinge=right,open=false": {
              "model": "$modId:block/${blockName}_top_right"
            },
            "facing=east,half=upper,hinge=right,open=true": {
              "model": "$modId:block/${blockName}_top_right_open",
              "y": 270
            }
          }
        }
    """.trimIndent()

    private fun generateTrapdoorBlockState(blockName: String, modId: String): String = """
        {
          "variants": {
            "facing=north,half=bottom,open=false": {
              "model": "$modId:block/${blockName}_bottom"
            },
            "facing=north,half=bottom,open=true": {
              "model": "$modId:block/${blockName}_open"
            },
            "facing=north,half=top,open=false": {
              "model": "$modId:block/${blockName}_top"
            },
            "facing=north,half=top,open=true": {
              "model": "$modId:block/${blockName}_open",
              "x": 180,
              "y": 180
            },
            "facing=south,half=bottom,open=false": {
              "model": "$modId:block/${blockName}_bottom",
              "y": 180
            },
            "facing=south,half=bottom,open=true": {
              "model": "$modId:block/${blockName}_open",
              "y": 180
            },
            "facing=south,half=top,open=false": {
              "model": "$modId:block/${blockName}_top",
              "y": 180
            },
            "facing=south,half=top,open=true": {
              "model": "$modId:block/${blockName}_open",
              "x": 180
            }
          }
        }
    """.trimIndent()

    private fun generateButtonBlockState(blockName: String, modId: String): String = """
        {
          "variants": {
            "face=floor,facing=east,powered=false": {
              "model": "$modId:block/$blockName",
              "y": 90
            },
            "face=floor,facing=east,powered=true": {
              "model": "$modId:block/${blockName}_pressed",
              "y": 90
            },
            "face=floor,facing=north,powered=false": {
              "model": "$modId:block/$blockName"
            },
            "face=floor,facing=north,powered=true": {
              "model": "$modId:block/${blockName}_pressed"
            },
            "face=wall,facing=east,powered=false": {
              "model": "$modId:block/$blockName",
              "uvlock": true,
              "x": 90,
              "y": 90
            },
            "face=wall,facing=east,powered=true": {
              "model": "$modId:block/${blockName}_pressed",
              "uvlock": true,
              "x": 90,
              "y": 90
            }
          }
        }
    """.trimIndent()

    private fun generatePressurePlateBlockState(blockName: String, modId: String): String = """
        {
          "variants": {
            "powered=false": {
              "model": "$modId:block/$blockName"
            },
            "powered=true": {
              "model": "$modId:block/${blockName}_down"
            }
          }
        }
    """.trimIndent()

    private fun generateCropBlockState(blockName: String, modId: String, maxAge: Int): String {
        val variants = (0..maxAge).joinToString(",\n    ") { age ->
            """"age=$age": {
      "model": "$modId:block/${blockName}_stage$age"
    }"""
        }

        return """
        {
          "variants": {
            $variants
          }
        }
        """.trimIndent()
    }
}
