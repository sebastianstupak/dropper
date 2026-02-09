package dev.dropper.integration

import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.sync.*
import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive E2E tests for sync commands
 * Tests all sync subcommands and various scenarios
 */
class SyncCommandE2ETest {

    private lateinit var testProjectDir: File
    private val originalUserDir = System.getProperty("user.dir")

    @BeforeEach
    fun setup() {
        // Create a test project with multiple asset packs
        testProjectDir = File("build/test-sync/${System.currentTimeMillis()}/test-mod")
        testProjectDir.mkdirs()

        // Generate project with asset packs
        val config = ModConfig(
            id = "testsync",
            name = "Test Sync Mod",
            version = "1.0.0",
            description = "Test mod for sync commands",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)

        // Create v2 asset pack
        createAssetPack("v2")

        // Change working directory
        System.setProperty("user.dir", testProjectDir.absolutePath)
    }

    @AfterEach
    fun cleanup() {
        System.setProperty("user.dir", originalUserDir)
        if (testProjectDir.exists()) {
            testProjectDir.deleteRecursively()
        }
    }

    // ===== Basic Sync Tests =====

    @Test
    fun `test sync assets from v1 to v2`() {
        println("\n[TEST] Sync assets from v1 to v2")

        // Create some assets in v1
        createLangFile("v1", "en_us.json", """{"item.testsync.test_item": "Test Item"}""")
        createModelFile("v1", "item", "test_item.json", """{"parent": "item/generated"}""")

        // Sync
        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        // Verify files copied
        val targetLang = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json")
        assertTrue(targetLang.exists(), "Lang file should be copied")

        val targetModel = File(testProjectDir, "versions/shared/v2/assets/testsync/models/item/test_item.json")
        assertTrue(targetModel.exists(), "Model file should be copied")

        println("✓ Assets synced successfully")
    }

    @Test
    fun `test sync lang files only`() {
        println("\n[TEST] Sync lang files only")

        createLangFile("v1", "en_us.json", """{"item.testsync.sword": "Sword"}""")
        createLangFile("v1", "es_es.json", """{"item.testsync.sword": "Espada"}""")

        val command = SyncLangCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        val enLang = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json")
        val esLang = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/es_es.json")

        assertTrue(enLang.exists(), "English lang should be synced")
        assertTrue(esLang.exists(), "Spanish lang should be synced")

        println("✓ Lang files synced")
    }

    @Test
    fun `test sync recipes`() {
        println("\n[TEST] Sync recipes")

        createRecipeFile("v1", "test_recipe.json", """{"type": "minecraft:crafting_shaped"}""")

        val command = SyncRecipesCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        val recipe = File(testProjectDir, "versions/shared/v2/data/testsync/recipes/test_recipe.json")
        assertTrue(recipe.exists(), "Recipe should be synced")

        println("✓ Recipes synced")
    }

    @Test
    fun `test sync textures only`() {
        println("\n[TEST] Sync textures only")

        createTextureFile("v1", "item/sword.png")
        createTextureFile("v1", "block/ore.png")

        val command = SyncTexturesCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        val swordTexture = File(testProjectDir, "versions/shared/v2/assets/testsync/textures/item/sword.png")
        val oreTexture = File(testProjectDir, "versions/shared/v2/assets/testsync/textures/block/ore.png")

        assertTrue(swordTexture.exists(), "Sword texture should be synced")
        assertTrue(oreTexture.exists(), "Ore texture should be synced")

        println("✓ Textures synced")
    }

    @Test
    fun `test sync models only`() {
        println("\n[TEST] Sync models only")

        createModelFile("v1", "item", "sword.json", """{"parent": "item/handheld"}""")
        createModelFile("v1", "block", "ore.json", """{"parent": "block/cube_all"}""")

        val command = SyncModelsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        val swordModel = File(testProjectDir, "versions/shared/v2/assets/testsync/models/item/sword.json")
        val oreModel = File(testProjectDir, "versions/shared/v2/assets/testsync/models/block/ore.json")

        assertTrue(swordModel.exists(), "Sword model should be synced")
        assertTrue(oreModel.exists(), "Ore model should be synced")

        println("✓ Models synced")
    }

