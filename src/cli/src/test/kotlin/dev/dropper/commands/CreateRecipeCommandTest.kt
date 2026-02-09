package dev.dropper.commands

import dev.dropper.util.FileUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CreateRecipeCommandTest {

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        // Create a minimal config.yml for testing
        val configFile = File(tempDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
              name: Test Mod
              version: 1.0.0
              description: Test mod for recipe creation
              author: Test Author
              license: MIT
        """.trimIndent())
    }

    @AfterEach
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test shaped crafting recipe creation`() {
        val recipeName = "iron_sword"

        // Create recipe
        executeRecipeCommand(recipeName, "crafting", mapOf("--shaped" to "true"))

        // Verify file exists
        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/iron_sword.json")
        assertTrue(recipeFile.exists(), "Recipe file should exist")

        // Verify content
        val content = FileUtil.readText(recipeFile)
        assertTrue(content.contains("\"type\": \"minecraft:crafting_shaped\""))
        assertTrue(content.contains("\"pattern\""))
        assertTrue(content.contains("\"key\""))
        assertTrue(content.contains("\"result\""))
        assertTrue(content.contains("testmod:iron_sword"))
        assertTrue(content.contains("minecraft:iron_ingot"))
        assertTrue(content.contains("minecraft:stick"))
    }

    @Test
    fun `test shapeless crafting recipe creation`() {
        val recipeName = "gold_nugget"

        // Create recipe with --shapeless flag
        executeRecipeCommand(recipeName, "crafting", mapOf("--shapeless" to ""))

        // Verify file exists
        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/gold_nugget.json")
        assertTrue(recipeFile.exists(), "Recipe file should exist")

        // Verify content
        val content = FileUtil.readText(recipeFile)
        assertTrue(content.contains("\"type\": \"minecraft:crafting_shapeless\""))
        assertTrue(content.contains("\"ingredients\""))
        assertTrue(content.contains("\"result\""))
        assertTrue(content.contains("testmod:gold_nugget"))
        assertFalse(content.contains("\"pattern\""))
        assertFalse(content.contains("\"key\""))
    }

    @Test
    fun `test default crafting is shaped`() {
        val recipeName = "default_recipe"

        // Create recipe without specifying shaped/shapeless
        executeRecipeCommand(recipeName, "crafting")

        // Verify it's shaped by default
        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/default_recipe.json")
        val content = FileUtil.readText(recipeFile)
        assertTrue(content.contains("\"type\": \"minecraft:crafting_shaped\""))
        assertTrue(content.contains("\"pattern\""))
    }

    @Test
    fun `test smelting recipe creation`() {
        val recipeName = "iron_ingot"

        // Create smelting recipe with custom experience and time
        executeRecipeCommand(recipeName, "smelting", mapOf(
            "--experience" to "0.7",
            "--cooking-time" to "200"
        ))

        // Verify file exists
        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/iron_ingot.json")
        assertTrue(recipeFile.exists(), "Recipe file should exist")

        // Verify content
        val content = FileUtil.readText(recipeFile)
        assertTrue(content.contains("\"type\": \"minecraft:smelting\""))
        assertTrue(content.contains("\"ingredient\""))
        assertTrue(content.contains("\"result\""))
        assertTrue(content.contains("\"experience\": 0.7"))
        assertTrue(content.contains("\"cookingtime\": 200"))
        assertTrue(content.contains("testmod:iron_ingot"))
    }

    @Test
    fun `test smelting default values`() {
        val recipeName = "cooked_beef"

        // Create smelting recipe without specifying experience/time
        executeRecipeCommand(recipeName, "smelting")

        // Verify default values
        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/cooked_beef.json")
        val content = FileUtil.readText(recipeFile)
        assertTrue(content.contains("\"experience\": 0.1"))
        assertTrue(content.contains("\"cookingtime\": 200"))
    }

    @Test
    fun `test blasting recipe creation`() {
        val recipeName = "iron_ingot_fast"

        // Create blasting recipe
        executeRecipeCommand(recipeName, "blasting", mapOf(
            "--experience" to "0.5",
            "--cooking-time" to "200"
        ))

        // Verify file exists
        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/iron_ingot_fast.json")
        assertTrue(recipeFile.exists(), "Recipe file should exist")

        // Verify content
        val content = FileUtil.readText(recipeFile)
        assertTrue(content.contains("\"type\": \"minecraft:blasting\""))
        assertTrue(content.contains("\"ingredient\""))
        assertTrue(content.contains("\"result\""))
        assertTrue(content.contains("\"experience\": 0.5"))
        // Blasting should halve the cooking time
        assertTrue(content.contains("\"cookingtime\": 100"))
        assertTrue(content.contains("testmod:iron_ingot_fast"))
    }

    @Test
    fun `test smoking recipe creation`() {
        val recipeName = "cooked_porkchop"

        // Create smoking recipe
        executeRecipeCommand(recipeName, "smoking", mapOf(
            "--experience" to "0.35",
            "--cooking-time" to "200"
        ))

        // Verify file exists
        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/cooked_porkchop.json")
        assertTrue(recipeFile.exists(), "Recipe file should exist")

        // Verify content
        val content = FileUtil.readText(recipeFile)
        assertTrue(content.contains("\"type\": \"minecraft:smoking\""))
        assertTrue(content.contains("\"ingredient\""))
        assertTrue(content.contains("\"result\""))
        assertTrue(content.contains("\"experience\": 0.35"))
        // Smoking should halve the cooking time
        assertTrue(content.contains("\"cookingtime\": 100"))
        assertTrue(content.contains("testmod:cooked_porkchop"))
    }

    @Test
    fun `test stonecutting recipe creation`() {
        val recipeName = "stone_slab"

        // Create stonecutting recipe
        executeRecipeCommand(recipeName, "stonecutting")

        // Verify file exists
        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/stone_slab.json")
        assertTrue(recipeFile.exists(), "Recipe file should exist")

        // Verify content
        val content = FileUtil.readText(recipeFile)
        assertTrue(content.contains("\"type\": \"minecraft:stonecutting\""))
        assertTrue(content.contains("\"ingredient\""))
        assertTrue(content.contains("\"result\""))
        assertTrue(content.contains("testmod:stone_slab"))
        assertTrue(content.contains("\"count\": 1"))
        assertFalse(content.contains("experience"))
        assertFalse(content.contains("cookingtime"))
    }

    @Test
    fun `test smithing recipe creation`() {
        val recipeName = "netherite_sword"

        // Create smithing recipe
        executeRecipeCommand(recipeName, "smithing")

        // Verify file exists
        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/netherite_sword.json")
        assertTrue(recipeFile.exists(), "Recipe file should exist")

        // Verify content
        val content = FileUtil.readText(recipeFile)
        assertTrue(content.contains("\"type\": \"minecraft:smithing_transform\""))
        assertTrue(content.contains("\"template\""))
        assertTrue(content.contains("\"base\""))
        assertTrue(content.contains("\"addition\""))
        assertTrue(content.contains("\"result\""))
        assertTrue(content.contains("minecraft:netherite_upgrade_smithing_template"))
        assertTrue(content.contains("minecraft:diamond_sword"))
        assertTrue(content.contains("minecraft:netherite_ingot"))
        assertTrue(content.contains("testmod:netherite_sword"))
    }

    @Test
    fun `test recipe with underscores in name`() {
        val recipeName = "my_custom_recipe_name"

        executeRecipeCommand(recipeName, "crafting")

        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/my_custom_recipe_name.json")
        assertTrue(recipeFile.exists())

        val content = FileUtil.readText(recipeFile)
        assertTrue(content.contains("testmod:my_custom_recipe_name"))
    }

    @Test
    fun `test recipe directory is created`() {
        val recipeName = "test_recipe"

        // Ensure recipe directory doesn't exist yet
        val recipeDir = File(tempDir, "versions/shared/v1/data/testmod/recipe")
        assertFalse(recipeDir.exists())

        executeRecipeCommand(recipeName, "crafting")

        // Verify directory was created
        assertTrue(recipeDir.exists())
        assertTrue(recipeDir.isDirectory)
    }

    @Test
    fun `test all recipe types create valid JSON structure`() {
        val types = listOf("crafting", "smelting", "blasting", "smoking", "stonecutting", "smithing")

        types.forEachIndexed { index, type ->
            val recipeName = "${type}_recipe_$index"
            executeRecipeCommand(recipeName, type)

            val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/$recipeName.json")
            assertTrue(recipeFile.exists(), "Recipe file for type $type should exist")

            val content = FileUtil.readText(recipeFile)
            // All recipes should have a type field
            assertTrue(content.contains("\"type\":"), "Recipe should have type field")
            // All recipes should have a result field
            assertTrue(content.contains("\"result\""), "Recipe should have result field")
            // Verify valid JSON structure (starts with {, ends with })
            assertTrue(content.trim().startsWith("{"), "Recipe should be valid JSON object")
            assertTrue(content.trim().endsWith("}"), "Recipe should be valid JSON object")
        }
    }

    @Test
    fun `test crafting recipe has correct structure`() {
        val recipeName = "test_shaped"
        executeRecipeCommand(recipeName, "crafting")

        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/test_shaped.json")
        val content = FileUtil.readText(recipeFile)

        // Verify pattern is an array
        assertTrue(content.contains("\"pattern\": ["))
        // Verify key is an object
        assertTrue(content.contains("\"key\": {"))
        // Verify result has id field
        assertTrue(content.contains("\"id\": \"testmod:test_shaped\""))
    }

    @Test
    fun `test smelting recipe has correct structure`() {
        val recipeName = "test_smelting"
        executeRecipeCommand(recipeName, "smelting")

        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/test_smelting.json")
        val content = FileUtil.readText(recipeFile)

        // Verify ingredient is an object
        assertTrue(content.contains("\"ingredient\": {"))
        // Verify ingredient has item field
        assertTrue(content.contains("\"item\": \"minecraft:iron_ore\""))
        // Verify result has id field
        assertTrue(content.contains("\"id\": \"testmod:test_smelting\""))
        // Verify numeric fields are not in quotes
        assertTrue(content.contains("\"experience\": 0.1"))
        assertTrue(content.contains("\"cookingtime\": 200"))
    }

    @Test
    fun `test stonecutting recipe has count field`() {
        val recipeName = "test_stonecutting"
        executeRecipeCommand(recipeName, "stonecutting")

        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/test_stonecutting.json")
        val content = FileUtil.readText(recipeFile)

        // Verify result has count field
        assertTrue(content.contains("\"count\": 1"))
    }

    @Test
    fun `test smithing recipe has all required fields`() {
        val recipeName = "test_smithing"
        executeRecipeCommand(recipeName, "smithing")

        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/test_smithing.json")
        val content = FileUtil.readText(recipeFile)

        // Verify all three components exist
        assertTrue(content.contains("\"template\":"))
        assertTrue(content.contains("\"base\":"))
        assertTrue(content.contains("\"addition\":"))
        // Verify smithing_transform type
        assertTrue(content.contains("minecraft:smithing_transform"))
    }

    @Test
    fun `test experience as float value`() {
        val recipeName = "test_exp"
        executeRecipeCommand(recipeName, "smelting", mapOf("--experience" to "1.5"))

        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/test_exp.json")
        val content = FileUtil.readText(recipeFile)

        assertTrue(content.contains("\"experience\": 1.5"))
    }

    @Test
    fun `test cooking time as integer`() {
        val recipeName = "test_time"
        executeRecipeCommand(recipeName, "smelting", mapOf("--cooking-time" to "300"))

        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/test_time.json")
        val content = FileUtil.readText(recipeFile)

        assertTrue(content.contains("\"cookingtime\": 300"))
    }

    @Test
    fun `test blasting halves cooking time`() {
        val recipeName = "test_blast"
        executeRecipeCommand(recipeName, "blasting", mapOf("--cooking-time" to "400"))

        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/test_blast.json")
        val content = FileUtil.readText(recipeFile)

        // 400 / 2 = 200
        assertTrue(content.contains("\"cookingtime\": 200"))
    }

    @Test
    fun `test smoking halves cooking time`() {
        val recipeName = "test_smoke"
        executeRecipeCommand(recipeName, "smoking", mapOf("--cooking-time" to "600"))

        val recipeFile = File(tempDir, "versions/shared/v1/data/testmod/recipe/test_smoke.json")
        val content = FileUtil.readText(recipeFile)

        // 600 / 2 = 300
        assertTrue(content.contains("\"cookingtime\": 300"))
    }

    // Helper methods

    private fun executeRecipeCommand(
        recipeName: String,
        type: String,
        extraOptions: Map<String, String> = emptyMap()
    ) {
        val command = CreateRecipeCommand()

        // Build command args
        val args = mutableListOf(recipeName, "--type", type)
        extraOptions.forEach { (key, value) ->
            args.add(key)
            if (value.isNotEmpty()) {
                args.add(value)
            }
        }

        // Set working directory
        System.setProperty("user.dir", tempDir.absolutePath)

        // Execute command
        command.main(args.toTypedArray())
    }
}
