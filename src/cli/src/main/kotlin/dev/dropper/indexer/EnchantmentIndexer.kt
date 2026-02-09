package dev.dropper.indexer

import java.io.File

/**
 * Indexes enchantments in a Dropper project
 */
class EnchantmentIndexer : ComponentIndexer {

    override fun getComponentType(): String = "enchantment"

    override fun index(projectDir: File): List<ComponentInfo> {
        val enchantments = mutableListOf<ComponentInfo>()

        // Read mod ID from config.yml
        val modId = extractModId(projectDir) ?: return emptyList()

        // Scan common enchantments directory
        val enchantmentsDir = File(projectDir, "shared/common/src/main/java/com/$modId/enchantments")
        if (!enchantmentsDir.exists()) return emptyList()

        enchantmentsDir.listFiles { file -> file.isFile && file.extension == "java" }?.forEach { file ->
            val className = file.nameWithoutExtension
            val enchantmentName = extractEnchantmentName(file) ?: toSnakeCase(className)

            enchantments.add(
                ComponentInfo(
                    name = enchantmentName,
                    type = "enchantment",
                    className = className,
                    packageName = "com.$modId.enchantments",
                    loaders = detectLoaders(projectDir),
                    versions = detectVersions(projectDir),
                    metadata = mapOf(
                        "enchantmentType" to detectEnchantmentType(file),
                        "maxLevel" to extractMaxLevel(file)
                    )
                )
            )
        }

        return enchantments.sortedBy { it.name }
    }

    private fun extractModId(projectDir: File): String? {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) return null

        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9-]+)").find(content)?.groupValues?.get(1)
    }

    private fun extractEnchantmentName(file: File): String? {
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

    private fun detectEnchantmentType(file: File): String {
        val content = file.readText()
        return when {
            content.contains("EnchantmentTarget.WEAPON") -> "weapon"
            content.contains("EnchantmentTarget.ARMOR") -> "armor"
            content.contains("EnchantmentTarget.TOOL") -> "tool"
            content.contains("EnchantmentTarget.BOW") -> "bow"
            content.contains("EnchantmentTarget.FISHING_ROD") -> "fishing_rod"
            else -> "custom"
        }
    }

    private fun extractMaxLevel(file: File): Int {
        val content = file.readText()
        val match = Regex("getMaxLevel\\(\\)\\s*\\{\\s*return\\s*(\\d+)").find(content)
        return match?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    private fun toSnakeCase(className: String): String {
        return className.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }
}