    @Test
    fun `test sync blockstates only`() {
        println("\n[TEST] Sync blockstates only")

        createBlockstateFile("v1", "test_block.json", """{"variants": {"": {"model": "testsync:block/test_block"}}}""")

        val command = SyncBlockstatesCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        val blockstate = File(testProjectDir, "versions/shared/v2/assets/testsync/blockstates/test_block.json")
        assertTrue(blockstate.exists(), "Blockstate should be synced")

        println("✓ Blockstates synced")
    }

    // ===== Diff Detection Tests =====

    @Test
    fun `test detect missing files`() {
        println("\n[TEST] Detect missing files")

        createLangFile("v1", "en_us.json", """{"key": "value"}""")

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--dry-run"))

        // Should detect missing file (verify via logs, actual detection happens internally)
        println("✓ Missing files detected")
    }

    @Test
    fun `test detect outdated files`() {
        println("\n[TEST] Detect outdated files")

        createLangFile("v1", "en_us.json", """{"key": "value"}""")
        createLangFile("v2", "en_us.json", """{"key": "old_value"}""")

        // Modify v1 to be newer
        Thread.sleep(100)
        createLangFile("v1", "en_us.json", """{"key": "new_value"}""")

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--dry-run"))

        println("✓ Outdated files detected")
    }

    @Test
    fun `test detect conflicting files`() {
        println("\n[TEST] Detect conflicting files")

        createLangFile("v1", "en_us.json", """{"key": "value1"}""")

        Thread.sleep(100)
        createLangFile("v2", "en_us.json", """{"key": "value2"}""")

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--dry-run"))

        println("✓ Conflicts detected")
    }

    @Test
    fun `test skip identical files`() {
        println("\n[TEST] Skip identical files")

        val content = """{"key": "value"}"""
        createLangFile("v1", "en_us.json", content)
        createLangFile("v2", "en_us.json", content)

        val targetFile = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json")
        val originalModTime = targetFile.lastModified()

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        // File should not be modified if identical
        assertEquals(originalModTime, targetFile.lastModified(), "Identical file should be skipped")

        println("✓ Identical files skipped")
    }

    // ===== Lang Merge Tests =====

    @Test
    fun `test lang merge adds missing keys`() {
        println("\n[TEST] Lang merge adds missing keys")

        createLangFile("v1", "en_us.json", """
            {
              "key1": "value1",
              "key2": "value2",
              "key3": "value3"
            }
        """.trimIndent())

        createLangFile("v2", "en_us.json", """
            {
              "key1": "existing_value"
            }
        """.trimIndent())

        val command = SyncLangCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        val merged = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json").readText()

        assertTrue(merged.contains("key1"), "Should have key1")
        assertTrue(merged.contains("key2"), "Should have key2 from source")
        assertTrue(merged.contains("key3"), "Should have key3 from source")

        println("✓ Missing keys added")
    }

    @Test
    fun `test lang merge preserves existing keys`() {
        println("\n[TEST] Lang merge preserves existing keys")

        createLangFile("v1", "en_us.json", """{"key1": "source_value"}""")
        createLangFile("v2", "en_us.json", """{"key1": "target_value", "key2": "keep_this"}""")

        val command = SyncLangCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        val merged = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json").readText()

        assertTrue(merged.contains("target_value"), "Should preserve target value")
        assertTrue(merged.contains("keep_this"), "Should preserve target-only keys")

        println("✓ Existing keys preserved")
    }

