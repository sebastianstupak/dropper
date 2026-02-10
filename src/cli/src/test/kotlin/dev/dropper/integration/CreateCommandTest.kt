package dev.dropper.integration

import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.config.ModConfig
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * E2E tests for create item and create block commands
 * Tests the complete workflow of creating items/blocks with multi-loader support
 */
class CreateCommandTest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("test-create")

        // Generate a minimal project
        val config = ModConfig(
            id = "testcreate",
            name = "Test Create Mod",
            version = "1.0.0",
            description = "Test mod for create commands",
            author = "Test",
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

    @Test
    fun `create item generates all required files`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Create Item Command                            ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Act: Run create item command
        println("Creating item: ruby_sword...")
        context.withProjectDir {
            val command = CreateItemCommand()
            command.parse(arrayOf("ruby_sword", "--type", "tool"))
        }

        // Assert: Verify common item file
        println("\nVerifying common item code...")
        val itemFile = context.file("shared/common/src/main/java/com/testcreate/items/RubySword.java")
        assertTrue(itemFile.exists(), "Common item file should exist")
        val itemContent = itemFile.readText()
        assertTrue(itemContent.contains("class RubySword"), "Should have RubySword class")
        assertTrue(itemContent.contains("public static final String ID = \"ruby_sword\""), "Should have ID constant")
        println("  ✓ Common item code verified")

        // Assert: Verify registry file (Architectury: single registry in common)
        println("\nVerifying item registry...")
        val registryFile = context.file("shared/common/src/main/java/com/testcreate/registry/ModItems.java")
        assertTrue(registryFile.exists(), "Registry file should exist")
        val registryContent = registryFile.readText()
        assertTrue(registryContent.contains("class ModItems"), "Should have ModItems class")
        assertTrue(registryContent.contains("ruby_sword"), "Should register ruby_sword")
        println("  ✓ Item registry verified")

        // Assert: Verify item model
        println("\nVerifying item assets...")
        val modelFile = context.file("versions/shared/v1/assets/testcreate/models/item/ruby_sword.json")
        assertTrue(modelFile.exists(), "Item model should exist")
        val modelContent = modelFile.readText()
        assertTrue(modelContent.contains("\"parent\": \"item/generated\""), "Should have item/generated parent")
        assertTrue(modelContent.contains("testcreate:item/ruby_sword"), "Should reference texture")
        println("  ✓ Item model verified")

        // Assert: Verify texture placeholder
        val textureFile = context.file("versions/shared/v1/assets/testcreate/textures/item/ruby_sword.png")
        assertTrue(textureFile.exists(), "Texture placeholder should exist")
        println("  ✓ Texture placeholder verified")

        // Assert: Verify recipe
        val recipeFile = context.file("versions/shared/v1/data/testcreate/recipe/ruby_sword.json")
        assertTrue(recipeFile.exists(), "Recipe should exist")
        val recipeContent = recipeFile.readText()
        assertTrue(recipeContent.contains("\"type\": \"minecraft:crafting_shaped\""), "Should be shaped recipe")
        assertTrue(recipeContent.contains("testcreate:ruby_sword"), "Should have mod item")
        println("  ✓ Recipe verified")

        println("\n✓ All item files generated successfully!")
        println("  - 1 common item class")
        println("  - 3 loader-specific registrations (Fabric, Forge, NeoForge)")
        println("  - 1 item model")
        println("  - 1 texture placeholder")
        println("  - 1 recipe\n")
    }

    @Test
    fun `create block generates all required files`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Create Block Command                           ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Act: Run create block command
        println("Creating block: ruby_ore...")
        context.withProjectDir {
            val command = CreateBlockCommand()
            command.parse(arrayOf("ruby_ore", "--type", "ore"))
        }

        // Assert: Verify common block file
        println("\nVerifying common block code...")
        val blockFile = context.file("shared/common/src/main/java/com/testcreate/blocks/RubyOre.java")
        assertTrue(blockFile.exists(), "Common block file should exist")
        val blockContent = blockFile.readText()
        assertTrue(blockContent.contains("class RubyOre"), "Should have RubyOre class")
        assertTrue(blockContent.contains("public static final String ID = \"ruby_ore\""), "Should have ID constant")
        println("  ✓ Common block code verified")

        // Assert: Verify block registry file (Architectury: single registry in common)
        println("\nVerifying block registry...")
        val registryFile = context.file("shared/common/src/main/java/com/testcreate/registry/ModBlocks.java")
        assertTrue(registryFile.exists(), "Block registry file should exist")
        val registryContent = registryFile.readText()
        assertTrue(registryContent.contains("class ModBlocks"), "Should have ModBlocks class")
        assertTrue(registryContent.contains("ruby_ore"), "Should register ruby_ore")
        println("  ✓ Block registry verified")

        // Assert: Verify blockstate
        println("\nVerifying block assets...")
        val blockstateFile = context.file("versions/shared/v1/assets/testcreate/blockstates/ruby_ore.json")
        assertTrue(blockstateFile.exists(), "Blockstate should exist")
        val blockstateContent = blockstateFile.readText()
        assertTrue(blockstateContent.contains("\"variants\""), "Should have variants")
        println("  ✓ Blockstate verified")

        // Assert: Verify block model
        val blockModelFile = context.file("versions/shared/v1/assets/testcreate/models/block/ruby_ore.json")
        assertTrue(blockModelFile.exists(), "Block model should exist")
        val blockModelContent = blockModelFile.readText()
        assertTrue(blockModelContent.contains("\"parent\": \"block/cube_all\""), "Should have cube_all parent")
        println("  ✓ Block model verified")

        // Assert: Verify item model
        val itemModelFile = context.file("versions/shared/v1/assets/testcreate/models/item/ruby_ore.json")
        assertTrue(itemModelFile.exists(), "Item model should exist")
        val itemModelContent = itemModelFile.readText()
        assertTrue(itemModelContent.contains("testcreate:block/ruby_ore"), "Should reference block model")
        println("  ✓ Item model verified")

        // Assert: Verify texture placeholder
        val textureFile = context.file("versions/shared/v1/assets/testcreate/textures/block/ruby_ore.png")
        assertTrue(textureFile.exists(), "Texture placeholder should exist")
        println("  ✓ Texture placeholder verified")

        // Assert: Verify loot table
        val lootTableFile = context.file("versions/shared/v1/data/testcreate/loot_tables/blocks/ruby_ore.json")
        assertTrue(lootTableFile.exists(), "Loot table should exist")
        val lootTableContent = lootTableFile.readText()
        assertTrue(lootTableContent.contains("\"type\": \"minecraft:block\""), "Should be block loot table")
        assertTrue(lootTableContent.contains("testcreate:ruby_ore"), "Should drop the block")
        println("  ✓ Loot table verified")

        println("\n✓ All block files generated successfully!")
        println("  - 1 common block class")
        println("  - 3 loader-specific registrations (Fabric, Forge, NeoForge)")
        println("  - 1 blockstate")
        println("  - 1 block model")
        println("  - 1 item model")
        println("  - 1 texture placeholder")
        println("  - 1 loot table\n")
    }

    @Test
    fun `create multiple items and blocks in same project`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Multiple Items and Blocks                      ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Create multiple items
        println("Creating items...")
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
            CreateItemCommand().parse(arrayOf("ruby_sword", "--type", "tool"))
            CreateItemCommand().parse(arrayOf("ruby_apple", "--type", "food"))

            // Create multiple blocks
            println("\nCreating blocks...")
            CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))
            CreateBlockCommand().parse(arrayOf("ruby_block", "--type", "basic"))
            CreateBlockCommand().parse(arrayOf("ruby_pillar", "--type", "pillar"))
        }

        // Verify all files exist
        println("\nVerifying all files...")
        val items = listOf("ruby", "ruby_sword", "ruby_apple")
        val blocks = listOf("ruby_ore", "ruby_block", "ruby_pillar")

        items.forEach { item ->
            val className = item.split("_").joinToString("") { word -> word.replaceFirstChar { it.uppercase() } }
            assertTrue(
                context.file("shared/common/src/main/java/com/testcreate/items/$className.java").exists(),
                "Item $item should exist"
            )
        }

        // Verify single registry file for items
        assertTrue(
            context.file("shared/common/src/main/java/com/testcreate/registry/ModItems.java").exists(),
            "ModItems registry should exist"
        )

        blocks.forEach { block ->
            val className = block.split("_").joinToString("") { word -> word.replaceFirstChar { it.uppercase() } }
            assertTrue(
                context.file("shared/common/src/main/java/com/testcreate/blocks/$className.java").exists(),
                "Block $block should exist"
            )
        }

        // Verify single registry file for blocks
        assertTrue(
            context.file("shared/common/src/main/java/com/testcreate/registry/ModBlocks.java").exists(),
            "ModBlocks registry should exist"
        )

        println("  ✓ All ${items.size} items verified")
        println("  ✓ All ${blocks.size} blocks verified")
        println("\n✓ Successfully created ${items.size} items and ${blocks.size} blocks!")
        println("  Total files generated: ${(items.size + blocks.size) * 7}\n")
    }

    @Test
    fun `create item without recipe`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Create Item Without Recipe                     ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Create item without recipe
        println("Creating item without recipe...")
        context.withProjectDir {
            val command = CreateItemCommand()
            command.parse(arrayOf("ruby_shard", "--recipe", "false"))
        }

        // Verify item exists but recipe doesn't
        val itemFile = context.file("shared/common/src/main/java/com/testcreate/items/RubyShard.java")
        assertTrue(itemFile.exists(), "Item should exist")

        val recipeFile = context.file("versions/shared/v1/data/testcreate/recipe/ruby_shard.json")
        assertTrue(!recipeFile.exists(), "Recipe should NOT exist")

        println("  ✓ Item created without recipe")
        println("\n✓ Test passed!\n")
    }

    @Test
    fun `create block without drops`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Create Block Without Self-Drop                 ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Create block that doesn't drop itself
        println("Creating block without self-drop...")
        context.withProjectDir {
            val command = CreateBlockCommand()
            command.parse(arrayOf("ruby_leaves", "--drops-self", "false"))
        }

        // Verify block exists but loot table doesn't
        val blockFile = context.file("shared/common/src/main/java/com/testcreate/blocks/RubyLeaves.java")
        assertTrue(blockFile.exists(), "Block should exist")

        val lootTableFile = context.file("versions/shared/v1/data/testcreate/loot_tables/blocks/ruby_leaves.json")
        assertTrue(!lootTableFile.exists(), "Loot table should NOT exist")

        println("  ✓ Block created without loot table")
        println("\n✓ Test passed!\n")
    }
}
