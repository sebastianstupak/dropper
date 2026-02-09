package dev.dropper.importers

import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Imports NeoForge mods into Dropper structure
 */
class NeoForgeImporter(
    private val analyzer: ProjectAnalyzer = ProjectAnalyzer(),
    private val mapper: FileMapper = FileMapper(),
    private val projectGenerator: ProjectGenerator = ProjectGenerator()
) : Importer {

    override fun import(source: File, target: File) {
        Logger.info("Importing NeoForge mod from: ${source.absolutePath}")

        val result = analyze(source)

        Logger.info("Detected mod: ${result.modName} (${result.modId})")
        Logger.info("Version: ${result.version}")
        Logger.info("Minecraft: ${result.minecraftVersion}")

        val config = ModConfig(
            id = result.modId,
            name = result.modName,
            version = result.version,
            description = result.description,
            author = result.author,
            license = result.license,
            minecraftVersions = listOf(result.minecraftVersion),
            loaders = listOf("neoforge")
        )

        projectGenerator.generate(target, config)

        Logger.info("Mapping source files...")
        mapSourceFiles(source, target, result)

        Logger.info("Mapping assets...")
        mapAssets(source, target, result.modId)

        Logger.info("Mapping data...")
        mapData(source, target, result.modId)

        Logger.success("Successfully imported NeoForge mod!")
    }

    override fun analyze(source: File): ImportResult {
        if (!source.exists() || !source.isDirectory) {
            throw IllegalArgumentException("Source must be an existing directory")
        }

        val metadataFile = analyzer.findMetadataFile(source, "neoforge")
            ?: throw IllegalArgumentException("META-INF/neoforge.mods.toml not found")

        val metadata = parseNeoForgeMetadata(metadataFile)
        val sourceFiles = analyzer.findSourceFiles(source)
        val basePackage = analyzer.detectBasePackage(source) ?: "com.unknown"

        return ImportResult(
            modId = metadata["modId"] ?: "unknown",
            modName = metadata["displayName"] ?: "Unknown Mod",
            version = metadata["version"] ?: "1.0.0",
            description = metadata["description"] ?: "",
            author = metadata["authors"] ?: "Unknown",
            license = metadata["license"] ?: "MIT",
            minecraftVersion = metadata["minecraftVersion"] ?: "1.20.1",
            loader = "neoforge",
            sourceFiles = sourceFiles.map { file ->
                SourceFile(
                    relativePath = file.toRelativeString(File(source, "src/main/java")),
                    packageName = analyzer.extractPackageName(file) ?: "",
                    className = analyzer.extractClassName(file) ?: ""
                )
            }
        )
    }

    private fun parseNeoForgeMetadata(file: File): Map<String, String> {
        val content = file.readText()
        val result = mutableMapOf<String, String>()

        val patterns = mapOf(
            "modId" to Regex("^modId\\s*=\\s*\"([^\"]+)\"", RegexOption.MULTILINE),
            "displayName" to Regex("^displayName\\s*=\\s*\"([^\"]+)\"", RegexOption.MULTILINE),
            "version" to Regex("^version\\s*=\\s*\"([^\"]+)\"", RegexOption.MULTILINE),
            "description" to Regex("^description\\s*=\\s*\"([^\"]+)\"", RegexOption.MULTILINE),
            "license" to Regex("^license\\s*=\\s*\"([^\"]+)\"", RegexOption.MULTILINE),
            "authors" to Regex("^authors\\s*=\\s*\"([^\"]+)\"", RegexOption.MULTILINE),
            "minecraftVersion" to Regex("^\\[\\[dependencies\\.[^\\]]+\\]\\].*?versionRange\\s*=\\s*\"\\[([0-9.]+)", RegexOption.MULTILINE)
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
        val categorized = mapper.categorizeSourceFiles(sourceFiles, "neoforge")

        categorized.common.forEach { file ->
            val relativePath = file.toRelativeString(srcDir)
            val targetFile = File(target, "shared/common/src/main/java/$relativePath")
            FileUtil.writeText(targetFile, file.readText())
        }

        categorized.loaderSpecific.forEach { file ->
            val relativePath = file.toRelativeString(srcDir)
            val targetFile = File(target, "shared/neoforge/src/main/java/$relativePath")
            FileUtil.writeText(targetFile, file.readText())
        }
    }

    private fun mapAssets(source: File, target: File, modId: String) {
        val assetDirs = analyzer.findAssetDirectories(source)
        mapper.mapAssets(assetDirs, target, modId)
    }

    private fun mapData(source: File, target: File, modId: String) {
        val dataDirs = analyzer.findDataDirectories(source)
        mapper.mapData(dataDirs, target, modId)
    }
}
