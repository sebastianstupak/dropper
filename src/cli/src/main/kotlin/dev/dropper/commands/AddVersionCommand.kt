package dev.dropper.commands

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to add a new Minecraft version to an existing Dropper project
 */
class AddVersionCommand : DropperCommand(
    name = "version",
    help = "Add a new Minecraft version to the project"
) {
    private val minecraftVersion by argument(
        name = "VERSION",
        help = "Minecraft version to add (e.g., 1.21.1)"
    )

    private val assetPack by option("--asset-pack", "-p", help = "Asset pack to use (v1, v2, etc.)")
        .default("v1")

    private val loaders by option("--loaders", "-l", help = "Mod loaders (comma-separated)")
        .default("fabric,forge,neoforge")

    private val javaVersion by option("--java", "-j", help = "Java version")
        .default("21")

    override fun run() {
        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        Logger.info("Adding Minecraft version: $minecraftVersion")

        // Create version directory name (e.g., 1.21.1 -> 1_21_1)
        val versionDir = minecraftVersion.replace(".", "_")

        // Check if version already exists
        val versionPath = File(projectDir, "versions/$versionDir")
        if (versionPath.exists()) {
            Logger.error("Version $minecraftVersion already exists")
            return
        }

        // Create version directory structure
        versionPath.mkdirs()

        // Parse loaders
        val loaderList = loaders.split(",").map { it.trim() }

        // Determine appropriate Fabric API version based on MC version
        val fabricApiVersion = when {
            minecraftVersion.startsWith("1.20.1") -> "0.92.0+1.20.1"
            minecraftVersion.startsWith("1.20.4") -> "0.96.0+1.20.4"
            minecraftVersion.startsWith("1.21") -> "0.100.0+1.21"  // May need adjustment
            else -> ""  // Unknown - user must specify manually
        }

        // Create version config.yml
        val versionConfig = """
            minecraft_version: "$minecraftVersion"
            asset_pack: "$assetPack"
            loaders: [${loaderList.joinToString(", ")}]
            java_version: $javaVersion
            neoforge_version: "21.1.0"
            forge_version: "51.0.0"
            fabric_loader_version: "0.16.9"
            ${if (fabricApiVersion.isNotEmpty()) "fabric_api_version: \"$fabricApiVersion\"" else "# fabric_api_version: \"\" # Specify Fabric API version for this MC version"}
        """.trimIndent()

        FileUtil.writeText(File(versionPath, "config.yml"), versionConfig)
        Logger.info("  ✓ Created version config: versions/$versionDir/config.yml")

        // Create loader-specific directories
        loaderList.forEach { loader ->
            val loaderDir = File(versionPath, loader)
            loaderDir.mkdirs()

            // Create src/main/java structure
            val srcMainJava = File(loaderDir, "src/main/java")
            srcMainJava.mkdirs()

            val srcMainResources = File(loaderDir, "src/main/resources")
            srcMainResources.mkdirs()

            Logger.info("  ✓ Created $loader directory structure")
        }

        // Create common directory
        val commonDir = File(versionPath, "common")
        commonDir.mkdirs()
        File(commonDir, "src/main/java").mkdirs()
        File(commonDir, "src/main/resources").mkdirs()
        Logger.info("  ✓ Created common directory structure")

        // Update root config.yml to include new version
        val rootConfig = configFile.readText()
        if (!rootConfig.contains("minecraftVersions:")) {
            // Find the versions line and add the new version
            val lines = rootConfig.lines().toMutableList()
            val versionsLineIndex = lines.indexOfFirst { it.trim().startsWith("versions:") }

            if (versionsLineIndex != -1) {
                // Add new version to the list
                val versionsLine = lines[versionsLineIndex]
                if (versionsLine.contains("[")) {
                    // Inline list format: versions: [1.20.1, 1.21.1]
                    val updatedLine = versionsLine.replace("]", ", $minecraftVersion]")
                    lines[versionsLineIndex] = updatedLine
                } else {
                    // Multi-line format - add new line
                    lines.add(versionsLineIndex + 1, "  - $minecraftVersion")
                }

                FileUtil.writeText(configFile, lines.joinToString("\n"))
                Logger.info("  ✓ Updated config.yml")
            }
        }

        Logger.success("Minecraft version $minecraftVersion added successfully!")
        Logger.info("Next steps:")
        Logger.info("  1. Add version-specific code in: versions/$versionDir/common/src/main/java")
        Logger.info("  2. Add loader-specific code in: versions/$versionDir/{loader}/src/main/java")
        Logger.info("  3. Build with: dropper build --version $minecraftVersion")
    }
}
