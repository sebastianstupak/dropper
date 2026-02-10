package dev.dropper.integration

import dev.dropper.util.MinecraftVersions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for version-aware generation behavior.
 */
class VersionAwareCreateTest {

    @Test
    fun `recipe directory is singular for 1_21`() {
        assertEquals("recipe", MinecraftVersions.recipeDir("1.21"))
        assertEquals("recipe", MinecraftVersions.recipeDir("1.21.1"))
        assertEquals("recipe", MinecraftVersions.recipeDir("1.21.4"))
    }

    @Test
    fun `recipe directory is plural for 1_20`() {
        assertEquals("recipes", MinecraftVersions.recipeDir("1.20.1"))
        assertEquals("recipes", MinecraftVersions.recipeDir("1.20.4"))
    }

    @Test
    fun `loot table directory is singular for 1_21`() {
        assertEquals("loot_table", MinecraftVersions.lootTableDir("1.21"))
    }

    @Test
    fun `loot table directory is plural for 1_20`() {
        assertEquals("loot_tables", MinecraftVersions.lootTableDir("1.20.1"))
    }

    @Test
    fun `enchantments are data-driven for 1_21`() {
        assertTrue(MinecraftVersions.usesDataDrivenEnchantments("1.21"))
        assertTrue(MinecraftVersions.usesDataDrivenEnchantments("1.21.1"))
    }

    @Test
    fun `enchantments are code-based for 1_20`() {
        assertFalse(MinecraftVersions.usesDataDrivenEnchantments("1.20.1"))
        assertFalse(MinecraftVersions.usesDataDrivenEnchantments("1.20.4"))
    }

    @Test
    fun `sword constructor is new style for 1_21`() {
        assertTrue(MinecraftVersions.usesNewSwordConstructor("1.21"))
    }

    @Test
    fun `sword constructor is old style for 1_20`() {
        assertFalse(MinecraftVersions.usesNewSwordConstructor("1.20.1"))
    }

    @Test
    fun `java version is correct per MC version`() {
        assertEquals(17, MinecraftVersions.requiredJavaVersion("1.20.1"))
        assertEquals(17, MinecraftVersions.requiredJavaVersion("1.20.4"))
        assertEquals(21, MinecraftVersions.requiredJavaVersion("1.21"))
        assertEquals(21, MinecraftVersions.requiredJavaVersion("1.21.4"))
    }

    @Test
    fun `architectury api version is returned for all supported MC versions`() {
        assertNotNull(MinecraftVersions.architecturyApiVersion("1.20.1"))
        assertNotNull(MinecraftVersions.architecturyApiVersion("1.21"))
        assertNotNull(MinecraftVersions.architecturyApiVersion("1.21.1"))
        assertNotNull(MinecraftVersions.architecturyApiVersion("1.21.4"))

        // Versions should be different across major boundaries
        assertNotEquals(
            MinecraftVersions.architecturyApiVersion("1.20.1"),
            MinecraftVersions.architecturyApiVersion("1.21.1")
        )
    }
}
