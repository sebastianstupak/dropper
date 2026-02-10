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

class CreateEnchantmentCommandTest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("enchantment-test")

        // Create a minimal config.yml for testing
        val configFile = File(context.projectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
              name: Test Mod
              version: 1.0.0
              description: Test mod for enchantment creation
              author: Test Author
              license: MIT
        """.trimIndent())
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    @Test
    fun `test basic enchantment creation with defaults`() {
        val enchantmentName = "test_enchantment"

        // Create enchantment with defaults
        executeEnchantmentCommand(enchantmentName)

        // Verify files exist
        assertEnchantmentFilesExist(enchantmentName, listOf(
            "shared/common/src/main/java/com/testmod/enchantments/TestEnchantment.java",
            "versions/shared/v1/assets/testmod/lang/en_us.json",
            "versions/shared/v1/data/testmod/enchantment/test_enchantment.json"
        ))

        // Verify enchantment class content
        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/TestEnchantment.java"))
        assertTrue(enchantmentClass.contains("public class TestEnchantment {"))
        assertTrue(enchantmentClass.contains("public static final String ID = \"test_enchantment\""))
        assertTrue(enchantmentClass.contains("public static final int MAX_LEVEL = 1"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_TREASURE = false"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_TRADEABLE = true"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_DISCOVERABLE = true"))

        // Verify class does NOT extend Enchantment
        assertFalse(enchantmentClass.contains("extends Enchantment"), "Class should not extend Enchantment")

        // Verify lang entry
        val langFile = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json"))
        assertTrue(langFile.contains("\"enchantment.testmod.test_enchantment\": \"Test Enchantment\""))
    }

    @Test
    fun `test common class does not extend Enchantment`() {
        val enchantmentName = "test_enchant"

        executeEnchantmentCommand(enchantmentName)

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/TestEnchant.java"))

        // Must NOT contain extends Enchantment
        assertFalse(enchantmentClass.contains("extends Enchantment"), "Should not subclass Enchantment")

        // Must NOT contain inner enums
        assertFalse(enchantmentClass.contains("public enum Rarity"), "Should not have inner Rarity enum")
        assertFalse(enchantmentClass.contains("public enum Category"), "Should not have inner Category enum")

        // Must NOT contain old imports
        assertFalse(enchantmentClass.contains("import net.minecraft.world.item.enchantment.EnchantmentCategory"), "Should not import EnchantmentCategory")
        assertFalse(enchantmentClass.contains("import net.minecraft.world.entity.EquipmentSlot"), "Should not import EquipmentSlot")
        assertFalse(enchantmentClass.contains("import net.minecraft.world.item.ItemStack"), "Should not import ItemStack")
    }

    @Test
    fun `test common class has required constants`() {
        val enchantmentName = "test_enchant"

        executeEnchantmentCommand(enchantmentName)

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/TestEnchant.java"))

        assertTrue(enchantmentClass.contains("public static final String ID"), "Should have ID constant")
        assertTrue(enchantmentClass.contains("public static final int MAX_LEVEL"), "Should have MAX_LEVEL constant")
        assertTrue(enchantmentClass.contains("public static final boolean IS_TREASURE"), "Should have IS_TREASURE constant")
        assertTrue(enchantmentClass.contains("public static final boolean IS_TRADEABLE"), "Should have IS_TRADEABLE constant")
        assertTrue(enchantmentClass.contains("public static final boolean IS_DISCOVERABLE"), "Should have IS_DISCOVERABLE constant")
    }

    @Test
    fun `test common class has cost methods`() {
        val enchantmentName = "test_enchant"

        executeEnchantmentCommand(enchantmentName)

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/TestEnchant.java"))

        assertTrue(enchantmentClass.contains("public static int getMinCost(int level)"), "Should have getMinCost method")
        assertTrue(enchantmentClass.contains("public static int getMaxCost(int level)"), "Should have getMaxCost method")
    }

    @Test
    fun `test enchantment creation with custom max level`() {
        val enchantmentName = "power_strike"

        executeEnchantmentCommand(enchantmentName, mapOf("--max-level" to "5"))

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/PowerStrike.java"))
        assertTrue(enchantmentClass.contains("public static final int MAX_LEVEL = 5"))

        // Also verify enchantment JSON has matching max_level
        val jsonFile = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/power_strike.json"))
        assertTrue(jsonFile.contains("\"max_level\": 5"))
    }

    @Test
    fun `test enchantment creation with different rarities`() {
        val rarityWeights = mapOf(
            "common" to 10,
            "uncommon" to 5,
            "rare" to 2,
            "very_rare" to 1
        )

        rarityWeights.forEach { (rarity, expectedWeight) ->
            val enchantmentName = "${rarity}_enchantment"
            executeEnchantmentCommand(enchantmentName, mapOf("--rarity" to rarity))

            // Verify enchantment JSON has correct weight for rarity
            val jsonFile = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/$enchantmentName.json"))
            assertTrue(jsonFile.contains("\"weight\": $expectedWeight"), "Rarity $rarity should map to weight $expectedWeight")
        }
    }

    @Test
    fun `test treasure enchantment creation`() {
        val enchantmentName = "soul_speed"

        executeEnchantmentCommand(enchantmentName, mapOf("--treasure" to ""))

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/SoulSpeed.java"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_TREASURE = true"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_TRADEABLE = false"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_DISCOVERABLE = false"))
    }

    @Test
    fun `test non-treasure enchantment flags`() {
        val enchantmentName = "efficiency"

        executeEnchantmentCommand(enchantmentName)

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/Efficiency.java"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_TREASURE = false"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_TRADEABLE = true"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_DISCOVERABLE = true"))
    }

    @Test
    fun `test complex enchantment configuration`() {
        val enchantmentName = "custom_strike"

        executeEnchantmentCommand(enchantmentName, mapOf(
            "--max-level" to "10",
            "--rarity" to "very_rare",
            "--category" to "weapon",
            "--treasure" to ""
        ))

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/CustomStrike.java"))
        assertTrue(enchantmentClass.contains("public static final int MAX_LEVEL = 10"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_TREASURE = true"))

        // Verify enchantment JSON
        val jsonFile = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/custom_strike.json"))
        assertTrue(jsonFile.contains("\"max_level\": 10"))
        assertTrue(jsonFile.contains("\"weight\": 1")) // very_rare = 1
        assertTrue(jsonFile.contains("#minecraft:enchantable/weapon"))
    }

    @Test
    fun `test enchantment JSON file exists`() {
        val enchantmentName = "fire_aspect"

        executeEnchantmentCommand(enchantmentName)

        val jsonFile = File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/fire_aspect.json")
        assertTrue(jsonFile.exists(), "Enchantment JSON file should exist")
    }

    @Test
    fun `test enchantment JSON has required keys`() {
        val enchantmentName = "fire_aspect"

        executeEnchantmentCommand(enchantmentName)

        val jsonContent = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/fire_aspect.json"))
        assertTrue(jsonContent.contains("\"max_level\""), "JSON should have max_level key")
        assertTrue(jsonContent.contains("\"weight\""), "JSON should have weight key")
        assertTrue(jsonContent.contains("\"min_cost\""), "JSON should have min_cost key")
        assertTrue(jsonContent.contains("\"max_cost\""), "JSON should have max_cost key")
        assertTrue(jsonContent.contains("\"supported_items\""), "JSON should have supported_items key")
        assertTrue(jsonContent.contains("\"slots\""), "JSON should have slots key")
    }

    @Test
    fun `test enchantment JSON cost structure`() {
        val enchantmentName = "sharpness"

        executeEnchantmentCommand(enchantmentName)

        val jsonContent = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/sharpness.json"))
        assertTrue(jsonContent.contains("\"base\": 1"), "min_cost should have base: 1")
        assertTrue(jsonContent.contains("\"per_level_above_first\": 10"), "Should have per_level_above_first: 10")
        assertTrue(jsonContent.contains("\"base\": 51"), "max_cost should have base: 51")
    }

    @Test
    fun `test enchantment JSON weapon category uses weapon supported items`() {
        val enchantmentName = "sharpness"

        executeEnchantmentCommand(enchantmentName, mapOf("--category" to "weapon"))

        val jsonContent = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/sharpness.json"))
        assertTrue(jsonContent.contains("#minecraft:enchantable/weapon"), "Weapon category should use weapon supported items")
        assertTrue(jsonContent.contains("\"mainhand\""), "Weapon category should use mainhand slot")
    }

    @Test
    fun `test enchantment JSON armor category uses armor supported items`() {
        val enchantmentName = "protection"

        executeEnchantmentCommand(enchantmentName, mapOf("--category" to "armor"))

        val jsonContent = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/protection.json"))
        assertTrue(jsonContent.contains("#minecraft:enchantable/armor"), "Armor category should use armor supported items")
    }

    @Test
    fun `test enchantment is data-driven only`() {
        val enchantmentName = "fire_aspect"

        executeEnchantmentCommand(enchantmentName)

        // Enchantments in Architectury are data-driven, no per-loader registration files
        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/FireAspect.java"))
        assertTrue(enchantmentClass.contains("public class FireAspect"))
        assertTrue(enchantmentClass.contains("data-driven"), "Should mention data-driven approach")

        // Per-loader files should NOT exist
        val fabricFile = File(context.projectDir, "shared/fabric/src/main/java/com/testmod/platform/fabric/FireAspectFabric.java")
        assertFalse(fabricFile.exists(), "Per-loader enchantment file should not exist")
        val forgeFile = File(context.projectDir, "shared/forge/src/main/java/com/testmod/platform/forge/FireAspectForge.java")
        assertFalse(forgeFile.exists(), "Per-loader enchantment file should not exist")
        val neoforgeFile = File(context.projectDir, "shared/neoforge/src/main/java/com/testmod/platform/neoforge/FireAspectNeoForge.java")
        assertFalse(neoforgeFile.exists(), "Per-loader enchantment file should not exist")
    }

    @Test
    fun `test lang entry formatting`() {
        val enchantmentName = "custom_enchant"

        executeEnchantmentCommand(enchantmentName)

        val langFile = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json"))
        assertTrue(langFile.contains("\"enchantment.testmod.custom_enchant\": \"Custom Enchant\""))
    }

    @Test
    fun `test multiple enchantments in lang file`() {
        // Create first enchantment
        executeEnchantmentCommand("first_enchant")

        // Create second enchantment
        executeEnchantmentCommand("second_enchant")

        val langFile = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json"))
        assertTrue(langFile.contains("\"enchantment.testmod.first_enchant\": \"First Enchant\""))
        assertTrue(langFile.contains("\"enchantment.testmod.second_enchant\": \"Second Enchant\""))
    }

    @Test
    fun `test lang file creation when not exists`() {
        val enchantmentName = "new_enchant"

        // Ensure lang file doesn't exist
        val langFile = File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        assertFalse(langFile.exists())

        executeEnchantmentCommand(enchantmentName)

        // Verify lang file was created
        assertTrue(langFile.exists())
        val content = FileUtil.readText(langFile)
        assertTrue(content.contains("\"enchantment.testmod.new_enchant\": \"New Enchant\""))
    }

    @Test
    fun `test snake_case to display name conversion`() {
        val testCases = mapOf(
            "fire_aspect" to "Fire Aspect",
            "soul_speed" to "Soul Speed",
            "swift_sneak" to "Swift Sneak",
            "multi_word_enchant" to "Multi Word Enchant"
        )

        testCases.forEach { (snakeName, expectedDisplay) ->
            executeEnchantmentCommand(snakeName)

            val langFile = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json"))
            assertTrue(langFile.contains("\"enchantment.testmod.$snakeName\": \"$expectedDisplay\""))

            // Clean up for next test
            File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json").delete()
        }
    }

    @Test
    fun `test snake_case to class name conversion`() {
        val testCases = mapOf(
            "fire_aspect" to "FireAspect",
            "soul_speed" to "SoulSpeed",
            "swift_sneak" to "SwiftSneak",
            "single" to "Single"
        )

        testCases.forEach { (snakeName, expectedClass) ->
            executeEnchantmentCommand(snakeName)

            val classFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/$expectedClass.java")
            assertTrue(classFile.exists(), "Expected class file: $expectedClass.java")

            val content = FileUtil.readText(classFile)
            assertTrue(content.contains("public class $expectedClass"))
        }
    }

    @Test
    fun `test enchantment common class has correct package structure`() {
        val enchantmentName = "test_enchant"

        executeEnchantmentCommand(enchantmentName)

        // Enchantments are data-driven; only common class exists
        val commonContent = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/TestEnchant.java"))
        assertTrue(commonContent.startsWith("package com.testmod.enchantments;"))
    }

    @Test
    fun `test enchantment documentation comments`() {
        val enchantmentName = "test_enchant"

        executeEnchantmentCommand(enchantmentName)

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/TestEnchant.java"))

        // Check for documentation
        assertTrue(enchantmentClass.contains("/**"))
        assertTrue(enchantmentClass.contains("Enchantment definition: TestEnchant"))
        assertTrue(enchantmentClass.contains("Holds configuration constants"))
        assertTrue(enchantmentClass.contains("data-driven"))
    }

    @Test
    fun `test high level enchantment`() {
        val enchantmentName = "ultra_power"

        executeEnchantmentCommand(enchantmentName, mapOf("--max-level" to "255"))

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/UltraPower.java"))
        assertTrue(enchantmentClass.contains("public static final int MAX_LEVEL = 255"))

        val jsonContent = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/ultra_power.json"))
        assertTrue(jsonContent.contains("\"max_level\": 255"))
    }

    @Test
    fun `test enchantment ID matches snake_case name`() {
        val enchantmentName = "custom_enchantment"

        executeEnchantmentCommand(enchantmentName)

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/CustomEnchantment.java"))
        assertTrue(enchantmentClass.contains("public static final String ID = \"custom_enchantment\""))
    }

    @Test
    fun `test enchantment JSON rarity weight mapping`() {
        // common = 10
        executeEnchantmentCommand("common_ench", mapOf("--rarity" to "common"))
        val commonJson = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/common_ench.json"))
        assertTrue(commonJson.contains("\"weight\": 10"), "common rarity should have weight 10")

        // uncommon = 5
        executeEnchantmentCommand("uncommon_ench", mapOf("--rarity" to "uncommon"))
        val uncommonJson = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/uncommon_ench.json"))
        assertTrue(uncommonJson.contains("\"weight\": 5"), "uncommon rarity should have weight 5")

        // rare = 2
        executeEnchantmentCommand("rare_ench", mapOf("--rarity" to "rare"))
        val rareJson = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/rare_ench.json"))
        assertTrue(rareJson.contains("\"weight\": 2"), "rare rarity should have weight 2")

        // very_rare = 1
        executeEnchantmentCommand("very_rare_ench", mapOf("--rarity" to "very_rare"))
        val veryRareJson = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/enchantment/very_rare_ench.json"))
        assertTrue(veryRareJson.contains("\"weight\": 1"), "very_rare rarity should have weight 1")
    }

    // Helper methods

    private fun executeEnchantmentCommand(
        enchantmentName: String,
        extraOptions: Map<String, String> = emptyMap()
    ) {
        val command = CreateEnchantmentCommand()

        // Build command args
        val args = mutableListOf(enchantmentName)
        extraOptions.forEach { (key, value) ->
            args.add(key)
            if (value.isNotEmpty()) {
                args.add(value)
            }
        }

        // Set project directory before parsing
        command.projectDir = context.projectDir

        // Execute command
        command.parse(args.toTypedArray())
    }

    private fun assertEnchantmentFilesExist(enchantmentName: String, expectedFiles: List<String>) {
        expectedFiles.forEach { filePath ->
            val file = File(context.projectDir, filePath)
            assertTrue(
                file.exists(),
                "Expected file to exist: $filePath (absolute: ${file.absolutePath})"
            )
        }
    }

    private fun toClassName(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { word -> word.replaceFirstChar { it.uppercase() } }
    }
}
