package dev.dropper.integration

import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.list.*
import dev.dropper.indexer.*
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive E2E tests for list commands
 * Tests indexing, filtering, formatting, caching, and export functionality
 */
class ListCommandE2ETest {

    private lateinit var context: TestProjectContext
    private val originalOut = System.out
    private lateinit var outputStream: ByteArrayOutputStream

    @BeforeEach
    fun setup() {
        // Create a test project context
        context = TestProjectContext.create("test-list")

        // Generate a minimal project
        context.createDefaultProject(
            id = "testlist",
            name = "Test List Mod",
            minecraftVersions = listOf("1.20.1", "1.21.0"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        // Capture output
        outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))
    }

    @AfterEach
    fun cleanup() {
        // Restore original output
        System.setOut(originalOut)

        // Clean up test project
        context.cleanup()
    }

    // ===== BASIC LISTING TESTS =====

    @Test
    fun `list items finds all items`() {
        println("\n=== Test: List Items ===")

        // Create test items
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_sword", "--type", "tool"))
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_apple", "--type", "food"))

        // Clear output
        outputStream.reset()

        // Run list items command
        val command = ListItemsCommand()
        command.parse(emptyArray())

        val output = outputStream.toString()
        assertTrue(output.contains("ruby"), "Should list ruby item")
        assertTrue(output.contains("ruby_sword"), "Should list ruby_sword item")
        assertTrue(output.contains("ruby_apple"), "Should list ruby_apple item")
        assertTrue(output.contains("3 total") || output.contains("ITEMS (3"), "Should show total count")

        println("PASS: All items found")
    }

    @Test
    fun `list blocks finds all blocks`() {
        println("\n=== Test: List Blocks ===")

        // Create test blocks
        CreateBlockCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_ore", "--type", "ore"))
        CreateBlockCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_block", "--type", "basic"))

        // Clear output
        outputStream.reset()

        // Run list blocks command
        val command = ListBlocksCommand()
        command.parse(emptyArray())

        val output = outputStream.toString()
        assertTrue(output.contains("ruby_ore"), "Should list ruby_ore block")
        assertTrue(output.contains("ruby_block"), "Should list ruby_block block")
        assertTrue(output.contains("2 total") || output.contains("BLOCKS (2"), "Should show total count")

        println("PASS: All blocks found")
    }

    @Test
    fun `list entities handles empty list`() {
        println("\n=== Test: List Entities (Empty) ===")

        // Run list entities command with no entities
        val command = ListEntitiesCommand()
        command.parse(emptyArray())

        val output = outputStream.toString()
        assertTrue(
            output.contains("No entities found") || output.contains("0 total"),
            "Should handle empty entity list"
        )

        println("PASS: Empty entity list handled correctly")
    }

    @Test
    fun `list recipes finds all recipes`() {
        println("\n=== Test: List Recipes ===")

        // Create items with recipes
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--recipe", "true"))
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("sapphire", "--recipe", "true"))

        // Clear output
        outputStream.reset()

        // Run list recipes command
        val command = ListRecipesCommand()
        command.parse(emptyArray())

        val output = outputStream.toString()
        assertTrue(output.contains("ruby") || output.contains("2 total"), "Should list recipes")

