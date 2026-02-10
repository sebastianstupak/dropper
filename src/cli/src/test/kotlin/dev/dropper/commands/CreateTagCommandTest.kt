package dev.dropper.commands

import dev.dropper.util.FileUtil
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

class CreateTagCommandTest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("tag-test")

        // Create a minimal config.yml for testing
        val configFile = File(context.projectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
              name: Test Mod
              version: 1.0.0
              description: Test mod for tag creation
              author: Test Author
              license: MIT
        """.trimIndent())
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    @Test
    fun `test basic block tag creation`() {
        val tagName = "custom_blocks"
        val values = "testmod:block_one,testmod:block_two"

        executeTagCommand(tagName, "block", values)

        // Verify file exists
        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/block/custom_blocks.json")
        assertTrue(tagFile.exists(), "Tag file should exist")

        // Verify content
        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"values\": ["))
        assertTrue(content.contains("\"testmod:block_one\""))
        assertTrue(content.contains("\"testmod:block_two\""))
        assertFalse(content.contains("\"replace\": true"), "Should not have replace flag by default")
    }

    @Test
    fun `test item tag creation`() {
        val tagName = "custom_items"
        val values = "testmod:item_one,testmod:item_two,testmod:item_three"

        executeTagCommand(tagName, "item", values)

        // Verify file exists
        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/item/custom_items.json")
        assertTrue(tagFile.exists(), "Tag file should exist")

        // Verify content
        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"testmod:item_one\""))
        assertTrue(content.contains("\"testmod:item_two\""))
        assertTrue(content.contains("\"testmod:item_three\""))
    }

    @Test
    fun `test entity_type tag creation`() {
        val tagName = "custom_entities"
        val values = "testmod:custom_zombie,testmod:custom_skeleton"

        executeTagCommand(tagName, "entity_type", values)

        // Verify file exists
        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/entity_type/custom_entities.json")
        assertTrue(tagFile.exists(), "Tag file should exist")

        // Verify content
        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"testmod:custom_zombie\""))
        assertTrue(content.contains("\"testmod:custom_skeleton\""))
    }

    @Test
    fun `test fluid tag creation`() {
        val tagName = "custom_fluids"
        val values = "testmod:molten_metal,testmod:acid"

        executeTagCommand(tagName, "fluid", values)

        // Verify file exists
        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/fluid/custom_fluids.json")
        assertTrue(tagFile.exists(), "Tag file should exist")

        // Verify content
        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"testmod:molten_metal\""))
        assertTrue(content.contains("\"testmod:acid\""))
    }

    @Test
    fun `test tag with replace flag`() {
        val tagName = "replaceable_blocks"
        val values = "testmod:block_one"

        executeTagCommand(tagName, "block", values, replace = true)

        // Verify content
        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/block/replaceable_blocks.json")
        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"replace\": true"), "Should have replace flag")
    }

    @Test
    fun `test tag without values creates placeholder`() {
        val tagName = "empty_tag"

        executeTagCommand(tagName, "block", null)

        // Verify file exists
        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/block/empty_tag.json")
        assertTrue(tagFile.exists(), "Tag file should exist")

        // Verify content is valid JSON with empty values array
        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"values\": []"), "Empty tag should have empty values array")
        assertFalse(content.contains("//"), "JSON should not contain comments")
    }

    @Test
    fun `test tag with tag references`() {
        val tagName = "all_ores"
        val values = "#forge:ores/iron,#forge:ores/gold,testmod:custom_ore"

        executeTagCommand(tagName, "block", values)

        // Verify content
        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/block/all_ores.json")
        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"#forge:ores/iron\""))
        assertTrue(content.contains("\"#forge:ores/gold\""))
        assertTrue(content.contains("\"testmod:custom_ore\""))
    }

    @Test
    fun `test minecraft namespace tag`() {
        val tagName = "minecraft:mineable/pickaxe"
        val values = "testmod:custom_ore,testmod:hard_block"

        executeTagCommand(tagName, "block", values)

        // Verify file is created in minecraft namespace
        val tagFile = File(context.projectDir, "versions/shared/v1/data/minecraft/tags/block/mineable/pickaxe.json")
        assertTrue(tagFile.exists(), "Tag file should exist in minecraft namespace")

        // Verify content
        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"testmod:custom_ore\""))
        assertTrue(content.contains("\"testmod:hard_block\""))
    }

    @Test
    fun `test forge namespace tag`() {
        val tagName = "forge:ores/ruby"
        val values = "testmod:ruby_ore,testmod:deepslate_ruby_ore"

        executeTagCommand(tagName, "block", values)

        // Verify file is created in forge namespace
        val tagFile = File(context.projectDir, "versions/shared/v1/data/forge/tags/block/ores/ruby.json")
        assertTrue(tagFile.exists(), "Tag file should exist in forge namespace")

        // Verify content
        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"testmod:ruby_ore\""))
        assertTrue(content.contains("\"testmod:deepslate_ruby_ore\""))
    }

    @Test
    fun `test mineable tag defaults to minecraft namespace`() {
        val tagName = "mineable/axe"
        val values = "testmod:custom_wood"

        executeTagCommand(tagName, "block", values)

        // Verify file is created in minecraft namespace
        val tagFile = File(context.projectDir, "versions/shared/v1/data/minecraft/tags/block/mineable/axe.json")
        assertTrue(tagFile.exists(), "Mineable tag should default to minecraft namespace")

        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"testmod:custom_wood\""))
    }

    @Test
    fun `test forge ores tag defaults to forge namespace`() {
        val tagName = "ores/silver"
        val values = "testmod:silver_ore"

        executeTagCommand(tagName, "block", values)

        // Verify file is created in forge namespace
        val tagFile = File(context.projectDir, "versions/shared/v1/data/forge/tags/block/ores/silver.json")
        assertTrue(tagFile.exists(), "Ores tag should default to forge namespace")

        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"testmod:silver_ore\""))
    }

    @Test
    fun `test tag with nested path`() {
        val tagName = "testmod:categories/magical/items"
        val values = "testmod:wand,testmod:staff"

        executeTagCommand(tagName, "item", values)

        // Verify file is created with nested path
        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/item/categories/magical/items.json")
        assertTrue(tagFile.exists(), "Tag file should exist with nested path")

        val content = FileUtil.readText(tagFile)
        assertTrue(content.contains("\"testmod:wand\""))
        assertTrue(content.contains("\"testmod:staff\""))
    }

    @Test
    fun `test tag JSON structure is valid`() {
        val tagName = "validation_test"
        val values = "testmod:item_one,testmod:item_two"

        executeTagCommand(tagName, "item", values)

        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/item/validation_test.json")
        val content = FileUtil.readText(tagFile)

        // Verify JSON structure
        assertTrue(content.trim().startsWith("{"))
        assertTrue(content.trim().endsWith("}"))
        assertTrue(content.contains("\"values\": ["))
        assertTrue(content.contains("]"))

        // Verify proper JSON formatting (commas between values)
        val lines = content.lines()
        val valueLines = lines.filter { it.contains("testmod:item") }
        assertEquals(2, valueLines.size)
        assertTrue(valueLines[0].trim().endsWith(","), "First value should end with comma")
        assertFalse(valueLines[1].trim().endsWith(","), "Last value should not end with comma")
    }

    @Test
    fun `test tag with single value`() {
        val tagName = "single_value"
        val values = "testmod:only_item"

        executeTagCommand(tagName, "item", values)

        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/item/single_value.json")
        val content = FileUtil.readText(tagFile)

        assertTrue(content.contains("\"testmod:only_item\""))
        // Single value should not have trailing comma
        val valueLines = content.lines().filter { it.contains("testmod:only_item") }
        assertFalse(valueLines[0].trim().endsWith(","), "Single value should not have trailing comma")
    }

    @Test
    fun `test tag with whitespace in values`() {
        val tagName = "whitespace_test"
        val values = " testmod:item_one , testmod:item_two , testmod:item_three "

        executeTagCommand(tagName, "item", values)

        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/item/whitespace_test.json")
        val content = FileUtil.readText(tagFile)

        // Verify whitespace is trimmed
        assertTrue(content.contains("\"testmod:item_one\""))
        assertTrue(content.contains("\"testmod:item_two\""))
        assertTrue(content.contains("\"testmod:item_three\""))
        assertFalse(content.contains(" testmod:item_one "))
    }

    @Test
    fun `test common vanilla tag names`() {
        val vanillaTags = listOf(
            "mineable/pickaxe" to "minecraft",
            "mineable/axe" to "minecraft",
            "mineable/shovel" to "minecraft",
            "mineable/hoe" to "minecraft",
            "logs" to "minecraft"
        )

        vanillaTags.forEach { (tagName, expectedNamespace) ->
            executeTagCommand(tagName, "block", "testmod:test_block")

            val tagFile = File(context.projectDir, "versions/shared/v1/data/$expectedNamespace/tags/block/${tagName}.json")
            assertTrue(
                tagFile.exists(),
                "Vanilla tag $tagName should be created in $expectedNamespace namespace"
            )
        }
    }

    @Test
    fun `test common forge tag names`() {
        val forgeTags = listOf(
            "ores/copper" to "forge",
            "ingots/iron" to "forge",
            "storage_blocks/diamond" to "forge"
        )

        forgeTags.forEach { (tagName, expectedNamespace) ->
            executeTagCommand(tagName, "item", "testmod:test_item")

            val tagFile = File(context.projectDir, "versions/shared/v1/data/$expectedNamespace/tags/item/${tagName}.json")
            assertTrue(
                tagFile.exists(),
                "Forge tag $tagName should be created in $expectedNamespace namespace"
            )
        }
    }

    @Test
    fun `test block tag with mixed content`() {
        val tagName = "mixed_blocks"
        val values = "minecraft:stone,#minecraft:logs,testmod:custom_block,#forge:ores"

        executeTagCommand(tagName, "block", values)

        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/block/mixed_blocks.json")
        val content = FileUtil.readText(tagFile)

        // Verify all types of entries
        assertTrue(content.contains("\"minecraft:stone\""), "Should contain regular block reference")
        assertTrue(content.contains("\"#minecraft:logs\""), "Should contain tag reference")
        assertTrue(content.contains("\"testmod:custom_block\""), "Should contain mod block reference")
        assertTrue(content.contains("\"#forge:ores\""), "Should contain forge tag reference")
    }

    @Test
    fun `test item tag without values creates valid empty JSON`() {
        val tagName = "empty_item_tag"

        executeTagCommand(tagName, "item", null)

        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/item/empty_item_tag.json")
        val content = FileUtil.readText(tagFile)

        assertTrue(content.contains("\"values\": []"), "Empty item tag should have empty values array")
        assertFalse(content.contains("//"), "JSON should not contain comments")
    }

    @Test
    fun `test block tag without values creates valid empty JSON`() {
        val tagName = "empty_block_tag"

        executeTagCommand(tagName, "block", null)

        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/block/empty_block_tag.json")
        val content = FileUtil.readText(tagFile)

        assertTrue(content.contains("\"values\": []"), "Empty block tag should have empty values array")
        assertFalse(content.contains("//"), "JSON should not contain comments")
    }

    @Test
    fun `test entity_type tag without values creates valid empty JSON`() {
        val tagName = "empty_entity_tag"

        executeTagCommand(tagName, "entity_type", null)

        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/entity_type/empty_entity_tag.json")
        val content = FileUtil.readText(tagFile)

        assertTrue(content.contains("\"values\": []"), "Empty entity_type tag should have empty values array")
        assertFalse(content.contains("//"), "JSON should not contain comments")
    }

    @Test
    fun `test fluid tag without values creates valid empty JSON`() {
        val tagName = "empty_fluid_tag"

        executeTagCommand(tagName, "fluid", null)

        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/fluid/empty_fluid_tag.json")
        val content = FileUtil.readText(tagFile)

        assertTrue(content.contains("\"values\": []"), "Empty fluid tag should have empty values array")
        assertFalse(content.contains("//"), "JSON should not contain comments")
    }

    @Test
    fun `test tag with replace and values`() {
        val tagName = "replaced_tag"
        val values = "testmod:replacement_one,testmod:replacement_two"

        executeTagCommand(tagName, "item", values, replace = true)

        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/item/replaced_tag.json")
        val content = FileUtil.readText(tagFile)

        assertTrue(content.contains("\"replace\": true"))
        assertTrue(content.contains("\"testmod:replacement_one\""))
        assertTrue(content.contains("\"testmod:replacement_two\""))

        // Verify structure: replace comes before values
        val replaceIndex = content.indexOf("\"replace\"")
        val valuesIndex = content.indexOf("\"values\"")
        assertTrue(replaceIndex < valuesIndex, "replace should come before values")
    }

    @Test
    fun `test tag directory structure is created`() {
        val tagName = "deep/nested/path/tag"
        val values = "testmod:item"

        executeTagCommand(tagName, "item", values)

        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/item/deep/nested/path/tag.json")
        assertTrue(tagFile.exists(), "Deep nested directory structure should be created")
        assertTrue(tagFile.parentFile.exists(), "Parent directories should exist")
        assertTrue(tagFile.parentFile.isDirectory, "Parent should be a directory")
    }

    @Test
    fun `test multiple tags with same type`() {
        val tags = listOf(
            "tag_one" to "testmod:item_one",
            "tag_two" to "testmod:item_two",
            "tag_three" to "testmod:item_three"
        )

        tags.forEach { (tagName, value) ->
            executeTagCommand(tagName, "item", value)
        }

        // Verify all files exist
        tags.forEach { (tagName, _) ->
            val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/item/$tagName.json")
            assertTrue(tagFile.exists(), "Tag $tagName should exist")
        }
    }

    @Test
    fun `test tag with empty string values is filtered`() {
        val tagName = "filtered_tag"
        val values = "testmod:item_one,,testmod:item_two,  ,testmod:item_three"

        executeTagCommand(tagName, "item", values)

        val tagFile = File(context.projectDir, "versions/shared/v1/data/testmod/tags/item/filtered_tag.json")
        val content = FileUtil.readText(tagFile)

        // Should only contain non-empty values
        assertTrue(content.contains("\"testmod:item_one\""))
        assertTrue(content.contains("\"testmod:item_two\""))
        assertTrue(content.contains("\"testmod:item_three\""))

        // Should have exactly 3 values
        val valueCount = content.split("\"testmod:").size - 1
        assertEquals(3, valueCount)
    }

    // Helper methods

    private fun executeTagCommand(
        tagName: String,
        type: String,
        values: String?,
        replace: Boolean = false
    ) {
        val command = CreateTagCommand()

        // Build command args
        val args = mutableListOf(tagName, "--type", type)
        if (values != null) {
            args.add("--values")
            args.add(values)
        }
        if (replace) {
            args.add("--replace")
        }

        // Set project directory before parsing
        command.projectDir = context.projectDir

        // Execute command
        command.parse(args.toTypedArray())
    }
}
