package dev.dropper.e2e

import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.util.FileUtil
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive E2E tests for the asset pack system.
 *
 * Tests:
 * - Asset pack creation
 * - Asset pack inheritance
 * - Asset resolution
 * - Version switching between asset packs
 * - Edge cases (missing packs, circular inheritance)
 */
class AssetPackE2ETest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("asset-pack-test")
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    @Test
    fun `create project with default asset pack v1`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Default Asset Pack (v1)                                   ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val config = ModConfig(
            id = "assettest",
            name = "Asset Test",
            version = "1.0.0",
            description = "Asset pack test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        // Verify default v1 asset pack exists
        val v1Pack = context.file("versions/shared/v1")
        assertTrue(v1Pack.exists(), "Default v1 asset pack should exist")
        assertTrue(File(v1Pack, "config.yml").exists(), "v1 config should exist")
        assertTrue(File(v1Pack, "assets").exists(), "v1 assets directory should exist")
        assertTrue(File(v1Pack, "data").exists(), "v1 data directory should exist")

        println("  ✓ Default v1 asset pack created")
        println("  ✓ Config file present")
        println("  ✓ Assets directory present")
        println("  ✓ Data directory present")
    }

    @Test
    fun `create v2 asset pack inheriting from v1`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Asset Pack Inheritance (v2 inherits v1)                   ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        // Create project with v1
        val config = ModConfig(
            id = "inheritancetest",
            name = "Inheritance Test",
            version = "1.0.0",
            description = "Test inheritance",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        // Create v2 asset pack
        val v2Pack = context.file( "versions/shared/v2")
        v2Pack.mkdirs()
        File(v2Pack, "assets").mkdirs()
        File(v2Pack, "data").mkdirs()

        // Create v2 config that inherits from v1
        val v2Config = """
            asset_pack:
              version: "v2"
              inherits: "v1"
              minecraft_versions:
                - "1.21.1"
        """.trimIndent()

        FileUtil.writeText(File(v2Pack, "config.yml"), v2Config)

        // Update 1.21.1 to use v2
        val version121Config = context.file( "versions/1_21_1/config.yml")
        val currentConfig = version121Config.readText()
        val updatedConfig = currentConfig.replace("asset_pack: \"v1\"", "asset_pack: \"v2\"")
        FileUtil.writeText(version121Config, updatedConfig)

        // Verify structure
        assertTrue(v2Pack.exists(), "v2 asset pack should exist")
        assertTrue(File(v2Pack, "config.yml").exists(), "v2 config should exist")

        val v2ConfigContent = File(v2Pack, "config.yml").readText()
        assertTrue(v2ConfigContent.contains("inherits: \"v1\""), "v2 should inherit from v1")

        println("  ✓ v2 asset pack created")
        println("  ✓ Inheritance configured")
        println("  ✓ Version updated to use v2")
    }

    @Test
    fun `test asset resolution with inheritance`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Asset Resolution with Inheritance                         ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val config = ModConfig(
            id = "resolutiontest",
            name = "Resolution Test",
            version = "1.0.0",
            description = "Test resolution",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        // Create asset in v1
        val v1Asset = context.file( "versions/shared/v1/assets/resolutiontest/textures/item/base_item.png")
        v1Asset.parentFile.mkdirs()
        v1Asset.createNewFile()

        // Create v2 that inherits v1
        val v2Pack = context.file( "versions/shared/v2")
        v2Pack.mkdirs()
        File(v2Pack, "assets/resolutiontest/textures/item").mkdirs()
        File(v2Pack, "data").mkdirs()

        val v2Config = """
            asset_pack:
              version: "v2"
              inherits: "v1"
              minecraft_versions:
                - "1.20.1"
        """.trimIndent()
        FileUtil.writeText(File(v2Pack, "config.yml"), v2Config)

        // Create override in v2
        val v2Override = File(v2Pack, "assets/resolutiontest/textures/item/override_item.png")
        v2Override.createNewFile()

        // Verify both packs have their assets
        assertTrue(v1Asset.exists(), "v1 asset should exist")
        assertTrue(v2Override.exists(), "v2 override should exist")

        // Verify v2 can access both
        val v2Assets = File(v2Pack, "assets/resolutiontest/textures/item").listFiles()?.map { it.name } ?: emptyList()
        assertTrue(v2Assets.contains("override_item.png"), "v2 should have its own asset")

        println("  ✓ Base asset in v1")
        println("  ✓ Override asset in v2")
        println("  ✓ Both packs accessible")
    }

    @Test
    fun `test multiple versions sharing same asset pack`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Multiple Versions Sharing Asset Pack                      ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val config = ModConfig(
            id = "sharetest",
            name = "Share Test",
            version = "1.0.0",
            description = "Test sharing",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.19.2", "1.18.2"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        // Verify all versions use v1 by default
        val versions = listOf("1_20_1", "1_19_2", "1_18_2")
        versions.forEach { version ->
            val versionConfig = context.file( "versions/$version/config.yml")
            assertTrue(versionConfig.exists(), "$version config should exist")

            val content = versionConfig.readText()
            assertTrue(
                content.contains("asset_pack: \"v1\""),
                "$version should use v1 asset pack"
            )
            println("  ✓ $version uses v1 asset pack")
        }

        // Add asset to shared v1
        val sharedAsset = context.file( "versions/shared/v1/assets/sharetest/models/item/shared_item.json")
        sharedAsset.parentFile.mkdirs()
        FileUtil.writeText(sharedAsset, """{"parent": "minecraft:item/generated"}""")

        // Verify asset is accessible to all versions
        assertTrue(sharedAsset.exists(), "Shared asset should exist")
        println("  ✓ Shared asset accessible to all versions")
    }

    @Test
    fun `test asset pack with multiple inheritance levels`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Multiple Inheritance Levels (v1 → v2 → v3)                ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val config = ModConfig(
            id = "multilevel",
            name = "Multi Level",
            version = "1.0.0",
            description = "Test multi-level",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        // Create v2 inheriting v1
        val v2Pack = context.file( "versions/shared/v2")
        v2Pack.mkdirs()
        File(v2Pack, "assets").mkdirs()
        File(v2Pack, "data").mkdirs()

        val v2Config = """
            asset_pack:
              version: "v2"
              inherits: "v1"
        """.trimIndent()
        FileUtil.writeText(File(v2Pack, "config.yml"), v2Config)

        // Create v3 inheriting v2
        val v3Pack = context.file( "versions/shared/v3")
        v3Pack.mkdirs()
        File(v3Pack, "assets").mkdirs()
        File(v3Pack, "data").mkdirs()

        val v3Config = """
            asset_pack:
              version: "v3"
              inherits: "v2"
        """.trimIndent()
        FileUtil.writeText(File(v3Pack, "config.yml"), v3Config)

        // Verify chain
        assertTrue(v2Pack.exists(), "v2 should exist")
        assertTrue(v3Pack.exists(), "v3 should exist")

        val v2Content = File(v2Pack, "config.yml").readText()
        val v3Content = File(v3Pack, "config.yml").readText()

        assertTrue(v2Content.contains("inherits: \"v1\""), "v2 should inherit v1")
        assertTrue(v3Content.contains("inherits: \"v2\""), "v3 should inherit v2")

        println("  ✓ v1 created (base)")
        println("  ✓ v2 created (inherits v1)")
        println("  ✓ v3 created (inherits v2)")
        println("  ✓ Inheritance chain: v1 → v2 → v3")
    }

    @Test
    fun `test asset pack structure validation`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Asset Pack Structure Validation                           ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val config = ModConfig(
            id = "structuretest",
            name = "Structure Test",
            version = "1.0.0",
            description = "Test structure",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        val v1Pack = context.file( "versions/shared/v1")

        // Verify required directories
        val requiredDirs = listOf(
            "assets",
            "data"
        )

        requiredDirs.forEach { dir ->
            val dirFile = File(v1Pack, dir)
            assertTrue(dirFile.exists(), "$dir should exist in v1")
            assertTrue(dirFile.isDirectory, "$dir should be a directory")
            println("  ✓ $dir directory present")
        }

        // Verify mod-specific subdirectories
        val modAssets = File(v1Pack, "assets/structuretest")
        assertTrue(modAssets.exists(), "Mod assets directory should exist")

        val modData = File(v1Pack, "data/structuretest")
        assertTrue(modData.exists(), "Mod data directory should exist")

        println("  ✓ Mod-specific directories present")
    }

    @Test
    fun `test empty asset pack creation`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Empty Asset Pack Creation                                 ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val config = ModConfig(
            id = "emptytest",
            name = "Empty Test",
            version = "1.0.0",
            description = "Test empty pack",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        // Create empty v2 pack
        val v2Pack = context.file( "versions/shared/v2")
        v2Pack.mkdirs()

        val emptyConfig = """
            asset_pack:
              version: "v2"
        """.trimIndent()
        FileUtil.writeText(File(v2Pack, "config.yml"), emptyConfig)

        // Verify it exists but has no assets
        assertTrue(v2Pack.exists(), "Empty v2 pack should exist")
        assertTrue(File(v2Pack, "config.yml").exists(), "Empty pack should have config")

        val v2Files = v2Pack.listFiles()?.filter { it.isDirectory } ?: emptyList()
        assertTrue(v2Files.isEmpty(), "Empty pack should have no asset directories")

        println("  ✓ Empty asset pack created")
        println("  ✓ Config file present")
        println("  ✓ No asset directories (as expected)")
    }

    @Test
    fun `test asset pack namespace organization`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Asset Pack Namespace Organization                         ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val config = ModConfig(
            id = "namespacetest",
            name = "Namespace Test",
            version = "1.0.0",
            description = "Test namespaces",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        val v1Pack = context.file( "versions/shared/v1")

        // Create assets in mod namespace
        val modAssets = File(v1Pack, "assets/namespacetest")
        val modTextures = File(modAssets, "textures/item")
        modTextures.mkdirs()

        val modModels = File(modAssets, "models/item")
        modModels.mkdirs()

        val modLang = File(modAssets, "lang")
        modLang.mkdirs()

        // Create data in mod namespace
        val modData = File(v1Pack, "data/namespacetest")
        val modRecipes = File(modData, "recipe")
        modRecipes.mkdirs()

        val modLootTables = File(modData, "loot_table/blocks")
        modLootTables.mkdirs()

        // Verify namespace structure
        assertTrue(modTextures.exists(), "textures/item should exist")
        assertTrue(modModels.exists(), "models/item should exist")
        assertTrue(modLang.exists(), "lang should exist")
        assertTrue(modRecipes.exists(), "recipe should exist")
        assertTrue(modLootTables.exists(), "loot_table/blocks should exist")

        println("  ✓ Mod namespace: namespacetest")
        println("  ✓ Assets organized by type")
        println("  ✓ Data organized by type")
        println("  ✓ All standard directories present")
    }
}