        println("PASS: All recipes found")
    }

    @Test
    fun `list all shows complete inventory`() {
        println("\n=== Test: List All ===")

        // Create various components
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))
        CreateBlockCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_ore", "--type", "ore"))

        // Clear output
        outputStream.reset()

        // Run list all command
        val command = ListAllCommand()
        command.parse(emptyArray())

        val output = outputStream.toString()
        assertTrue(output.contains("COMPLETE PROJECT INVENTORY"), "Should show complete inventory header")

        println("PASS: Complete inventory shown")
    }

    // ===== FORMAT TESTS =====

    @Test
    fun `list items with table format`() {
        println("\n=== Test: Table Format ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))

        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(arrayOf("--format", "table"))

        val output = outputStream.toString()
        assertTrue(output.contains("ruby"), "Should show item in table format")

        println("PASS: Table format works")
    }

    @Test
    fun `list items with json format`() {
        println("\n=== Test: JSON Format ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))

        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(arrayOf("--format", "json"))

        val output = outputStream.toString()
        assertTrue(output.contains("\"type\"") && output.contains("\"count\""), "Should be valid JSON")
        assertTrue(output.contains("\"name\"") && output.contains("ruby"), "Should contain item data")

        println("PASS: JSON format works")
    }

    @Test
    fun `list items with csv format`() {
        println("\n=== Test: CSV Format ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("sapphire", "--type", "basic"))

        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(arrayOf("--format", "csv"))

        val output = outputStream.toString()
        assertTrue(output.contains("name,type,class,package"), "Should have CSV header")
        assertTrue(output.contains("ruby,item"), "Should contain ruby data")
        assertTrue(output.contains("sapphire,item"), "Should contain sapphire data")

        println("PASS: CSV format works")
    }

    @Test
    fun `list items with tree format`() {
        println("\n=== Test: Tree Format ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_sword", "--type", "tool"))

        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(arrayOf("--format", "tree"))

        val output = outputStream.toString()
        assertTrue(output.contains("ITEMS"), "Should show ITEMS header")
        assertTrue(output.contains("ruby"), "Should list ruby item")

        println("PASS: Tree format works")
    }

    // ===== FILTER TESTS =====

    @Test
    fun `list items with search filter`() {
        println("\n=== Test: Search Filter ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_sword", "--type", "tool"))
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("iron_sword", "--type", "tool"))
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_apple", "--type", "food"))

        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(arrayOf("--search", "ruby"))

        val output = outputStream.toString()
        assertTrue(output.contains("ruby_sword"), "Should find ruby_sword")
        assertTrue(output.contains("ruby_apple"), "Should find ruby_apple")
        assertFalse(output.contains("iron_sword"), "Should not find iron_sword")

        println("PASS: Search filter works")
    }

    @Test
    fun `list items with loader filter`() {
        println("\n=== Test: Loader Filter ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))

        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(arrayOf("--loader", "fabric"))

        val output = outputStream.toString()
        assertTrue(output.contains("ruby"), "Should find items for fabric loader")

        println("PASS: Loader filter works")
    }

    @Test
    fun `list items with combined filters`() {
        println("\n=== Test: Combined Filters ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_sword", "--type", "tool"))
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("iron_sword", "--type", "tool"))

        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(arrayOf("--search", "ruby", "--loader", "fabric"))

        val output = outputStream.toString()
        assertTrue(output.contains("ruby_sword"), "Should find ruby_sword with combined filters")

        println("PASS: Combined filters work")
    }

    // ===== EXPORT TESTS =====

    @Test
    fun `export items to json file`() {
        println("\n=== Test: Export JSON ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))

        val exportFile = File(context.projectDir, "items.json")

        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(arrayOf("--format", "json", "--export", exportFile.absolutePath))

        assertTrue(exportFile.exists(), "Export file should exist")
        val content = exportFile.readText()
        assertTrue(content.contains("\"name\""), "Exported file should contain JSON data")
        assertTrue(content.contains("ruby"), "Exported file should contain item data")

        println("PASS: JSON export works")
    }

    @Test
    fun `export items to csv file`() {
        println("\n=== Test: Export CSV ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("sapphire", "--type", "basic"))

        val exportFile = File(context.projectDir, "items.csv")

        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(arrayOf("--format", "csv", "--export", exportFile.absolutePath))

        assertTrue(exportFile.exists(), "Export file should exist")
        val content = exportFile.readText()
        assertTrue(content.contains("name,type,class"), "Should have CSV header")
        assertTrue(content.contains("ruby,item"), "Should contain ruby")
        assertTrue(content.contains("sapphire,item"), "Should contain sapphire")

        println("PASS: CSV export works")
    }

    @Test
    fun `export all components to file`() {
        println("\n=== Test: Export All ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))
        CreateBlockCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_ore", "--type", "ore"))

        val exportFile = File(context.projectDir, "inventory.txt")

        outputStream.reset()
        val command = ListAllCommand()
        command.parse(arrayOf("--export", exportFile.absolutePath))

        assertTrue(exportFile.exists(), "Export file should exist")
        val content = exportFile.readText()
        assertTrue(content.contains("COMPLETE PROJECT INVENTORY"), "Should have inventory header")

        println("PASS: Export all works")
    }

    // ===== INDEXING TESTS =====

    @Test
    fun `item indexer detects all item properties`() {
        println("\n=== Test: Item Indexer Properties ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_sword", "--type", "tool"))

        val indexer = ItemIndexer()
        val items = indexer.index(context.projectDir)

        assertEquals(1, items.size, "Should find 1 item")
        val item = items[0]
        assertEquals("ruby_sword", item.name, "Should have correct name")
        assertEquals("item", item.type, "Should have correct type")
        assertEquals("RubySword", item.className, "Should have correct class name")
        assertTrue(item.hasModel, "Should detect model")
        assertTrue(item.loaders.isNotEmpty(), "Should detect loaders")

        println("PASS: Item indexer works correctly")
    }

    @Test
    fun `block indexer detects all block properties`() {
        println("\n=== Test: Block Indexer Properties ===")

        CreateBlockCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_ore", "--type", "ore"))

        val indexer = BlockIndexer()
        val blocks = indexer.index(context.projectDir)

        assertEquals(1, blocks.size, "Should find 1 block")
        val block = blocks[0]
        assertEquals("ruby_ore", block.name, "Should have correct name")
        assertEquals("block", block.type, "Should have correct type")
        assertEquals("RubyOre", block.className, "Should have correct class name")
        assertTrue(block.hasModel, "Should detect model")
        assertTrue(block.hasLootTable, "Should detect loot table")

        println("PASS: Block indexer works correctly")
    }

    @Test
    fun `recipe indexer detects recipe types`() {
        println("\n=== Test: Recipe Indexer ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--recipe", "true"))

        val indexer = RecipeIndexer()
        val recipes = indexer.index(context.projectDir)

        assertEquals(1, recipes.size, "Should find 1 recipe")
        val recipe = recipes[0]
        assertEquals("ruby", recipe.name, "Should have correct name")
        assertEquals("recipe", recipe.type, "Should have correct type")

        println("PASS: Recipe indexer works correctly")
    }

    // ===== CACHE TESTS =====

    @Test
    fun `cache is created after first index`() {
        println("\n=== Test: Cache Creation ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))

        // First index should create cache
        val command = ListItemsCommand()
        command.parse(emptyArray())

        val cacheFile = File(context.projectDir, ".dropper/cache/index.json")
        assertTrue(cacheFile.exists(), "Cache file should be created")

        println("PASS: Cache created")
    }

    @Test
    fun `cache is reused on subsequent calls`() {
        println("\n=== Test: Cache Reuse ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))

        // First call creates cache
        ListItemsCommand().apply { projectDir = context.projectDir }.parse(emptyArray())

        val cacheFile = File(context.projectDir, ".dropper/cache/index.json")
        val firstModified = cacheFile.lastModified()

        // Wait a bit
        Thread.sleep(100)

        // Second call should reuse cache
        ListItemsCommand().apply { projectDir = context.projectDir }.parse(emptyArray())

        val secondModified = cacheFile.lastModified()
        assertEquals(firstModified, secondModified, "Cache should be reused")

        println("PASS: Cache reused")
    }

    @Test
    fun `cache is invalidated on file changes`() {
        println("\n=== Test: Cache Invalidation ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))

        // Create cache
        ListItemsCommand().apply { projectDir = context.projectDir }.parse(emptyArray())

        val cacheFile = File(context.projectDir, ".dropper/cache/index.json")
        val firstModified = cacheFile.lastModified()

        // Wait and create new item
        Thread.sleep(100)
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("sapphire", "--type", "basic"))

        // Next index should invalidate cache
        ListItemsCommand().apply { projectDir = context.projectDir }.parse(emptyArray())

        val secondModified = cacheFile.lastModified()
        assertTrue(secondModified > firstModified, "Cache should be invalidated and recreated")

        println("PASS: Cache invalidated correctly")
    }

    @Test
    fun `manual cache invalidation works`() {
        println("\n=== Test: Manual Cache Invalidation ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))

        // Create cache
        ListItemsCommand().apply { projectDir = context.projectDir }.parse(emptyArray())

        val cacheFile = File(context.projectDir, ".dropper/cache/index.json")
        assertTrue(cacheFile.exists(), "Cache should exist")

        // Invalidate cache
        IndexCache.invalidate(context.projectDir)

        assertFalse(cacheFile.exists(), "Cache should be deleted")

        println("PASS: Manual invalidation works")
    }

    // ===== EDGE CASES =====

    @Test
    fun `list command handles empty project`() {
        println("\n=== Test: Empty Project ===")

        // Don't create any components
        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(emptyArray())

        val output = outputStream.toString()
        assertTrue(
            output.contains("No items found") || output.contains("0 total"),
            "Should handle empty project"
        )

        println("PASS: Empty project handled")
    }

    @Test
    fun `list command handles large project`() {
        println("\n=== Test: Large Project (20+ items) ===")

        // Create many items
        for (i in 1..25) {
            CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("item_$i", "--type", "basic", "--recipe", "false"))
        }

        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(emptyArray())

        val output = outputStream.toString()
        assertTrue(output.contains("25 total") || output.contains("ITEMS (25"), "Should list all 25 items")

        println("PASS: Large project handled")
    }

    @Test
    fun `list command handles missing files gracefully`() {
        println("\n=== Test: Missing Files ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))

        // Delete texture file
        val textureFile = File(context.projectDir, "versions/shared/v1/assets/testlist/textures/item/ruby.png")
        textureFile.delete()

        outputStream.reset()
        val command = ListItemsCommand()
        command.parse(emptyArray())

        val output = outputStream.toString()
        assertTrue(output.contains("ruby"), "Should still list item even with missing texture")

        println("PASS: Missing files handled gracefully")
    }

    // ===== INTEGRATION TESTS =====

    @Test
    fun `list updates after create item command`() {
        println("\n=== Test: List After Create Item ===")

        // Initial list should be empty
        var items = ItemIndexer().index(context.projectDir)
        assertEquals(0, items.size, "Should start with no items")

        // Create item
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))

        // Invalidate cache to force reindex
        IndexCache.invalidate(context.projectDir)

        // List should now show item
        items = ItemIndexer().index(context.projectDir)
        assertEquals(1, items.size, "Should have 1 item after creation")

        println("PASS: List updates after create")
    }

    @Test
    fun `list updates after create block command`() {
        println("\n=== Test: List After Create Block ===")

        var blocks = BlockIndexer().index(context.projectDir)
        assertEquals(0, blocks.size, "Should start with no blocks")

        CreateBlockCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_ore", "--type", "ore"))

        IndexCache.invalidate(context.projectDir)
        blocks = BlockIndexer().index(context.projectDir)
        assertEquals(1, blocks.size, "Should have 1 block after creation")

        println("PASS: List updates after create block")
    }

    @Test
    fun `list all after multiple creates`() {
        println("\n=== Test: List All After Multiple Creates ===")

        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby", "--type", "basic"))
        CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("sapphire", "--type", "basic"))
        CreateBlockCommand().apply { projectDir = context.projectDir }.parse(arrayOf("ruby_ore", "--type", "ore"))
        CreateBlockCommand().apply { projectDir = context.projectDir }.parse(arrayOf("sapphire_ore", "--type", "ore"))

        IndexCache.invalidate(context.projectDir)

        outputStream.reset()
        val command = ListAllCommand()
        command.parse(emptyArray())

        val output = outputStream.toString()
        assertTrue(output.contains("ruby"), "Should list ruby")
        assertTrue(output.contains("sapphire"), "Should list sapphire")
        assertTrue(output.contains("ruby_ore"), "Should list ruby_ore")
        assertTrue(output.contains("sapphire_ore"), "Should list sapphire_ore")

        println("PASS: List all shows all components")
    }

    // ===== PERFORMANCE TESTS =====

    @Test
    fun `indexing 50 components completes in reasonable time`() {
        println("\n=== Test: Performance (50 components) ===")

        // Create 50 items
        for (i in 1..50) {
            CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("item_$i", "--type", "basic", "--recipe", "false"))
        }

        val startTime = System.currentTimeMillis()

        val indexer = ItemIndexer()
        val items = indexer.index(context.projectDir)

        val duration = System.currentTimeMillis() - startTime

        assertEquals(50, items.size, "Should index all 50 items")
        assertTrue(duration < 5000, "Indexing should complete in less than 5 seconds (took ${duration}ms)")

        println("PASS: Performance test completed in ${duration}ms")
    }

    @Test
    fun `cache improves performance`() {
        println("\n=== Test: Cache Performance ===")

        // Create items
        for (i in 1..20) {
            CreateItemCommand().apply { projectDir = context.projectDir }.parse(arrayOf("item_$i", "--type", "basic", "--recipe", "false"))
        }

        // First call (no cache)
        IndexCache.invalidate(context.projectDir)
        val startTime1 = System.currentTimeMillis()
        ListItemsCommand().apply { projectDir = context.projectDir }.parse(emptyArray())
        val duration1 = System.currentTimeMillis() - startTime1

        // Second call (with cache)
        val startTime2 = System.currentTimeMillis()
        ListItemsCommand().apply { projectDir = context.projectDir }.parse(emptyArray())
        val duration2 = System.currentTimeMillis() - startTime2

        println("First call: ${duration1}ms, Second call: ${duration2}ms")
        assertTrue(duration2 <= duration1, "Cached call should be faster or equal")

        println("PASS: Cache improves performance")
    }
}
