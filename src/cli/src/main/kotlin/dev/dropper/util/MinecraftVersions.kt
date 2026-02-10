package dev.dropper.util

/**
 * Utility for Minecraft version feature flags and dependency version lookups.
 *
 * Key version boundaries:
 * - 1.20.1: Java 17, recipes/ (plural), loot_tables/ (plural), code-based enchantments
 * - 1.20.2-1.20.4: Transitional
 * - 1.21+: Java 21, recipe/ (singular), loot_table/ (singular), data-driven enchantments,
 *          new item components, new SwordItem constructor, Identifier.of(), ResourceLocation.fromNamespaceAndPath()
 */
object MinecraftVersions {

    /**
     * Compare two Minecraft version strings.
     * Returns negative if v1 < v2, zero if equal, positive if v1 > v2.
     */
    fun compare(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) return p1 - p2
        }
        return 0
    }

    /**
     * Returns true if [version] >= [target].
     */
    fun isAtLeast(version: String, target: String): Boolean = compare(version, target) >= 0

    // ── Feature flags ──────────────────────────────────────────────────

    /** 1.21+ uses singular `recipe/` directory instead of `recipes/`. */
    fun usesSingularRecipeDir(v: String): Boolean = isAtLeast(v, "1.21")

    /** 1.21+ uses singular `loot_table/` directory instead of `loot_tables/`. */
    fun usesSingularLootTableDir(v: String): Boolean = isAtLeast(v, "1.21")

    /** 1.21+ enchantments are fully data-driven (JSON only, no code). */
    fun usesDataDrivenEnchantments(v: String): Boolean = isAtLeast(v, "1.21")

    /** 1.21+ uses new item component system instead of old Properties methods. */
    fun usesNewItemComponents(v: String): Boolean = isAtLeast(v, "1.21")

    /** 1.21+ SwordItem uses SwordItem(Tier, Properties) instead of SwordItem(Tier, int, float, Properties). */
    fun usesNewSwordConstructor(v: String): Boolean = isAtLeast(v, "1.21")

    /** 1.21+ Fabric uses Identifier.of() instead of new Identifier(). */
    fun usesIdentifierOf(v: String): Boolean = isAtLeast(v, "1.21")

    /** 1.21+ Forge uses ResourceLocation.fromNamespaceAndPath() instead of new ResourceLocation(). */
    fun usesResourceLocationOf(v: String): Boolean = isAtLeast(v, "1.21")

    /** 1.20.3+ DoorBlock uses DoorBlock(BlockSetType, Properties). Before that: DoorBlock(Properties). */
    fun usesDoorBlockSetType(v: String): Boolean = isAtLeast(v, "1.20.3")

    // ── Data directory helpers ──────────────────────────────────────────

    fun recipeDir(v: String): String = if (usesSingularRecipeDir(v)) "recipe" else "recipes"

    fun lootTableDir(v: String): String = if (usesSingularLootTableDir(v)) "loot_table" else "loot_tables"

    // ── Required Java version ──────────────────────────────────────────

    fun requiredJavaVersion(v: String): Int = if (isAtLeast(v, "1.21")) 21 else 17

    // ── Dependency versions ────────────────────────────────────────────

    fun architecturyApiVersion(v: String): String = when {
        isAtLeast(v, "1.21.4") -> "15.0.1"
        isAtLeast(v, "1.21.1") -> "13.0.8"
        isAtLeast(v, "1.21")   -> "12.1.4"
        isAtLeast(v, "1.20.6") -> "12.0.30"
        isAtLeast(v, "1.20.4") -> "11.1.17"
        isAtLeast(v, "1.20.2") -> "10.0.17"
        isAtLeast(v, "1.20.1") -> "9.2.14"
        else -> "9.2.14"
    }

    fun fabricApiVersion(v: String): String = when {
        isAtLeast(v, "1.21.4") -> "0.114.0+1.21.4"
        isAtLeast(v, "1.21.1") -> "0.106.1+1.21.1"
        isAtLeast(v, "1.21")   -> "0.100.8+1.21"
        isAtLeast(v, "1.20.6") -> "0.97.2+1.20.6"
        isAtLeast(v, "1.20.4") -> "0.97.0+1.20.4"
        isAtLeast(v, "1.20.2") -> "0.91.6+1.20.2"
        isAtLeast(v, "1.20.1") -> "0.92.2+1.20.1"
        else -> "0.92.2+1.20.1"
    }

    fun fabricLoaderVersion(v: String): String = when {
        isAtLeast(v, "1.21") -> "0.16.9"
        else -> "0.15.11"
    }

    fun forgeVersion(v: String): String = when {
        isAtLeast(v, "1.20.4") -> "1.20.4-49.0.31"
        isAtLeast(v, "1.20.2") -> "1.20.2-48.1.0"
        isAtLeast(v, "1.20.1") -> "1.20.1-47.3.0"
        else -> "1.20.1-47.3.0"
    }

    fun neoforgeVersion(v: String): String = when {
        isAtLeast(v, "1.21.4") -> "21.4.50-beta"
        isAtLeast(v, "1.21.1") -> "21.1.77"
        isAtLeast(v, "1.21")   -> "21.0.167"
        isAtLeast(v, "1.20.6") -> "20.6.119"
        isAtLeast(v, "1.20.4") -> "20.4.237"
        isAtLeast(v, "1.20.2") -> "20.2.88"
        isAtLeast(v, "1.20.1") -> "47.1.106"
        else -> "47.1.106"
    }

    fun minecraftVersion(v: String): String = v

    fun architecturyLoomVersion(): String = "1.7-SNAPSHOT"
}
