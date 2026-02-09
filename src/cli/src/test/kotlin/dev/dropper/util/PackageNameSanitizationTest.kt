package dev.dropper.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for mod ID to package name sanitization
 */
class PackageNameSanitizationTest {

    @Test
    fun `sanitizeModId removes underscores`() {
        assertEquals("mymod", FileUtil.sanitizeModId("my_mod"))
        assertEquals("testmod", FileUtil.sanitizeModId("test_mod"))
        assertEquals("myfancymod", FileUtil.sanitizeModId("my_fancy_mod"))
    }

    @Test
    fun `sanitizeModId removes hyphens`() {
        assertEquals("coolmod", FileUtil.sanitizeModId("cool-mod"))
        assertEquals("testmod", FileUtil.sanitizeModId("test-mod"))
        assertEquals("myfancymod", FileUtil.sanitizeModId("my-fancy-mod"))
    }

    @Test
    fun `sanitizeModId removes both hyphens and underscores`() {
        assertEquals("mycoolmod", FileUtil.sanitizeModId("my_cool-mod"))
        assertEquals("testmodpack", FileUtil.sanitizeModId("test-mod_pack"))
        assertEquals("complexmodname", FileUtil.sanitizeModId("complex_mod-name"))
    }

    @Test
    fun `sanitizeModId handles multiple consecutive special chars`() {
        assertEquals("mymod", FileUtil.sanitizeModId("my__mod"))
        assertEquals("coolmod", FileUtil.sanitizeModId("cool--mod"))
        assertEquals("testmod", FileUtil.sanitizeModId("test-_-mod"))
    }

    @Test
    fun `sanitizeModId preserves alphanumeric characters`() {
        assertEquals("mymod123", FileUtil.sanitizeModId("my_mod_123"))
        assertEquals("test2mod", FileUtil.sanitizeModId("test2-mod"))
        assertEquals("mod1a2b3c", FileUtil.sanitizeModId("mod1-a2-b3-c"))
    }

    @Test
    fun `sanitizeModId handles already clean mod IDs`() {
        assertEquals("mymod", FileUtil.sanitizeModId("mymod"))
        assertEquals("testmod", FileUtil.sanitizeModId("testmod"))
        assertEquals("coolmod123", FileUtil.sanitizeModId("coolmod123"))
    }

    @Test
    fun `sanitizeModId handles single character mod IDs`() {
        assertEquals("a", FileUtil.sanitizeModId("a"))
        assertEquals("z", FileUtil.sanitizeModId("z"))
        assertEquals("m", FileUtil.sanitizeModId("m"))
    }

    @Test
    fun `sanitizeModId handles very long mod IDs`() {
        val longModId = "my_super_long_mod_name_with_many_underscores_and_hyphens"
        val expected = "mysuperlongmodnamewithmanyunderscoresandhyphens"
        assertEquals(expected, FileUtil.sanitizeModId(longModId))
    }

    @ParameterizedTest
    @CsvSource(
        "my_mod, mymod",
        "cool-mod, coolmod",
        "test_123, test123",
        "a_b_c, abc",
        "x-y-z, xyz",
        "mod_v2, modv2",
        "test-mod-pack, testmodpack",
        "my_awesome_mod, myawesomemod",
        "super-cool_mod, supercoolmod",
        "m, m"
    )
    fun `sanitizeModId parameterized tests`(input: String, expected: String) {
        assertEquals(expected, FileUtil.sanitizeModId(input))
    }

    @Test
    fun `sanitized mod IDs are valid Java package identifiers`() {
        val testCases = listOf(
            "my_mod",
            "cool-mod",
            "test_123",
            "super-cool_mod",
            "my_fancy_mod_2"
        )

        testCases.forEach { modId ->
            val sanitized = FileUtil.sanitizeModId(modId)

            // Check it contains only valid characters (lowercase letters and digits)
            assertTrue(
                sanitized.matches(Regex("^[a-z][a-z0-9]*$")),
                "Sanitized mod ID '$sanitized' from '$modId' should only contain lowercase letters and digits"
            )
        }
    }

    @Test
    fun `package names generated from sanitized mod IDs are valid`() {
        val testCases = mapOf(
            "my_mod" to "com.mymod",
            "cool-mod" to "com.coolmod",
            "test_123" to "com.test123",
            "super-cool_mod" to "com.supercoolmod"
        )

        testCases.forEach { (modId, expectedPackage) ->
            val sanitized = FileUtil.sanitizeModId(modId)
            val packageName = "com.$sanitized"

            assertEquals(expectedPackage, packageName)

            // Verify it's a valid Java package name
            assertTrue(
                packageName.matches(Regex("^[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*$")),
                "Package name '$packageName' should be a valid Java package identifier"
            )
        }
    }

    @Test
    fun `edge case - numbers at start are preserved`() {
        // Note: While Java packages shouldn't start with numbers,
        // the sanitization function only removes hyphens/underscores
        val result = FileUtil.sanitizeModId("123mod")
        assertEquals("123mod", result)
    }

    @Test
    fun `edge case - empty-like inputs`() {
        // These are edge cases that shouldn't happen in practice,
        // but we document the behavior
        assertEquals("", FileUtil.sanitizeModId("_"))
        assertEquals("", FileUtil.sanitizeModId("-"))
        assertEquals("", FileUtil.sanitizeModId("_-_"))
    }
}
