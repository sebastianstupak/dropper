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
            .gradle/
            build/
            build-temp/
            .idea/
            *.iml
            *.ipr
            *.iws
            out/
            .DS_Store
        """.trimIndent())
    }

    private fun copyBuildLogic(projectDir: File) {
        val buildLogicTarget = File(projectDir, "build-logic")
        buildLogicTarget.mkdirs()

        // Copy build-logic from embedded resources
        val resourceRoot = javaClass.getResource("/build-logic")
        if (resourceRoot != null) {
            // When running from JAR/resources
            copyBuildLogicFromResources(buildLogicTarget)
        } else {
            Logger.warn("build-logic resources not found, skipping")
        }
    }

    private fun copyBuildLogicFromResources(target: File) {
        // List of files to copy from build-logic
        val files = mapOf(
            "settings.gradle.kts" to """
                dependencyResolutionManagement {
                    repositories {
                        gradlePluginPortal()
                        mavenCentral()
                    }
                }

                rootProject.name = "build-logic"
            """.trimIndent(),
            "build.gradle.kts" to """
                plugins {
                    `kotlin-dsl`
                }

                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                    maven("https://maven.fabricmc.net/")
                    maven("https://maven.neoforged.net/releases/")
                    maven("https://maven.minecraftforge.net/")
                }

                dependencies {
                    implementation("org.yaml:snakeyaml:2.2")
                    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
                }
            """.trimIndent()
        )

        files.forEach { (filename, content) ->
            FileUtil.writeText(File(target, filename), content)
        }

        // Create minimal build-logic structure
        val srcDir = File(target, "src/main/kotlin")
        srcDir.mkdirs()

        // Copy actual build-logic files if they exist in resources
        copyResourceFile("/build-logic/src/main/kotlin/config/ConfigModels.kt",
            File(srcDir, "config/ConfigModels.kt"))
        copyResourceFile("/build-logic/src/main/kotlin/config/ConfigLoader.kt",
            File(srcDir, "config/ConfigLoader.kt"))
        copyResourceFile("/build-logic/src/main/kotlin/tasks/GenerateMetadataTask.kt",
            File(srcDir, "tasks/GenerateMetadataTask.kt"))
        copyResourceFile("/build-logic/src/main/kotlin/tasks/AssemblePackagesTask.kt",
            File(srcDir, "tasks/AssemblePackagesTask.kt"))
        copyResourceFile("/build-logic/src/main/kotlin/mod.common.gradle.kts",
            File(srcDir, "mod.common.gradle.kts"))
        copyResourceFile("/build-logic/src/main/kotlin/mod.loader.gradle.kts",
            File(srcDir, "mod.loader.gradle.kts"))
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
        val versionConfig = """
            minecraft_version: "$mcVersion"
            asset_pack: "v1"
            loaders: [${config.loaders.joinToString(", ")}]
            java_version: 21
            neoforge_version: "21.1.0"
            forge_version: "51.0.0"
            fabric_loader_version: "0.16.9"
            fabric_api_version: "0.100.0"
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
