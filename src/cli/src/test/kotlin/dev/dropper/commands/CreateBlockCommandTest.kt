package dev.dropper.commands

import dev.dropper.util.FileUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class CreateBlockCommandTest {

    @TempDir
    lateinit var tempDir: File

    @BeforeEach
    fun setup() {
        // Create a minimal config.yml for testing
        val configFile = File(tempDir, "config.yml")
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
        tempDir.deleteRecursively()
    }

    @Test
    fun `test basic block creation`() {
        val blockName = "test_block"

        // Create block
        executeBlockCommand(blockName, "basic")

        // Verify files exist
        assertBlockFilesExist(blockName, listOf(
            "shared/common/src/main/java/com/testmod/blocks/TestBlock.java",
            "shared/fabric/src/main/java/com/testmod/platform/fabric/TestBlockFabric.java",
            "shared/forge/src/main/java/com/testmod/platform/forge/TestBlockForge.java",
            "shared/neoforge/src/main/java/com/testmod/platform/neoforge/TestBlockNeoForge.java",
            "versions/shared/v1/assets/testmod/blockstates/test_block.json",
            "versions/shared/v1/assets/testmod/models/block/test_block.json",
            "versions/shared/v1/assets/testmod/models/item/test_block.json",
            "versions/shared/v1/assets/testmod/textures/block/test_block.png",
            "versions/shared/v1/data/testmod/loot_table/blocks/test_block.json"
        ))

        // Verify blockstate content
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/test_block.json"))
        assertTrue(blockstate.contains("\"model\": \"testmod:block/test_block\""))

        // Verify model content
        val model = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/test_block.json"))
        assertTrue(model.contains("\"parent\": \"block/cube_all\""))
        assertTrue(model.contains("\"all\": \"testmod:block/test_block\""))
    }

    @Test
    fun `test ore block creation`() {
        val blockName = "ruby_ore"

        executeBlockCommand(blockName, "ore")

        assertBlockFilesExist(blockName, listOf(
            "versions/shared/v1/assets/testmod/blockstates/ruby_ore.json",
            "versions/shared/v1/assets/testmod/models/block/ruby_ore.json",
            "versions/shared/v1/data/testmod/loot_table/blocks/ruby_ore.json"
        ))

        // Verify loot table has Fortune enchantment
        val lootTable = FileUtil.readText(File(tempDir, "versions/shared/v1/data/testmod/loot_table/blocks/ruby_ore.json"))
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

        // Verify blockstate has axis variants
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/marble_pillar.json"))
        assertTrue(blockstate.contains("\"axis=x\""))
        assertTrue(blockstate.contains("\"axis=y\""))
        assertTrue(blockstate.contains("\"axis=z\""))

        // Verify model uses cube_column
        val model = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/marble_pillar.json"))
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

        // Verify blockstate has type variants
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/stone_slab.json"))
        assertTrue(blockstate.contains("\"type=bottom\""))
        assertTrue(blockstate.contains("\"type=top\""))
        assertTrue(blockstate.contains("\"type=double\""))

        // Verify models
        val model = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/stone_slab.json"))
        assertTrue(model.contains("\"parent\": \"block/slab\""))

        val topModel = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/stone_slab_top.json"))
        assertTrue(topModel.contains("\"parent\": \"block/slab_top\""))

        val doubleModel = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/stone_slab_double.json"))
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
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/stone_stairs.json"))
        assertTrue(blockstate.contains("facing="))
        assertTrue(blockstate.contains("half="))
        assertTrue(blockstate.contains("shape="))

        // Verify model
        val model = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/stone_stairs.json"))
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
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/oak_fence.json"))
        assertTrue(blockstate.contains("\"multipart\""))
        assertTrue(blockstate.contains("oak_fence_post"))
        assertTrue(blockstate.contains("oak_fence_side"))

        // Verify models
        val postModel = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/oak_fence_post.json"))
        assertTrue(postModel.contains("\"parent\": \"block/fence_post\""))

        val sideModel = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/oak_fence_side.json"))
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
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/cobblestone_wall.json"))
        assertTrue(blockstate.contains("\"multipart\""))
        assertTrue(blockstate.contains("cobblestone_wall_post"))
        assertTrue(blockstate.contains("cobblestone_wall_side"))

        // Verify models
        val postModel = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/cobblestone_wall_post.json"))
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
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/oak_door.json"))
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
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/oak_trapdoor.json"))
        assertTrue(blockstate.contains("half="))
        assertTrue(blockstate.contains("open="))

        // Verify models
        val bottomModel = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/oak_trapdoor_bottom.json"))
        assertTrue(bottomModel.contains("template_orientable_trapdoor_bottom"))

        val openModel = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/oak_trapdoor_open.json"))
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
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/stone_button.json"))
        assertTrue(blockstate.contains("powered="))
        assertTrue(blockstate.contains("face="))

        // Verify models
        val model = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/stone_button.json"))
        assertTrue(model.contains("\"parent\": \"block/button\""))

        val pressedModel = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/stone_button_pressed.json"))
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
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/stone_pressure_plate.json"))
        assertTrue(blockstate.contains("powered=false"))
        assertTrue(blockstate.contains("powered=true"))

        // Verify models
        val model = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/stone_pressure_plate.json"))
        assertTrue(model.contains("\"parent\": \"block/pressure_plate_up\""))

        val downModel = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/stone_pressure_plate_down.json"))
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
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/wheat.json"))
        for (age in 0..7) {
            assertTrue(blockstate.contains("\"age=$age\""))
        }

        // Verify model
        val model = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/models/block/wheat_stage0.json"))
        assertTrue(model.contains("\"parent\": \"block/crop\""))
        assertTrue(model.contains("\"crop\": \"testmod:block/wheat_stage0\""))
    }

    @Test
    fun `test crop block with custom max age`() {
        val blockName = "custom_crop"

        executeBlockCommand(blockName, "crop", mapOf("--max-age" to "3"))

        // Verify only stages 0-3 exist
        for (age in 0..3) {
            assertTrue(File(tempDir, "versions/shared/v1/assets/testmod/models/block/custom_crop_stage$age.json").exists())
        }

        // Verify stage 4 doesn't exist
        assertTrue(!File(tempDir, "versions/shared/v1/assets/testmod/models/block/custom_crop_stage4.json").exists())

        // Verify blockstate
        val blockstate = FileUtil.readText(File(tempDir, "versions/shared/v1/assets/testmod/blockstates/custom_crop.json"))
        assertTrue(blockstate.contains("\"age=0\""))
        assertTrue(blockstate.contains("\"age=3\""))
        assertTrue(!blockstate.contains("\"age=4\""))
    }

    @Test
    fun `test block without drops-self`() {
        val blockName = "grass_block"

        executeBlockCommand(blockName, "basic", mapOf("--drops-self" to "false"))

        // Verify loot table doesn't exist
        val lootTablePath = "versions/shared/v1/data/testmod/loot_table/blocks/grass_block.json"
        assertTrue(!File(tempDir, lootTablePath).exists())
    }

    @Test
    fun `test all loader registrations created`() {
        val blockName = "test_block"

        executeBlockCommand(blockName, "basic")

        // Check Fabric registration
        val fabricFile = File(tempDir, "shared/fabric/src/main/java/com/testmod/platform/fabric/TestBlockFabric.java")
        assertTrue(fabricFile.exists())
        val fabricContent = FileUtil.readText(fabricFile)
        assertTrue(fabricContent.contains("Registry.register"))
        assertTrue(fabricContent.contains("Registries.BLOCK"))

        // Check Forge registration
        val forgeFile = File(tempDir, "shared/forge/src/main/java/com/testmod/platform/forge/TestBlockForge.java")
        assertTrue(forgeFile.exists())
        val forgeContent = FileUtil.readText(forgeFile)
        assertTrue(forgeContent.contains("DeferredRegister"))
        assertTrue(forgeContent.contains("ForgeRegistries.BLOCKS"))

        // Check NeoForge registration
        val neoforgeFile = File(tempDir, "shared/neoforge/src/main/java/com/testmod/platform/neoforge/TestBlockNeoForge.java")
        assertTrue(neoforgeFile.exists())
        val neoforgeContent = FileUtil.readText(neoforgeFile)
        assertTrue(neoforgeContent.contains("DeferredRegister.Blocks"))
        assertTrue(neoforgeContent.contains("DeferredBlock"))
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

        // Set working directory
        System.setProperty("user.dir", tempDir.absolutePath)

        // Execute command
        command.main(args.toTypedArray())
    }

    private fun assertBlockFilesExist(blockName: String, expectedFiles: List<String>) {
        expectedFiles.forEach { filePath ->
            val file = File(tempDir, filePath)
            assertTrue(
                file.exists(),
                "Expected file to exist: $filePath (absolute: ${file.absolutePath})"
            )
        }
    }
}
