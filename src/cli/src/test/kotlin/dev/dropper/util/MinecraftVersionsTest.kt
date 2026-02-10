package dev.dropper.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MinecraftVersionsTest {

    @Test
    fun `compare returns negative when first version is older`() {
        assertTrue(MinecraftVersions.compare("1.20.1", "1.21") < 0)
        assertTrue(MinecraftVersions.compare("1.20.1", "1.20.2") < 0)
        assertTrue(MinecraftVersions.compare("1.19", "1.20") < 0)
    }

    @Test
    fun `compare returns zero for equal versions`() {
        assertEquals(0, MinecraftVersions.compare("1.20.1", "1.20.1"))
        assertEquals(0, MinecraftVersions.compare("1.21", "1.21"))
        assertEquals(0, MinecraftVersions.compare("1.21.0", "1.21"))
    }

    @Test
    fun `compare returns positive when first version is newer`() {
        assertTrue(MinecraftVersions.compare("1.21", "1.20.1") > 0)
        assertTrue(MinecraftVersions.compare("1.21.1", "1.21") > 0)
        assertTrue(MinecraftVersions.compare("1.20.4", "1.20.1") > 0)
    }

    @Test
    fun `isAtLeast detects version boundaries correctly`() {
        assertTrue(MinecraftVersions.isAtLeast("1.21", "1.21"))
        assertTrue(MinecraftVersions.isAtLeast("1.21.1", "1.21"))
        assertFalse(MinecraftVersions.isAtLeast("1.20.6", "1.21"))
        assertFalse(MinecraftVersions.isAtLeast("1.20.1", "1.21"))
    }

    @Test
    fun `1_20_1 uses plural recipe and loot_table dirs`() {
        assertFalse(MinecraftVersions.usesSingularRecipeDir("1.20.1"))
        assertFalse(MinecraftVersions.usesSingularLootTableDir("1.20.1"))
        assertEquals("recipes", MinecraftVersions.recipeDir("1.20.1"))
        assertEquals("loot_tables", MinecraftVersions.lootTableDir("1.20.1"))
    }

    @Test
    fun `1_21 uses singular recipe and loot_table dirs`() {
        assertTrue(MinecraftVersions.usesSingularRecipeDir("1.21"))
        assertTrue(MinecraftVersions.usesSingularLootTableDir("1.21"))
        assertEquals("recipe", MinecraftVersions.recipeDir("1.21"))
        assertEquals("loot_table", MinecraftVersions.lootTableDir("1.21"))
    }

    @Test
    fun `1_20_1 uses code-based enchantments`() {
        assertFalse(MinecraftVersions.usesDataDrivenEnchantments("1.20.1"))
    }

    @Test
    fun `1_21 uses data-driven enchantments`() {
        assertTrue(MinecraftVersions.usesDataDrivenEnchantments("1.21"))
        assertTrue(MinecraftVersions.usesDataDrivenEnchantments("1.21.1"))
    }

    @Test
    fun `1_21 uses new item components and sword constructor`() {
        assertTrue(MinecraftVersions.usesNewItemComponents("1.21"))
        assertTrue(MinecraftVersions.usesNewSwordConstructor("1.21"))
        assertFalse(MinecraftVersions.usesNewItemComponents("1.20.4"))
        assertFalse(MinecraftVersions.usesNewSwordConstructor("1.20.1"))
    }

    @Test
    fun `java version is 17 for 1_20_x and 21 for 1_21_x`() {
        assertEquals(17, MinecraftVersions.requiredJavaVersion("1.20.1"))
        assertEquals(17, MinecraftVersions.requiredJavaVersion("1.20.6"))
        assertEquals(21, MinecraftVersions.requiredJavaVersion("1.21"))
        assertEquals(21, MinecraftVersions.requiredJavaVersion("1.21.4"))
    }

    @Test
    fun `architectury api version varies by minecraft version`() {
        assertEquals("9.2.14", MinecraftVersions.architecturyApiVersion("1.20.1"))
        assertEquals("13.0.8", MinecraftVersions.architecturyApiVersion("1.21.1"))
        assertEquals("15.0.1", MinecraftVersions.architecturyApiVersion("1.21.4"))
    }

    @Test
    fun `fabric api version varies by minecraft version`() {
        assertEquals("0.92.2+1.20.1", MinecraftVersions.fabricApiVersion("1.20.1"))
        assertEquals("0.106.1+1.21.1", MinecraftVersions.fabricApiVersion("1.21.1"))
    }

    @Test
    fun `door block set type is 1_20_3 plus`() {
        assertFalse(MinecraftVersions.usesDoorBlockSetType("1.20.1"))
        assertFalse(MinecraftVersions.usesDoorBlockSetType("1.20.2"))
        assertTrue(MinecraftVersions.usesDoorBlockSetType("1.20.3"))
        assertTrue(MinecraftVersions.usesDoorBlockSetType("1.21"))
    }

    @Test
    fun `identifier_of and resource_location_of are 1_21 plus`() {
        assertFalse(MinecraftVersions.usesIdentifierOf("1.20.1"))
        assertTrue(MinecraftVersions.usesIdentifierOf("1.21"))
        assertFalse(MinecraftVersions.usesResourceLocationOf("1.20.4"))
        assertTrue(MinecraftVersions.usesResourceLocationOf("1.21.1"))
    }
}
