package dev.dropper.updaters

/**
 * Resolves latest versions for Minecraft and mod loaders
 */
class VersionResolver {

    /**
     * Get latest stable Minecraft version
     * In a real implementation, this would query the Minecraft API
     */
    fun getLatestMinecraftVersion(): String {
        // Hardcoded for now - in production would query:
        // https://launchermeta.mojang.com/mc/game/version_manifest.json
        return "1.21.1"
    }

    /**
     * Get latest Fabric Loader version
     */
    fun getLatestFabricLoaderVersion(): String {
        // Would query: https://meta.fabricmc.net/v2/versions/loader
        return "0.16.9"
    }

    /**
     * Get latest Fabric API version for a Minecraft version
     */
    fun getLatestFabricApiVersion(minecraftVersion: String): String? {
        // Would query: https://api.modrinth.com/v2/project/fabric-api/version
        return when {
            minecraftVersion.startsWith("1.20.1") -> "0.92.2+1.20.1"
            minecraftVersion.startsWith("1.20.4") -> "0.97.0+1.20.4"
            minecraftVersion.startsWith("1.21") -> "0.100.8+1.21"
            else -> null
        }
    }

    /**
     * Get latest Forge version for a Minecraft version
     */
    fun getLatestForgeVersion(minecraftVersion: String): String? {
        // Would query: https://files.minecraftforge.net/net/minecraftforge/forge/
        return when {
            minecraftVersion.startsWith("1.20.1") -> "47.3.0"
            minecraftVersion.startsWith("1.20.4") -> "49.1.0"
            minecraftVersion.startsWith("1.21") -> "51.0.33"
            else -> null
        }
    }

    /**
     * Get latest NeoForge version for a Minecraft version
     */
    fun getLatestNeoForgeVersion(minecraftVersion: String): String? {
        // Would query: https://maven.neoforged.net/releases/net/neoforged/neoforge/
        return when {
            minecraftVersion.startsWith("1.20.1") -> "47.1.106"
            minecraftVersion.startsWith("1.20.4") -> "20.4.237"
            minecraftVersion.startsWith("1.21") -> "21.1.77"
            else -> null
        }
    }

    /**
     * Compare versions
     * @return true if v1 is less than v2
     */
    fun isOlderVersion(v1: String, v2: String): Boolean {
        val parts1 = parseVersion(v1)
        val parts2 = parseVersion(v2)

        for (i in 0 until maxOf(parts1.size, parts2.size)) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }

            if (p1 < p2) return true
            if (p1 > p2) return false
        }

        return false
    }

    private fun parseVersion(version: String): List<Int> {
        // Extract numeric parts from version string
        val cleanVersion = version.split("+", "-").first()
        return cleanVersion.split(".")
            .mapNotNull { it.toIntOrNull() }
    }
}
