package dev.dropper.config

/**
 * Configuration for a specific Minecraft version
 */
data class VersionConfig(
    val minecraftVersion: String,
    val assetPack: String,
    val loaders: List<String>,
    val javaVersion: Int = 17,
    val neoforgeVersion: String? = null,
    val forgeVersion: String? = null,
    val fabricLoaderVersion: String? = null,
    val fabricApiVersion: String? = null
)
