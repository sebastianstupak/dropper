package dev.dropper.generator

import dev.dropper.config.ModConfig
import dev.dropper.config.VersionConfig
import dev.dropper.template.TemplateContext
import dev.dropper.template.TemplateEngine
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
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
        val context = TemplateContext.create()
            .put("modId", config.id)
            .put("modName", config.name)
            .put("modVersion", config.version)
            .put("modDescription", config.description)
            .put("author", config.author)
            .put("license", config.license)
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
            }

            dependencies {
                // YAML and JSON parsing
                implementation("org.yaml:snakeyaml:2.2")
                implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")

                // Mod loader Gradle plugins
                // Fabric Loom - support for MC 1.20.x and 1.21.x
                implementation("net.fabricmc:fabric-loom:1.6-SNAPSHOT")

                // ForgeGradle 6.x - compatible with Gradle 8.6+
                implementation("net.minecraftforge.gradle:ForgeGradle:6.0.+")

                // Note: NeoGradle requires Gradle 9.1+ for newer versions
                // Users can add manually if needed
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

        // Generate platform helper interface
        val platformHelperContent = """
            package ${config.fullPackage}.platform;

            /**
             * Platform-specific helper interface
             */
            public interface PlatformHelper {
                String getPlatformName();
                boolean isModLoaded(String modId);
            }
        """.trimIndent()

        val platformHelperFile = File(projectDir, "shared/common/src/main/java/$basePackagePath/platform/PlatformHelper.java")
        FileUtil.writeText(platformHelperFile, platformHelperContent)

        // Generate Services class
        val servicesContent = """
            package ${config.fullPackage};

            import ${config.fullPackage}.platform.PlatformHelper;
            import java.util.ServiceLoader;

            /**
             * Service loader for platform-specific implementations
             */
            public class Services {
                public static final PlatformHelper PLATFORM = load(PlatformHelper.class);

                private static <T> T load(Class<T> clazz) {
                    return ServiceLoader.load(clazz).findFirst()
                        .orElseThrow(() -> new NullPointerException("Failed to load " + clazz));
                }
            }
        """.trimIndent()

        val servicesFile = File(projectDir, "shared/common/src/main/java/$basePackagePath/Services.java")
        FileUtil.writeText(servicesFile, servicesContent)

        // Generate main mod class
        val modClassName = config.name.replace(" ", "").replace("-", "").replace("_", "")
        val modClassContent = """
            package ${config.fullPackage};

            /**
             * Main mod class for ${config.name}
             */
            public class $modClassName {
                public static final String MOD_ID = "${config.id}";

                public static void init() {
                    System.out.println("Initializing ${config.name} on " + Services.PLATFORM.getPlatformName());
                }
            }
        """.trimIndent()

        val modClassFile = File(projectDir, "shared/common/src/main/java/$basePackagePath/$modClassName.java")
        FileUtil.writeText(modClassFile, modClassContent)
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
        // Determine appropriate Fabric API version based on MC version
        val fabricApiVersion = when {
            mcVersion.startsWith("1.20.1") -> "0.92.0+1.20.1"
            mcVersion.startsWith("1.20.4") -> "0.96.0+1.20.4"
            mcVersion.startsWith("1.21") -> "0.100.0+1.21"
            else -> ""  // Unknown - user must specify manually
        }

        val versionConfig = """
            minecraft_version: "$mcVersion"
            asset_pack: "v1"
            loaders: [${config.loaders.joinToString(", ")}]
            java_version: 21
            neoforge_version: "21.1.0"
            forge_version: "51.0.0"
            fabric_loader_version: "0.16.9"
            ${if (fabricApiVersion.isNotEmpty()) "fabric_api_version: \"$fabricApiVersion\"" else "# fabric_api_version: \"\" # Specify Fabric API version for this MC version"}
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
