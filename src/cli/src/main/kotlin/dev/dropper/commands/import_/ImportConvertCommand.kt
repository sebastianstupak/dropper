package dev.dropper.commands.import_

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.importers.FabricImporter
import dev.dropper.importers.ForgeImporter
import dev.dropper.importers.NeoForgeImporter
import dev.dropper.importers.ProjectAnalyzer
import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Convert a single-loader mod project into a Dropper multi-loader project.
 *
 * This command auto-detects the source loader (Fabric, Forge, NeoForge),
 * reads mod metadata, and restructures the project into the Dropper
 * layered directory layout.
 */
class ImportConvertCommand : CliktCommand(
    name = "convert",
    help = "Convert a single-loader mod project to Dropper multi-loader structure"
) {
    private val sourcePath by argument(
        name = "SOURCE",
        help = "Path to existing single-loader mod project"
    )

    private val targetPath by option(
        "--target", "-t",
        help = "Target directory for converted project (default: <source>-dropper)"
    )

    private val dryRun by option(
        "--dry-run",
        help = "Preview changes without modifying files"
    ).flag()

    private val loaders by option(
        "--loaders", "-l",
        help = "Target loaders (comma-separated, default: fabric,forge,neoforge)"
    )

    override fun run() {
        val source = File(sourcePath).absoluteFile

        if (!source.exists() || !source.isDirectory) {
            Logger.error("Source directory does not exist: ${source.absolutePath}")
            return
        }

        val target = if (targetPath != null) {
            File(targetPath!!).absoluteFile
        } else {
            File(source.parent, source.name + "-dropper")
        }

        if (!dryRun && target.exists()) {
            Logger.error("Target directory already exists: ${target.absolutePath}")
            Logger.info("Remove it first or specify a different --target")
            return
        }

        // Step 1: Detect the source loader
        val analyzer = ProjectAnalyzer()
        val detectedLoader = analyzer.detectLoader(source)

        if (detectedLoader == null) {
            Logger.error("Could not detect mod loader in: ${source.absolutePath}")
            Logger.info("Expected one of:")
            Logger.info("  - src/main/resources/fabric.mod.json (Fabric)")
            Logger.info("  - src/main/resources/META-INF/mods.toml (Forge)")
            Logger.info("  - src/main/resources/META-INF/neoforge.mods.toml (NeoForge)")
            return
        }

        Logger.info("Detected source loader: $detectedLoader")

        // Step 2: Analyze the source project using the appropriate importer
        val importResult = try {
            when (detectedLoader) {
                "fabric" -> FabricImporter(analyzer).analyze(source)
                "forge" -> ForgeImporter(analyzer).analyze(source)
                "neoforge" -> NeoForgeImporter(analyzer).analyze(source)
                else -> {
                    Logger.error("Unsupported loader: $detectedLoader")
                    return
                }
            }
        } catch (e: Exception) {
            Logger.error("Failed to analyze source project: ${e.message}")
            return
        }

        Logger.info("Detected mod: ${importResult.modName} (${importResult.modId})")
        Logger.info("  Version: ${importResult.version}")
        Logger.info("  Description: ${importResult.description}")
        Logger.info("  Author: ${importResult.author}")
        Logger.info("  Minecraft: ${importResult.minecraftVersion}")

        // Determine target loaders
        val targetLoaders = loaders?.split(",")?.map { it.trim() }
            ?: listOf("fabric", "forge", "neoforge")

        val validLoaders = listOf("fabric", "forge", "neoforge")
        val invalidLoaders = targetLoaders.filter { it !in validLoaders }
        if (invalidLoaders.isNotEmpty()) {
            Logger.error("Invalid loaders: ${invalidLoaders.joinToString(", ")}")
            Logger.info("Valid loaders: ${validLoaders.joinToString(", ")}")
            return
        }

        // Step 3: Dry run - preview what will happen
        if (dryRun) {
            printDryRun(source, target, importResult, detectedLoader, targetLoaders)
            return
        }

        // Step 4: Generate the Dropper multi-loader project
        Logger.info("Converting to multi-loader project...")
        Logger.info("Target loaders: ${targetLoaders.joinToString(", ")}")

        val config = ModConfig(
            id = importResult.modId,
            name = importResult.modName,
            version = importResult.version,
            description = importResult.description,
            author = importResult.author,
            license = importResult.license,
            minecraftVersions = listOf(importResult.minecraftVersion),
            loaders = targetLoaders
        )

        // Generate the base project structure
        val generator = ProjectGenerator()
        generator.generate(target, config)

        // Step 5: Map source files to the Dropper structure
        Logger.info("Mapping source files...")
        mapSourceFilesToDropper(source, target, detectedLoader, analyzer)

        // Step 6: Map assets
        Logger.info("Mapping assets...")
        mapAssetsToDropper(source, target, importResult.modId, analyzer)

        // Step 7: Map data
        Logger.info("Mapping data...")
        mapDataToDropper(source, target, importResult.modId, analyzer)

        // Step 8: Generate loader-specific stubs for target loaders
        Logger.info("Generating loader stubs...")
        generateLoaderStubs(target, config, detectedLoader, targetLoaders)

        Logger.success("Conversion complete!")
        Logger.info("Project created at: ${target.absolutePath}")
        Logger.info("")
        Logger.info("Next steps:")
        Logger.info("  cd ${target.name}")
        Logger.info("  Review shared/common/ for cross-loader code")
        Logger.info("  Review shared/$detectedLoader/ for loader-specific code")
        targetLoaders.filter { it != detectedLoader }.forEach { loader ->
            Logger.info("  Implement shared/$loader/ platform code")
        }
        Logger.info("  Run: dropper build")
    }

    private fun printDryRun(
        source: File,
        target: File,
        result: dev.dropper.importers.ImportResult,
        detectedLoader: String,
        targetLoaders: List<String>
    ) {
        Logger.section("Dry Run - Conversion Preview")

        Logger.info("Source: ${source.absolutePath}")
        Logger.info("Target: ${target.absolutePath}")
        Logger.info("Source loader: $detectedLoader")
        Logger.info("Target loaders: ${targetLoaders.joinToString(", ")}")
        Logger.info("")

        Logger.info("Config to generate:")
        Logger.info("  id: ${result.modId}")
        Logger.info("  name: \"${result.modName}\"")
        Logger.info("  version: \"${result.version}\"")
        Logger.info("  description: \"${result.description}\"")
        Logger.info("  author: \"${result.author}\"")
        Logger.info("  minecraft_versions: [${result.minecraftVersion}]")
        Logger.info("  loaders: [${targetLoaders.joinToString(", ")}]")
        Logger.info("")

        Logger.info("Directory structure to create:")
        Logger.info("  ${target.name}/")
        Logger.info("    config.yml")
        Logger.info("    build.gradle.kts")
        Logger.info("    settings.gradle.kts")
        Logger.info("    buildSrc/")
        Logger.info("    shared/")
        Logger.info("      common/src/main/java/  (cross-loader code)")
        targetLoaders.forEach { loader ->
            val label = if (loader == detectedLoader) "(from source)" else "(stub)"
            Logger.info("      $loader/src/main/java/  $label")
        }
        Logger.info("    versions/")
        Logger.info("      shared/v1/assets/${result.modId}/")
        Logger.info("      shared/v1/data/${result.modId}/")
        Logger.info("      ${result.minecraftVersion.replace(".", "_")}/")
        Logger.info("")

        // Count source files
        val srcDir = File(source, "src/main/java")
        val javaFiles = if (srcDir.exists()) {
            srcDir.walkTopDown().filter { it.isFile && it.extension == "java" }.count()
        } else 0

        val assetsDir = File(source, "src/main/resources/assets")
        val assetFiles = if (assetsDir.exists()) {
            assetsDir.walkTopDown().filter { it.isFile }.count()
        } else 0

        val dataDir = File(source, "src/main/resources/data")
        val dataFiles = if (dataDir.exists()) {
            dataDir.walkTopDown().filter { it.isFile }.count()
        } else 0

        Logger.info("Files to process:")
        Logger.info("  Java sources: $javaFiles")
        Logger.info("  Asset files: $assetFiles")
        Logger.info("  Data files: $dataFiles")
        Logger.info("")
        Logger.info("Run without --dry-run to apply changes.")
    }

    private fun mapSourceFilesToDropper(
        source: File,
        target: File,
        detectedLoader: String,
        analyzer: ProjectAnalyzer
    ) {
        val srcDir = File(source, "src/main/java")
        if (!srcDir.exists()) return

        val sourceFiles = analyzer.findSourceFiles(source)
        val mapper = dev.dropper.importers.FileMapper()
        val categorized = mapper.categorizeSourceFiles(sourceFiles, detectedLoader)

        // Map common sources to shared/common
        categorized.common.forEach { file ->
            val relativePath = file.toRelativeString(srcDir)
            val targetFile = File(target, "shared/common/src/main/java/$relativePath")
            FileUtil.writeText(targetFile, file.readText())
        }

        // Map loader-specific sources to shared/<loader>
        categorized.loaderSpecific.forEach { file ->
            val relativePath = file.toRelativeString(srcDir)
            val targetFile = File(target, "shared/$detectedLoader/src/main/java/$relativePath")
            FileUtil.writeText(targetFile, file.readText())
        }

        // Map platform-specific sources to shared/<loader>
        categorized.platform.forEach { file ->
            val relativePath = file.toRelativeString(srcDir)
            val targetFile = File(target, "shared/$detectedLoader/src/main/java/$relativePath")
            FileUtil.writeText(targetFile, file.readText())
        }

        val total = categorized.common.size + categorized.loaderSpecific.size + categorized.platform.size
        Logger.info("  Mapped $total source file(s): " +
            "${categorized.common.size} common, " +
            "${categorized.loaderSpecific.size} loader-specific, " +
            "${categorized.platform.size} platform")
    }

    private fun mapAssetsToDropper(
        source: File,
        target: File,
        modId: String,
        analyzer: ProjectAnalyzer
    ) {
        val assetDirs = analyzer.findAssetDirectories(source)
        if (assetDirs.isEmpty()) {
            Logger.info("  No assets found")
            return
        }

        val mapper = dev.dropper.importers.FileMapper()
        mapper.mapAssets(assetDirs, target, modId)
        Logger.info("  Mapped ${assetDirs.size} asset namespace(s)")
    }

    private fun mapDataToDropper(
        source: File,
        target: File,
        modId: String,
        analyzer: ProjectAnalyzer
    ) {
        val dataDirs = analyzer.findDataDirectories(source)
        if (dataDirs.isEmpty()) {
            Logger.info("  No data found")
            return
        }

        val mapper = dev.dropper.importers.FileMapper()
        mapper.mapData(dataDirs, target, modId)
        Logger.info("  Mapped ${dataDirs.size} data namespace(s)")
    }

    private fun generateLoaderStubs(
        target: File,
        config: ModConfig,
        sourceLoader: String,
        targetLoaders: List<String>
    ) {
        val newLoaders = targetLoaders.filter { it != sourceLoader }
        if (newLoaders.isEmpty()) {
            Logger.info("  No additional loaders to generate stubs for")
            return
        }

        val sanitizedModId = FileUtil.sanitizeModId(config.id)
        newLoaders.forEach { loader ->
            val loaderCapitalized = loader.replaceFirstChar { it.uppercase() }
            val platformDir = File(target, "shared/$loader/src/main/java/com/$sanitizedModId/platform")
            platformDir.mkdirs()

            // Generate PlatformHelper implementation stub
            val platformContent = when (loader) {
                "fabric" -> generateFabricPlatformStub(sanitizedModId)
                "forge" -> generateForgePlatformStub(sanitizedModId)
                "neoforge" -> generateNeoForgePlatformStub(sanitizedModId)
                else -> null
            }

            if (platformContent != null) {
                val platformFile = File(platformDir, "${loaderCapitalized}PlatformHelper.java")
                FileUtil.writeText(platformFile, platformContent)
                Logger.info("  Generated $loader platform helper stub")
            }
        }
    }

    private fun generateFabricPlatformStub(sanitizedModId: String): String {
        return """
            package com.$sanitizedModId.platform;

            import net.fabricmc.loader.api.FabricLoader;

            /**
             * Fabric implementation of PlatformHelper.
             * Implement loader-specific logic here.
             */
            public class FabricPlatformHelper implements PlatformHelper {
                @Override
                public String getPlatformName() {
                    return "Fabric";
                }

                @Override
                public boolean isModLoaded(String modId) {
                    return FabricLoader.getInstance().isModLoaded(modId);
                }
            }
        """.trimIndent()
    }

    private fun generateForgePlatformStub(sanitizedModId: String): String {
        return """
            package com.$sanitizedModId.platform;

            import net.minecraftforge.fml.ModList;

            /**
             * Forge implementation of PlatformHelper.
             * Implement loader-specific logic here.
             */
            public class ForgePlatformHelper implements PlatformHelper {
                @Override
                public String getPlatformName() {
                    return "Forge";
                }

                @Override
                public boolean isModLoaded(String modId) {
                    return ModList.get().isLoaded(modId);
                }
            }
        """.trimIndent()
    }

    private fun generateNeoForgePlatformStub(sanitizedModId: String): String {
        return """
            package com.$sanitizedModId.platform;

            import net.neoforged.fml.ModList;

            /**
             * NeoForge implementation of PlatformHelper.
             * Implement loader-specific logic here.
             */
            public class NeoForgePlatformHelper implements PlatformHelper {
                @Override
                public String getPlatformName() {
                    return "NeoForge";
                }

                @Override
                public boolean isModLoaded(String modId) {
                    return ModList.get().isLoaded(modId);
                }
            }
        """.trimIndent()
    }
}
