package dev.dropper.integration

import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.CreateRecipeCommand
import dev.dropper.commands.CreateTagCommand
import dev.dropper.commands.remove.*
import dev.dropper.config.ModConfig
import dev.dropper.removers.*
import dev.dropper.util.FileUtil
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive E2E tests for Remove command
 * Tests ALL removal scenarios with extensive coverage
 */
class RemoveCommandE2ETest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("test-remove")

        // Generate a minimal project
        val config = ModConfig(
            id = "testremove",
            name = "Test Remove Mod",
            version = "1.0.0",
            description = "Test mod for remove commands",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        context.createProject(config)
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    // ========== BASIC REMOVAL TESTS ==========

    @Test
    fun `remove item deletes all related files`() {
        println("\n[TEST] Remove item - basic functionality")

        // Create an item first
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby_gem"))
        }

        // Verify item was created
        val itemFile = context.file("shared/common/src/main/java/com/testremove/items/RubyGem.java")
        assertTrue(itemFile.exists(), "Item should exist before removal")

        // Remove the item with force flag
        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "ruby_gem", "testremove", RemovalOptions(force = true))

        // Verify removal was successful
        assertTrue(result.success, "Removal should succeed")
        assertFalse(itemFile.exists(), "Item file should be deleted")

        // Verify all related files are removed
        assertFalse(context.file( "shared/fabric/src/main/java/com/testremove/platform/fabric/RubyGemFabric.java").exists())
        assertFalse(context.file( "shared/forge/src/main/java/com/testremove/platform/forge/RubyGemForge.java").exists())
        assertFalse(context.file( "shared/neoforge/src/main/java/com/testremove/platform/neoforge/RubyGemNeoForge.java").exists())
        assertFalse(context.file( "versions/shared/v1/assets/testremove/models/item/ruby_gem.json").exists())
        assertFalse(context.file( "versions/shared/v1/assets/testremove/textures/item/ruby_gem.png").exists())

        println("✓ Item removed successfully")
    }

    @Test
    fun `remove block deletes all related files including variants`() {
        println("\n[TEST] Remove block - with all variants")

        // Create a slab block (has multiple model files)
        context.withProjectDir {
            CreateBlockCommand().parse(arrayOf("ruby_slab", "--type", "slab"))
        }

        // Verify block and all variants exist
        val blockFile = context.file( "shared/common/src/main/java/com/testremove/blocks/RubySlab.java")
        assertTrue(blockFile.exists())

        // Remove with force
        val remover = BlockRemover()
        val result = remover.remove(context.projectDir, "ruby_slab", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)
        assertFalse(blockFile.exists())

        // Verify blockstate removed
        assertFalse(context.file( "versions/shared/v1/assets/testremove/blockstates/ruby_slab.json").exists())

        // Verify all slab variant models removed
        assertFalse(context.file( "versions/shared/v1/assets/testremove/models/block/ruby_slab.json").exists())
        assertFalse(context.file( "versions/shared/v1/assets/testremove/models/block/ruby_slab_top.json").exists())
        assertFalse(context.file( "versions/shared/v1/assets/testremove/models/block/ruby_slab_double.json").exists())

        // Verify item model removed
        assertFalse(context.file( "versions/shared/v1/assets/testremove/models/item/ruby_slab.json").exists())

        // Verify loot table removed
        assertFalse(context.file( "versions/shared/v1/data/testremove/loot_table/blocks/ruby_slab.json").exists())

        println("✓ Block and all variants removed successfully")
    }

    @Test
    fun `remove recipe deletes recipe file`() {
        println("\n[TEST] Remove recipe")

        // Create item with recipe
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("diamond_sword"))
        }

        val recipeFile = context.file( "versions/shared/v1/data/testremove/recipe/diamond_sword.json")
        assertTrue(recipeFile.exists())

        // Remove just the recipe
        val remover = RecipeRemover()
        val result = remover.remove(context.projectDir, "diamond_sword", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)
        assertFalse(recipeFile.exists())

        println("✓ Recipe removed successfully")
    }

    @Test
    fun `remove tag deletes tag file`() {
        println("\n[TEST] Remove tag")

        // Create a tag file manually
        val tagFile = context.file( "versions/shared/v1/data/testremove/tags/items/test_tag.json")
        FileUtil.writeText(tagFile, """{"values": []}""")

        assertTrue(tagFile.exists())

        // Remove tag
        val remover = TagRemover()
        val result = remover.remove(context.projectDir, "test_tag", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)
        assertFalse(tagFile.exists())

        println("✓ Tag removed successfully")
    }

    @Test
    fun `remove entity deletes all entity files`() {
        println("\n[TEST] Remove entity")

        // Create entity files manually (since we don't have CreateEntityCommand in this test)
        val entityFile = context.file( "shared/common/src/main/java/com/testremove/entities/TestEntity.java")
        FileUtil.writeText(entityFile, "public class TestEntity {}")

        val fabricFile = context.file( "shared/fabric/src/main/java/com/testremove/platform/fabric/TestEntityFabric.java")
        FileUtil.writeText(fabricFile, "public class TestEntityFabric {}")

        assertTrue(entityFile.exists())

        // Remove entity
        val remover = EntityRemover()
        val result = remover.remove(context.projectDir, "test_entity", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)
        assertFalse(entityFile.exists())
        assertFalse(fabricFile.exists())

        println("✓ Entity removed successfully")
    }

    @Test
    fun `remove enchantment deletes all enchantment files`() {
        println("\n[TEST] Remove enchantment")

        // Create enchantment files manually
        val enchFile = context.file( "shared/common/src/main/java/com/testremove/enchantments/TestEnchantment.java")
        FileUtil.writeText(enchFile, "public class TestEnchantment {}")

        assertTrue(enchFile.exists())

        // Remove enchantment
        val remover = EnchantmentRemover()
        val result = remover.remove(context.projectDir, "test_enchantment", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)
        assertFalse(enchFile.exists())

        println("✓ Enchantment removed successfully")
    }

    @Test
    fun `remove biome deletes biome file`() {
        println("\n[TEST] Remove biome")

        // Create biome file manually
        val biomeFile = context.file( "versions/shared/v1/data/testremove/worldgen/biome/test_biome.json")
        FileUtil.writeText(biomeFile, """{"effects": {}}""")

        assertTrue(biomeFile.exists())

        // Remove biome
        val remover = BiomeRemover()
        val result = remover.remove(context.projectDir, "test_biome", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)
        assertFalse(biomeFile.exists())

        println("✓ Biome removed successfully")
    }

    // ========== DRY-RUN TESTS ==========

    @Test
    fun `dry-run shows files to delete without deleting`() {
        println("\n[TEST] Dry-run mode")

        // Create item
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("test_item"))
        }

        val itemFile = context.file( "shared/common/src/main/java/com/testremove/items/TestItem.java")
        assertTrue(itemFile.exists())

        // Dry-run removal
        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "test_item", "testremove", RemovalOptions(dryRun = true))

        // Should succeed but not actually delete
        assertTrue(result.success)
        assertTrue(itemFile.exists(), "File should still exist after dry-run")
        assertTrue(result.filesRemoved.isNotEmpty(), "Should report files that would be removed")

        println("✓ Dry-run completed without deleting files")
    }

    @Test
    fun `dry-run shows accurate file count`() {
        println("\n[TEST] Dry-run accurate preview")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("preview_item"))
        }

        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "preview_item", "testremove", RemovalOptions(dryRun = true))

        // Should list all files that would be removed
        assertTrue(result.filesRemoved.size >= 4, "Should preview at least 4 files (common + 3 loaders)")

        println("✓ Dry-run shows ${result.filesRemoved.size} files would be removed")
    }

    // ========== DEPENDENCY DETECTION TESTS ==========

    @Test
    fun `removal blocked when recipe uses item`() {
        println("\n[TEST] Dependency detection - recipe uses item")

        // Create item
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("used_item"))
        }

        // Create another recipe that uses this item
        val recipeFile = context.file( "versions/shared/v1/data/testremove/recipe/consumer_recipe.json")
        FileUtil.writeText(recipeFile, """
            {
              "type": "minecraft:crafting_shaped",
              "pattern": ["#"],
              "key": {
                "#": {"item": "testremove:used_item"}
              },
              "result": {"id": "minecraft:diamond"}
            }
        """.trimIndent())

        // Try to remove without force
        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "used_item", "testremove", RemovalOptions(force = false))

        // Should fail due to dependency
        assertFalse(result.success, "Should fail when dependencies exist")
        assertTrue(result.errors.isNotEmpty())
        assertTrue(result.warnings.any { it.contains("referenced") })

        println("✓ Removal blocked due to recipe dependency")
    }

    @Test
    fun `removal proceeds with force when dependencies exist`() {
        println("\n[TEST] Force removal ignores dependencies")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("force_item"))
        }

        // Create recipe that uses it
        val recipeFile = context.file( "versions/shared/v1/data/testremove/recipe/using_force_item.json")
        FileUtil.writeText(recipeFile, """
            {
              "type": "minecraft:crafting_shapeless",
              "ingredients": [{"item": "testremove:force_item"}],
              "result": {"id": "minecraft:gold_ingot"}
            }
        """.trimIndent())

        // Remove with force
        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "force_item", "testremove", RemovalOptions(force = true))

        // Should succeed despite dependency
        assertTrue(result.success)

        println("✓ Force removal succeeded despite dependencies")
    }

    @Test
    fun `warn when tag references block`() {
        println("\n[TEST] Tag dependency detection")

        context.withProjectDir {
            CreateBlockCommand().parse(arrayOf("tagged_block"))
        }

        // Create tag that references the block
        val tagFile = context.file( "versions/shared/v1/data/testremove/tags/blocks/stone_blocks.json")
        FileUtil.writeText(tagFile, """
            {
              "values": ["testremove:tagged_block", "minecraft:stone"]
            }
        """.trimIndent())

        // Try to remove without force
        val remover = BlockRemover()
        val result = remover.remove(context.projectDir, "tagged_block", "testremove", RemovalOptions(force = false))

        // Should fail
        assertFalse(result.success)
        assertTrue(result.warnings.any { it.contains("referenced") || it.contains("tag") })

        println("✓ Tag dependency detected")
    }

    @Test
    fun `detect loot table dependencies`() {
        println("\n[TEST] Loot table dependency detection")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("loot_item"))
        }

        // Create loot table that references the item
        val lootFile = context.file( "versions/shared/v1/data/testremove/loot_table/chests/custom_chest.json")
        FileUtil.writeText(lootFile, """
            {
              "type": "minecraft:chest",
              "pools": [{
                "rolls": 1,
                "entries": [{
                  "type": "minecraft:item",
                  "name": "testremove:loot_item"
                }]
              }]
            }
        """.trimIndent())

        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "loot_item", "testremove", RemovalOptions(force = false))

        assertFalse(result.success)

        println("✓ Loot table dependency detected")
    }

    // ========== PARTIAL REMOVAL TESTS ==========

    @Test
    fun `keep-assets removes code but preserves textures`() {
        println("\n[TEST] Keep assets option")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("asset_item"))
        }

        val itemFile = context.file( "shared/common/src/main/java/com/testremove/items/AssetItem.java")
        val textureFile = context.file( "versions/shared/v1/assets/testremove/textures/item/asset_item.png")

        assertTrue(itemFile.exists())
        assertTrue(textureFile.exists())

        // Remove with keep-assets
        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "asset_item", "testremove", RemovalOptions(force = true, keepAssets = true))

        assertTrue(result.success)
        assertFalse(itemFile.exists(), "Code should be removed")
        assertTrue(textureFile.exists(), "Texture should be preserved")

        println("✓ Code removed, assets preserved")
    }

    // ========== CLEANUP TESTS ==========

    @Test
    fun `empty directories are removed after deletion`() {
        println("\n[TEST] Empty directory cleanup")

        // Create item in unique package
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("cleanup_item"))
        }

        val itemFile = context.file( "shared/common/src/main/java/com/testremove/items/CleanupItem.java")
        val itemsDir = itemFile.parentFile

        assertTrue(itemsDir.exists())

        // Remove item
        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "cleanup_item", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)

        // Check if empty items directory was cleaned
        // Note: Directory might not be empty if other items exist, so we check removedDirs list
        assertTrue(result.directoriesRemoved.isNotEmpty() || itemsDir.listFiles()?.isNotEmpty() == true)

        println("✓ Empty directories cleaned: ${result.directoriesRemoved.size}")
    }

    // ========== ERROR HANDLING TESTS ==========

    @Test
    fun `removing non-existent component fails gracefully`() {
        println("\n[TEST] Non-existent component")

        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "nonexistent_item", "testremove", RemovalOptions(force = true))

        assertFalse(result.success)
        assertTrue(result.errors.any { it.contains("not found") })

        println("✓ Graceful failure for non-existent component")
    }

    @Test
    fun `removing already deleted component fails`() {
        println("\n[TEST] Already deleted component")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("double_delete"))
        }

        val remover = ItemRemover()

        // First deletion
        val result1 = remover.remove(context.projectDir, "double_delete", "testremove", RemovalOptions(force = true))
        assertTrue(result1.success)

        // Second deletion (already removed)
        val result2 = remover.remove(context.projectDir, "double_delete", "testremove", RemovalOptions(force = true))
        assertFalse(result2.success)
        assertTrue(result2.errors.any { it.contains("not found") })

        println("✓ Second deletion correctly fails")
    }

    // ========== MULTI-FILE TESTS ==========

    @Test
    fun `remove block with crop stages deletes all stage models`() {
        println("\n[TEST] Remove crop with multiple stages")

        context.withProjectDir {
            CreateBlockCommand().parse(arrayOf("test_crop", "--type", "crop", "--max-age", "7"))
        }

        // Verify all 8 stage models exist (0-7)
        for (age in 0..7) {
            val stageModel = context.file( "versions/shared/v1/assets/testremove/models/block/test_crop_stage$age.json")
            assertTrue(stageModel.exists(), "Stage $age model should exist")
        }

        // Remove crop
        val remover = BlockRemover()
        val result = remover.remove(context.projectDir, "test_crop", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)

        // Verify all stage models removed
        for (age in 0..7) {
            val stageModel = context.file( "versions/shared/v1/assets/testremove/models/block/test_crop_stage$age.json")
            assertFalse(stageModel.exists(), "Stage $age model should be removed")
        }

        println("✓ All 8 crop stages removed")
    }

    @Test
    fun `remove stairs block deletes all variant models`() {
        println("\n[TEST] Remove stairs with variants")

        context.withProjectDir {
            CreateBlockCommand().parse(arrayOf("test_stairs", "--type", "stairs"))
        }

        // Remove stairs
        val remover = BlockRemover()
        val result = remover.remove(context.projectDir, "test_stairs", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)
        assertFalse(context.file( "shared/common/src/main/java/com/testremove/blocks/TestStairs.java").exists())
        assertFalse(context.file( "versions/shared/v1/assets/testremove/blockstates/test_stairs.json").exists())

        println("✓ Stairs block removed")
    }

    // ========== INTEGRATION TESTS ==========

    @Test
    fun `create then remove item workflow`() {
        println("\n[TEST] Create-Remove workflow")

        // Create
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("workflow_item"))
        }

        val itemFile = context.file( "shared/common/src/main/java/com/testremove/items/WorkflowItem.java")
        assertTrue(itemFile.exists())

        // Remove
        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "workflow_item", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)
        assertFalse(itemFile.exists())

        println("✓ Create-Remove workflow successful")
    }

    @Test
    fun `create multiple items then remove one`() {
        println("\n[TEST] Multiple items - remove one")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("item_a"))
        }
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("item_b"))
        }
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("item_c"))
        }

        // Remove only item_b
        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "item_b", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)

        // Verify item_a and item_c still exist
        assertTrue(context.file( "shared/common/src/main/java/com/testremove/items/ItemA.java").exists())
        assertFalse(context.file( "shared/common/src/main/java/com/testremove/items/ItemB.java").exists())
        assertTrue(context.file( "shared/common/src/main/java/com/testremove/items/ItemC.java").exists())

        println("✓ Removed one item, others preserved")
    }

    @Test
    fun `backup is created before deletion`() {
        println("\n[TEST] Backup creation")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("backup_item"))
        }

        val backupDir = context.file( ".dropper/backups")

        // Remove with backup
        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "backup_item", "testremove", RemovalOptions(force = true, createBackup = true))

        assertTrue(result.success)

        // Check backup was created
        assertTrue(backupDir.exists())
        val backups = backupDir.listFiles()?.filter { it.name.contains("backup_item") }
        assertTrue(backups?.isNotEmpty() == true, "Backup should be created")

        println("✓ Backup created successfully")
    }

    @Test
    fun `remove item without recipe works`() {
        println("\n[TEST] Remove item created without recipe")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("no_recipe_item", "--recipe", "false"))
        }

        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "no_recipe_item", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)

        println("✓ Item without recipe removed successfully")
    }

    @Test
    fun `remove block without loot table works`() {
        println("\n[TEST] Remove block created without loot table")

        context.withProjectDir {
            CreateBlockCommand().parse(arrayOf("no_loot_block", "--drops-self", "false"))
        }

        val remover = BlockRemover()
        val result = remover.remove(context.projectDir, "no_loot_block", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)

        println("✓ Block without loot table removed successfully")
    }

    @Test
    fun `file count in result is accurate`() {
        println("\n[TEST] Accurate file count")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("count_item"))
        }

        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "count_item", "testremove", RemovalOptions(force = true))

        assertTrue(result.success)
        assertTrue(result.filesRemoved.size >= 4, "Should remove at least 4 files")

        println("✓ Removed ${result.filesRemoved.size} files")
    }

    @Test
    fun `removal result contains warnings for dependencies`() {
        println("\n[TEST] Warning messages")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("warn_item"))
        }

        // Create dependency
        val recipeFile = context.file( "versions/shared/v1/data/testremove/recipe/uses_warn_item.json")
        FileUtil.writeText(recipeFile, """
            {
              "type": "minecraft:crafting_shapeless",
              "ingredients": [{"item": "testremove:warn_item"}],
              "result": {"id": "minecraft:iron_ingot"}
            }
        """.trimIndent())

        val remover = ItemRemover()
        val result = remover.remove(context.projectDir, "warn_item", "testremove", RemovalOptions(force = false))

        assertFalse(result.success)
        assertTrue(result.warnings.isNotEmpty())

        println("✓ Warnings provided: ${result.warnings.size}")
    }

    // ========== STRESS TESTS ==========

    @Test
    fun `remove many items in sequence`() {
        println("\n[TEST] Remove many items sequentially")

        // Create 10 items
        for (i in 1..10) {
            context.withProjectDir {
                CreateItemCommand().parse(arrayOf("bulk_item_$i"))
            }
        }

        // Remove all
        val remover = ItemRemover()
        var successCount = 0
        for (i in 1..10) {
            val result = remover.remove(context.projectDir, "bulk_item_$i", "testremove", RemovalOptions(force = true))
            if (result.success) successCount++
        }

        assertEquals(10, successCount, "All 10 items should be removed")

        println("✓ Removed $successCount items successfully")
    }

    @Test
    fun `remove different component types in same project`() {
        println("\n[TEST] Mixed component removal")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("mixed_item"))
        }
        context.withProjectDir {
            CreateBlockCommand().parse(arrayOf("mixed_block"))
        }

        val itemRemover = ItemRemover()
        val blockRemover = BlockRemover()

        val itemResult = itemRemover.remove(context.projectDir, "mixed_item", "testremove", RemovalOptions(force = true))
        val blockResult = blockRemover.remove(context.projectDir, "mixed_block", "testremove", RemovalOptions(force = true))

        assertTrue(itemResult.success)
        assertTrue(blockResult.success)

        println("✓ Mixed component types removed")
    }

    @Test
    fun `dependency analyzer finds all reference types`() {
        println("\n[TEST] Comprehensive dependency detection")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("dep_test_item"))
        }

        // Create multiple dependency types

        // Recipe dependency
        val recipeFile = context.file( "versions/shared/v1/data/testremove/recipe/uses_dep_test.json")
        FileUtil.writeText(recipeFile, """{"ingredients": [{"item": "testremove:dep_test_item"}]}""")

        // Tag dependency
        val tagFile = context.file( "versions/shared/v1/data/testremove/tags/items/test_tags.json")
        FileUtil.writeText(tagFile, """{"values": ["testremove:dep_test_item"]}""")

        // Loot table dependency
        val lootFile = context.file( "versions/shared/v1/data/testremove/loot_table/blocks/test_block.json")
        FileUtil.writeText(lootFile, """{"pools": [{"entries": [{"name": "testremove:dep_test_item"}]}]}""")

        val dependencies = DependencyAnalyzer.findAllDependencies(context.projectDir, "dep_test_item", "testremove")

        assertTrue(dependencies.size >= 3, "Should find at least 3 dependencies")
        assertTrue(dependencies.any { it.type == DependencyType.RECIPE })
        assertTrue(dependencies.any { it.type == DependencyType.TAG })
        assertTrue(dependencies.any { it.type == DependencyType.LOOT_TABLE })

        println("✓ Found ${dependencies.size} dependencies of different types")
    }
}
