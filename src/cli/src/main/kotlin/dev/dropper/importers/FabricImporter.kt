package dev.dropper.importers

import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Imports Fabric mods into Dropper structure
 */
class FabricImporter(
    private val analyzer: ProjectAnalyzer = ProjectAnalyzer(),
    private val mapper: FileMapper = FileMapper(),
    private val projectGenerator: ProjectGenerator = ProjectGenerator()
) : Importer {

    override fun import(source: File, target: File) {
        Logger.info("Importing Fabric mod from: ${source.absolutePath}")

        // Analyze source project
        val result = analyze(source)

        Logger.info("Detected mod: ${result.modName} (${result.modId})")
        Logger.info("Version: ${result.version}")
        Logger.info("Minecraft: ${result.minecraftVersion}")

        // Generate Dropper project structure
        val config = ModConfig(
            id = result.modId,
            name = result.modName,
            version = result.version,
            description = result.description,
            author = result.author,
            license = result.license,
            minecraftVersions = listOf(result.minecraftVersion),
            loaders = listOf("fabric")
        )

        projectGenerator.generate(target, config)

        // Map source files
        Logger.info("Mapping source files...")
        mapSourceFiles(source, target, result)

        // Map assets
        Logger.info("Mapping assets...")
        mapAssets(source, target, result.modId)

        // Map data
        Logger.info("Mapping data...")
        mapData(source, target, result.modId)

        Logger.success("Successfully imported Fabric mod!")
        Logger.info("Next steps:")
        Logger.info("  cd ${target.name}")
        Logger.info("  Review and test the imported project")
        Logger.info("  Consider adding more loaders with: dropper import convert")
    }

    override fun analyze(source: File): ImportResult {
        if (!source.exists() || !source.isDirectory) {
            throw IllegalArgumentException("Source must be an existing directory")
        }

        // Find fabric.mod.json
        val metadataFile = analyzer.findMetadataFile(source, "fabric")
            ?: throw IllegalArgumentException("fabric.mod.json not found")

        // Parse metadata
        val metadata = parseFabricMetadata(metadataFile)

        // Find source files
        val sourceFiles = analyzer.findSourceFiles(source)
        val basePackage = analyzer.detectBasePackage(source) ?: "com.unknown"

        return ImportResult(
            modId = metadata["id"] ?: "unknown",
            modName = metadata["name"] ?: "Unknown Mod",
            version = metadata["version"] ?: "1.0.0",
            description = metadata["description"] ?: "",
            author = metadata["authors"]?.split(",")?.firstOrNull()?.trim() ?: "Unknown",
            license = metadata["license"] ?: "MIT",
            minecraftVersion = metadata["minecraft"] ?: "1.20.1",
            loader = "fabric",
            sourceFiles = sourceFiles.map { file ->
                SourceFile(
                    relativePath = file.toRelativeString(File(source, "src/main/java")),
                    packageName = analyzer.extractPackageName(file) ?: "",
                    className = analyzer.extractClassName(file) ?: ""
                )
            }
        )
    }

    private fun parseFabricMetadata(file: File): Map<String, String> {
        val content = file.readText()
        val result = mutableMapOf<String, String>()

        // Simple JSON parsing (avoiding external dependencies)
        val patterns = mapOf(
            "id" to Regex("\"id\"\\s*:\\s*\"([^\"]+)\""),
            "name" to Regex("\"name\"\\s*:\\s*\"([^\"]+)\""),
            "version" to Regex("\"version\"\\s*:\\s*\"([^\"]+)\""),
            "description" to Regex("\"description\"\\s*:\\s*\"([^\"]+)\""),
            "license" to Regex("\"license\"\\s*:\\s*\"([^\"]+)\""),
            "minecraft" to Regex("\"minecraft\"\\s*:\\s*\"([^\"]+)\""),
            "authors" to Regex("\"authors\"\\s*:\\s*\\[\\s*\"([^\"]+)\"")
        )

        patterns.forEach { (key, regex) ->
            regex.find(content)?.groupValues?.get(1)?.let { value ->
                result[key] = value
            }
        }

        return result
    }

    private fun mapSourceFiles(source: File, target: File, result: ImportResult) {
        val srcDir = File(source, "src/main/java")
        if (!srcDir.exists()) return

        val sourceFiles = analyzer.findSourceFiles(source)
        val categorized = mapper.categorizeSourceFiles(sourceFiles, "fabric")

        // Map common sources
        categorized.common.forEach { file ->
            val relativePath = file.toRelativeString(srcDir)
            val targetFile = File(target, "shared/common/src/main/java/$relativePath")
            FileUtil.writeText(targetFile, file.readText())
        }

        // Map Fabric-specific sources
        categorized.loaderSpecific.forEach { file ->
            val relativePath = file.toRelativeString(srcDir)
            val targetFile = File(target, "shared/fabric/src/main/java/$relativePath")
            FileUtil.writeText(targetFile, file.readText())
        }
    }

    private fun mapAssets(source: File, target: File, modId: String) {
        val assetsDir = File(source, "src/main/resources/assets")
        if (!assetsDir.exists()) return

        val assetDirs = analyzer.findAssetDirectories(source)
        mapper.mapAssets(assetDirs, target, modId)
    }

    private fun mapData(source: File, target: File, modId: String) {
        val dataDir = File(source, "src/main/resources/data")
        if (!dataDir.exists()) return

        val dataDirs = analyzer.findDataDirectories(source)
        mapper.mapData(dataDirs, target, modId)
    }
}
