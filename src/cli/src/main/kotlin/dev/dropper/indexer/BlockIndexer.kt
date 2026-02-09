package dev.dropper.indexer

import java.io.File

/**
 * Indexes blocks in a Dropper project
 */
class BlockIndexer : ComponentIndexer {

    override fun getComponentType(): String = "block"

    override fun index(projectDir: File): List<ComponentInfo> {
        val blocks = mutableListOf<ComponentInfo>()

        // Read mod ID from config.yml
        val modId = extractModId(projectDir) ?: return emptyList()

        // Scan common blocks directory
        val blocksDir = File(projectDir, "shared/common/src/main/java/com/$modId/blocks")
        if (!blocksDir.exists()) return emptyList()

        blocksDir.listFiles { file -> file.isFile && file.extension == "java" }?.forEach { file ->
            val className = file.nameWithoutExtension
            val blockName = extractBlockName(file) ?: toSnakeCase(className)

            blocks.add(
                ComponentInfo(
                    name = blockName,
                    type = "block",
                    className = className,
                    packageName = "com.$modId.blocks",
                    hasTexture = hasTexture(projectDir, modId, blockName),
                    hasModel = hasModel(projectDir, modId, blockName),
                    hasLootTable = hasLootTable(projectDir, modId, blockName),
                    loaders = detectLoaders(projectDir),
                    versions = detectVersions(projectDir),
                    metadata = mapOf(
                        "blockType" to detectBlockType(file),
                        "hasBlockstate" to hasBlockstate(projectDir, modId, blockName)
                    )
                )
            )
        }

        return blocks.sortedBy { it.name }
    }

    private fun extractModId(projectDir: File): String? {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) return null

        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
    }

    private fun extractBlockName(file: File): String? {
        val content = file.readText()
        val match = Regex("public static final String ID = \"([a-z0-9_]+)\"").find(content)
        return match?.groupValues?.get(1)
    }

    private fun hasTexture(projectDir: File, modId: String, blockName: String): Boolean {
        val textureFile = File(projectDir, "versions/shared/v1/assets/$modId/textures/block/$blockName.png")
        return textureFile.exists() && textureFile.length() > 0
    }

    private fun hasModel(projectDir: File, modId: String, blockName: String): Boolean {
        val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/block/$blockName.json")
        return modelFile.exists()
    }

    private fun hasBlockstate(projectDir: File, modId: String, blockName: String): Boolean {
        val blockstateFile = File(projectDir, "versions/shared/v1/assets/$modId/blockstates/$blockName.json")
        return blockstateFile.exists()
    }

    private fun hasLootTable(projectDir: File, modId: String, blockName: String): Boolean {
        val lootTableFile = File(projectDir, "versions/shared/v1/data/$modId/loot_table/blocks/$blockName.json")
        return lootTableFile.exists()
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

    private fun detectBlockType(file: File): String {
        val content = file.readText()
        return when {
            content.contains("OreBlock") -> "ore"
            content.contains("PillarBlock") -> "pillar"
            content.contains("SlabBlock") -> "slab"
            content.contains("StairsBlock") -> "stairs"
            content.contains("FenceBlock") -> "fence"
            content.contains("WallBlock") -> "wall"
            content.contains("DoorBlock") -> "door"
            content.contains("TrapDoorBlock") -> "trapdoor"
            else -> "basic"
        }
    }

    private fun toSnakeCase(className: String): String {
        return className.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }
}