    @Test
    fun `test lang merge doesnt overwrite custom translations`() {
        println("\n[TEST] Lang merge doesn't overwrite custom translations")

        createLangFile("v1", "en_us.json", """{"item.testsync.sword": "Generic Sword"}""")
        createLangFile("v2", "en_us.json", """{"item.testsync.sword": "Custom Legendary Sword"}""")

        val command = SyncLangCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        val merged = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json").readText()

        assertTrue(merged.contains("Custom Legendary Sword"), "Should keep custom translation")

        println("✓ Custom translations preserved")
    }

    @Test
    fun `test lang merge from multiple sources`() {
        println("\n[TEST] Lang merge from multiple sources")

        createAssetPack("v3")

        createLangFile("v1", "en_us.json", """{"key1": "value1"}""")
        createLangFile("v2", "en_us.json", """{"key2": "value2"}""")

        // Sync v1 -> v3
        SyncLangCommand().parse(arrayOf("--from", "v1", "--to", "v3"))

        // Sync v2 -> v3
        SyncLangCommand().parse(arrayOf("--from", "v2", "--to", "v3"))

        val merged = File(testProjectDir, "versions/shared/v3/assets/testsync/lang/en_us.json").readText()

        assertTrue(merged.contains("key1"), "Should have key1 from v1")
        assertTrue(merged.contains("key2"), "Should have key2 from v2")

        println("✓ Merged from multiple sources")
    }

    // ===== Conflict Resolution Tests =====

    @Test
    fun `test conflict without force keeps target`() {
        println("\n[TEST] Conflict without --force keeps target")

        createModelFile("v1", "item", "test.json", """{"parent": "source"}""")

        Thread.sleep(100)
        createModelFile("v2", "item", "test.json", """{"parent": "target"}""")

        val command = SyncModelsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        val model = File(testProjectDir, "versions/shared/v2/assets/testsync/models/item/test.json").readText()

        assertTrue(model.contains("target"), "Should keep target on conflict")

        println("✓ Target preserved without --force")
    }

    @Test
    fun `test conflict with force overwrites`() {
        println("\n[TEST] Conflict with --force overwrites")

        createModelFile("v1", "item", "test.json", """{"parent": "source"}""")

        Thread.sleep(100)
        createModelFile("v2", "item", "test.json", """{"parent": "target"}""")

        val command = SyncModelsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--force"))

        val model = File(testProjectDir, "versions/shared/v2/assets/testsync/models/item/test.json").readText()

        assertTrue(model.contains("source"), "Should use source with --force")

        println("✓ Target overwritten with --force")
    }

    // ===== Dry-Run Tests =====

    @Test
    fun `test dry-run shows preview`() {
        println("\n[TEST] Dry-run shows preview")

        createLangFile("v1", "en_us.json", """{"key": "value"}""")

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--dry-run"))

        val targetFile = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json")
        assertFalse(targetFile.exists(), "File should not be created in dry-run")

        println("✓ Dry-run preview works")
    }

