package dev.dropper.e2e

import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateEntityCommand
import dev.dropper.commands.CreateEnchantmentCommand
import dev.dropper.commands.CreateRecipeCommand
import dev.dropper.commands.CreateBiomeCommand
import dev.dropper.commands.CreateTagCommand
import dev.dropper.config.ModConfig
import dev.dropper.util.TestProjectContext
import dev.dropper.util.TestValidationUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive E2E tests that generate a full project with ALL component types
 * and validate every generated file for correctness.
 *
 * Covers:
 * - Java syntax validity across all generators
 * - JSON validity across all generated assets
 * - Build system completeness
 * - Package declarations matching file paths
 * - Platform registration API correctness
 * - No leftover template variables or TODO comments
 */
class ComprehensiveCodeValidationE2ETest {

    private lateinit var context: TestProjectContext
    private val modId = "testmod"
    private val sanitizedModId = "testmod"

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("comprehensive-validation")

        val config = ModConfig(
            id = modId,
            name = "Test Mod",
            version = "1.0.0",
            description = "Comprehensive validation test",
            author = "Test Author",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        context.createProject(config)
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    // ========================================================================
    // Helper: generate all component types
    // ========================================================================

    /**
     * Generate all supported component types in the project.
     * Each command is invoked with parse() to simulate CLI invocation.
     */
    private fun generateAllComponents() {
        // Items: basic, tool, food
        CreateItemCommand().parse(arrayOf("ruby_sword", "--type", "tool"))
        CreateItemCommand().parse(arrayOf("magic_apple", "--type", "food"))
        CreateItemCommand().parse(arrayOf("copper_ingot"))

        // Blocks: basic, ore
        CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))
        CreateBlockCommand().parse(arrayOf("crystal_block"))

        // Entity: mob (default type)
        CreateEntityCommand().parse(arrayOf("crystal_golem"))

        // Enchantment
        CreateEnchantmentCommand().parse(arrayOf("frost_bite"))

        // Recipe: shaped crafting (default)
        CreateRecipeCommand().parse(arrayOf("ruby_pickaxe"))

        // Biome
        CreateBiomeCommand().parse(arrayOf("crystal_plains"))

