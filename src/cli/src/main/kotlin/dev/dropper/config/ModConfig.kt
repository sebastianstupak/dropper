package dev.dropper.config

/**
 * Configuration for a mod project
 */
data class ModConfig(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val description: String,
    val author: String,
    val license: String = "MIT",
    val minecraftVersions: List<String>,
    val loaders: List<String>
) {
    /**
     * Get the base package name derived from mod ID
     * e.g., "my-mod" -> "mymod"
     */
    val basePackage: String
        get() = id.replace("-", "").replace("_", "")

    /**
     * Get package path for directory creation
     * e.g., "mymod" -> "com/mymod"
     */
    val packagePath: String
        get() = "com/$basePackage"

    /**
     * Get full package name
     * e.g., "com.mymod"
     */
    val fullPackage: String
        get() = "com.$basePackage"
}
