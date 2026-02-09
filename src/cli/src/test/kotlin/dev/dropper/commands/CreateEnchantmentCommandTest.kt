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
            "shared/fabric/src/main/java/com/testmod/platform/fabric/TestEnchantmentFabric.java",
            "shared/forge/src/main/java/com/testmod/platform/forge/TestEnchantmentForge.java",
            "shared/neoforge/src/main/java/com/testmod/platform/neoforge/TestEnchantmentNeoForge.java",
            "versions/shared/v1/assets/testmod/lang/en_us.json"
        ))

        // Verify enchantment class content
        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/TestEnchantment.java"))
        assertTrue(enchantmentClass.contains("public class TestEnchantment"))
        assertTrue(enchantmentClass.contains("public static final String ID = \"test_enchantment\""))
        assertTrue(enchantmentClass.contains("public static final int MAX_LEVEL = 1"))
        assertTrue(enchantmentClass.contains("public static final Rarity RARITY = Rarity.COMMON"))
        assertTrue(enchantmentClass.contains("public static final Category CATEGORY = Category.BREAKABLE"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_TREASURE = false"))

        // Verify lang entry
        val langFile = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json"))
        assertTrue(langFile.contains("\"enchantment.testmod.test_enchantment\": \"Test Enchantment\""))
    }

    @Test
    fun `test enchantment creation with custom max level`() {
        val enchantmentName = "power_strike"

        executeEnchantmentCommand(enchantmentName, mapOf("--max-level" to "5"))

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/PowerStrike.java"))
        assertTrue(enchantmentClass.contains("public static final int MAX_LEVEL = 5"))
    }

    @Test
    fun `test enchantment creation with different rarities`() {
        val rarities = listOf("common", "uncommon", "rare", "very_rare")

        rarities.forEach { rarity ->
            val enchantmentName = "${rarity}_enchantment"
            executeEnchantmentCommand(enchantmentName, mapOf("--rarity" to rarity))

            val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/${toClassName(enchantmentName)}.java"))
            assertTrue(enchantmentClass.contains("public static final Rarity RARITY = Rarity.${rarity.uppercase()}"))
        }
    }

    @Test
    fun `test enchantment creation with weapon category`() {
        val enchantmentName = "sharpness"

        executeEnchantmentCommand(enchantmentName, mapOf("--category" to "weapon"))

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/Sharpness.java"))
        assertTrue(enchantmentClass.contains("public static final Category CATEGORY = Category.WEAPON"))
    }

    @Test
    fun `test enchantment creation with armor category`() {
        val enchantmentName = "protection"

        executeEnchantmentCommand(enchantmentName, mapOf("--category" to "armor"))

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/Protection.java"))
        assertTrue(enchantmentClass.contains("public static final Category CATEGORY = Category.ARMOR"))
    }

    @Test
    fun `test enchantment creation with armor piece categories`() {
        val categories = listOf("armor_feet", "armor_legs", "armor_chest", "armor_head")

        categories.forEach { category ->
            val enchantmentName = "${category.replace("_", "")}_enchant"
            executeEnchantmentCommand(enchantmentName, mapOf("--category" to category))

            val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/${toClassName(enchantmentName)}.java"))
            assertTrue(enchantmentClass.contains("public static final Category CATEGORY = Category.${category.uppercase()}"))
        }
    }

    @Test
    fun `test enchantment creation with tool categories`() {
        val categories = listOf("digger", "fishing_rod", "trident", "bow", "crossbow")

        categories.forEach { category ->
            val enchantmentName = "${category.replace("_", "")}_enchant"
            executeEnchantmentCommand(enchantmentName, mapOf("--category" to category))

            val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/${toClassName(enchantmentName)}.java"))
            assertTrue(enchantmentClass.contains("public static final Category CATEGORY = Category.${category.uppercase()}"))
        }
    }

    @Test
    fun `test enchantment creation with wearable category`() {
        val enchantmentName = "frost_walker"

        executeEnchantmentCommand(enchantmentName, mapOf("--category" to "wearable"))

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/FrostWalker.java"))
        assertTrue(enchantmentClass.contains("public static final Category CATEGORY = Category.WEARABLE"))
    }

    @Test
    fun `test enchantment creation with vanishable category`() {
        val enchantmentName = "vanishing_curse"

        executeEnchantmentCommand(enchantmentName, mapOf("--category" to "vanishable"))

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/VanishingCurse.java"))
        assertTrue(enchantmentClass.contains("public static final Category CATEGORY = Category.VANISHABLE"))
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
        assertTrue(enchantmentClass.contains("public static final Rarity RARITY = Rarity.VERY_RARE"))
        assertTrue(enchantmentClass.contains("public static final Category CATEGORY = Category.WEAPON"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_TREASURE = true"))
    }

    @Test
    fun `test enchantment enums are defined`() {
        val enchantmentName = "test_enchant"

        executeEnchantmentCommand(enchantmentName)

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/TestEnchant.java"))

        // Check Rarity enum
        assertTrue(enchantmentClass.contains("public enum Rarity {"))
        assertTrue(enchantmentClass.contains("COMMON,"))
        assertTrue(enchantmentClass.contains("UNCOMMON,"))
        assertTrue(enchantmentClass.contains("RARE,"))
        assertTrue(enchantmentClass.contains("VERY_RARE"))

        // Check Category enum
        assertTrue(enchantmentClass.contains("public enum Category {"))
        assertTrue(enchantmentClass.contains("ARMOR,"))
        assertTrue(enchantmentClass.contains("WEAPON,"))
        assertTrue(enchantmentClass.contains("DIGGER,"))
        assertTrue(enchantmentClass.contains("BREAKABLE,"))
    }

    @Test
    fun `test fabric registration content`() {
        val enchantmentName = "fire_aspect"

        executeEnchantmentCommand(enchantmentName)

        val fabricFile = File(context.projectDir, "shared/fabric/src/main/java/com/testmod/platform/fabric/FireAspectFabric.java")
        assertTrue(fabricFile.exists())

        val fabricContent = FileUtil.readText(fabricFile)
        assertTrue(fabricContent.contains("import com.testmod.enchantments.FireAspect"))
        assertTrue(fabricContent.contains("import net.minecraft.enchantment.Enchantment"))
        assertTrue(fabricContent.contains("import net.minecraft.registry.Registries"))
        assertTrue(fabricContent.contains("import net.minecraft.registry.Registry"))
        assertTrue(fabricContent.contains("public class FireAspectFabric"))
        assertTrue(fabricContent.contains("public static void register()"))
    }

    @Test
    fun `test forge registration content`() {
        val enchantmentName = "smite"

        executeEnchantmentCommand(enchantmentName)

        val forgeFile = File(context.projectDir, "shared/forge/src/main/java/com/testmod/platform/forge/SmiteForge.java")
        assertTrue(forgeFile.exists())

        val forgeContent = FileUtil.readText(forgeFile)
        assertTrue(forgeContent.contains("import com.testmod.enchantments.Smite"))
        assertTrue(forgeContent.contains("import net.minecraft.world.item.enchantment.Enchantment"))
        assertTrue(forgeContent.contains("import net.minecraftforge.registries.DeferredRegister"))
        assertTrue(forgeContent.contains("import net.minecraftforge.registries.ForgeRegistries"))
        assertTrue(forgeContent.contains("public class SmiteForge"))
    }

    @Test
    fun `test neoforge registration content`() {
        val enchantmentName = "looting"

        executeEnchantmentCommand(enchantmentName)

        val neoforgeFile = File(context.projectDir, "shared/neoforge/src/main/java/com/testmod/platform/neoforge/LootingNeoForge.java")
        assertTrue(neoforgeFile.exists())

        val neoforgeContent = FileUtil.readText(neoforgeFile)
        assertTrue(neoforgeContent.contains("import com.testmod.enchantments.Looting"))
        assertTrue(neoforgeContent.contains("import net.minecraft.core.registries.Registries"))
        assertTrue(neoforgeContent.contains("import net.minecraft.world.item.enchantment.Enchantment"))
        assertTrue(neoforgeContent.contains("import net.neoforged.neoforge.registries.DeferredRegister"))
        assertTrue(neoforgeContent.contains("import net.neoforged.neoforge.registries.DeferredHolder"))
        assertTrue(neoforgeContent.contains("public class LootingNeoForge"))
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
    fun `test all loader registrations have correct package structure`() {
        val enchantmentName = "test_enchant"

        executeEnchantmentCommand(enchantmentName)

        // Check Fabric
        val fabricContent = FileUtil.readText(File(context.projectDir, "shared/fabric/src/main/java/com/testmod/platform/fabric/TestEnchantFabric.java"))
        assertTrue(fabricContent.startsWith("package com.testmod.platform.fabric;"))

        // Check Forge
        val forgeContent = FileUtil.readText(File(context.projectDir, "shared/forge/src/main/java/com/testmod/platform/forge/TestEnchantForge.java"))
        assertTrue(forgeContent.startsWith("package com.testmod.platform.forge;"))

        // Check NeoForge
        val neoforgeContent = FileUtil.readText(File(context.projectDir, "shared/neoforge/src/main/java/com/testmod/platform/neoforge/TestEnchantNeoForge.java"))
        assertTrue(neoforgeContent.startsWith("package com.testmod.platform.neoforge;"))
    }

    @Test
    fun `test enchantment with all categories`() {
        val categories = listOf(
            "armor", "armor_feet", "armor_legs", "armor_chest", "armor_head",
            "weapon", "digger", "fishing_rod", "trident", "breakable",
            "bow", "wearable", "crossbow", "vanishable"
        )

        categories.forEach { category ->
            val enchantmentName = "test_${category.replace("_", "")}"
            executeEnchantmentCommand(enchantmentName, mapOf("--category" to category))

            val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/${toClassName(enchantmentName)}.java"))
            assertTrue(enchantmentClass.contains("public static final Category CATEGORY = Category.${category.uppercase()}"))
        }
    }

    @Test
    fun `test enchantment documentation comments`() {
        val enchantmentName = "test_enchant"

        executeEnchantmentCommand(enchantmentName)

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/TestEnchant.java"))

        // Check for documentation
        assertTrue(enchantmentClass.contains("/**"))
        assertTrue(enchantmentClass.contains("* Custom enchantment: TestEnchant"))
        assertTrue(enchantmentClass.contains("* Registration pattern for multi-loader compatibility"))
        assertTrue(enchantmentClass.contains("* Enchantment rarity levels"))
        assertTrue(enchantmentClass.contains("* Enchantment categories"))
    }

    @Test
    fun `test enchantment with breakable category default`() {
        val enchantmentName = "mending"

        executeEnchantmentCommand(enchantmentName)

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/Mending.java"))
        assertTrue(enchantmentClass.contains("public static final Category CATEGORY = Category.BREAKABLE"))
    }

    @Test
    fun `test high level enchantment`() {
        val enchantmentName = "ultra_power"

        executeEnchantmentCommand(enchantmentName, mapOf("--max-level" to "255"))

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/UltraPower.java"))
        assertTrue(enchantmentClass.contains("public static final int MAX_LEVEL = 255"))
    }

    @Test
    fun `test enchantment ID matches snake_case name`() {
        val enchantmentName = "custom_enchantment"

        executeEnchantmentCommand(enchantmentName)

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/CustomEnchantment.java"))
        assertTrue(enchantmentClass.contains("public static final String ID = \"custom_enchantment\""))
    }

    @Test
    fun `test curse flag is always false`() {
        val enchantmentName = "test_curse"

        executeEnchantmentCommand(enchantmentName)

        val enchantmentClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/enchantments/TestCurse.java"))
        assertTrue(enchantmentClass.contains("public static final boolean IS_CURSE = false"))
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
        return snakeCase.split("_").joinToString("") { it.capitalize() }
    }
}
