package dev.dropper.util

import org.junit.jupiter.api.Test
import kotlin.test.*

/**
 * Tests for ValidationUtil
 */
class ValidationUtilTest {

    @Test
    fun `valid names should pass`() {
        val validNames = listOf(
            "ruby_sword",
            "test",
            "a",
            "item123",
            "my_cool_item_2",
            "verylongmodnamewith64chars"
        )

        validNames.forEach { name ->
            val result = ValidationUtil.validateName(name)
            assertTrue(result.isValid, "Expected '$name' to be valid, but got: ${result.errorMessage}")
        }
    }

    @Test
    fun `names with spaces should fail`() {
        val result = ValidationUtil.validateName("test item")
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("spaces"))
        assertNotNull(result.suggestion)
        assertTrue(result.suggestion!!.contains("test_item") || result.suggestion == "test_item",
            "Expected suggestion to be 'test_item', got: ${result.suggestion}")
    }

    @Test
    fun `names with uppercase should fail`() {
        val result = ValidationUtil.validateName("TestItem")
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
        assertTrue(result.errorMessage!!.contains("lowercase"))
        assertNotNull(result.suggestion)
        assertTrue(result.suggestion!!.contains("testitem") || result.suggestion == "testitem",
            "Expected suggestion to contain 'testitem', got: ${result.suggestion}")
    }

    @Test
    fun `names with special characters should fail`() {
        val invalidNames = listOf(
            "test@item",
            "test.item",
            "test-item",
            "test/item",
            "test\\item",
            "test!item"
        )

        invalidNames.forEach { name ->
            val result = ValidationUtil.validateName(name)
            assertFalse(result.isValid, "Expected '$name' to be invalid")
            assertNotNull(result.suggestion)
        }
    }

    @Test
    fun `names starting with number should fail`() {
        val result = ValidationUtil.validateName("123test")
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `names ending with underscore should fail`() {
        val result = ValidationUtil.validateName("test_")
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `reserved keywords should fail`() {
        val reservedWords = listOf(
            "class",
            "public",
            "static",
            "void",
            "item",
            "block",
            "minecraft"
        )

        reservedWords.forEach { word ->
            val result = ValidationUtil.validateName(word)
            assertFalse(result.isValid, "Expected '$word' to be rejected as reserved keyword")
            assertTrue(result.errorMessage!!.contains("reserved"))
        }
    }

    @Test
    fun `empty name should fail`() {
        val result = ValidationUtil.validateName("")
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("empty"))
    }

    @Test
    fun `name too long should fail`() {
        val longName = "a".repeat(65)
        val result = ValidationUtil.validateName(longName)
        assertFalse(result.isValid)
        assertTrue(result.errorMessage!!.contains("too long"))
    }

    @Test
    fun `valid mod IDs should pass`() {
        val validModIds = listOf(
            "mymod",
            "my-mod",
            "my_mod",
            "test123",
            "a",
            "cool-mod-2"
        )

        validModIds.forEach { modId ->
            val result = ValidationUtil.validateModId(modId)
            assertTrue(result.isValid, "Expected '$modId' to be valid, but got: ${result.errorMessage}")
        }
    }

    @Test
    fun `mod ID with uppercase should fail`() {
        val result = ValidationUtil.validateModId("MyMod")
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `mod ID with spaces should fail`() {
        val result = ValidationUtil.validateModId("my mod")
        assertFalse(result.isValid)
        assertNotNull(result.errorMessage)
    }

    @Test
    fun `validation provides helpful suggestions`() {
        val tests = mapOf(
            "Test Item" to "test_item",
            "My Cool Mod!" to "my_cool_mod",
            "123Test" to "test"
        )

        tests.forEach { (input, expectedSuggestion) ->
            val result = ValidationUtil.validateName(input)
            assertFalse(result.isValid)
            assertNotNull(result.suggestion)
            assertTrue(
                result.suggestion!!.contains(expectedSuggestion) || result.suggestion == expectedSuggestion,
                "Expected suggestion to contain '$expectedSuggestion' for input '$input', but got: ${result.suggestion}"
            )
        }
    }
}