        // Tag (with explicit values to generate valid JSON)
        CreateTagCommand().parse(arrayOf("custom_ores", "--type", "block", "--values", "$modId:ruby_ore"))
    }

    // ========================================================================
    // Test 1: All generated Java files have valid syntax
    // ========================================================================

    @Test
    fun `all generated Java files have valid syntax`() {
        generateAllComponents()

        val javaFiles = context.projectDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()

        assertTrue(javaFiles.size >= 10, "Should generate at least 10 Java files, found ${javaFiles.size}")

        println("Validating ${javaFiles.size} Java files for syntax correctness...")

        javaFiles.forEach { javaFile ->
            val content = javaFile.readText()
            val relativePath = javaFile.relativeTo(context.projectDir).path

            // Full Java syntax validation
            TestValidationUtils.assertValidJavaSyntax(content, relativePath)

            // Class name matches file name
            TestValidationUtils.assertClassNameMatchesFile(content, javaFile.name)

            // No template variables remain
            TestValidationUtils.assertNoTemplateVariables(content, relativePath)

            println("  OK: $relativePath")
        }

        println("All ${javaFiles.size} Java files passed syntax validation.")
    }

    // ========================================================================
    // Test 2: Package declarations match directory paths
    // ========================================================================

    @Test
    fun `package declarations match directory paths for all Java files`() {
        generateAllComponents()

        val javaFiles = context.projectDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()

        assertTrue(javaFiles.isNotEmpty(), "Should have generated Java files")

        println("Validating package-path consistency for ${javaFiles.size} Java files...")

        javaFiles.forEach { javaFile ->
            val content = javaFile.readText()
            TestValidationUtils.assertPackageMatchesPath(
                content,
                javaFile.absolutePath,
                context.projectDir.absolutePath
            )

            println("  OK: ${javaFile.relativeTo(context.projectDir).path}")
        }

        println("All package declarations match their directory paths.")
    }

    // ========================================================================
    // Test 3: No generated files contain unresolved template variables
    // ========================================================================

    @Test
    fun `no generated files contain unresolved template variables`() {
        generateAllComponents()

        val allCodeFiles = context.projectDir.walkTopDown()
            .filter { it.isFile }
            .filter { it.extension in listOf("java", "json", "yml", "yaml", "toml", "properties", "kts") }
            .toList()

        assertTrue(allCodeFiles.isNotEmpty(), "Should have generated files to check")

        println("Checking ${allCodeFiles.size} files for unresolved template variables...")

        allCodeFiles.forEach { file ->
            val content = file.readText()
            val relativePath = file.relativeTo(context.projectDir).path

            assertFalse(
                content.contains("{{"),
                "File '$relativePath' contains unresolved template opening '{{'"
            )
            assertFalse(
                content.contains("}}"),
                "File '$relativePath' contains unresolved template closing '}}'"
            )
        }

        println("No unresolved template variables found in ${allCodeFiles.size} files.")
    }

    // ========================================================================
    // Test 4: All generated JSON files are valid
    // ========================================================================

    @Test
    fun `all generated JSON files are valid`() {
        generateAllComponents()

        val jsonFiles = context.projectDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .toList()

        assertTrue(jsonFiles.size >= 5, "Should generate at least 5 JSON files, found ${jsonFiles.size}")

        println("Validating ${jsonFiles.size} JSON files...")

        jsonFiles.forEach { jsonFile ->
            val content = jsonFile.readText()
            val relativePath = jsonFile.relativeTo(context.projectDir).path

            TestValidationUtils.assertValidJson(content, relativePath)

            println("  OK: $relativePath")
        }

        println("All ${jsonFiles.size} JSON files passed validation.")
    }

    // ========================================================================
    // Test 5: Generated items extend Item class
    // ========================================================================

    @Test
    fun `generated items extend Item class`() {
        CreateItemCommand().parse(arrayOf("basic_gem"))
        CreateItemCommand().parse(arrayOf("iron_dagger", "--type", "tool"))
        CreateItemCommand().parse(arrayOf("golden_berry", "--type", "food"))

        val itemDir = context.file("shared/common/src/main/java/com/$sanitizedModId/items")
        assertTrue(itemDir.exists(), "Items directory should exist")

        val itemFiles = itemDir.listFiles()?.filter { it.extension == "java" } ?: emptyList()
        assertTrue(itemFiles.size >= 3, "Should have at least 3 item Java files, found ${itemFiles.size}")

        itemFiles.forEach { file ->
            val content = file.readText()
            val relativePath = file.relativeTo(context.projectDir).path

            // Basic items extend Item, tool items extend SwordItem (which extends Item)
            val extendsItem = content.contains("extends Item")
            val extendsSwordItem = content.contains("extends SwordItem")
            assertTrue(
                extendsItem || extendsSwordItem,
                "Item file '$relativePath' should extend Item or SwordItem"
            )

            // Full syntax validation
            TestValidationUtils.assertValidJavaSyntax(content, relativePath)

            println("  OK: ${file.name} - extends ${if (extendsSwordItem) "SwordItem" else "Item"}")
        }
    }

    // ========================================================================
    // Test 6: Generated blocks extend Block class (or subclass)
    // ========================================================================

    @Test
    fun `generated blocks extend Block class`() {
        CreateBlockCommand().parse(arrayOf("test_stone"))
        CreateBlockCommand().parse(arrayOf("test_ore", "--type", "ore"))

        val blockDir = context.file("shared/common/src/main/java/com/$sanitizedModId/blocks")
        assertTrue(blockDir.exists(), "Blocks directory should exist")

        val blockFiles = blockDir.listFiles()?.filter { it.extension == "java" } ?: emptyList()
        assertTrue(blockFiles.size >= 2, "Should have at least 2 block Java files, found ${blockFiles.size}")

        // Valid Minecraft block base classes
        val validBaseClasses = listOf(
            "extends Block",
            "extends RotatedPillarBlock",
            "extends SlabBlock",
            "extends StairBlock",
            "extends FenceBlock",
            "extends WallBlock",
            "extends DoorBlock",
            "extends TrapDoorBlock",
            "extends ButtonBlock",
            "extends PressurePlateBlock",
            "extends CropBlock"
        )

        blockFiles.forEach { file ->
            val content = file.readText()
            val relativePath = file.relativeTo(context.projectDir).path

            val extendsBlock = validBaseClasses.any { content.contains(it) }
            assertTrue(
                extendsBlock,
                "Block file '$relativePath' should extend a Block base class"
            )

            TestValidationUtils.assertValidJavaSyntax(content, relativePath)

            println("  OK: ${file.name}")
        }
    }

    // ========================================================================
    // Test 7: Generated entities extend proper entity base class
    // ========================================================================

    @Test
    fun `generated entities extend proper entity base class`() {
        CreateEntityCommand().parse(arrayOf("test_creature"))

        val entityDir = context.file("shared/common/src/main/java/com/$sanitizedModId/entities")
        assertTrue(entityDir.exists(), "Entities directory should exist")

        val entityFiles = entityDir.listFiles()?.filter { it.extension == "java" } ?: emptyList()
        assertTrue(entityFiles.isNotEmpty(), "Should have entity Java files")

        // Valid Minecraft entity base classes
        // The generator uses its own naming convention: PathAwareEntity, AnimalEntity, etc.
        // These map to the Mojmap classes (PathfinderMob, Animal, Monster, etc.)
        val validBaseClasses = listOf(
            "extends PathfinderMob",
            "extends PathAwareEntity",
            "extends Animal",
            "extends AnimalEntity",
            "extends Monster",
            "extends HostileEntity",
            "extends Villager",
            "extends VillagerEntity",
            "extends ThrowableProjectile",
            "extends ProjectileEntity"
        )

        entityFiles.forEach { file ->
            val content = file.readText()
            val relativePath = file.relativeTo(context.projectDir).path

            val extendsEntity = validBaseClasses.any { content.contains(it) }
            assertTrue(
                extendsEntity,
                "Entity file '$relativePath' should extend a Minecraft entity base class. " +
                    "Content snippet: ${content.take(300)}"
            )

            TestValidationUtils.assertValidJavaSyntax(content, relativePath)

            println("  OK: ${file.name}")
        }
    }

    // ========================================================================
    // Test 8: Generated project has complete build system
    // ========================================================================

    @Test
    fun `generated project has complete build system`() {
        // Verify all required build files exist
        val requiredFiles = listOf(
            "build.gradle.kts",
            "settings.gradle.kts",
            "gradle.properties",
            "config.yml",
            ".gitignore",
            "README.md",
            "AGENTS.md"
        )

        requiredFiles.forEach { file ->
            assertTrue(
                context.file(file).exists(),
                "Build system file should exist: $file"
            )
        }

        // Verify build.gradle.kts content
        val buildGradle = context.file("build.gradle.kts").readText()
        assertTrue(buildGradle.contains("plugins"), "build.gradle.kts should have plugins block")
        assertTrue(buildGradle.contains("subprojects"), "build.gradle.kts should configure subprojects")
        assertTrue(
            buildGradle.contains("maven.fabricmc.net") || buildGradle.contains("fabricmc"),
            "build.gradle.kts should have Fabric Maven repository"
        )
        assertTrue(
            buildGradle.contains("maven.neoforged.net") || buildGradle.contains("neoforged"),
            "build.gradle.kts should have NeoForge Maven repository"
        )

        // Verify settings.gradle.kts content
        val settingsGradle = context.file("settings.gradle.kts").readText()
        assertTrue(
            settingsGradle.contains("rootProject.name"),
            "settings.gradle.kts should set rootProject.name"
        )
        assertTrue(
            settingsGradle.contains("versionsDir") || settingsGradle.contains("versions"),
            "settings.gradle.kts should discover version directories"
        )
        assertTrue(
            settingsGradle.contains("include"),
            "settings.gradle.kts should include subprojects"
        )

        // Verify build-logic exists
        assertTrue(
            context.file("build-logic").exists() && context.file("build-logic").isDirectory,
            "build-logic directory should exist"
        )
        assertTrue(
            context.file("build-logic/build.gradle.kts").exists(),
            "build-logic/build.gradle.kts should exist"
        )

        // Verify build-logic has Fabric Loom dependency
        val buildLogicGradle = context.file("build-logic/build.gradle.kts").readText()
        assertTrue(
            buildLogicGradle.contains("fabric-loom") || buildLogicGradle.contains("fabricmc"),
            "build-logic should have Fabric Loom dependency"
        )

        // Verify version directory structure
        assertTrue(
            context.file("versions/1_20_1").exists(),
            "Version directory 1_20_1 should exist"
        )
        assertTrue(
            context.file("versions/1_20_1/config.yml").exists(),
            "Version config should exist"
        )

        println("Build system validation passed - all required files present and configured.")
    }

    // ========================================================================
    // Test 9: Fabric registration uses Registry API
    // ========================================================================

    @Test
    fun `common registry uses Architectury DeferredRegister`() {
        CreateItemCommand().parse(arrayOf("fabric_test_item"))
        CreateBlockCommand().parse(arrayOf("fabric_test_block"))

        // Architectury pattern: common registry files instead of per-loader files
        val registryDir = context.file("shared/common/src/main/java/com/$sanitizedModId/registry")
        assertTrue(registryDir.exists(), "Registry directory should exist")

        val modItemsFile = File(registryDir, "ModItems.java")
        assertTrue(modItemsFile.exists(), "ModItems.java should exist")

        val itemsContent = modItemsFile.readText()
        assertTrue(
            itemsContent.contains("DeferredRegister"),
            "ModItems should use Architectury DeferredRegister"
        )
        assertTrue(
            itemsContent.contains("RegistrySupplier"),
            "ModItems should use RegistrySupplier"
        )
        TestValidationUtils.assertValidJavaSyntax(itemsContent, "ModItems.java")

        val modBlocksFile = File(registryDir, "ModBlocks.java")
        assertTrue(modBlocksFile.exists(), "ModBlocks.java should exist")

        val blocksContent = modBlocksFile.readText()
        assertTrue(
            blocksContent.contains("DeferredRegister"),
            "ModBlocks should use Architectury DeferredRegister"
        )
        TestValidationUtils.assertValidJavaSyntax(blocksContent, "ModBlocks.java")

        println("  OK: Common registry uses Architectury DeferredRegister")
    }

    // ========================================================================
    // Test 10: Forge registration uses DeferredRegister
    // ========================================================================

    @Test
    fun `item registry contains all created items`() {
        CreateItemCommand().parse(arrayOf("forge_test_item"))
        CreateItemCommand().parse(arrayOf("another_item"))

        val modItemsFile = context.file("shared/common/src/main/java/com/$sanitizedModId/registry/ModItems.java")
        assertTrue(modItemsFile.exists(), "ModItems.java should exist")

        val content = modItemsFile.readText()
        assertTrue(content.contains("forge_test_item"), "ModItems should contain forge_test_item entry")
        assertTrue(content.contains("another_item"), "ModItems should contain another_item entry")
        assertTrue(content.contains("DeferredRegister"), "Should use Architectury DeferredRegister")

        TestValidationUtils.assertValidJavaSyntax(content, "ModItems.java")
        println("  OK: Item registry contains all created items")
    }

    // ========================================================================
    // Test 11: NeoForge registration uses DeferredRegister
    // ========================================================================

    @Test
    fun `block registry contains all created blocks`() {
        CreateBlockCommand().parse(arrayOf("neo_test_block"))
        CreateBlockCommand().parse(arrayOf("another_block"))

        val modBlocksFile = context.file("shared/common/src/main/java/com/$sanitizedModId/registry/ModBlocks.java")
        assertTrue(modBlocksFile.exists(), "ModBlocks.java should exist")

        val content = modBlocksFile.readText()
        assertTrue(content.contains("neo_test_block"), "ModBlocks should contain neo_test_block entry")
        assertTrue(content.contains("another_block"), "ModBlocks should contain another_block entry")
        assertTrue(content.contains("DeferredRegister"), "Should use Architectury DeferredRegister")

        TestValidationUtils.assertValidJavaSyntax(content, "ModBlocks.java")
        println("  OK: Block registry contains all created blocks")
    }

    // ========================================================================
    // Test 12: Comprehensive file count verification
    // ========================================================================

    @Test
    fun `generating all components produces expected file counts`() {
        generateAllComponents()

        val javaFiles = context.projectDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()

        val jsonFiles = context.projectDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .toList()

        // Architectury pattern: common classes + common registry files (no per-loader registration)
        // Project starts with: TestMod.java + 3 entry points (Fabric, Forge, NeoForge) = 4
        // Each item adds: 1 common class (registry entry added to ModItems.java)
        // Each block adds: 1 common class (registry entry added to ModBlocks.java)
        // Entity adds: 1 common class + 1 renderer + 1 model (registry entry added to ModEntities.java)
        // Enchantment adds: 1 common class
        // Registry files: ModItems.java, ModBlocks.java, ModEntities.java = 3
        // Minimum: 4 + 3 + 2 + 3 + 1 + 3 = 16
        assertTrue(
            javaFiles.size >= 10,
            "Should have at least 10 Java files after generating all components, found ${javaFiles.size}"
        )

        // JSON: item models, block models, blockstates, recipes, biomes, tags, entity model, loot tables, lang
        assertTrue(
            jsonFiles.size >= 10,
            "Should have at least 10 JSON files after generating all components, found ${jsonFiles.size}"
        )

        println("Generated ${javaFiles.size} Java files and ${jsonFiles.size} JSON files.")

        // Validate ALL of them
        val javaCount = TestValidationUtils.validateAllJavaFiles(context.projectDir)
        val jsonCount = TestValidationUtils.validateAllJsonFiles(context.projectDir)

        println("Validated $javaCount Java files and $jsonCount JSON files successfully.")
    }

    // ========================================================================
    // Test 13: Enchantment uses data-driven registry helper
    // ========================================================================

    @Test
    fun `generated enchantments use data-driven registry helper`() {
        CreateEnchantmentCommand().parse(arrayOf("test_enchant"))

        val enchantDir = context.file("shared/common/src/main/java/com/$sanitizedModId/enchantments")
        assertTrue(enchantDir.exists(), "Enchantments directory should exist")

        val enchantFiles = enchantDir.listFiles()?.filter { it.extension == "java" } ?: emptyList()
        assertTrue(enchantFiles.isNotEmpty(), "Should have enchantment Java files")

        enchantFiles.forEach { file ->
            val content = file.readText()
            val relativePath = file.relativeTo(context.projectDir).path

            assertTrue(
                content.contains("public static final String ID"),
                "Enchantment file '$relativePath' should have ID constant"
            )

            assertTrue(
                content.contains("MAX_LEVEL"),
                "Enchantment should have MAX_LEVEL constant"
            )

            TestValidationUtils.assertValidJavaSyntax(content, relativePath)

            println("  OK: ${file.name} - data-driven registry helper")
        }
    }

    // ========================================================================
    // Test 14: Verify all loaders have consistent registration files
    // ========================================================================

    @Test
    fun `item registration uses common Architectury registry`() {
        CreateItemCommand().parse(arrayOf("consistency_item"))

        // With Architectury, items are registered in a single common registry
        val modItemsFile = context.file("shared/common/src/main/java/com/$sanitizedModId/registry/ModItems.java")
        assertTrue(modItemsFile.exists(), "ModItems.java should exist")

        val content = modItemsFile.readText()
        TestValidationUtils.assertValidJavaSyntax(content, "ModItems.java")

        assertTrue(
            content.contains("consistency_item"),
            "ModItems should contain consistency_item registration"
        )
        assertTrue(
            content.contains("DeferredRegister"),
            "ModItems should use Architectury DeferredRegister"
        )
        assertTrue(
            content.contains("package com.$sanitizedModId.registry"),
            "Registry should have correct package"
        )

        // Per-loader registration files should NOT exist
        val fabricFile = context.file("shared/fabric/src/main/java/com/$sanitizedModId/platform/fabric/ConsistencyItemFabric.java")
        assertFalse(fabricFile.exists(), "Per-loader Fabric file should NOT exist with Architectury")

        println("  OK: Common Architectury registry for items")
    }

    // ========================================================================
    // Test 15: Recipe JSON has required fields
    // ========================================================================

    @Test
    fun `generated recipes have required JSON fields`() {
        // Shaped crafting
        CreateRecipeCommand().parse(arrayOf("shaped_test"))
        // Smelting
        CreateRecipeCommand().parse(arrayOf("smelting_test", "--type", "smelting"))
        // Stonecutting
        CreateRecipeCommand().parse(arrayOf("stone_test", "--type", "stonecutting"))

        val recipeDir = context.file("versions/shared/v1/data/$modId/recipe")
        assertTrue(recipeDir.exists(), "Recipe directory should exist")

        // Shaped recipe
        val shapedRecipe = File(recipeDir, "shaped_test.json")
        assertTrue(shapedRecipe.exists(), "Shaped recipe should exist")
        val shapedContent = shapedRecipe.readText()
        TestValidationUtils.assertValidJson(shapedContent, "shaped_test.json")
        TestValidationUtils.assertJsonHasKeys(shapedContent, listOf("type", "pattern", "key", "result"), "shaped_test.json")

        // Smelting recipe
        val smeltingRecipe = File(recipeDir, "smelting_test.json")
        assertTrue(smeltingRecipe.exists(), "Smelting recipe should exist")
        val smeltingContent = smeltingRecipe.readText()
        TestValidationUtils.assertValidJson(smeltingContent, "smelting_test.json")
        TestValidationUtils.assertJsonHasKeys(smeltingContent, listOf("type", "ingredient", "result", "experience", "cookingtime"), "smelting_test.json")

        // Stonecutting recipe
        val stoneRecipe = File(recipeDir, "stone_test.json")
        assertTrue(stoneRecipe.exists(), "Stonecutting recipe should exist")
        val stoneContent = stoneRecipe.readText()
        TestValidationUtils.assertValidJson(stoneContent, "stone_test.json")
        TestValidationUtils.assertJsonHasKeys(stoneContent, listOf("type", "ingredient", "result"), "stone_test.json")

        println("All recipe JSON files have required fields.")
    }

    // ========================================================================
    // Test 16: Biome JSON has required fields
    // ========================================================================

    @Test
    fun `generated biome has required JSON fields`() {
        CreateBiomeCommand().parse(arrayOf("test_biome"))

        val biomeFile = context.file("versions/shared/v1/data/$modId/worldgen/biome/test_biome.json")
        assertTrue(biomeFile.exists(), "Biome JSON should exist")

        val content = biomeFile.readText()
        TestValidationUtils.assertValidJson(content, "test_biome.json")
        TestValidationUtils.assertJsonHasKeys(
            content,
            listOf("has_precipitation", "temperature", "downfall", "effects", "spawners", "features"),
            "test_biome.json"
        )

        println("Biome JSON has all required fields.")
    }

    // ========================================================================
    // Test 17: Tag JSON has required fields
    // ========================================================================

    @Test
    fun `generated tag has required JSON fields`() {
        CreateTagCommand().parse(arrayOf("test_tag", "--type", "block", "--values", "$modId:ruby_ore,$modId:crystal_block"))

        val tagFile = context.file("versions/shared/v1/data/$modId/tags/block/test_tag.json")
        assertTrue(tagFile.exists(), "Tag JSON should exist")

        val content = tagFile.readText()
        TestValidationUtils.assertValidJson(content, "test_tag.json")
        TestValidationUtils.assertJsonHasKeys(content, listOf("values"), "test_tag.json")

        // Verify tag values are present
        assertTrue(
            content.contains("$modId:ruby_ore"),
            "Tag should contain ruby_ore value"
        )
        assertTrue(
            content.contains("$modId:crystal_block"),
            "Tag should contain crystal_block value"
        )

        println("Tag JSON has required fields and values.")
    }

    // ========================================================================
    // Test 18: Entity spawn egg is generated correctly
    // ========================================================================

    @Test
    fun `entity spawn egg generates valid item class`() {
        CreateEntityCommand().parse(arrayOf("spawn_test_mob", "--spawn-egg", "true"))

        val spawnEggFile = context.file(
            "shared/common/src/main/java/com/$sanitizedModId/items/SpawnTestMobSpawnEgg.java"
        )
        assertTrue(spawnEggFile.exists(), "Spawn egg Java file should exist")

        val content = spawnEggFile.readText()
        TestValidationUtils.assertValidJavaSyntax(content, spawnEggFile.name)
        TestValidationUtils.assertClassNameMatchesFile(content, spawnEggFile.name)

        assertTrue(
            content.contains("SpawnEggItem"),
            "Spawn egg should reference SpawnEggItem"
        )
        assertTrue(
            content.contains("PRIMARY_COLOR"),
            "Spawn egg should define PRIMARY_COLOR"
        )
        assertTrue(
            content.contains("SECONDARY_COLOR"),
            "Spawn egg should define SECONDARY_COLOR"
        )

        // Verify spawn egg model JSON exists
        val modelFile = context.file(
            "versions/shared/v1/assets/$modId/models/item/spawn_test_mob_spawn_egg.json"
        )
        assertTrue(modelFile.exists(), "Spawn egg model JSON should exist")
        TestValidationUtils.assertValidJson(modelFile.readText(), "spawn_test_mob_spawn_egg.json")

        println("Entity spawn egg generated correctly with valid class and model.")
    }

    // ========================================================================
    // Test 19: Shared common code has correct Architectury pattern
    // ========================================================================

    @Test
    fun `shared common code has correct Architectury pattern`() {
        // Main mod class should exist (Architectury replaces Services.java/PlatformHelper.java)
        val modClassFile = context.file(
            "shared/common/src/main/java/com/$sanitizedModId/TestMod.java"
        )
        assertTrue(modClassFile.exists(), "TestMod.java should exist")

        val modContent = modClassFile.readText()
        TestValidationUtils.assertValidJavaSyntax(modContent, "TestMod.java")

        assertTrue(
            modContent.contains("MOD_ID"),
            "TestMod should have MOD_ID constant"
        )
        assertTrue(
            modContent.contains("\"$modId\""),
            "MOD_ID should equal the configured mod ID"
        )

        // Services.java and PlatformHelper.java should NOT exist (replaced by Architectury)
        val servicesFile = context.file("shared/common/src/main/java/com/$sanitizedModId/Services.java")
        assertFalse(servicesFile.exists(), "Services.java should NOT exist with Architectury")

        val platformHelperFile = context.file(
            "shared/common/src/main/java/com/$sanitizedModId/platform/PlatformHelper.java"
        )
        assertFalse(platformHelperFile.exists(), "PlatformHelper.java should NOT exist with Architectury")

        println("Shared common code has correct Architectury pattern.")
    }

    // ========================================================================
    // Test 20: Block loot tables and blockstates are generated
    // ========================================================================

    @Test
    fun `block generation creates loot tables and blockstates`() {
        CreateBlockCommand().parse(arrayOf("loot_test_block"))

        // Blockstate
        val blockstateFile = context.file(
            "versions/shared/v1/assets/$modId/blockstates/loot_test_block.json"
        )
        assertTrue(blockstateFile.exists(), "Blockstate JSON should exist")
        TestValidationUtils.assertValidJson(blockstateFile.readText(), "blockstate")

        // Block model
        val blockModelFile = context.file(
            "versions/shared/v1/assets/$modId/models/block/loot_test_block.json"
        )
        assertTrue(blockModelFile.exists(), "Block model JSON should exist")
        TestValidationUtils.assertValidJson(blockModelFile.readText(), "block model")

        // Item model for block
        val itemModelFile = context.file(
            "versions/shared/v1/assets/$modId/models/item/loot_test_block.json"
        )
        assertTrue(itemModelFile.exists(), "Block item model JSON should exist")
        TestValidationUtils.assertValidJson(itemModelFile.readText(), "block item model")

        // Loot table
        val lootTableFile = context.file(
            "versions/shared/v1/data/$modId/loot_tables/blocks/loot_test_block.json"
        )
        assertTrue(lootTableFile.exists(), "Loot table JSON should exist")
        TestValidationUtils.assertValidJson(lootTableFile.readText(), "loot table")

        println("Block generation creates complete loot tables, blockstates, and models.")
    }
}
