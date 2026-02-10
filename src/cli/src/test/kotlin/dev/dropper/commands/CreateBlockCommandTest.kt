package dev.dropper.commands

import dev.dropper.util.FileUtil
import dev.dropper.util.TestProjectContext
import dev.dropper.util.TestValidationUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class CreateBlockCommandTest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("block-test")

        // Create a minimal config.yml for testing
        val configFile = File(context.projectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
              name: Test Mod
              version: 1.0.0
              description: Test mod for block creation
              author: Test Author
              license: MIT
        """.trimIndent())
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    @Test
    fun `test basic block creation`() {
        val blockName = "test_block"

        // Create block
        executeBlockCommand(blockName, "basic")

        // Verify files exist
        assertBlockFilesExist(blockName, listOf(
            "shared/common/src/main/java/com/testmod/blocks/TestBlock.java",
            "shared/common/src/main/java/com/testmod/registry/ModBlocks.java",
            "versions/shared/v1/assets/testmod/blockstates/test_block.json",
            "versions/shared/v1/assets/testmod/models/block/test_block.json",
            "versions/shared/v1/assets/testmod/models/item/test_block.json",
            "versions/shared/v1/assets/testmod/textures/block/test_block.png",
            "versions/shared/v1/data/testmod/loot_tables/blocks/test_block.json"
        ))

        // Verify blockstate content with real JSON validation
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/test_block.json"))
        TestValidationUtils.assertValidJson(blockstate, "test_block blockstate")
        TestValidationUtils.assertJsonHasKeys(blockstate, listOf("variants"), "test_block blockstate")
        assertTrue(blockstate.contains("\"model\": \"testmod:block/test_block\""))

        // Verify model content with real JSON validation
        val model = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/test_block.json"))
        TestValidationUtils.assertValidJson(model, "test_block model")
        TestValidationUtils.assertJsonHasKeys(model, listOf("parent", "textures"), "test_block model")
        assertTrue(model.contains("\"parent\": \"block/cube_all\""))
        assertTrue(model.contains("\"all\": \"testmod:block/test_block\""))

        // Verify item model is valid JSON
        val itemModel = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/item/test_block.json"))
        TestValidationUtils.assertValidJson(itemModel, "test_block item model")
        TestValidationUtils.assertJsonHasKeys(itemModel, listOf("parent"), "test_block item model")

        // Verify loot table is valid JSON
        val lootTable = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/loot_tables/blocks/test_block.json"))
        TestValidationUtils.assertValidJson(lootTable, "test_block loot table")
        TestValidationUtils.assertJsonHasKeys(lootTable, listOf("type", "pools"), "test_block loot table")

        // Verify all Java files have valid syntax
        val javaFiles = listOf(
            "shared/common/src/main/java/com/testmod/blocks/TestBlock.java",
            "shared/common/src/main/java/com/testmod/registry/ModBlocks.java"
        )
        javaFiles.forEach { path ->
            val file = File(context.projectDir, path)
            val content = file.readText()
            TestValidationUtils.assertValidJavaSyntax(content, file.name)
            TestValidationUtils.assertClassNameMatchesFile(content, file.name)
            TestValidationUtils.assertPackageMatchesPath(content, file.absolutePath, context.projectDir.absolutePath)
        }
    }

    @Test
    fun `test ore block creation`() {
        val blockName = "ruby_ore"

        executeBlockCommand(blockName, "ore")

        assertBlockFilesExist(blockName, listOf(
            "versions/shared/v1/assets/testmod/blockstates/ruby_ore.json",
            "versions/shared/v1/assets/testmod/models/block/ruby_ore.json",
            "versions/shared/v1/data/testmod/loot_tables/blocks/ruby_ore.json"
        ))

        // Verify all JSON files are valid
        val blockstateContent = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/ruby_ore.json"))
        TestValidationUtils.assertValidJson(blockstateContent, "ruby_ore blockstate")

        val modelContent = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/ruby_ore.json"))
        TestValidationUtils.assertValidJson(modelContent, "ruby_ore model")

        // Verify loot table has Fortune enchantment and is valid JSON
        val lootTable = FileUtil.readText(File(context.projectDir, "versions/shared/v1/data/testmod/loot_tables/blocks/ruby_ore.json"))
        TestValidationUtils.assertValidJson(lootTable, "ruby_ore loot table")
        TestValidationUtils.assertJsonHasKeys(lootTable, listOf("type", "pools"), "ruby_ore loot table")
        assertTrue(lootTable.contains("minecraft:apply_bonus"))
        assertTrue(lootTable.contains("minecraft:fortune"))
    }

    @Test
    fun `test pillar block creation`() {
        val blockName = "marble_pillar"

        executeBlockCommand(blockName, "pillar")

        assertBlockFilesExist(blockName, listOf(
            "versions/shared/v1/assets/testmod/blockstates/marble_pillar.json",
            "versions/shared/v1/assets/testmod/models/block/marble_pillar.json",
            "versions/shared/v1/assets/testmod/textures/block/marble_pillar.png",
            "versions/shared/v1/assets/testmod/textures/block/marble_pillar_top.png"
        ))

        // Verify blockstate has axis variants and is valid JSON
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/marble_pillar.json"))
        TestValidationUtils.assertValidJson(blockstate, "marble_pillar blockstate")
        TestValidationUtils.assertJsonHasKeys(blockstate, listOf("variants"), "marble_pillar blockstate")
        assertTrue(blockstate.contains("\"axis=x\""))
        assertTrue(blockstate.contains("\"axis=y\""))
        assertTrue(blockstate.contains("\"axis=z\""))

        // Verify model uses cube_column and is valid JSON
        val model = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/marble_pillar.json"))
        TestValidationUtils.assertValidJson(model, "marble_pillar model")
        TestValidationUtils.assertJsonHasKeys(model, listOf("parent", "textures"), "marble_pillar model")
        assertTrue(model.contains("\"parent\": \"block/cube_column\""))
        assertTrue(model.contains("\"end\": \"testmod:block/marble_pillar_top\""))
        assertTrue(model.contains("\"side\": \"testmod:block/marble_pillar\""))
    }

    @Test
    fun `test slab block creation`() {
        val blockName = "stone_slab"

        executeBlockCommand(blockName, "slab")

        assertBlockFilesExist(blockName, listOf(
            "versions/shared/v1/assets/testmod/blockstates/stone_slab.json",
            "versions/shared/v1/assets/testmod/models/block/stone_slab.json",
            "versions/shared/v1/assets/testmod/models/block/stone_slab_top.json",
            "versions/shared/v1/assets/testmod/models/block/stone_slab_double.json"
        ))

        // Verify blockstate has type variants and is valid JSON
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/stone_slab.json"))
        TestValidationUtils.assertValidJson(blockstate, "stone_slab blockstate")
        assertTrue(blockstate.contains("\"type=bottom\""))
        assertTrue(blockstate.contains("\"type=top\""))
        assertTrue(blockstate.contains("\"type=double\""))

        // Verify models are all valid JSON
        val model = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/stone_slab.json"))
        TestValidationUtils.assertValidJson(model, "stone_slab model")
        assertTrue(model.contains("\"parent\": \"block/slab\""))

        val topModel = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/stone_slab_top.json"))
        TestValidationUtils.assertValidJson(topModel, "stone_slab_top model")
        assertTrue(topModel.contains("\"parent\": \"block/slab_top\""))

        val doubleModel = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/stone_slab_double.json"))
        TestValidationUtils.assertValidJson(doubleModel, "stone_slab_double model")
        assertTrue(doubleModel.contains("\"parent\": \"block/cube_all\""))
    }

    @Test
    fun `test stairs block creation`() {
        val blockName = "stone_stairs"

        executeBlockCommand(blockName, "stairs")

        assertBlockFilesExist(blockName, listOf(
            "versions/shared/v1/assets/testmod/blockstates/stone_stairs.json",
            "versions/shared/v1/assets/testmod/models/block/stone_stairs.json"
        ))

        // Verify blockstate has facing and half variants
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/stone_stairs.json"))
        assertTrue(blockstate.contains("facing="))
        assertTrue(blockstate.contains("half="))
        assertTrue(blockstate.contains("shape="))

        // Verify model
        val model = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/stone_stairs.json"))
        assertTrue(model.contains("\"parent\": \"block/stairs\""))
    }

    @Test
    fun `test fence block creation`() {
        val blockName = "oak_fence"

        executeBlockCommand(blockName, "fence")

        assertBlockFilesExist(blockName, listOf(
            "versions/shared/v1/assets/testmod/blockstates/oak_fence.json",
            "versions/shared/v1/assets/testmod/models/block/oak_fence_post.json",
            "versions/shared/v1/assets/testmod/models/block/oak_fence_side.json"
        ))

        // Verify blockstate uses multipart
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/oak_fence.json"))
        assertTrue(blockstate.contains("\"multipart\""))
        assertTrue(blockstate.contains("oak_fence_post"))
        assertTrue(blockstate.contains("oak_fence_side"))

        // Verify models
        val postModel = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/oak_fence_post.json"))
        assertTrue(postModel.contains("\"parent\": \"block/fence_post\""))

        val sideModel = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/oak_fence_side.json"))
        assertTrue(sideModel.contains("\"parent\": \"block/fence_side\""))
    }

    @Test
    fun `test wall block creation`() {
        val blockName = "cobblestone_wall"

        executeBlockCommand(blockName, "wall")

        assertBlockFilesExist(blockName, listOf(
            "versions/shared/v1/assets/testmod/blockstates/cobblestone_wall.json",
            "versions/shared/v1/assets/testmod/models/block/cobblestone_wall_post.json",
            "versions/shared/v1/assets/testmod/models/block/cobblestone_wall_side.json"
        ))

        // Verify blockstate uses multipart
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/cobblestone_wall.json"))
        assertTrue(blockstate.contains("\"multipart\""))
        assertTrue(blockstate.contains("cobblestone_wall_post"))
        assertTrue(blockstate.contains("cobblestone_wall_side"))

        // Verify models
        val postModel = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/cobblestone_wall_post.json"))
        assertTrue(postModel.contains("\"parent\": \"block/template_wall_post\""))
    }

    @Test
    fun `test door block creation`() {
        val blockName = "oak_door"

        executeBlockCommand(blockName, "door")

        assertBlockFilesExist(blockName, listOf(
            "versions/shared/v1/assets/testmod/blockstates/oak_door.json",
            "versions/shared/v1/assets/testmod/textures/block/oak_door_bottom.png",
            "versions/shared/v1/assets/testmod/textures/block/oak_door_top.png"
        ))

        // Verify blockstate has door variants
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/oak_door.json"))
        assertTrue(blockstate.contains("half=lower"))
        assertTrue(blockstate.contains("half=upper"))
        assertTrue(blockstate.contains("hinge="))
        assertTrue(blockstate.contains("open="))
    }

    @Test
    fun `test trapdoor block creation`() {
        val blockName = "oak_trapdoor"

        executeBlockCommand(blockName, "trapdoor")

        assertBlockFilesExist(blockName, listOf(
            "versions/shared/v1/assets/testmod/blockstates/oak_trapdoor.json",
            "versions/shared/v1/assets/testmod/models/block/oak_trapdoor_bottom.json",
            "versions/shared/v1/assets/testmod/models/block/oak_trapdoor_top.json",
            "versions/shared/v1/assets/testmod/models/block/oak_trapdoor_open.json"
        ))

        // Verify blockstate
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/oak_trapdoor.json"))
        assertTrue(blockstate.contains("half="))
        assertTrue(blockstate.contains("open="))

        // Verify models
        val bottomModel = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/oak_trapdoor_bottom.json"))
        assertTrue(bottomModel.contains("template_orientable_trapdoor_bottom"))

        val openModel = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/oak_trapdoor_open.json"))
        assertTrue(openModel.contains("template_orientable_trapdoor_open"))
    }

    @Test
    fun `test button block creation`() {
        val blockName = "stone_button"

        executeBlockCommand(blockName, "button")

        assertBlockFilesExist(blockName, listOf(
            "versions/shared/v1/assets/testmod/blockstates/stone_button.json",
            "versions/shared/v1/assets/testmod/models/block/stone_button.json",
            "versions/shared/v1/assets/testmod/models/block/stone_button_pressed.json"
        ))

        // Verify blockstate
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/stone_button.json"))
        assertTrue(blockstate.contains("powered="))
        assertTrue(blockstate.contains("face="))

        // Verify models
        val model = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/stone_button.json"))
        assertTrue(model.contains("\"parent\": \"block/button\""))

        val pressedModel = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/stone_button_pressed.json"))
        assertTrue(pressedModel.contains("\"parent\": \"block/button_pressed\""))
    }

    @Test
    fun `test pressure plate block creation`() {
        val blockName = "stone_pressure_plate"

        executeBlockCommand(blockName, "pressure_plate")

        assertBlockFilesExist(blockName, listOf(
            "versions/shared/v1/assets/testmod/blockstates/stone_pressure_plate.json",
            "versions/shared/v1/assets/testmod/models/block/stone_pressure_plate.json",
            "versions/shared/v1/assets/testmod/models/block/stone_pressure_plate_down.json"
        ))

        // Verify blockstate
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/stone_pressure_plate.json"))
        assertTrue(blockstate.contains("powered=false"))
        assertTrue(blockstate.contains("powered=true"))

        // Verify models
        val model = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/stone_pressure_plate.json"))
        assertTrue(model.contains("\"parent\": \"block/pressure_plate_up\""))

        val downModel = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/stone_pressure_plate_down.json"))
        assertTrue(downModel.contains("\"parent\": \"block/pressure_plate_down\""))
    }

    @Test
    fun `test crop block creation`() {
        val blockName = "wheat"

        executeBlockCommand(blockName, "crop", mapOf("--max-age" to "7"))

        // Verify all crop stages exist
        for (age in 0..7) {
            assertBlockFilesExist(blockName, listOf(
                "versions/shared/v1/assets/testmod/models/block/wheat_stage$age.json",
                "versions/shared/v1/assets/testmod/textures/block/wheat_stage$age.png"
            ))
        }

        // Verify blockstate has all age variants
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/wheat.json"))
        for (age in 0..7) {
            assertTrue(blockstate.contains("\"age=$age\""))
        }

        // Verify model
        val model = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/wheat_stage0.json"))
        assertTrue(model.contains("\"parent\": \"block/crop\""))
        assertTrue(model.contains("\"crop\": \"testmod:block/wheat_stage0\""))
    }

    @Test
    fun `test crop block with custom max age`() {
        val blockName = "custom_crop"

        executeBlockCommand(blockName, "crop", mapOf("--max-age" to "3"))

        // Verify only stages 0-3 exist
        for (age in 0..3) {
            assertTrue(File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/custom_crop_stage$age.json").exists())
        }

        // Verify stage 4 doesn't exist
        assertTrue(!File(context.projectDir, "versions/shared/v1/assets/testmod/models/block/custom_crop_stage4.json").exists())

        // Verify blockstate
        val blockstate = FileUtil.readText(File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/custom_crop.json"))
        assertTrue(blockstate.contains("\"age=0\""))
        assertTrue(blockstate.contains("\"age=3\""))
        assertTrue(!blockstate.contains("\"age=4\""))
    }

    @Test
    fun `test block without drops-self`() {
        val blockName = "grass_block"

        executeBlockCommand(blockName, "basic", mapOf("--drops-self" to "false"))

        // Verify loot table doesn't exist
        val lootTablePath = "versions/shared/v1/data/testmod/loot_tables/blocks/grass_block.json"
        assertTrue(!File(context.projectDir, lootTablePath).exists())
    }

    @Test
    fun `test all loader registrations created`() {
        val blockName = "test_block"

        executeBlockCommand(blockName, "basic")

        // Check registry file with Java syntax validation
        val registryFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/registry/ModBlocks.java")
        assertTrue(registryFile.exists())
        val registryContent = FileUtil.readText(registryFile)
        TestValidationUtils.assertValidJavaSyntax(registryContent, "ModBlocks.java")
        TestValidationUtils.assertClassNameMatchesFile(registryContent, registryFile.name)
        assertTrue(registryContent.contains("test_block"))
    }

    // Helper methods

    private fun executeBlockCommand(
        blockName: String,
        type: String,
        extraOptions: Map<String, String> = emptyMap()
    ) {
        val command = CreateBlockCommand()

        // Build command args
        val args = mutableListOf(blockName, "--type", type)
        extraOptions.forEach { (key, value) ->
            args.add(key)
            args.add(value)
        }

        // Set project directory before parsing
        command.projectDir = context.projectDir

        // Execute command
        command.parse(args.toTypedArray())
    }

    private fun assertBlockFilesExist(blockName: String, expectedFiles: List<String>) {
        expectedFiles.forEach { filePath ->
            val file = File(context.projectDir, filePath)
            assertTrue(
                file.exists(),
                "Expected file to exist: $filePath (absolute: ${file.absolutePath})"
            )
        }
    }
}
