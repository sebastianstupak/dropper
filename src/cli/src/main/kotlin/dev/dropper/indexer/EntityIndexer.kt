package dev.dropper.indexer

import java.io.File

/**
 * Indexes entities in a Dropper project
 */
class EntityIndexer : ComponentIndexer {

    override fun getComponentType(): String = "entity"

    override fun index(projectDir: File): List<ComponentInfo> {
        val entities = mutableListOf<ComponentInfo>()

        // Read mod ID from config.yml
        val modId = extractModId(projectDir) ?: return emptyList()

        // Scan common entities directory
        val entitiesDir = File(projectDir, "shared/common/src/main/java/com/$modId/entities")
        if (!entitiesDir.exists()) return emptyList()

        entitiesDir.listFiles { file -> file.isFile && file.extension == "java" }?.forEach { file ->
            val className = file.nameWithoutExtension
            val entityName = extractEntityName(file) ?: toSnakeCase(className)

            entities.add(
                ComponentInfo(
                    name = entityName,
                    type = "entity",
                    className = className,
                    packageName = "com.$modId.entities",
                    hasTexture = hasTexture(projectDir, modId, entityName),
                    hasModel = hasModel(projectDir, modId, entityName),
                    loaders = detectLoaders(projectDir),
                    versions = detectVersions(projectDir),
                    metadata = mapOf(
                        "entityType" to detectEntityType(file)
                    )
                )
            )
        }

        return entities.sortedBy { it.name }
    }

    private fun extractModId(projectDir: File): String? {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) return null

        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
    }

    private fun extractEntityName(file: File): String? {
        val content = file.readText()
        val match = Regex("public static final String ID = \"([a-z0-9_]+)\"").find(content)
        return match?.groupValues?.get(1)
    }

    private fun hasTexture(projectDir: File, modId: String, entityName: String): Boolean {
        val textureFile = File(projectDir, "versions/shared/v1/assets/$modId/textures/entity/$entityName.png")
        return textureFile.exists() && textureFile.length() > 0
    }

    private fun hasModel(projectDir: File, modId: String, entityName: String): Boolean {
        val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/entity/$entityName.json")
        return modelFile.exists()
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

    private fun detectEntityType(file: File): String {
        val content = file.readText()
        return when {
            content.contains("extends AnimalEntity") -> "animal"
            content.contains("extends HostileEntity") -> "hostile"
            content.contains("extends PassiveEntity") -> "passive"
            content.contains("extends ProjectileEntity") -> "projectile"
            else -> "custom"
        }
    }

    private fun toSnakeCase(className: String): String {
        return className.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }
}
