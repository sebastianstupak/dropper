package dev.dropper.templates

import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Template for creating a custom dimension
 */
class DimensionTemplate : TemplateGenerator {

    override fun generate(projectDir: File, name: String, material: String?): Boolean {
        Logger.info("Generating dimension: $name")

        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) {
            Logger.error("config.yml not found")
            return false
        }

        // Extract mod ID
        val content = configFile.readText()
        val modId = Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)

        if (modId == null) {
            Logger.error("Could not extract mod ID")
            return false
        }

        // Find data directory
        val dataDir = File(projectDir, "versions/shared/v1/data/$modId")
        dataDir.mkdirs()

        // Create dimension JSON
        val dimensionDir = File(dataDir, "dimension")
        dimensionDir.mkdirs()

        val dimensionJson = """
            {
              "type": "$modId:$name",
              "generator": {
                "type": "minecraft:noise",
                "biome_source": {
                  "type": "minecraft:fixed",
                  "biome": "$modId:${name}_biome"
                },
                "settings": "minecraft:overworld"
              }
            }
        """.trimIndent()

        FileUtil.writeText(File(dimensionDir, "$name.json"), dimensionJson)
        Logger.success("Dimension '$name' created successfully!")

        return true
    }
}
