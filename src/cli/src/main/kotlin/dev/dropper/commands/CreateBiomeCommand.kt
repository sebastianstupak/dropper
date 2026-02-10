package dev.dropper.commands

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.float
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import dev.dropper.util.ValidationUtil
import java.io.File

/**
 * Command to create a new biome in the mod
 * Generates worldgen biome JSON with climate settings, effects, and spawns
 */
class CreateBiomeCommand : DropperCommand(
    name = "biome",
    help = "Create a new biome with worldgen JSON configuration"
) {
    private val name by argument(help = "Biome name in snake_case (e.g., crystal_plains)")
    private val temperature by option("--temperature", help = "Temperature value (default: 0.8)").float().default(0.8f)
    private val downfall by option("--downfall", help = "Downfall/humidity value (default: 0.4)").float().default(0.4f)
    private val category by option("--category", help = "Biome category: plains, forest, desert, savanna, taiga, jungle, ocean, river, swamp, mountain, nether, the_end, beach, mesa, mushroom_fields, ice").default("plains")
    private val precipitation by option("--precipitation", help = "Precipitation type: rain, snow, none").default("rain")

    override fun run() {
        // Validate project directory
        val projectValidation = ValidationUtil.validateDropperProject(projectDir)
        if (!projectValidation.isValid) {
            ValidationUtil.exitWithError(projectValidation)
            return
        }

        // Validate biome name
        val nameValidation = ValidationUtil.validateName(name, "biome")
        if (!nameValidation.isValid) {
            ValidationUtil.exitWithError(nameValidation)
            return
        }

        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val modId = extractModId(configFile)
        if (modId == null) {
            Logger.error("Could not read mod ID from config.yml")
            return
        }

        Logger.info("Creating biome: $name")

        // Validate inputs
        if (temperature < -2.0f || temperature > 2.0f) {
            Logger.error("Temperature must be between -2.0 and 2.0")
            return
        }

        if (downfall < 0.0f || downfall > 1.0f) {
            Logger.error("Downfall must be between 0.0 and 1.0")
            return
        }

        val validCategories = listOf("plains", "forest", "desert", "savanna", "taiga", "jungle", "ocean", "river", "swamp", "mountain", "nether", "the_end", "beach", "mesa", "mushroom_fields", "ice")
        if (category !in validCategories) {
            Logger.error("Invalid category. Valid options: ${validCategories.joinToString(", ")}")
            return
        }

        val validPrecipitation = listOf("rain", "snow", "none")
        if (precipitation !in validPrecipitation) {
            Logger.error("Invalid precipitation. Valid options: ${validPrecipitation.joinToString(", ")}")
            return
        }

        // Generate biome JSON
        generateBiomeJson(projectDir, name, modId)

        Logger.success("Biome '$name' created successfully!")
        Logger.info("Next steps:")
        Logger.info("  1. Customize biome settings: versions/shared/v1/data/$modId/worldgen/biome/$name.json")
        Logger.info("  2. Add custom features, spawns, and carvers as needed")
        Logger.info("  3. Build with: dropper build")
    }

    private fun generateBiomeJson(projectDir: File, biomeName: String, modId: String) {
        val colors = getColorScheme(category)
        val (skyColor, fogColor, waterColor, waterFogColor, grassColor) = colors
        val foliageColor = colors[5]

        val content = """
            {
              "has_precipitation": ${precipitation != "none"},
              "temperature": $temperature,
              "temperature_modifier": "${getTemperatureModifier()}",
              "downfall": $downfall,
              "effects": {
                "sky_color": $skyColor,
                "fog_color": $fogColor,
                "water_color": $waterColor,
                "water_fog_color": $waterFogColor,
                "grass_color": $grassColor,
                "foliage_color": $foliageColor,
                "mood_sound": {
                  "sound": "minecraft:ambient.cave",
                  "tick_delay": 6000,
                  "block_search_extent": 8,
                  "offset": 2.0
                }
              },
              "spawners": {
                "monster": ${getMonsterSpawns()},
                "creature": ${getCreatureSpawns()},
                "ambient": ${getAmbientSpawns()},
                "water_creature": [],
                "water_ambient": [],
                "misc": []
              },
              "spawn_costs": {},
              "carvers": {
                "air": [
                  "minecraft:cave",
                  "minecraft:cave_extra_underground",
                  "minecraft:canyon"
                ]
              },
              "features": [
                [],
                ["minecraft:lake_lava_underground", "minecraft:lake_lava_surface"],
                [],
                ["minecraft:monster_room", "minecraft:monster_room_deep"],
                [],
                [],
                ["minecraft:ore_dirt", "minecraft:ore_gravel", "minecraft:ore_granite_upper", "minecraft:ore_diorite_upper", "minecraft:ore_andesite_upper"],
                [],
                ["minecraft:spring_water"],
                ["minecraft:spring_lava"],
                []
              ]
            }
        """.trimIndent()

        val biomeFile = File(projectDir, "versions/shared/v1/data/$modId/worldgen/biome/$biomeName.json")
        FileUtil.writeText(biomeFile, content)

        Logger.info("  ✓ Created biome: versions/shared/v1/data/$modId/worldgen/biome/$biomeName.json")
    }

    private fun getTemperatureModifier(): String {
        return when {
            temperature <= 0.0f -> "frozen"
            else -> "none"
        }
    }

    private fun getColorScheme(category: String): List<Int> {
        return when (category) {
            "desert" -> listOf(
                7907327,    // sky_color (light blue)
                12638463,   // fog_color (sandy beige)
                4159204,    // water_color (blue)
                329011,     // water_fog_color (dark blue)
                9470285,    // grass_color (tan)
                10387789    // foliage_color (sandy green)
            )
            "forest" -> listOf(
                7972607,    // sky_color
                12638463,   // fog_color
                4159204,    // water_color
                329011,     // water_fog_color
                5011004,    // grass_color (dark green)
                6975545     // foliage_color (forest green)
            )
            "jungle" -> listOf(
                7842047,    // sky_color
                12638463,   // fog_color
                4159204,    // water_color
                329011,     // water_fog_color
                4764952,    // grass_color (jungle green)
                3090560     // foliage_color (dark jungle green)
            )
            "taiga" -> listOf(
                8233983,    // sky_color
                12638463,   // fog_color
                4159204,    // water_color
                329011,     // water_fog_color
                6726447,    // grass_color (cool green)
                8107825     // foliage_color (pine green)
            )
            "swamp" -> listOf(
                7907327,    // sky_color
                12638463,   // fog_color
                6388580,    // water_color (murky)
                2302743,    // water_fog_color (dark murky)
                5011004,    // grass_color (swamp green)
                6975545     // foliage_color
            )
            "ocean" -> listOf(
                8103167,    // sky_color
                12638463,   // fog_color
                4159204,    // water_color
                329011,     // water_fog_color
                5011004,    // grass_color
                6975545     // foliage_color
            )
            "mountain", "ice" -> listOf(
                8364543,    // sky_color (light)
                12638463,   // fog_color
                4159204,    // water_color
                329011,     // water_fog_color
                8168286,    // grass_color (light green)
                8168286     // foliage_color (light green)
            )
            "savanna" -> listOf(
                7776511,    // sky_color
                12638463,   // fog_color
                4159204,    // water_color
                329011,     // water_fog_color
                7504122,    // grass_color (yellowish)
                9470285     // foliage_color (dry)
            )
            "mesa" -> listOf(
                7254527,    // sky_color
                12638463,   // fog_color
                4159204,    // water_color
                329011,     // water_fog_color
                9470285,    // grass_color (reddish)
                10387789    // foliage_color (dry red)
            )
            "nether" -> listOf(
                3344392,    // sky_color (dark red)
                3344392,    // fog_color (dark red)
                4159204,    // water_color (not used)
                329011,     // water_fog_color (not used)
                6533741,    // grass_color (not used)
                6533741     // foliage_color (not used)
            )
            "the_end" -> listOf(
                0,          // sky_color (black)
                10518688,   // fog_color (purple)
                4159204,    // water_color (not used)
                329011,     // water_fog_color (not used)
                6533741,    // grass_color (not used)
                6533741     // foliage_color (not used)
            )
            else -> listOf(  // plains (default)
                7907327,    // sky_color
                12638463,   // fog_color
                4159204,    // water_color
                329011,     // water_fog_color
                5011004,    // grass_color
                6975545     // foliage_color
            )
        }
    }

    private fun getMonsterSpawns(): String {
        return when (category) {
            "nether" -> """
                [
                  {
                    "type": "minecraft:zombified_piglin",
                    "weight": 100,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:ghast",
                    "weight": 50,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:magma_cube",
                    "weight": 2,
                    "minCount": 4,
                    "maxCount": 4
                  }
                ]
            """.trimIndent()
            "the_end" -> """
                [
                  {
                    "type": "minecraft:enderman",
                    "weight": 10,
                    "minCount": 4,
                    "maxCount": 4
                  }
                ]
            """.trimIndent()
            "mushroom_fields" -> "[]"
            else -> """
                [
                  {
                    "type": "minecraft:spider",
                    "weight": 100,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:zombie",
                    "weight": 95,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:zombie_villager",
                    "weight": 5,
                    "minCount": 1,
                    "maxCount": 1
                  },
                  {
                    "type": "minecraft:skeleton",
                    "weight": 100,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:creeper",
                    "weight": 100,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:slime",
                    "weight": 100,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:enderman",
                    "weight": 10,
                    "minCount": 1,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:witch",
                    "weight": 5,
                    "minCount": 1,
                    "maxCount": 1
                  }
                ]
            """.trimIndent()
        }
    }

    private fun getCreatureSpawns(): String {
        return when (category) {
            "nether", "the_end" -> "[]"
            "ocean" -> "[]"
            "desert", "mesa" -> """
                [
                  {
                    "type": "minecraft:rabbit",
                    "weight": 4,
                    "minCount": 2,
                    "maxCount": 3
                  }
                ]
            """.trimIndent()
            "jungle" -> """
                [
                  {
                    "type": "minecraft:sheep",
                    "weight": 12,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:pig",
                    "weight": 10,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:chicken",
                    "weight": 10,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:cow",
                    "weight": 8,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:parrot",
                    "weight": 40,
                    "minCount": 1,
                    "maxCount": 2
                  },
                  {
                    "type": "minecraft:chicken",
                    "weight": 10,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:panda",
                    "weight": 1,
                    "minCount": 1,
                    "maxCount": 2
                  },
                  {
                    "type": "minecraft:ocelot",
                    "weight": 2,
                    "minCount": 1,
                    "maxCount": 1
                  }
                ]
            """.trimIndent()
            else -> """
                [
                  {
                    "type": "minecraft:sheep",
                    "weight": 12,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:pig",
                    "weight": 10,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:chicken",
                    "weight": 10,
                    "minCount": 4,
                    "maxCount": 4
                  },
                  {
                    "type": "minecraft:cow",
                    "weight": 8,
                    "minCount": 4,
                    "maxCount": 4
                  }
                ]
            """.trimIndent()
        }
    }

    private fun getAmbientSpawns(): String {
        return when (category) {
            "nether", "the_end" -> "[]"
            else -> """
                [
                  {
                    "type": "minecraft:bat",
                    "weight": 10,
                    "minCount": 8,
                    "maxCount": 8
                  }
                ]
            """.trimIndent()
        }
    }
}
