package tasks

import config.RootConfig
import config.VersionConfig
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import java.io.File

/**
 * Generates loader-specific metadata from config.yml.
 *
 * Auto-generates:
 * - mods.toml for NeoForge/Forge
 * - fabric.mod.json for Fabric
 * - META-INF/services/ for ServiceLoader
 */
abstract class GenerateMetadataTask : DefaultTask() {

    @get:Input
    abstract val loader: Property<String>

    @get:Internal
    abstract val rootConfig: Property<RootConfig>

    @get:Internal
    abstract val versionConfig: Property<VersionConfig>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        group = "build"
        description = "Generates loader-specific metadata from config.yml"
    }

    @TaskAction
    fun generate() {
        val loaderName = loader.get()
        val outDir = outputDir.get().asFile
        outDir.mkdirs()

        logger.lifecycle("Generating metadata for $loaderName")

        when (loaderName) {
            "neoforge" -> generateNeoForge(outDir)
            "forge" -> generateForge(outDir)
            "fabric" -> generateFabric(outDir)
            else -> throw IllegalArgumentException("Unknown loader: $loaderName")
        }

        // Generate ServiceLoader configurations
        generateServiceLoaderConfig(outDir)

        logger.lifecycle("Generated metadata in ${outDir.name}")
    }

    private fun generateNeoForge(outDir: File) {
        val config = rootConfig.get()
        val versionCfg = versionConfig.get()

        val metaInf = File(outDir, "META-INF")
        metaInf.mkdirs()

        val toml = buildString {
            appendLine("modLoader = \"javafml\"")
            appendLine("loaderVersion = \"[${versionCfg.neoforge_version},)\"")
            appendLine("license = \"${config.mod.license}\"")
            appendLine()
            appendLine("[[mods]]")
            appendLine("modId = \"${config.mod.id}\"")
            appendLine("version = \"${config.mod.version}\"")
            appendLine("displayName = \"${config.mod.name}\"")
            appendLine("description = '''${config.mod.description}'''")
            appendLine("authors = \"${config.mod.author}\"")
            appendLine()
            appendLine("[[dependencies.${config.mod.id}]]")
            appendLine("modId = \"neoforge\"")
            appendLine("type = \"required\"")
            appendLine("versionRange = \"[${versionCfg.neoforge_version},)\"")
            appendLine("ordering = \"NONE\"")
            appendLine("side = \"BOTH\"")
            appendLine()
            appendLine("[[dependencies.${config.mod.id}]]")
            appendLine("modId = \"minecraft\"")
            appendLine("type = \"required\"")
            appendLine("versionRange = \"[${versionCfg.minecraft_version}]\"")
            appendLine("ordering = \"NONE\"")
            appendLine("side = \"BOTH\"")
        }

        File(metaInf, "mods.toml").writeText(toml)
    }

    private fun generateForge(outDir: File) {
        val config = rootConfig.get()
        val versionCfg = versionConfig.get()

        val metaInf = File(outDir, "META-INF")
        metaInf.mkdirs()

        val toml = buildString {
            appendLine("modLoader = \"javafml\"")
            appendLine("loaderVersion = \"[${versionCfg.forge_version},)\"")
            appendLine("license = \"${config.mod.license}\"")
            appendLine()
            appendLine("[[mods]]")
            appendLine("modId = \"${config.mod.id}\"")
            appendLine("version = \"${config.mod.version}\"")
            appendLine("displayName = \"${config.mod.name}\"")
            appendLine("description = '''${config.mod.description}'''")
            appendLine("authors = \"${config.mod.author}\"")
            appendLine()
            appendLine("[[dependencies.${config.mod.id}]]")
            appendLine("modId = \"forge\"")
            appendLine("mandatory = true")
            appendLine("versionRange = \"[${versionCfg.forge_version},)\"")
            appendLine("ordering = \"NONE\"")
            appendLine("side = \"BOTH\"")
            appendLine()
            appendLine("[[dependencies.${config.mod.id}]]")
            appendLine("modId = \"minecraft\"")
            appendLine("mandatory = true")
            appendLine("versionRange = \"[${versionCfg.minecraft_version}]\"")
            appendLine("ordering = \"NONE\"")
            appendLine("side = \"BOTH\"")
        }

        File(metaInf, "mods.toml").writeText(toml)
    }

    private fun generateFabric(outDir: File) {
        val config = rootConfig.get()
        val versionCfg = versionConfig.get()
        val basePackage = config.mod.id.replace("-", "")
        val mainClass = config.mod.id.split("-", "_")
            .joinToString("") { it.capitalize() } + "Fabric"

        val fabricMod = mapOf(
            "schemaVersion" to 1,
            "id" to config.mod.id,
            "version" to config.mod.version,
            "name" to config.mod.name,
            "description" to config.mod.description,
            "authors" to listOf(config.mod.author),
            "license" to config.mod.license,
            "environment" to "*",
            "entrypoints" to mapOf(
                "main" to listOf("com.$basePackage.fabric.$mainClass")
            ),
            "depends" to mapOf(
                "fabricloader" to ">=${versionCfg.fabric_loader_version}",
                "minecraft" to versionCfg.minecraft_version,
                "java" to ">=${versionCfg.java_version}"
            )
        )

        val mapper = ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
        File(outDir, "fabric.mod.json").writeText(mapper.writeValueAsString(fabricMod))
    }

    private fun generateServiceLoaderConfig(outDir: File) {
        val config = rootConfig.get()
        val loaderName = loader.get()
        val servicesDir = File(outDir, "META-INF/services")
        servicesDir.mkdirs()

        val basePackage = config.mod.id.replace("-", "")
        val loaderPackage = when (loaderName) {
            "neoforge" -> "neoforge"
            "forge" -> "forge"
            "fabric" -> "fabric"
            else -> throw IllegalArgumentException("Unknown loader: $loaderName")
        }

        // Register PlatformHelper implementation
        val platformHelperService = "com.$basePackage.platform.PlatformHelper"
        val platformHelperImpl = "com.$basePackage.platform.${loaderPackage.capitalize()}PlatformHelper"

        File(servicesDir, platformHelperService).writeText(platformHelperImpl)
    }
}
