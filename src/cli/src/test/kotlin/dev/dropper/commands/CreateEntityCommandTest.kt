package dev.dropper.commands

import dev.dropper.util.FileUtil
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class CreateEntityCommandTest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("entity-test")

        // Create a minimal config.yml for testing
        val configFile = File(context.projectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
              name: Test Mod
              version: 1.0.0
              description: Test mod for entity creation
              author: Test Author
              license: MIT
        """.trimIndent())
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    @Test
    fun `test basic mob entity creation`() {
        val entityName = "test_mob"

        executeEntityCommand(entityName, "mob")

        // Verify entity files exist
        assertEntityFilesExist(entityName, listOf(
            "shared/common/src/main/java/com/testmod/entities/TestMob.java",
            "shared/fabric/src/main/java/com/testmod/platform/fabric/TestMobFabric.java",
            "shared/forge/src/main/java/com/testmod/platform/forge/TestMobForge.java",
            "shared/neoforge/src/main/java/com/testmod/platform/neoforge/TestMobNeoForge.java",
            "shared/fabric/src/main/java/com/testmod/client/renderer/fabric/TestMobRenderer.java",
            "shared/forge/src/main/java/com/testmod/client/renderer/forge/TestMobRenderer.java",
            "shared/neoforge/src/main/java/com/testmod/client/renderer/neoforge/TestMobRenderer.java",
            "versions/shared/v1/assets/testmod/models/entity/test_mob.json",
            "versions/shared/v1/assets/testmod/textures/entity/test_mob.png",
            "shared/common/src/main/java/com/testmod/items/TestMobSpawnEgg.java",
            "versions/shared/v1/assets/testmod/models/item/test_mob_spawn_egg.json",
            "versions/shared/v1/assets/testmod/textures/item/test_mob_spawn_egg.png",
            "versions/shared/v1/assets/testmod/lang/en_us.json"
        ))

        // Verify entity class content
        val entityClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/TestMob.java"))
        assertTrue(entityClass.contains("package com.testmod.entities;"))
        assertTrue(entityClass.contains("public class TestMob"))
        assertTrue(entityClass.contains("public static final String ID = \"test_mob\";"))
        assertTrue(entityClass.contains("PathAwareEntity"))

        // Verify Fabric registration
        val fabricEntity = FileUtil.readText(File(context.projectDir, "shared/fabric/src/main/java/com/testmod/platform/fabric/TestMobFabric.java"))
        assertTrue(fabricEntity.contains("FabricEntityTypeBuilder"))
        assertTrue(fabricEntity.contains("SpawnGroup.CREATURE"))

        // Verify Forge registration
        val forgeEntity = FileUtil.readText(File(context.projectDir, "shared/forge/src/main/java/com/testmod/platform/forge/TestMobForge.java"))
        assertTrue(forgeEntity.contains("DeferredRegister"))
        assertTrue(forgeEntity.contains("MobCategory.CREATURE"))

        // Verify NeoForge registration
        val neoforgeEntity = FileUtil.readText(File(context.projectDir, "shared/neoforge/src/main/java/com/testmod/platform/neoforge/TestMobNeoForge.java"))
        assertTrue(neoforgeEntity.contains("DeferredRegister"))
        assertTrue(neoforgeEntity.contains("DeferredHolder"))
    }

    @Test
    fun `test animal entity creation`() {
        val entityName = "custom_cow"

        executeEntityCommand(entityName, "animal")

        assertEntityFilesExist(entityName, listOf(
            "shared/common/src/main/java/com/testmod/entities/CustomCow.java",
            "versions/shared/v1/assets/testmod/models/entity/custom_cow.json",
            "versions/shared/v1/assets/testmod/textures/entity/custom_cow.png"
        ))

        // Verify entity class mentions AnimalEntity
        val entityClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/CustomCow.java"))
        assertTrue(entityClass.contains("AnimalEntity"))
        assertTrue(entityClass.contains("PassiveEntity"))
        assertTrue(entityClass.contains("createChild"))
    }

    @Test
    fun `test monster entity creation`() {
        val entityName = "fire_demon"

        executeEntityCommand(entityName, "monster")

        assertEntityFilesExist(entityName, listOf(
            "shared/common/src/main/java/com/testmod/entities/FireDemon.java",
            "versions/shared/v1/assets/testmod/models/entity/fire_demon.json",
            "versions/shared/v1/assets/testmod/textures/entity/fire_demon.png"
        ))

        // Verify entity class mentions HostileEntity
        val entityClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/FireDemon.java"))
        assertTrue(entityClass.contains("HostileEntity"))
        assertTrue(entityClass.contains("Entity type: monster"))
    }

    @Test
    fun `test villager entity creation`() {
        val entityName = "custom_villager"

        executeEntityCommand(entityName, "villager")

        assertEntityFilesExist(entityName, listOf(
            "shared/common/src/main/java/com/testmod/entities/CustomVillager.java",
            "versions/shared/v1/assets/testmod/models/entity/custom_villager.json",
            "versions/shared/v1/assets/testmod/textures/entity/custom_villager.png"
        ))

        // Verify entity class mentions VillagerEntity
        val entityClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/CustomVillager.java"))
        assertTrue(entityClass.contains("VillagerEntity"))
    }

    @Test
    fun `test projectile entity creation`() {
        val entityName = "magic_bolt"

        executeEntityCommand(entityName, "projectile")

        assertEntityFilesExist(entityName, listOf(
            "shared/common/src/main/java/com/testmod/entities/MagicBolt.java",
            "shared/fabric/src/main/java/com/testmod/platform/fabric/MagicBoltFabric.java",
            "shared/forge/src/main/java/com/testmod/platform/forge/MagicBoltForge.java",
            "shared/neoforge/src/main/java/com/testmod/platform/neoforge/MagicBoltNeoForge.java"
        ))

        // Verify entity class mentions ProjectileEntity
        val entityClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/MagicBolt.java"))
        assertTrue(entityClass.contains("ProjectileEntity"))
        assertTrue(entityClass.contains("onCollision"))

        // Verify no model is generated for projectiles
        val modelFile = File(context.projectDir, "versions/shared/v1/assets/testmod/models/entity/magic_bolt.json")
        assertFalse(modelFile.exists(), "Projectile should not have a model file")
    }

    @Test
    fun `test entity without spawn egg`() {
        val entityName = "test_entity"

        executeEntityCommand(entityName, "mob", mapOf("--spawn-egg" to "false"))

        // Verify entity exists but spawn egg doesn't
        val entityFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/TestEntity.java")
        assertTrue(entityFile.exists())

        val spawnEggFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/items/TestEntitySpawnEgg.java")
        assertFalse(spawnEggFile.exists(), "Spawn egg should not be created when --spawn-egg is false")

        val spawnEggModel = File(context.projectDir, "versions/shared/v1/assets/testmod/models/item/test_entity_spawn_egg.json")
        assertFalse(spawnEggModel.exists(), "Spawn egg model should not exist")

        // Verify lang file doesn't contain spawn egg entry
        val langFile = File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        val langContent = FileUtil.readText(langFile)
        assertFalse(langContent.contains("spawn_egg"), "Lang file should not contain spawn egg entry")
    }

    @Test
    fun `test all loader-specific renderer files created`() {
        val entityName = "test_mob"

        executeEntityCommand(entityName, "mob")

        // Check Fabric renderer
        val fabricRenderer = File(context.projectDir, "shared/fabric/src/main/java/com/testmod/client/renderer/fabric/TestMobRenderer.java")
        assertTrue(fabricRenderer.exists())
        val fabricContent = FileUtil.readText(fabricRenderer)
        assertTrue(fabricContent.contains("MobEntityRenderer"))
        assertTrue(fabricContent.contains("EntityRendererFactory.Context"))
        assertTrue(fabricContent.contains("textures/entity/test_mob.png"))

        // Check Forge renderer
        val forgeRenderer = File(context.projectDir, "shared/forge/src/main/java/com/testmod/client/renderer/forge/TestMobRenderer.java")
        assertTrue(forgeRenderer.exists())
        val forgeContent = FileUtil.readText(forgeRenderer)
        assertTrue(forgeContent.contains("MobRenderer"))
        assertTrue(forgeContent.contains("EntityRendererProvider.Context"))
        assertTrue(forgeContent.contains("ResourceLocation"))

        // Check NeoForge renderer
        val neoforgeRenderer = File(context.projectDir, "shared/neoforge/src/main/java/com/testmod/client/renderer/neoforge/TestMobRenderer.java")
        assertTrue(neoforgeRenderer.exists())
        val neoforgeContent = FileUtil.readText(neoforgeRenderer)
        assertTrue(neoforgeContent.contains("MobRenderer"))
        assertTrue(neoforgeContent.contains("ResourceLocation"))
    }

    @Test
    fun `test entity model generation for mobs`() {
        val entityName = "zombie_guard"

        executeEntityCommand(entityName, "mob")

        val modelFile = File(context.projectDir, "versions/shared/v1/assets/testmod/models/entity/zombie_guard.json")
        assertTrue(modelFile.exists())

        val modelContent = FileUtil.readText(modelFile)
        assertTrue(modelContent.contains("\"format_version\": \"1.12.0\""))
        assertTrue(modelContent.contains("\"minecraft:geometry\""))
        assertTrue(modelContent.contains("\"identifier\": \"geometry.testmod.zombie_guard\""))
        assertTrue(modelContent.contains("\"head\""))
        assertTrue(modelContent.contains("\"body\""))
    }

    @Test
    fun `test spawn egg item registration`() {
        val entityName = "friendly_slime"

        executeEntityCommand(entityName, "animal")

        val spawnEggFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/items/FriendlySlimeSpawnEgg.java")
        assertTrue(spawnEggFile.exists())

        val spawnEggContent = FileUtil.readText(spawnEggFile)
        assertTrue(spawnEggContent.contains("package com.testmod.items;"))
        assertTrue(spawnEggContent.contains("public class FriendlySlimeSpawnEgg"))
        assertTrue(spawnEggContent.contains("public static final String ID = \"friendly_slime_spawn_egg\";"))
        assertTrue(spawnEggContent.contains("SpawnEggItem"))

        // Verify spawn egg model
        val modelFile = File(context.projectDir, "versions/shared/v1/assets/testmod/models/item/friendly_slime_spawn_egg.json")
        assertTrue(modelFile.exists())
        val modelContent = FileUtil.readText(modelFile)
        assertTrue(modelContent.contains("\"parent\": \"item/template_spawn_egg\""))
    }

    @Test
    fun `test lang file generation with entity and spawn egg`() {
        val entityName = "magic_sheep"

        executeEntityCommand(entityName, "animal")

        val langFile = File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        assertTrue(langFile.exists())

        val langContent = FileUtil.readText(langFile)
        assertTrue(langContent.contains("\"entity.testmod.magic_sheep\": \"Magic Sheep\""))
        assertTrue(langContent.contains("\"item.testmod.magic_sheep_spawn_egg\": \"Magic Sheep Spawn Egg\""))
    }

    @Test
    fun `test lang file merging with existing entries`() {
        // Create initial lang file
        val langFile = File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        langFile.parentFile.mkdirs()
        langFile.writeText("""
            {
              "item.testmod.ruby": "Ruby",
              "block.testmod.ruby_ore": "Ruby Ore"
            }
        """.trimIndent())

        executeEntityCommand("ice_golem", "monster")

        val langContent = FileUtil.readText(langFile)
        // Verify old entries are preserved
        assertTrue(langContent.contains("\"item.testmod.ruby\": \"Ruby\""))
        assertTrue(langContent.contains("\"block.testmod.ruby_ore\": \"Ruby Ore\""))
        // Verify new entries are added
        assertTrue(langContent.contains("\"entity.testmod.ice_golem\": \"Ice Golem\""))
        assertTrue(langContent.contains("\"item.testmod.ice_golem_spawn_egg\": \"Ice Golem Spawn Egg\""))
    }

    @Test
    fun `test entity with underscores in name`() {
        val entityName = "ancient_fire_dragon"

        executeEntityCommand(entityName, "monster")

        // Verify class name is properly formatted
        val entityFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/AncientFireDragon.java")
        assertTrue(entityFile.exists())

        val entityContent = FileUtil.readText(entityFile)
        assertTrue(entityContent.contains("public class AncientFireDragon"))
        assertTrue(entityContent.contains("public static final String ID = \"ancient_fire_dragon\";"))

        // Verify lang file has proper display name
        val langFile = File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        val langContent = FileUtil.readText(langFile)
        assertTrue(langContent.contains("\"entity.testmod.ancient_fire_dragon\": \"Ancient Fire Dragon\""))
    }

    @Test
    fun `test entity texture placeholder creation`() {
        val entityName = "crystal_golem"

        executeEntityCommand(entityName, "mob")

        val textureFile = File(context.projectDir, "versions/shared/v1/assets/testmod/textures/entity/crystal_golem.png")
        assertTrue(textureFile.exists(), "Entity texture placeholder should be created")
        assertTrue(textureFile.length() == 0L, "Texture placeholder should be empty")
    }

    @Test
    fun `test all entity types have correct base class suggestions`() {
        val testCases = mapOf(
            "mob" to ("test_mob_entity" to "PathAwareEntity"),
            "animal" to ("test_animal_entity" to "AnimalEntity"),
            "monster" to ("test_monster_entity" to "HostileEntity"),
            "villager" to ("test_villager_entity" to "VillagerEntity"),
            "projectile" to ("test_projectile_entity" to "ProjectileEntity")
        )

        testCases.forEach { (type, pair) ->
            val (entityName, baseClass) = pair
            executeEntityCommand(entityName, type)

            val className = toClassName(entityName)
            val entityFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/$className.java")
            assertTrue(entityFile.exists(), "Entity file for type $type should exist")

            val content = FileUtil.readText(entityFile)
            assertTrue(content.contains("Base class suggestion: $baseClass"), "Entity type $type should suggest $baseClass")
        }
    }

    private fun toClassName(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    @Test
    fun `test fabric entity dimensions and tracking`() {
        val entityName = "test_entity"

        executeEntityCommand(entityName, "mob")

        val fabricEntity = FileUtil.readText(File(context.projectDir, "shared/fabric/src/main/java/com/testmod/platform/fabric/TestEntityFabric.java"))
        assertTrue(fabricEntity.contains("dimensions(EntityDimensions.fixed(0.6f, 1.8f))"))
        assertTrue(fabricEntity.contains("trackRangeChunks(8)"))
        assertTrue(fabricEntity.contains("trackedUpdateRate(3)"))
    }

    @Test
    fun `test forge entity builder configuration`() {
        val entityName = "test_entity"

        executeEntityCommand(entityName, "mob")

        val forgeEntity = FileUtil.readText(File(context.projectDir, "shared/forge/src/main/java/com/testmod/platform/forge/TestEntityForge.java"))
        assertTrue(forgeEntity.contains("EntityType.Builder.of"))
        assertTrue(forgeEntity.contains("sized(0.6f, 1.8f)"))
        assertTrue(forgeEntity.contains("clientTrackingRange(8)"))
        assertTrue(forgeEntity.contains("updateInterval(3)"))
        assertTrue(forgeEntity.contains("registerAttributes"))
    }

    @Test
    fun `test neoforge entity registration pattern`() {
        val entityName = "test_entity"

        executeEntityCommand(entityName, "mob")

        val neoforgeEntity = FileUtil.readText(File(context.projectDir, "shared/neoforge/src/main/java/com/testmod/platform/neoforge/TestEntityNeoForge.java"))
        assertTrue(neoforgeEntity.contains("DeferredRegister.create(Registries.ENTITY_TYPE"))
        assertTrue(neoforgeEntity.contains("DeferredHolder<EntityType<?>, EntityType<"))
        assertTrue(neoforgeEntity.contains("registerAttributes"))
    }

    @Test
    fun `test entity model JSON structure`() {
        val entityName = "knight"

        executeEntityCommand(entityName, "mob")

        val modelFile = File(context.projectDir, "versions/shared/v1/assets/testmod/models/entity/knight.json")
        val modelContent = FileUtil.readText(modelFile)

        // Verify JSON structure
        assertTrue(modelContent.contains("\"format_version\""))
        assertTrue(modelContent.contains("\"minecraft:geometry\""))
        assertTrue(modelContent.contains("\"description\""))
        assertTrue(modelContent.contains("\"texture_width\": 64"))
        assertTrue(modelContent.contains("\"texture_height\": 64"))
        assertTrue(modelContent.contains("\"bones\""))
        assertTrue(modelContent.contains("\"head\""))
        assertTrue(modelContent.contains("\"body\""))
        assertTrue(modelContent.contains("\"pivot\""))
        assertTrue(modelContent.contains("\"cubes\""))
    }

    // Helper methods

    private fun executeEntityCommand(
        entityName: String,
        type: String,
        extraOptions: Map<String, String> = emptyMap()
    ) {
        val command = CreateEntityCommand()

        // Build command args
        val args = mutableListOf(entityName, "--type", type)
        extraOptions.forEach { (key, value) ->
            args.add(key)
            args.add(value)
        }

        // Set project directory before parsing
        command.projectDir = context.projectDir

        // Execute command
        command.parse(args.toTypedArray())
    }

    private fun assertEntityFilesExist(entityName: String, expectedFiles: List<String>) {
        expectedFiles.forEach { filePath ->
            val file = File(context.projectDir, filePath)
            assertTrue(
                file.exists(),
                "Expected file to exist: $filePath (absolute: ${file.absolutePath})"
            )
        }
    }
}
