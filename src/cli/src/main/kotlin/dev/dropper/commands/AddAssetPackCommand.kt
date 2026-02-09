package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to add a new asset pack (version-agnostic assets/data)
 * Asset packs allow sharing assets across multiple Minecraft versions
 */
class AddAssetPackCommand : CliktCommand(
    name = "asset-pack",
    help = "Add a new shared asset pack (for multi-version asset reuse)"
) {
    private val packVersion by argument(
        name = "PACK_VERSION",
        help = "Asset pack version (e.g., v2, v3)"
    )

    private val inherits by option("--inherits", "-i", help = "Asset pack to inherit from (e.g., v1)")

    private val minecraftVersions by option("--mc-versions", "-m", help = "Minecraft versions using this pack (comma-separated)")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        Logger.info("Creating asset pack: $packVersion")

        // Create asset pack directory
        val packDir = File(projectDir, "versions/shared/$packVersion")
        if (packDir.exists()) {
            Logger.error("Asset pack $packVersion already exists")
            return
        }

        packDir.mkdirs()

        // Create asset pack config
        val mcVersionsList = minecraftVersions?.split(",")?.map { it.trim() } ?: emptyList()
        val assetPackConfig = buildString {
            appendLine("asset_pack:")
            appendLine("  version: \"$packVersion\"")
            if (mcVersionsList.isNotEmpty()) {
                appendLine("  minecraft_versions: [${mcVersionsList.joinToString(", ")}]")
            } else {
                appendLine("  minecraft_versions: []")
            }
            appendLine("  description: \"Asset pack $packVersion\"")
            appendLine("  inherits: ${inherits ?: "null"}")
        }

        FileUtil.writeText(File(packDir, "config.yml"), assetPackConfig)
        Logger.info("  ✓ Created config: versions/shared/$packVersion/config.yml")

        // Create directory structure
        val dirs = listOf(
            "assets",
            "data",
            "common/src/main/java",
            "common/src/main/resources"
        )

        dirs.forEach { dir ->
            File(packDir, dir).mkdirs()
        }

        Logger.info("  ✓ Created directory structure")
        Logger.success("Asset pack '$packVersion' created successfully!")

        if (inherits != null) {
            Logger.info("\nInheritance chain: $inherits → $packVersion")
            Logger.info("Assets in $packVersion will override assets from $inherits")
        }

        Logger.info("\nDirectory structure:")
        Logger.info("  versions/shared/$packVersion/")
        Logger.info("    ├── config.yml          # Asset pack configuration")
        Logger.info("    ├── assets/             # Textures, models, sounds")
        Logger.info("    ├── data/               # Recipes, loot tables, tags")
        Logger.info("    └── common/             # MC version-specific code (optional)")
        Logger.info("        └── src/main/java/  # Code for this MC version range")

        Logger.info("\nNext steps:")
        Logger.info("  1. Add assets to: versions/shared/$packVersion/assets/")
        Logger.info("  2. Add data to: versions/shared/$packVersion/data/")
        if (mcVersionsList.isNotEmpty()) {
            Logger.info("  3. Assign to versions: ${mcVersionsList.joinToString(", ")}")
        } else {
            Logger.info("  3. Assign to Minecraft versions in their config.yml")
        }
    }
}
