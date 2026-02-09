package dev.dropper.integration

import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.indexer.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Basic E2E tests for list command indexers
 * Tests core indexing functionality without running full CLI commands
 */
class ListCommandBasicTest {

    private lateinit var testProjectDir: File
    private val originalUserDir = System.getProperty("user.dir")

    @BeforeEach
    fun setup() {
        // Create a test project
        testProjectDir = File("build/test-list-basic/${System.currentTimeMillis()}/test-mod")
        testProjectDir.mkdirs()

        // Generate a minimal project
        val config = ModConfig(
            id = "testlist",
            name = "Test List Mod",
            version = "1.0.0",
            description = "Test mod for list indexing",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)

        // Change working directory to test project
        System.setProperty("user.dir", testProjectDir.absolutePath)
    }

    @AfterEach
    fun cleanup() {
        // Restore original working directory
        System.setProperty("user.dir", originalUserDir)

        // Clean up test project
        if (testProjectDir.exists()) {
            testProjectDir.deleteRecursively()
        }
    }

    @Test
    fun `item indexer finds created items`() {
        println("\n=== Test: Item Indexer ===")

        // Create test items
        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        CreateItemCommand().parse(arrayOf("ruby_sword", "--type", "tool"))

        // Index items
        val indexer = ItemIndexer()
        val items = indexer.index(testProjectDir)

        // Verify
        assertEquals(2, items.size, "Should find 2 items")
        assertTrue(items.any { it.name == "ruby" }, "Should find ruby item")
        assertTrue(items.any { it.name == "ruby_sword" }, "Should find ruby_sword item")

        println("PASS: Found ${items.size} items")
    }

    @Test
    fun `block indexer finds created blocks`() {
        println("\n=== Test: Block Indexer ===")

        // Create test blocks
        CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))

        // Index blocks
        val indexer = BlockIndexer()
        val blocks = indexer.index(testProjectDir)

        // Verify
        assertEquals(1, blocks.size, "Should find 1 block")
        assertEquals("ruby_ore", blocks[0].name, "Should find ruby_ore block")
        assertTrue(blocks[0].hasModel, "Should detect model")
        assertTrue(blocks[0].hasLootTable, "Should detect loot table")

        println("PASS: Found ${blocks.size} blocks")
    }

    @Test
    fun `recipe indexer finds created recipes`() {
        println("\n=== Test: Recipe Indexer ===")

        // Create item with recipe
        CreateItemCommand().parse(arrayOf("ruby", "--recipe", "true"))

        // Index recipes
        val indexer = RecipeIndexer()
        val recipes = indexer.index(testProjectDir)

        // Verify
        assertEquals(1, recipes.size, "Should find 1 recipe")
        assertEquals("ruby", recipes[0].name, "Should find ruby recipe")

        println("PASS: Found ${recipes.size} recipes")
    }

    @Test
    fun `formatters produce correct output`() {
        println("\n=== Test: Output Formatters ===")

        // Create test data
        val components = listOf(
            ComponentInfo(
                name = "test_item",
                type = "item",
                className = "TestItem",
                packageName = "com.testlist.items",
                hasModel = true,
                hasTexture = false,
                loaders = listOf("fabric", "forge")
            )
        )

        // Test table formatter
        val tableFormatter = TableFormatter()
        val tableOutput = tableFormatter.format(components, "item")
        assertTrue(tableOutput.contains("test_item"), "Table should contain item name")
        assertTrue(tableOutput.contains("1 total"), "Table should show count")

        // Test JSON formatter
        val jsonFormatter = JsonFormatter()
        val jsonOutput = jsonFormatter.format(components, "item")
        assertTrue(jsonOutput.contains("\"name\""), "JSON should have name field")
        assertTrue(jsonOutput.contains("test_item"), "JSON should contain item")

        // Test CSV formatter
        val csvFormatter = CsvFormatter()
        val csvOutput = csvFormatter.format(components, "item")
        assertTrue(csvOutput.contains("name,type,class"), "CSV should have header")
        assertTrue(csvOutput.contains("test_item,item"), "CSV should contain item")

        // Test tree formatter
        val treeFormatter = TreeFormatter()
        val treeOutput = treeFormatter.format(components, "item")
        assertTrue(treeOutput.contains("test_item"), "Tree should contain item")

        println("PASS: All formatters work correctly")
    }

    @Test
    fun `cache saves and loads correctly`() {
        println("\n=== Test: Cache System ===")

        // Create test item
        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Index and cache
        val components = mapOf(
            "items" to ItemIndexer().index(testProjectDir)
        )

        IndexCache.save(testProjectDir, components)

        val cacheFile = File(testProjectDir, ".dropper/cache/index.json")
        assertTrue(cacheFile.exists(), "Cache file should exist")

        // Load cache
        val cached = IndexCache.get(testProjectDir)
        assertTrue(cached != null, "Should load cache")
        assertEquals(1, cached!!["items"]?.size, "Should have 1 cached item")

        println("PASS: Cache system works")
    }

    @Test
    fun `indexer handles empty project`() {
        println("\n=== Test: Empty Project ===")

        // Index empty project
        val itemIndexer = ItemIndexer()
        val items = itemIndexer.index(testProjectDir)

        val blockIndexer = BlockIndexer()
        val blocks = blockIndexer.index(testProjectDir)

        // Verify
        assertEquals(0, items.size, "Should find no items")
        assertEquals(0, blocks.size, "Should find no blocks")

        println("PASS: Empty project handled correctly")
    }

    @Test
    fun `indexer handles multiple components`() {
        println("\n=== Test: Multiple Components ===")

        // Create many components
        for (i in 1..10) {
            CreateItemCommand().parse(arrayOf("item_$i", "--type", "basic", "--recipe", "false"))
        }

        // Index
        val indexer = ItemIndexer()
        val items = indexer.index(testProjectDir)

        // Verify
        assertEquals(10, items.size, "Should find 10 items")
        assertTrue(items.all { it.name.startsWith("item_") }, "All items should have correct prefix")

        println("PASS: Multiple components indexed correctly")
    }

    @Test
    fun `component info contains correct metadata`() {
        println("\n=== Test: Component Metadata ===")

        // Create item
        CreateItemCommand().parse(arrayOf("ruby_sword", "--type", "tool"))

        // Index
        val indexer = ItemIndexer()
        val items = indexer.index(testProjectDir)

        // Verify metadata
        val item = items[0]
        assertEquals("ruby_sword", item.name)
        assertEquals("item", item.type)
        assertEquals("RubySword", item.className)
        assertEquals("com.testlist.items", item.packageName)
        assertTrue(item.hasModel, "Should detect model")
        assertTrue(item.loaders.contains("fabric"), "Should detect fabric loader")
        assertTrue(item.loaders.contains("forge"), "Should detect forge loader")
        assertTrue(item.loaders.contains("neoforge"), "Should detect neoforge loader")

        println("PASS: Metadata is correct")
    }
}
