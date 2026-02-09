package dev.dropper.indexer

import java.io.File

/**
 * Indexes items in a Dropper project
 */
class ItemIndexer : ComponentIndexer {

    override fun getComponentType(): String = "item"

    override fun index(projectDir: File): List<ComponentInfo> {
        val items = mutableListOf<ComponentInfo>()

        // Read mod ID from config.yml
        val modId = extractModId(projectDir) ?: return emptyList()

        // Scan common items directory
        val itemsDir = File(projectDir, "shared/common/src/main/java/com/$modId/items")
        if (!itemsDir.exists()) return emptyList()

        itemsDir.listFiles { file -> file.isFile && file.extension == "java" }?.forEach { file ->
            val className = file.nameWithoutExtension
            val itemName = extractItemName(file) ?: toSnakeCase(className)

            items.add(
                ComponentInfo(
                    name = itemName,
                    type = "item",
                    className = className,
                    packageName = "com.$modId.items",
                    hasTexture = hasTexture(projectDir, modId, itemName),
                    hasModel = hasModel(projectDir, modId, itemName),
                    hasRecipe = hasRecipe(projectDir, modId, itemName),
                    loaders = detectLoaders(projectDir, className),
                    versions = detectVersions(projectDir),
                    metadata = mapOf(
                        "itemType" to detectItemType(file)
                    )
                )
            )
        }

        return items.sortedBy { it.name }
    }

    private fun extractModId(projectDir: File): String? {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) return null

        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
    }

    private fun extractItemName(file: File): String? {
        val content = file.readText()
        val match = Regex("public static final String ID = \"([a-z0-9_]+)\"").find(content)
        return match?.groupValues?.get(1)
    }

    private fun hasTexture(projectDir: File, modId: String, itemName: String): Boolean {
        val textureFile = File(projectDir, "versions/shared/v1/assets/$modId/textures/item/$itemName.png")
        return textureFile.exists() && textureFile.length() > 0
    }

    private fun hasModel(projectDir: File, modId: String, itemName: String): Boolean {
        val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/item/$itemName.json")
        return modelFile.exists()
    }

    private fun hasRecipe(projectDir: File, modId: String, itemName: String): Boolean {
        val recipeFile = File(projectDir, "versions/shared/v1/data/$modId/recipe/$itemName.json")
        return recipeFile.exists()
    }

    private fun detectLoaders(projectDir: File, className: String): List<String> {
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

    private fun detectItemType(file: File): String {
        val content = file.readText()
        return when {
            content.contains("SwordItem") || content.contains("AxeItem") ||
            content.contains("PickaxeItem") || content.contains("ShovelItem") -> "tool"
            content.contains("FoodComponent") || content.contains("food(") -> "food"
            content.contains("ArmorItem") -> "armor"
            else -> "basic"
        }
    }

    private fun toSnakeCase(className: String): String {
        return className.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }
}
