package dev.dropper.commands

import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import com.google.gson.JsonParser
import com.google.gson.Gson
import com.google.gson.GsonBuilder

class CreateBiomeCommandTest {

    private lateinit var context: TestProjectContext

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("biome-test")

        // Create a minimal config.yml for testing
        val configFile = File(context.projectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
              name: Test Mod
              version: 1.0.0
              description: Test mod for biome creation
              author: Test Author
              license: MIT
        """.trimIndent())
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    @Test
    fun `test basic biome creation with defaults`() {
        val biomeName = "crystal_plains"

        executeBiomeCommand(biomeName)

        // Verify biome file exists
        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        assertTrue(biomeFile.exists(), "Biome file should exist")

        // Parse and verify JSON structure
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        // Verify climate settings
        assertTrue(json.has("has_precipitation"))
        assertTrue(json.has("temperature"))
        assertTrue(json.has("downfall"))
        assertEquals(0.8f, json.get("temperature").asFloat, 0.01f)
        assertEquals(0.4f, json.get("downfall").asFloat, 0.01f)

        // Verify effects
        assertTrue(json.has("effects"))
        val effects = json.getAsJsonObject("effects")
        assertTrue(effects.has("sky_color"))
        assertTrue(effects.has("fog_color"))
        assertTrue(effects.has("water_color"))
        assertTrue(effects.has("water_fog_color"))
        assertTrue(effects.has("grass_color"))
        assertTrue(effects.has("foliage_color"))
        assertTrue(effects.has("mood_sound"))

        // Verify mood sound structure
        val moodSound = effects.getAsJsonObject("mood_sound")
        assertTrue(moodSound.has("sound"))
        assertTrue(moodSound.has("tick_delay"))
        assertTrue(moodSound.has("block_search_extent"))
        assertTrue(moodSound.has("offset"))

        // Verify spawners
        assertTrue(json.has("spawners"))
        val spawners = json.getAsJsonObject("spawners")
        assertTrue(spawners.has("monster"))
        assertTrue(spawners.has("creature"))
        assertTrue(spawners.has("ambient"))
        assertTrue(spawners.has("water_creature"))
        assertTrue(spawners.has("water_ambient"))
        assertTrue(spawners.has("misc"))

        // Verify carvers
        assertTrue(json.has("carvers"))
        val carvers = json.getAsJsonObject("carvers")
        assertTrue(carvers.has("air"))

        // Verify features
        assertTrue(json.has("features"))
        assertTrue(json.getAsJsonArray("features").size() > 0)
    }

    @Test
    fun `test biome with custom temperature`() {
        val biomeName = "hot_desert"

        executeBiomeCommand(biomeName, mapOf(
            "--temperature" to "2.0",
            "--category" to "desert"
        ))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        assertTrue(biomeFile.exists())

        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject
        assertEquals(2.0f, json.get("temperature").asFloat, 0.01f)
        assertEquals("none", json.get("temperature_modifier").asString)
    }

    @Test
    fun `test biome with freezing temperature`() {
        val biomeName = "frozen_tundra"

        executeBiomeCommand(biomeName, mapOf(
            "--temperature" to "-0.5",
            "--category" to "ice"
        ))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        assertTrue(biomeFile.exists())

        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject
        assertEquals(-0.5f, json.get("temperature").asFloat, 0.01f)
        assertEquals("frozen", json.get("temperature_modifier").asString)
    }

    @Test
    fun `test biome with custom downfall`() {
        val biomeName = "dry_savanna"

        executeBiomeCommand(biomeName, mapOf(
            "--downfall" to "0.1",
            "--category" to "savanna"
        ))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        assertTrue(biomeFile.exists())

        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject
        assertEquals(0.1f, json.get("downfall").asFloat, 0.01f)
    }

    @Test
    fun `test biome with snow precipitation`() {
        val biomeName = "snowy_forest"

        executeBiomeCommand(biomeName, mapOf(
            "--precipitation" to "snow",
            "--category" to "taiga",
            "--temperature" to "0.0"
        ))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        assertTrue(biomeFile.exists())

        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject
        assertTrue(json.get("has_precipitation").asBoolean)
    }

    @Test
    fun `test biome with no precipitation`() {
        val biomeName = "barren_wasteland"

        executeBiomeCommand(biomeName, mapOf(
            "--precipitation" to "none",
            "--category" to "desert"
        ))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        assertTrue(biomeFile.exists())

        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject
        assertFalse(json.get("has_precipitation").asBoolean)
    }

    @Test
    fun `test desert biome colors`() {
        val biomeName = "sandy_desert"

        executeBiomeCommand(biomeName, mapOf("--category" to "desert"))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject
        val effects = json.getAsJsonObject("effects")

        // Desert should have specific color scheme
        assertEquals(7907327, effects.get("sky_color").asInt)
        assertEquals(12638463, effects.get("fog_color").asInt)
        assertEquals(9470285, effects.get("grass_color").asInt)
        assertEquals(10387789, effects.get("foliage_color").asInt)
    }

    @Test
    fun `test forest biome colors`() {
        val biomeName = "dense_forest"

        executeBiomeCommand(biomeName, mapOf("--category" to "forest"))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject
        val effects = json.getAsJsonObject("effects")

        assertEquals(5011004, effects.get("grass_color").asInt)
        assertEquals(6975545, effects.get("foliage_color").asInt)
    }

    @Test
    fun `test jungle biome colors`() {
        val biomeName = "tropical_jungle"

        executeBiomeCommand(biomeName, mapOf("--category" to "jungle"))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject
        val effects = json.getAsJsonObject("effects")

        assertEquals(4764952, effects.get("grass_color").asInt)
        assertEquals(3090560, effects.get("foliage_color").asInt)
    }

    @Test
    fun `test swamp biome colors`() {
        val biomeName = "murky_swamp"

        executeBiomeCommand(biomeName, mapOf("--category" to "swamp"))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject
        val effects = json.getAsJsonObject("effects")

        // Swamp has distinctive murky water
        assertEquals(6388580, effects.get("water_color").asInt)
        assertEquals(2302743, effects.get("water_fog_color").asInt)
    }

    @Test
    fun `test nether biome`() {
        val biomeName = "crimson_wastes"

        executeBiomeCommand(biomeName, mapOf("--category" to "nether"))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        // Verify nether-specific colors
        val effects = json.getAsJsonObject("effects")
        assertEquals(3344392, effects.get("sky_color").asInt)
        assertEquals(3344392, effects.get("fog_color").asInt)

        // Verify nether mob spawns
        val spawners = json.getAsJsonObject("spawners")
        val monsters = spawners.getAsJsonArray("monster")
        assertTrue(monsters.size() > 0)

        val monsterTypes = monsters.map { it.asJsonObject.get("type").asString }
        assertTrue(monsterTypes.contains("minecraft:zombified_piglin"))
        assertTrue(monsterTypes.contains("minecraft:ghast"))
        assertTrue(monsterTypes.contains("minecraft:magma_cube"))

        // No passive creatures in nether
        assertEquals(0, spawners.getAsJsonArray("creature").size())
    }

    @Test
    fun `test the_end biome`() {
        val biomeName = "void_expanse"

        executeBiomeCommand(biomeName, mapOf("--category" to "the_end"))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        // Verify End-specific colors
        val effects = json.getAsJsonObject("effects")
        assertEquals(0, effects.get("sky_color").asInt)  // Black sky
        assertEquals(10518688, effects.get("fog_color").asInt)  // Purple fog

        // Verify enderman spawns
        val spawners = json.getAsJsonObject("spawners")
        val monsters = spawners.getAsJsonArray("monster")
        assertEquals(1, monsters.size())
        assertEquals("minecraft:enderman", monsters.get(0).asJsonObject.get("type").asString)

        // No passive creatures in the End
        assertEquals(0, spawners.getAsJsonArray("creature").size())
    }

    @Test
    fun `test mushroom_fields biome has no monster spawns`() {
        val biomeName = "mushroom_island"

        executeBiomeCommand(biomeName, mapOf("--category" to "mushroom_fields"))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        val spawners = json.getAsJsonObject("spawners")
        assertEquals(0, spawners.getAsJsonArray("monster").size())
    }

    @Test
    fun `test jungle biome has special creature spawns`() {
        val biomeName = "bamboo_jungle"

        executeBiomeCommand(biomeName, mapOf("--category" to "jungle"))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        val spawners = json.getAsJsonObject("spawners")
        val creatures = spawners.getAsJsonArray("creature")

        val creatureTypes = creatures.map { it.asJsonObject.get("type").asString }
        assertTrue(creatureTypes.contains("minecraft:parrot"))
        assertTrue(creatureTypes.contains("minecraft:panda"))
        assertTrue(creatureTypes.contains("minecraft:ocelot"))
    }

    @Test
    fun `test standard overworld biome has normal mob spawns`() {
        val biomeName = "meadow"

        executeBiomeCommand(biomeName, mapOf("--category" to "plains"))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        val spawners = json.getAsJsonObject("spawners")

        // Verify monster spawns
        val monsters = spawners.getAsJsonArray("monster")
        val monsterTypes = monsters.map { it.asJsonObject.get("type").asString }
        assertTrue(monsterTypes.contains("minecraft:zombie"))
        assertTrue(monsterTypes.contains("minecraft:skeleton"))
        assertTrue(monsterTypes.contains("minecraft:creeper"))
        assertTrue(monsterTypes.contains("minecraft:spider"))
        assertTrue(monsterTypes.contains("minecraft:enderman"))
        assertTrue(monsterTypes.contains("minecraft:witch"))

        // Verify creature spawns
        val creatures = spawners.getAsJsonArray("creature")
        val creatureTypes = creatures.map { it.asJsonObject.get("type").asString }
        assertTrue(creatureTypes.contains("minecraft:sheep"))
        assertTrue(creatureTypes.contains("minecraft:pig"))
        assertTrue(creatureTypes.contains("minecraft:chicken"))
        assertTrue(creatureTypes.contains("minecraft:cow"))

        // Verify ambient spawns
        val ambient = spawners.getAsJsonArray("ambient")
        assertEquals(1, ambient.size())
        assertEquals("minecraft:bat", ambient.get(0).asJsonObject.get("type").asString)
    }

    @Test
    fun `test desert biome has rabbit spawns`() {
        val biomeName = "red_desert"

        executeBiomeCommand(biomeName, mapOf("--category" to "desert"))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        val spawners = json.getAsJsonObject("spawners")
        val creatures = spawners.getAsJsonArray("creature")

        assertEquals(1, creatures.size())
        assertEquals("minecraft:rabbit", creatures.get(0).asJsonObject.get("type").asString)
    }

    @Test
    fun `test biome spawn weights and counts`() {
        val biomeName = "test_plains"

        executeBiomeCommand(biomeName, mapOf("--category" to "plains"))

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        val spawners = json.getAsJsonObject("spawners")
        val monsters = spawners.getAsJsonArray("monster")

        // Find zombie spawn entry
        val zombie = monsters.find {
            it.asJsonObject.get("type").asString == "minecraft:zombie"
        }?.asJsonObject

        assertTrue(zombie != null)
        assertEquals(95, zombie!!.get("weight").asInt)
        assertEquals(4, zombie.get("minCount").asInt)
        assertEquals(4, zombie.get("maxCount").asInt)
    }

    @Test
    fun `test biome features array structure`() {
        val biomeName = "feature_test"

        executeBiomeCommand(biomeName)

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        val features = json.getAsJsonArray("features")

        // Features should be array of arrays (11 generation steps)
        assertTrue(features.size() >= 11)

        // Each element should be an array
        for (i in 0 until features.size()) {
            assertTrue(features.get(i).isJsonArray)
        }
    }

    @Test
    fun `test biome carvers structure`() {
        val biomeName = "cave_test"

        executeBiomeCommand(biomeName)

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        val carvers = json.getAsJsonObject("carvers")
        assertTrue(carvers.has("air"))

        val airCarvers = carvers.getAsJsonArray("air")
        assertTrue(airCarvers.size() > 0)
        assertTrue(airCarvers.toString().contains("minecraft:cave"))
        assertTrue(airCarvers.toString().contains("minecraft:canyon"))
    }

    @Test
    fun `test all biome categories are valid`() {
        val categories = listOf(
            "plains", "forest", "desert", "savanna", "taiga", "jungle",
            "ocean", "river", "swamp", "mountain", "nether", "the_end",
            "beach", "mesa", "mushroom_fields", "ice"
        )

        for (category in categories) {
            val biomeName = "test_${category.replace("_", "")}"

            executeBiomeCommand(biomeName, mapOf("--category" to category))

            val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
            assertTrue(biomeFile.exists(), "Biome file should exist for category: $category")

            // Verify it's valid JSON
            val json = JsonParser.parseString(biomeFile.readText()).asJsonObject
            assertTrue(json.has("effects"))
            assertTrue(json.has("spawners"))
        }
    }

    @Test
    fun `test biome JSON is properly formatted`() {
        val biomeName = "format_test"

        executeBiomeCommand(biomeName)

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val content = biomeFile.readText()

        // Verify it's valid JSON by parsing
        val json = JsonParser.parseString(content).asJsonObject

        // Verify proper indentation (should contain newlines)
        assertTrue(content.contains("\n"))

        // Verify no trailing commas or syntax errors
        assertTrue(json.isJsonObject)
    }

    @Test
    fun `test multiple biomes can be created`() {
        val biomes = listOf("biome_one", "biome_two", "biome_three")

        for (biome in biomes) {
            executeBiomeCommand(biome)
        }

        for (biome in biomes) {
            val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biome.json")
            assertTrue(biomeFile.exists(), "$biome should exist")
        }
    }

    @Test
    fun `test biome name with underscores`() {
        val biomeName = "my_custom_biome_name"

        executeBiomeCommand(biomeName)

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        assertTrue(biomeFile.exists())
    }

    @Test
    fun `test spawn_costs is empty object`() {
        val biomeName = "costs_test"

        executeBiomeCommand(biomeName)

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        assertTrue(json.has("spawn_costs"))
        val spawnCosts = json.getAsJsonObject("spawn_costs")
        assertEquals(0, spawnCosts.size())
    }

    @Test
    fun `test water spawners are empty arrays by default`() {
        val biomeName = "water_test"

        executeBiomeCommand(biomeName)

        val biomeFile = File(context.projectDir, "versions/shared/v1/data/testmod/worldgen/biome/$biomeName.json")
        val json = JsonParser.parseString(biomeFile.readText()).asJsonObject

        val spawners = json.getAsJsonObject("spawners")
        assertEquals(0, spawners.getAsJsonArray("water_creature").size())
        assertEquals(0, spawners.getAsJsonArray("water_ambient").size())
        assertEquals(0, spawners.getAsJsonArray("misc").size())
    }

    // Helper methods

    private fun executeBiomeCommand(
        biomeName: String,
        extraOptions: Map<String, String> = emptyMap()
    ) {
        val command = CreateBiomeCommand()

        // Build command args
        val args = mutableListOf(biomeName)
        extraOptions.forEach { (key, value) ->
            args.add(key)
            args.add(value)
        }

        // Set project directory before parsing
        command.projectDir = context.projectDir

        // Execute command
        command.parse(args.toTypedArray())
    }
}
