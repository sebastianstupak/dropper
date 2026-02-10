package dev.dropper.generator

import dev.dropper.config.ModConfig
import dev.dropper.config.VersionConfig
import dev.dropper.template.TemplateContext
import dev.dropper.template.TemplateEngine
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import dev.dropper.util.MinecraftVersions
import java.io.File

/**
 * Generates a complete multi-loader Minecraft mod project
 */
class ProjectGenerator(
    private val templateEngine: TemplateEngine = TemplateEngine()
) {

    fun generate(projectDir: File, config: ModConfig) {
        Logger.info("Generating project: ${config.name}")

        // Create directory structure
        createDirectoryStructure(projectDir, config)

        // Generate root files
        generateRootFiles(projectDir, config)

        // Copy build-logic
        copyBuildLogic(projectDir)

        // Generate shared code
        generateSharedCode(projectDir, config)

        // Generate version directories
        generateVersions(projectDir, config)

        Logger.success("Project '${projectDir.name}' created successfully!")
        Logger.info("Next steps:")
        Logger.info("  cd ${projectDir.name}")
        Logger.info("  ./gradlew build")
    }

    private fun createDirectoryStructure(projectDir: File, config: ModConfig) {
        val dirs = listOf(
            "shared/common/src/main/java/${config.packagePath}",
            "shared/common/src/test/java/${config.packagePath}",
            "shared/neoforge/src/main/java/${config.packagePath}/platform",
            "shared/fabric/src/main/java/${config.packagePath}/platform",
            "shared/forge/src/main/java/${config.packagePath}/platform",
            "versions/shared/v1/assets/${config.id}",
            "versions/shared/v1/data/${config.id}",
            "gradle",
            "build-temp"
        )

        dirs.forEach { path ->
            FileUtil.createDirectories(File(projectDir, path))
        }
    }

    private fun generateRootFiles(projectDir: File, config: ModConfig) {
        // Build YAML fragments for minecraft_versions and loaders
        val mcVersionsYaml = if (config.minecraftVersions.isNotEmpty()) {
            "minecraft_versions:\n" + config.minecraftVersions.joinToString("\n") { "  - \"$it\"" }
        } else ""

        val loadersYaml = if (config.loaders.isNotEmpty()) {
            "loaders:\n" + config.loaders.joinToString("\n") { "  - \"$it\"" }
        } else ""

        val context = TemplateContext.create()
            .put("modId", config.id)
            .put("modName", config.name)
            .put("modVersion", config.version)
            .put("modDescription", config.description)
            .put("author", config.author)
            .put("license", config.license)
            .put("minecraftVersionsYaml", mcVersionsYaml)
            .put("loadersYaml", loadersYaml)
            .build()

        // config.yml
        val configContent = templateEngine.render("project/config.yml.mustache", context)
        FileUtil.writeText(File(projectDir, "config.yml"), configContent)

        // build.gradle.kts
        val buildGradleContent = templateEngine.render("project/build.gradle.kts.mustache", context)
        FileUtil.writeText(File(projectDir, "build.gradle.kts"), buildGradleContent)

        // settings.gradle.kts
        val settingsGradleContent = templateEngine.render("project/settings.gradle.kts.mustache", context)
        FileUtil.writeText(File(projectDir, "settings.gradle.kts"), settingsGradleContent)

        // README.md
        val readmeContent = templateEngine.render("project/README.md.mustache", context)
        FileUtil.writeText(File(projectDir, "README.md"), readmeContent)

        // AGENTS.md - explaining structure and referencing modloader docs
        val agentsContext = context + mapOf(
            "fullPackage" to config.fullPackage,
            "minecraftVersions" to config.minecraftVersions.map { it.replace(".", "_") }
        )
        val agentsContent = templateEngine.render("project/AGENTS.md.mustache", agentsContext)
        FileUtil.writeText(File(projectDir, "AGENTS.md"), agentsContent)

        // gradle.properties
        FileUtil.writeText(File(projectDir, "gradle.properties"), """
            org.gradle.jvmargs=-Xmx2G
            org.gradle.daemon=true
            org.gradle.caching=true
        """.trimIndent())

        // .gitignore
        FileUtil.writeText(File(projectDir, ".gitignore"), """
            # Gradle
            .gradle/
            build/
            build-temp/

            # IDE
            .idea/
            *.iml
            *.ipr
            *.iws
            out/

            # OS
            .DS_Store

            # Note: buildSrc/ is NOT ignored - it contains build logic and should be committed
        """.trimIndent())
    }

    private fun copyBuildLogic(projectDir: File) {
        val buildSrcTarget = File(projectDir, "buildSrc")
        buildSrcTarget.mkdirs()

        // Copy buildSrc from embedded resources
        val resourceRoot = javaClass.getResource("/buildSrc")
        if (resourceRoot != null) {
            // When running from JAR/resources
            copyBuildSrcFromResources(buildSrcTarget)
        } else {
            Logger.warn("buildSrc resources not found, skipping")
        }

        // Create build-logic directory (composite build structure)
        createBuildLogic(projectDir)
    }

    private fun createBuildLogic(projectDir: File) {
        val buildLogicDir = File(projectDir, "build-logic")
        buildLogicDir.mkdirs()

        // Create build.gradle.kts for build-logic
        val buildGradleContent = """
            plugins {
                `kotlin-dsl`
            }

            repositories {
                gradlePluginPortal()
                mavenCentral()
                maven("https://maven.fabricmc.net/") { name = "Fabric" }
                maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
                maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
                maven("https://maven.architectury.dev/") { name = "Architectury" }
            }

            dependencies {
                implementation("org.yaml:snakeyaml:2.2")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
                implementation("dev.architectury:architectury-loom:${MinecraftVersions.architecturyLoomVersion()}")
            }
        """.trimIndent()

        FileUtil.writeText(File(buildLogicDir, "build.gradle.kts"), buildGradleContent)

        // Create settings.gradle.kts for build-logic
        val settingsContent = """
            rootProject.name = "build-logic"
        """.trimIndent()

        FileUtil.writeText(File(buildLogicDir, "settings.gradle.kts"), settingsContent)

        // Create src/main/kotlin directory structure
        val srcDir = File(buildLogicDir, "src/main/kotlin")
        srcDir.mkdirs()
    }

    private fun copyBuildSrcFromResources(target: File) {
        // Create build.gradle.kts for buildSrc
        val buildGradleContent = """
            plugins {
                `kotlin-dsl`
            }

            repositories {
                gradlePluginPortal()
                mavenCentral()
                maven("https://maven.fabricmc.net/") { name = "Fabric" }
                maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
                maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
                maven("https://maven.architectury.dev/") { name = "Architectury" }
            }

            dependencies {
                // YAML and JSON parsing
                implementation("org.yaml:snakeyaml:2.2")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")

                // Architectury Loom - unified build toolchain for all loaders
                implementation("dev.architectury:architectury-loom:${MinecraftVersions.architecturyLoomVersion()}")
            }
        """.trimIndent()

        FileUtil.writeText(File(target, "build.gradle.kts"), buildGradleContent)

        // Create src/main/kotlin structure
        val srcDir = File(target, "src/main/kotlin")
        srcDir.mkdirs()

        // Create plugin descriptor directory
        val pluginDescriptorDir = File(target, "src/main/resources/META-INF/gradle-plugins")
        pluginDescriptorDir.mkdirs()

        // Create plugin properties file
        FileUtil.writeText(
            File(pluginDescriptorDir, "ModLoaderPlugin.properties"),
            "implementation-class=ModLoaderPlugin\n"
        )

        // Copy support files
        copyResourceFile("/buildSrc/src/main/kotlin/config/ConfigModels.kt",
            File(srcDir, "config/ConfigModels.kt"))
        copyResourceFile("/buildSrc/src/main/kotlin/config/ConfigLoader.kt",
            File(srcDir, "config/ConfigLoader.kt"))
        copyResourceFile("/buildSrc/src/main/kotlin/utils/AssetPackResolver.kt",
            File(srcDir, "utils/AssetPackResolver.kt"))
        copyResourceFile("/buildSrc/src/main/kotlin/tasks/GenerateMetadataTask.kt",
            File(srcDir, "tasks/GenerateMetadataTask.kt"))
        copyResourceFile("/buildSrc/src/main/kotlin/tasks/AssemblePackagesTask.kt",
            File(srcDir, "tasks/AssemblePackagesTask.kt"))
        copyResourceFile("/buildSrc/src/main/kotlin/ModLoaderPlugin.kt",
            File(srcDir, "ModLoaderPlugin.kt"))
    }

    private fun copyResourceFile(resourcePath: String, targetFile: File) {
        try {
            val stream = javaClass.getResourceAsStream(resourcePath)
            if (stream != null) {
                targetFile.parentFile?.mkdirs()
                targetFile.writeText(stream.bufferedReader().use { it.readText() })
            }
        } catch (e: Exception) {
            Logger.warn("Could not copy $resourcePath: ${e.message}")
        }
    }

    private fun generateSharedCode(projectDir: File, config: ModConfig) {
        val basePackagePath = config.packagePath
        val modClassName = config.name.replace(" ", "").replace("-", "").replace("_", "")

        // Generate main mod class (common, shared across all loaders)
        val modClassContent = """
            package ${config.fullPackage};

            /**
             * Main mod class for ${config.name}
             *
             * This common class is called from each loader's entry point.
             * Registration uses Architectury API's DeferredRegister for cross-loader compat.
             */
            public class $modClassName {
                public static final String MOD_ID = "${config.id}";

                public static void init() {
                    // Registries initialized here. 'dropper create' commands auto-update this method.
                }
            }
        """.trimIndent()

        val modClassFile = File(projectDir, "shared/common/src/main/java/$basePackagePath/$modClassName.java")
        FileUtil.writeText(modClassFile, modClassContent)

        // Generate Fabric entry point
        val fabricEntryContent = """
            package ${config.fullPackage}.platform.fabric;

            import ${config.fullPackage}.$modClassName;
            import net.fabricmc.api.ModInitializer;

            public class ${modClassName}Fabric implements ModInitializer {
                @Override
                public void onInitialize() {
                    $modClassName.init();
                }
            }
        """.trimIndent()

        val fabricEntryFile = File(projectDir, "shared/fabric/src/main/java/$basePackagePath/platform/fabric/${modClassName}Fabric.java")
        FileUtil.writeText(fabricEntryFile, fabricEntryContent)

        // Generate Forge entry point
        val forgeEntryContent = """
            package ${config.fullPackage}.platform.forge;

            import ${config.fullPackage}.$modClassName;
            import net.minecraftforge.fml.common.Mod;

            @Mod($modClassName.MOD_ID)
            public class ${modClassName}Forge {
                public ${modClassName}Forge() {
                    $modClassName.init();
                }
            }
        """.trimIndent()

        val forgeEntryFile = File(projectDir, "shared/forge/src/main/java/$basePackagePath/platform/forge/${modClassName}Forge.java")
        FileUtil.writeText(forgeEntryFile, forgeEntryContent)

        // Generate NeoForge entry point
        val neoforgeEntryContent = """
            package ${config.fullPackage}.platform.neoforge;

            import ${config.fullPackage}.$modClassName;
            import net.neoforged.fml.common.Mod;

            @Mod($modClassName.MOD_ID)
            public class ${modClassName}NeoForge {
                public ${modClassName}NeoForge() {
                    $modClassName.init();
                }
            }
        """.trimIndent()

        val neoforgeEntryFile = File(projectDir, "shared/neoforge/src/main/java/$basePackagePath/platform/neoforge/${modClassName}NeoForge.java")
        FileUtil.writeText(neoforgeEntryFile, neoforgeEntryContent)
    }

    private fun generateVersions(projectDir: File, config: ModConfig) {
        // Generate asset pack v1 config
        val assetPackConfig = """
            asset_pack:
              version: "v1"
              minecraft_versions: [${config.minecraftVersions.joinToString(", ")}]
              description: "Asset pack for ${config.name}"
              inherits: null
        """.trimIndent()

        FileUtil.writeText(
            File(projectDir, "versions/shared/v1/config.yml"),
            assetPackConfig
        )

        // Generate version configs
        config.minecraftVersions.forEach { version ->
            val versionDir = version.replace(".", "_")
            generateVersionConfig(projectDir, versionDir, version, config)
        }
    }

    private fun generateVersionConfig(
        projectDir: File,
        versionDir: String,
        mcVersion: String,
        config: ModConfig
    ) {
        val javaVersion = MinecraftVersions.requiredJavaVersion(mcVersion)
        val fabricApiVersion = MinecraftVersions.fabricApiVersion(mcVersion)
        val fabricLoaderVersion = MinecraftVersions.fabricLoaderVersion(mcVersion)
        val forgeVersion = MinecraftVersions.forgeVersion(mcVersion)
        val neoforgeVersion = MinecraftVersions.neoforgeVersion(mcVersion)
        val archApiVersion = MinecraftVersions.architecturyApiVersion(mcVersion)

        val versionConfig = """
            minecraft_version: "$mcVersion"
            asset_pack: "v1"
            loaders: [${config.loaders.joinToString(", ")}]
            java_version: $javaVersion
            neoforge_version: "$neoforgeVersion"
            forge_version: "$forgeVersion"
            fabric_loader_version: "$fabricLoaderVersion"
            fabric_api_version: "$fabricApiVersion"
            architectury_api_version: "$archApiVersion"
        """.trimIndent()

        val versionPath = File(projectDir, "versions/$versionDir")
        versionPath.mkdirs()
        FileUtil.writeText(File(versionPath, "config.yml"), versionConfig)

        // Create loader-specific directories
        config.loaders.forEach { loader ->
            val loaderDir = File(versionPath, loader)
            loaderDir.mkdirs()
        }
    }
}