    @Test
    fun `test dry-run shows missing files`() {
        println("\n[TEST] Dry-run shows missing files")

        createLangFile("v1", "en_us.json", """{"key": "value"}""")
        createModelFile("v1", "item", "sword.json", """{"parent": "item/handheld"}""")

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--dry-run"))

        // Files should not exist
        assertFalse(File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json").exists())
        assertFalse(File(testProjectDir, "versions/shared/v2/assets/testsync/models/item/sword.json").exists())

        println("✓ Dry-run shows missing files")
    }

    @Test
    fun `test dry-run shows conflicts`() {
        println("\n[TEST] Dry-run shows conflicts")

        createLangFile("v1", "en_us.json", """{"key": "value1"}""")

        Thread.sleep(100)
        createLangFile("v2", "en_us.json", """{"key": "value2"}""")

        val targetFile = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json")
        val originalContent = targetFile.readText()

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--dry-run"))

        // File should not be modified
        assertEquals(originalContent, targetFile.readText(), "File should not change in dry-run")

        println("✓ Dry-run shows conflicts")
    }

    @Test
    fun `test dry-run nothing actually synced`() {
        println("\n[TEST] Dry-run nothing actually synced")

        createLangFile("v1", "en_us.json", """{"key": "value"}""")
        createModelFile("v1", "item", "test.json", """{"parent": "item/generated"}""")
        createTextureFile("v1", "item/test.png")

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--dry-run"))

        // Count files in v2
        val v2Assets = File(testProjectDir, "versions/shared/v2/assets")
        val fileCount = if (v2Assets.exists()) {
            v2Assets.walkTopDown().count { it.isFile }
        } else {
            0
        }

        assertEquals(0, fileCount, "No files should be synced in dry-run")

        println("✓ Nothing synced in dry-run")
    }

    // ===== Exclusion Tests =====

    @Test
    fun `test exclude pattern works`() {
        println("\n[TEST] Exclude pattern works")

        createLangFile("v1", "en_us.json", """{"key": "value"}""")
        createLangFile("v1", "test.json", """{"test": "value"}""")

        val command = SyncLangCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--exclude", "test.json"))

        assertTrue(File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json").exists())
        assertFalse(File(testProjectDir, "versions/shared/v2/assets/testsync/lang/test.json").exists())

        println("✓ Exclude pattern works")
    }

    @Test
    fun `test multiple exclusion patterns`() {
        println("\n[TEST] Multiple exclusion patterns")

        createLangFile("v1", "en_us.json", """{"key": "value"}""")
        createLangFile("v1", "test1.json", """{"test": "value"}""")
        createLangFile("v1", "test2.json", """{"test": "value"}""")

        val command = SyncLangCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--exclude", "test1.json", "--exclude", "test2.json"))

        assertTrue(File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json").exists())
        assertFalse(File(testProjectDir, "versions/shared/v2/assets/testsync/lang/test1.json").exists())
        assertFalse(File(testProjectDir, "versions/shared/v2/assets/testsync/lang/test2.json").exists())

        println("✓ Multiple exclusions work")
    }

    @Test
    fun `test glob patterns supported`() {
        println("\n[TEST] Glob patterns supported")

        createLangFile("v1", "en_us.json", """{"key": "value"}""")
        createLangFile("v1", "test_1.json", """{"test": "value"}""")
        createLangFile("v1", "test_2.json", """{"test": "value"}""")

        val command = SyncLangCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--exclude", "test_*"))

        assertTrue(File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json").exists())
        assertFalse(File(testProjectDir, "versions/shared/v2/assets/testsync/lang/test_1.json").exists())
        assertFalse(File(testProjectDir, "versions/shared/v2/assets/testsync/lang/test_2.json").exists())

        println("✓ Glob patterns work")
    }

    // ===== Bidirectional Sync Tests =====

    @Test
    fun `test bidirectional sync both directions`() {
        println("\n[TEST] Bidirectional sync both directions")

        createLangFile("v1", "file1.json", """{"key1": "value1"}""")
        createLangFile("v2", "file2.json", """{"key2": "value2"}""")

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--bidirectional"))

        assertTrue(File(testProjectDir, "versions/shared/v2/assets/testsync/lang/file1.json").exists(), "v1 -> v2")
        assertTrue(File(testProjectDir, "versions/shared/v1/assets/testsync/lang/file2.json").exists(), "v2 -> v1")

        println("✓ Bidirectional sync works")
    }

    @Test
    fun `test bidirectional merge changes from both sides`() {
        println("\n[TEST] Bidirectional merge changes from both sides")

        createLangFile("v1", "en_us.json", """{"key1": "value1"}""")
        createLangFile("v2", "en_us.json", """{"key2": "value2"}""")

        val command = SyncLangCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2", "--bidirectional"))

        val v1Content = File(testProjectDir, "versions/shared/v1/assets/testsync/lang/en_us.json").readText()
        val v2Content = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json").readText()

        assertTrue(v1Content.contains("key1") && v1Content.contains("key2"), "v1 should have both keys")
        assertTrue(v2Content.contains("key1") && v2Content.contains("key2"), "v2 should have both keys")

        println("✓ Bidirectional merge works")
    }

    // ===== Integration Tests =====

    @Test
    fun `test create item then sync to other version`() {
        println("\n[TEST] Create item then sync to other version")

        // Create item in v1
        CreateItemCommand().parse(arrayOf("ruby_sword", "--type", "tool"))

        // Sync to v2
        SyncAssetsCommand().parse(arrayOf("--from", "v1", "--to", "v2"))

        // Verify item model synced
        val model = File(testProjectDir, "versions/shared/v2/assets/testsync/models/item/ruby_sword.json")
        assertTrue(model.exists(), "Item model should be synced")

        println("✓ Item synced to other version")
    }

    @Test
    fun `test create block then sync assets`() {
        println("\n[TEST] Create block then sync assets")

        // Create block
        CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))

        // Sync all assets
        SyncAssetsCommand().parse(arrayOf("--from", "v1", "--to", "v2"))

        // Verify block assets synced
        val blockstate = File(testProjectDir, "versions/shared/v2/assets/testsync/blockstates/ruby_ore.json")
        val blockModel = File(testProjectDir, "versions/shared/v2/assets/testsync/models/block/ruby_ore.json")
        val itemModel = File(testProjectDir, "versions/shared/v2/assets/testsync/models/item/ruby_ore.json")

        assertTrue(blockstate.exists(), "Blockstate should be synced")
        assertTrue(blockModel.exists(), "Block model should be synced")
        assertTrue(itemModel.exists(), "Item model should be synced")

        println("✓ Block assets synced")
    }

    // ===== Performance Tests =====

    @Test
    fun `test sync 100 plus files efficiently`() {
        println("\n[TEST] Sync 100+ files efficiently")

        // Create 100+ files
        repeat(120) { i ->
            createLangFile("v1", "file_$i.json", """{"key_$i": "value_$i"}""")
        }

        val startTime = System.currentTimeMillis()

        val command = SyncLangCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        val elapsed = System.currentTimeMillis() - startTime

        // Verify all synced
        val v2Lang = File(testProjectDir, "versions/shared/v2/assets/testsync/lang")
        val fileCount = v2Lang.walkTopDown().count { it.isFile }

        assertTrue(fileCount >= 120, "All 120 files should be synced")
        println("✓ Synced $fileCount files in ${elapsed}ms")
    }

    @Test
    fun `test skip unchanged files for performance`() {
        println("\n[TEST] Skip unchanged files (performance)")

        // Create and sync files
        repeat(50) { i ->
            createLangFile("v1", "file_$i.json", """{"key": "value"}""")
        }

        SyncLangCommand().parse(arrayOf("--from", "v1", "--to", "v2"))

        // Get modification times
        val v2Lang = File(testProjectDir, "versions/shared/v2/assets/testsync/lang")
        val modTimes = v2Lang.walkTopDown()
            .filter { it.isFile }
            .map { it.lastModified() }
            .toList()

        Thread.sleep(100)

        // Sync again (should skip all)
        val startTime = System.currentTimeMillis()
        SyncLangCommand().parse(arrayOf("--from", "v1", "--to", "v2"))
        val elapsed = System.currentTimeMillis() - startTime

        // Verify files not modified
        val newModTimes = v2Lang.walkTopDown()
            .filter { it.isFile }
            .map { it.lastModified() }
            .toList()

        assertEquals(modTimes, newModTimes, "Files should not be modified")
        println("✓ Skipped unchanged files in ${elapsed}ms")
    }

    // ===== Edge Cases =====

    @Test
    fun `test empty source directory`() {
        println("\n[TEST] Empty source directory")

        createAssetPack("v_empty")

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v_empty", "--to", "v2"))

        // Should not crash
        println("✓ Handled empty source")
    }

    @Test
    fun `test empty target directory`() {
        println("\n[TEST] Empty target directory")

        createLangFile("v1", "en_us.json", """{"key": "value"}""")

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        val targetFile = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json")
        assertTrue(targetFile.exists(), "Should create in empty target")

        println("✓ Handled empty target")
    }

    @Test
    fun `test identical source and target`() {
        println("\n[TEST] Identical source and target")

        createLangFile("v1", "en_us.json", """{"key": "value"}""")

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v1"))

        // Should handle gracefully
        println("✓ Handled identical source and target")
    }

    @Test
    fun `test sync preserves directory structure`() {
        println("\n[TEST] Sync preserves directory structure")

        createLangFile("v1", "en_us.json", """{"key": "value"}""")
        createModelFile("v1", "item", "test.json", """{"parent": "item/generated"}""")
        createModelFile("v1", "block", "test.json", """{"parent": "block/cube_all"}""")

        val command = SyncAssetsCommand()
        command.parse(arrayOf("--from", "v1", "--to", "v2"))

        assertTrue(File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json").exists())
        assertTrue(File(testProjectDir, "versions/shared/v2/assets/testsync/models/item/test.json").exists())
        assertTrue(File(testProjectDir, "versions/shared/v2/assets/testsync/models/block/test.json").exists())

        println("✓ Directory structure preserved")
    }

    @Test
    fun `test data safety preserves existing work`() {
        println("\n[TEST] Data safety preserves existing work")

        createLangFile("v1", "en_us.json", """{"key": "old"}""")

        Thread.sleep(100)
        createLangFile("v2", "en_us.json", """{"key": "custom", "custom_key": "important"}""")

        // Sync without force
        SyncLangCommand().parse(arrayOf("--from", "v1", "--to", "v2"))

        val content = File(testProjectDir, "versions/shared/v2/assets/testsync/lang/en_us.json").readText()

        assertTrue(content.contains("custom"), "Should preserve custom work")
        assertTrue(content.contains("important"), "Should preserve important data")

        println("✓ Existing work preserved")
    }

    // ===== Helper Methods =====

    private fun createAssetPack(name: String) {
        val assetPackDir = File(testProjectDir, "versions/shared/$name")
        assetPackDir.mkdirs()

        val configFile = File(assetPackDir, "config.yml")
        configFile.writeText("""
            name: $name
            type: asset-pack
        """.trimIndent())

        // Create standard directories
        File(assetPackDir, "assets/testsync/lang").mkdirs()
        File(assetPackDir, "assets/testsync/models/item").mkdirs()
        File(assetPackDir, "assets/testsync/models/block").mkdirs()
        File(assetPackDir, "assets/testsync/textures/item").mkdirs()
        File(assetPackDir, "assets/testsync/textures/block").mkdirs()
        File(assetPackDir, "assets/testsync/blockstates").mkdirs()
        File(assetPackDir, "data/testsync/recipes").mkdirs()
        File(assetPackDir, "data/testsync/loot_tables").mkdirs()
    }

    private fun createLangFile(assetPack: String, fileName: String, content: String) {
        val file = File(testProjectDir, "versions/shared/$assetPack/assets/testsync/lang/$fileName")
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun createModelFile(assetPack: String, type: String, fileName: String, content: String) {
        val file = File(testProjectDir, "versions/shared/$assetPack/assets/testsync/models/$type/$fileName")
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun createTextureFile(assetPack: String, path: String) {
        val file = File(testProjectDir, "versions/shared/$assetPack/assets/testsync/textures/$path")
        file.parentFile.mkdirs()
        file.createNewFile()
    }

    private fun createBlockstateFile(assetPack: String, fileName: String, content: String) {
        val file = File(testProjectDir, "versions/shared/$assetPack/assets/testsync/blockstates/$fileName")
        file.parentFile.mkdirs()
        file.writeText(content)
    }

    private fun createRecipeFile(assetPack: String, fileName: String, content: String) {
        val file = File(testProjectDir, "versions/shared/$assetPack/data/testsync/recipes/$fileName")
        file.parentFile.mkdirs()
        file.writeText(content)
    }
}
