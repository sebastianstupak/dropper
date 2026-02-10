package dev.dropper.indexer

import java.io.File

/**
 * Indexes biomes in a Dropper project
 */
class BiomeIndexer : ComponentIndexer {

    override fun getComponentType(): String = "biome"

    override fun index(projectDir: File): List<ComponentInfo> {
        val biomes = mutableListOf<ComponentInfo>()

        // Read mod ID from config.yml
        val modId = extractModId(projectDir) ?: return emptyList()

        // Scan common biomes directory
        val biomesDir = File(projectDir, "shared/common/src/main/java/com/$modId/biomes")
        if (!biomesDir.exists()) return emptyList()

        biomesDir.listFiles { file -> file.isFile && file.extension == "java" }?.forEach { file ->
            val className = file.nameWithoutExtension
            val biomeName = extractBiomeName(file) ?: toSnakeCase(className)

            biomes.add(
                ComponentInfo(
                    name = biomeName,
                    type = "biome",
                    className = className,
                    packageName = "com.$modId.biomes",
                    loaders = detectLoaders(projectDir),
                    versions = detectVersions(projectDir),
                    metadata = mapOf(
                        "biomeCategory" to detectBiomeCategory(file)
                    )
                )
            )
        }

        return biomes.sortedBy { it.name }
    }

    private fun extractModId(projectDir: File): String? {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) return null

        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9_-]+)").find(content)?.groupValues?.get(1)
    }

    private fun extractBiomeName(file: File): String? {
        val content = file.readText()
        val match = Regex("public static final String ID = \"([a-z0-9_]+)\"").find(content)
        return match?.groupValues?.get(1)
    }

    private fun detectLoaders(projectDir: File): List<String> {
        val loaders = mutableListOf<String>()

        if (File(projectDir, "shared/fabric/src/main/java").exists()) {
            loaders.add("fabric")
        }
        if (File(projectDir, "shared/forge/src/main/java").exists()) {
            loaders.add("forge")
        }
        if (File(projectDir, "shared/neoforge/src/main/java").exists()) {
            loaders.add("neoforge")
        }

        return loaders
    }

    private fun detectVersions(projectDir: File): List<String> {
        val versionsDir = File(projectDir, "versions")
        if (!versionsDir.exists()) return emptyList()

        return versionsDir.listFiles { file -> file.isDirectory && file.name.matches(Regex("\\d+_\\d+_\\d+")) }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }

    private fun detectBiomeCategory(file: File): String {
        val content = file.readText()
        return when {
            content.contains("Biome.Category.FOREST") -> "forest"
            content.contains("Biome.Category.PLAINS") -> "plains"
            content.contains("Biome.Category.DESERT") -> "desert"
            content.contains("Biome.Category.TAIGA") -> "taiga"
            content.contains("Biome.Category.OCEAN") -> "ocean"
            content.contains("Biome.Category.NETHER") -> "nether"
            content.contains("Biome.Category.THEEND") -> "the_end"
            else -> "custom"
        }
    }

    private fun toSnakeCase(className: String): String {
        return className.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }
}
