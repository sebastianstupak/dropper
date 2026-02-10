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
            "shared/common/src/main/java/com/testmod/registry/ModEntities.java",
            "shared/common/src/main/java/com/testmod/client/renderer/TestMobRenderer.java",
            "shared/common/src/main/java/com/testmod/client/model/TestMobModel.java",
            "versions/shared/v1/assets/testmod/textures/entity/test_mob.png",
            "shared/common/src/main/java/com/testmod/items/TestMobSpawnEgg.java",
            "versions/shared/v1/assets/testmod/models/item/test_mob_spawn_egg.json",
            "versions/shared/v1/assets/testmod/textures/item/test_mob_spawn_egg.png",
            "versions/shared/v1/assets/testmod/lang/en_us.json"
        ))

        // Verify no Bedrock JSON model is generated
        val bedrockModelFile = File(context.projectDir, "versions/shared/v1/assets/testmod/models/entity/test_mob.json")
        assertFalse(bedrockModelFile.exists(), "Bedrock JSON model should not be generated for Java Edition entities")

        // Verify entity class content
        val entityClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/TestMob.java"))
        assertTrue(entityClass.contains("package com.testmod.entities;"))
        assertTrue(entityClass.contains("public class TestMob"))
        assertTrue(entityClass.contains("public static final String ID = \"test_mob\";"))
        assertTrue(entityClass.contains("PathfinderMob"))

        // Verify registry file exists
        val registryFile = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/registry/ModEntities.java"))
        assertTrue(registryFile.contains("test_mob"))
    }

    @Test
    fun `test animal entity creation`() {
        val entityName = "custom_cow"

        executeEntityCommand(entityName, "animal")

        assertEntityFilesExist(entityName, listOf(
            "shared/common/src/main/java/com/testmod/entities/CustomCow.java",
            "shared/common/src/main/java/com/testmod/client/model/CustomCowModel.java",
            "versions/shared/v1/assets/testmod/textures/entity/custom_cow.png"
        ))

        // Verify no Bedrock JSON model
        val bedrockModelFile = File(context.projectDir, "versions/shared/v1/assets/testmod/models/entity/custom_cow.json")
        assertFalse(bedrockModelFile.exists(), "Bedrock JSON model should not be generated")

        // Verify entity class uses Animal and getBreedOffspring
        val entityClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/CustomCow.java"))
        assertTrue(entityClass.contains("Animal"))
        assertTrue(entityClass.contains("getBreedOffspring"))
        assertTrue(entityClass.contains("AgeableMob"))
        assertFalse(entityClass.contains("PassiveEntity"), "Should not use Fabric-mapped PassiveEntity")
        assertFalse(entityClass.contains("createChild"), "Should use getBreedOffspring instead of createChild")
    }

    @Test
    fun `test monster entity creation`() {
        val entityName = "fire_demon"

        executeEntityCommand(entityName, "monster")

        assertEntityFilesExist(entityName, listOf(
            "shared/common/src/main/java/com/testmod/entities/FireDemon.java",
            "shared/common/src/main/java/com/testmod/client/model/FireDemonModel.java",
            "versions/shared/v1/assets/testmod/textures/entity/fire_demon.png"
        ))

        // Verify no Bedrock JSON model
        val bedrockModelFile = File(context.projectDir, "versions/shared/v1/assets/testmod/models/entity/fire_demon.json")
        assertFalse(bedrockModelFile.exists(), "Bedrock JSON model should not be generated")

        // Verify entity class mentions Monster
        val entityClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/FireDemon.java"))
        assertTrue(entityClass.contains("Monster"))
        assertTrue(entityClass.contains("Entity type: monster"))
    }

    @Test
    fun `test villager entity creation`() {
        val entityName = "custom_villager"

        executeEntityCommand(entityName, "villager")

        assertEntityFilesExist(entityName, listOf(
            "shared/common/src/main/java/com/testmod/entities/CustomVillager.java",
            "shared/common/src/main/java/com/testmod/client/model/CustomVillagerModel.java",
            "versions/shared/v1/assets/testmod/textures/entity/custom_villager.png"
        ))

        // Verify no Bedrock JSON model
        val bedrockModelFile = File(context.projectDir, "versions/shared/v1/assets/testmod/models/entity/custom_villager.json")
        assertFalse(bedrockModelFile.exists(), "Bedrock JSON model should not be generated")

        // Verify entity class uses AbstractVillager (not VillagerEntity)
        val entityClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/CustomVillager.java"))
        assertTrue(entityClass.contains("AbstractVillager"))
        assertFalse(entityClass.contains("VillagerEntity"), "Should use AbstractVillager instead of VillagerEntity")
    }

    @Test
    fun `test projectile entity creation`() {
        val entityName = "magic_bolt"

        executeEntityCommand(entityName, "projectile")

        assertEntityFilesExist(entityName, listOf(
            "shared/common/src/main/java/com/testmod/entities/MagicBolt.java",
            "shared/common/src/main/java/com/testmod/registry/ModEntities.java"
        ))

        // Verify entity class mentions ThrowableProjectile (Mojang mapping)
        val entityClass = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/entities/MagicBolt.java"))
        assertTrue(entityClass.contains("ThrowableProjectile"))
        assertTrue(entityClass.contains("onCollision"))

        // Verify no model is generated for projectiles (neither JSON nor Java)
        val modelFile = File(context.projectDir, "versions/shared/v1/assets/testmod/models/entity/magic_bolt.json")
        assertFalse(modelFile.exists(), "Projectile should not have a model file")

        val commonModelFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/client/model/MagicBoltModel.java")
        assertFalse(commonModelFile.exists(), "Projectile should not have a Java model class")
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
    fun `test common renderer file created`() {
        val entityName = "test_mob"

        executeEntityCommand(entityName, "mob")

        // Check common renderer (Architectury uses Mojang mappings in shared/common)
        val commonRenderer = File(context.projectDir, "shared/common/src/main/java/com/testmod/client/renderer/TestMobRenderer.java")
        assertTrue(commonRenderer.exists())
        val content = FileUtil.readText(commonRenderer)
        assertTrue(content.contains("MobRenderer"))
        assertTrue(content.contains("EntityRendererProvider.Context"))
        assertTrue(content.contains("textures/entity/test_mob.png"))
        assertTrue(content.contains("import com.testmod.client.model.TestMobModel;"), "Renderer should import model from common package")
    }

    @Test
    fun `test entity model Java class generated for mobs in common`() {
        val entityName = "zombie_guard"

        executeEntityCommand(entityName, "mob")

        // Verify no Bedrock JSON model
        val bedrockModelFile = File(context.projectDir, "versions/shared/v1/assets/testmod/models/entity/zombie_guard.json")
        assertFalse(bedrockModelFile.exists(), "Should not generate Bedrock JSON model")

        // Verify common model class (Mojang mappings via Architectury)
        val commonModel = File(context.projectDir, "shared/common/src/main/java/com/testmod/client/model/ZombieGuardModel.java")
        assertTrue(commonModel.exists(), "Common model class should exist")
        val commonContent = FileUtil.readText(commonModel)
        assertTrue(commonContent.contains("package com.testmod.client.model;"))
        assertTrue(commonContent.contains("extends EntityModel<ZombieGuard>"))
        assertTrue(commonContent.contains("setupAnim"), "Common model should use Mojang-mapped setupAnim method")
        assertTrue(commonContent.contains("PoseStack"), "Common model should use Mojang-mapped PoseStack")
        assertTrue(commonContent.contains("renderToBuffer"), "Common model should use Mojang-mapped renderToBuffer method")
        assertTrue(commonContent.contains("VertexConsumer"))
        assertTrue(commonContent.contains("ModelPart"))
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
            "mob" to ("test_mob_entity" to "PathfinderMob"),
            "animal" to ("test_animal_entity" to "Animal"),
            "monster" to ("test_monster_entity" to "Monster"),
            "villager" to ("test_villager_entity" to "AbstractVillager"),
            "projectile" to ("test_projectile_entity" to "ThrowableProjectile")
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
    fun `test entity registry has dimensions and tracking`() {
        val entityName = "test_entity"

        executeEntityCommand(entityName, "mob")

        val registryFile = FileUtil.readText(File(context.projectDir, "shared/common/src/main/java/com/testmod/registry/ModEntities.java"))
        assertTrue(registryFile.contains("sized(0.6f, 1.8f)"))
        assertTrue(registryFile.contains("clientTrackingRange(8)"))
    }

    @Test
    fun `test entity model class structure in common`() {
        val entityName = "knight"

        executeEntityCommand(entityName, "mob")

        // Verify no Bedrock JSON model exists
        val bedrockModel = File(context.projectDir, "versions/shared/v1/assets/testmod/models/entity/knight.json")
        assertFalse(bedrockModel.exists(), "Should not generate Bedrock JSON model")

        // Verify common model uses Mojang mappings (Architectury)
        val commonModel = File(context.projectDir, "shared/common/src/main/java/com/testmod/client/model/KnightModel.java")
        assertTrue(commonModel.exists(), "Common model class should exist")
        val commonContent = FileUtil.readText(commonModel)
        assertTrue(commonContent.contains("EntityModel<Knight>"))
        assertTrue(commonContent.contains("setupAnim"))
        assertTrue(commonContent.contains("PoseStack"))
        assertTrue(commonContent.contains("renderToBuffer"))
        assertFalse(commonContent.contains("MatrixStack"), "Common model should not use Yarn-mapped MatrixStack")
        assertFalse(commonContent.contains("setAngles"), "Common model should not use Yarn-mapped setAngles")
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
