package dev.dropper.commands

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import dev.dropper.util.StringUtil
import dev.dropper.util.ValidationUtil
import java.io.File

/**
 * Command to create a new block in the mod
 * Generates registration code, block states, models, and textures
 */
class CreateBlockCommand : DropperCommand(
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
        val projectValidation = ValidationUtil.validateDropperProject(projectDir)
        if (!projectValidation.isValid) {
            ValidationUtil.exitWithError(projectValidation)
            return
        }

        val configFile = getConfigFile()

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

        // Generate/update registry class using Architectury DeferredRegister
        generateOrUpdateModBlocks(projectDir, name, modId, sanitizedModId)

        // Update main mod class init() to call ModBlocks.init()
        updateMainModInit(projectDir, sanitizedModId, "ModBlocks")

        // Generate blockstates
        generateBlockState(projectDir, name, modId, type)

        // Generate block models
        generateBlockModels(projectDir, name, modId, type)

        // Generate item model (for inventory)
        generateBlockItemModel(projectDir, name, modId)

        // Generate loot table if drops self
        if (dropsSelf == "true") {
            generateLootTable(projectDir, name, modId, type)
        }

        Logger.success("Block '$name' created successfully!")
        Logger.info("Next steps:")
        Logger.info("  1. Add texture: versions/shared/v1/assets/$modId/textures/block/$name.png")
        Logger.info("  2. Customize blockstate: versions/shared/v1/assets/$modId/blockstates/$name.json")
        Logger.info("  3. Build with: dropper build")
    }

    private fun generateBlockRegistration(projectDir: File, blockName: String, sanitizedModId: String, type: String) {
        val className = toClassName(blockName)

        val (imports, extendsClause, constructorBody) = when (type) {
            "ore" -> Triple(
                listOf(
                    "net.minecraft.world.level.block.Block",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour"
                ),
                "Block",
                """BlockBehaviour.Properties.of()
            .strength(3.0f, 3.0f)
            .requiresCorrectToolForDrops()
            .sound(SoundType.STONE)"""
            )
            "pillar" -> Triple(
                listOf(
                    "net.minecraft.world.level.block.RotatedPillarBlock",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour"
                ),
                "RotatedPillarBlock",
                """BlockBehaviour.Properties.of()
            .strength(2.0f, 2.0f)
            .sound(SoundType.WOOD)"""
            )
            "slab" -> Triple(
                listOf(
                    "net.minecraft.world.level.block.SlabBlock",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour"
                ),
                "SlabBlock",
                """BlockBehaviour.Properties.of()
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)"""
            )
            "stairs" -> Triple(
                listOf(
                    "net.minecraft.world.level.block.Block",
                    "net.minecraft.world.level.block.Blocks",
                    "net.minecraft.world.level.block.StairBlock",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour"
                ),
                "StairBlock",
                """Blocks.STONE.defaultBlockState(), BlockBehaviour.Properties.of()
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)"""
            )
            "fence" -> Triple(
                listOf(
                    "net.minecraft.world.level.block.FenceBlock",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour"
                ),
                "FenceBlock",
                """BlockBehaviour.Properties.of()
            .strength(2.0f, 3.0f)
            .sound(SoundType.WOOD)"""
            )
            "wall" -> Triple(
                listOf(
                    "net.minecraft.world.level.block.WallBlock",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour"
                ),
                "WallBlock",
                """BlockBehaviour.Properties.of()
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)"""
            )
            "door" -> Triple(
                listOf(
                    "net.minecraft.world.level.block.DoorBlock",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour",
                    "net.minecraft.world.level.block.state.properties.BlockSetType"
                ),
                "DoorBlock",
                """BlockSetType.OAK, BlockBehaviour.Properties.of()
            .strength(3.0f)
            .sound(SoundType.WOOD)
            .noOcclusion()"""
            )
            "trapdoor" -> Triple(
                listOf(
                    "net.minecraft.world.level.block.TrapDoorBlock",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour",
                    "net.minecraft.world.level.block.state.properties.BlockSetType"
                ),
                "TrapDoorBlock",
                """BlockSetType.OAK, BlockBehaviour.Properties.of()
            .strength(3.0f)
            .sound(SoundType.WOOD)
            .noOcclusion()"""
            )
            "button" -> Triple(
                listOf(
                    "net.minecraft.world.level.block.ButtonBlock",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour",
                    "net.minecraft.world.level.block.state.properties.BlockSetType"
                ),
                "ButtonBlock",
                """BlockSetType.STONE, 20, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.STONE)
            .noCollission()"""
            )
            "pressure_plate" -> Triple(
                listOf(
                    "net.minecraft.world.level.block.PressurePlateBlock",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour",
                    "net.minecraft.world.level.block.state.properties.BlockSetType"
                ),
                "PressurePlateBlock",
                """BlockSetType.STONE, BlockBehaviour.Properties.of()
            .strength(0.5f)
            .sound(SoundType.STONE)
            .noCollission()"""
            )
            "crop" -> Triple(
                listOf(
                    "net.minecraft.world.level.block.CropBlock",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour"
                ),
                "CropBlock",
                """BlockBehaviour.Properties.of()
            .noCollission()
            .randomTicks()
            .instabreak()
            .sound(SoundType.CROP)"""
            )
            else -> Triple(
                listOf(
                    "net.minecraft.world.level.block.Block",
                    "net.minecraft.world.level.block.SoundType",
                    "net.minecraft.world.level.block.state.BlockBehaviour"
                ),
                "Block",
                """BlockBehaviour.Properties.of()
            .strength(2.0f, 6.0f)
            .sound(SoundType.STONE)"""
            )
        }

        val importLines = imports.joinToString("\n") { "import $it;" }

        val content = """
            package com.$sanitizedModId.blocks;

            $importLines

            /**
             * Custom block: $className
             *
             * Loader-specific registration happens in platform code.
             */
            public class $className extends $extendsClause {
                public static final String ID = "$blockName";

                public static final $className INSTANCE = new $className();

                public $className() {
                    super($constructorBody);
                }
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

    private fun generateLootTable(projectDir: File, blockName: String, modId: String, blockType: String = "basic") {
        val content = if (blockType == "ore") {
            generateOreLootTable(blockName, modId)
        } else {
            """
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
        }

        val lootTableFile = File(projectDir, "versions/shared/v1/data/$modId/loot_tables/blocks/$blockName.json")
        FileUtil.writeText(lootTableFile, content)

        Logger.info("  ✓ Created loot table: versions/shared/v1/data/$modId/loot_tables/blocks/$blockName.json")
    }

    private fun generateOreLootTable(blockName: String, modId: String): String = """
        {
          "type": "minecraft:block",
          "pools": [
            {
              "rolls": 1,
              "bonus_rolls": 0,
              "entries": [
                {
                  "type": "minecraft:alternatives",
                  "children": [
                    {
                      "type": "minecraft:item",
                      "conditions": [
                        {
                          "condition": "minecraft:match_tool",
                          "predicate": {
                            "enchantments": [
                              {
                                "enchantment": "minecraft:silk_touch",
                                "levels": {
                                  "min": 1
                                }
                              }
                            ]
                          }
                        }
                      ],
                      "name": "$modId:$blockName"
                    },
                    {
                      "type": "minecraft:item",
                      "functions": [
                        {
                          "function": "minecraft:apply_bonus",
                          "enchantment": "minecraft:fortune",
                          "formula": "minecraft:ore_drops"
                        },
                        {
                          "function": "minecraft:explosion_decay"
                        }
                      ],
                      "name": "$modId:$blockName"
                    }
                  ]
                }
              ]
            }
          ]
        }
    """.trimIndent()

    /**
     * Generate or update the common ModBlocks registry class using Architectury DeferredRegister.
     */
    private fun generateOrUpdateModBlocks(
        projectDir: File,
        blockName: String,
        modId: String,
        sanitizedModId: String
    ) {
        val className = toClassName(blockName)
        val constantName = blockName.uppercase()
        val registryFile = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/registry/ModBlocks.java")

        if (registryFile.exists()) {
            val existingContent = registryFile.readText()

            val newBlockField = "    public static final RegistrySupplier<Block> $constantName = BLOCKS.register(\"$blockName\", () -> new ${className}());"
            val newItemField = "    public static final RegistrySupplier<Item> ${constantName}_ITEM = ITEMS.register(\"$blockName\", () -> new BlockItem($constantName.get(), new Item.Properties()));"
            val newImport = "import com.$sanitizedModId.blocks.$className;"

            val updatedContent = existingContent
                .replace("    public static void init()", "$newBlockField\n$newItemField\n\n    public static void init()")
                .let { content ->
                    if (!content.contains(newImport)) {
                        content.replace("import net.minecraft.world.level.block.Block;", "import net.minecraft.world.level.block.Block;\nimport com.$sanitizedModId.blocks.$className;")
                    } else content
                }

            FileUtil.writeText(registryFile, updatedContent)
            Logger.info("  ✓ Added $className to registry/ModBlocks.java")
        } else {
            val content = """
                package com.$sanitizedModId.registry;

                import com.$sanitizedModId.blocks.$className;
                import dev.architectury.registry.registries.DeferredRegister;
                import dev.architectury.registry.registries.RegistrySupplier;
                import net.minecraft.core.registries.Registries;
                import net.minecraft.world.item.BlockItem;
                import net.minecraft.world.item.Item;
                import net.minecraft.world.level.block.Block;

                public class ModBlocks {
                    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create("$modId", Registries.BLOCK);
                    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create("$modId", Registries.ITEM);

                    public static final RegistrySupplier<Block> $constantName = BLOCKS.register("$blockName", () -> new ${className}());
                    public static final RegistrySupplier<Item> ${constantName}_ITEM = ITEMS.register("$blockName", () -> new BlockItem($constantName.get(), new Item.Properties()));

                    public static void init() {
                        BLOCKS.register();
                        ITEMS.register();
                    }
                }
            """.trimIndent()

            FileUtil.writeText(registryFile, content)
            Logger.info("  ✓ Created registry/ModBlocks.java with $className")
        }
    }

    private fun toClassName(snakeCase: String): String = StringUtil.toClassName(snakeCase)

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
