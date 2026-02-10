import config.ConfigLoader
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import tasks.AssemblePackagesTask
import tasks.GenerateMetadataTask
import utils.AssetPackResolver
import java.io.File

/**
 * Gradle plugin for multi-loader Minecraft mod builds.
 *
 * Automatically configures:
 * - Java toolchain (17 for MC 1.20.x, 21 for MC 1.21.x)
 * - Source assembly from layered structure
 * - Metadata generation (fabric.mod.json, mods.toml, etc.)
 * - Asset pack inheritance
 * - Mod loader dependencies (Fabric, Forge, NeoForge)
 */
class ModLoaderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Extract version and loader from project name (e.g., "1_20_1-fabric")
        val projectParts = project.name.split("-")
        require(projectParts.size == 2) {
            "Project name must be format: VERSION-LOADER (e.g., 1_20_1-fabric)"
        }

        val mcVersion = projectParts[0]
        val loader = projectParts[1]

        // Get root directory (parent of build-temp)
        val rootDir = project.projectDir.parentFile.parentFile

        // Load configurations
        val rootConfig = ConfigLoader.loadRootConfig(rootDir)
        val versionConfig = ConfigLoader.loadVersionConfig(rootDir, mcVersion)
        val assetResolver = AssetPackResolver(rootDir)

        // Apply loader-specific plugins FIRST (before JavaPlugin)
        applyLoaderPlugin(project, loader, versionConfig)

        // Apply Java plugin
        project.plugins.apply(JavaPlugin::class.java)

        // Set project properties
        project.group = "com.${rootConfig.mod.id.replace("-", "")}"
        project.version = rootConfig.mod.version

        // Java toolchain - auto-detect based on Minecraft version
        val requiredJavaVersion = when {
            versionConfig.minecraft_version.startsWith("1.20") -> 17
            versionConfig.minecraft_version.startsWith("1.21") -> 21
            else -> versionConfig.java_version
        }

        project.extensions.getByType(org.gradle.api.plugins.JavaPluginExtension::class.java).apply {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(requiredJavaVersion))
            }
        }

        // Ensure bytecode compatibility
        project.tasks.withType(org.gradle.api.tasks.compile.JavaCompile::class.java).configureEach {
            options.release.set(requiredJavaVersion)
        }

        // Register tasks
        val assemblePackages = project.tasks.register("assemblePackages", AssemblePackagesTask::class.java) {
            rootDirectory.set(rootDir)
            this.mcVersion.set(mcVersion)
            this.loader.set(loader)
            basePackage.set("com.${rootConfig.mod.id.replace("-", "")}")
            outputDir.set(File(project.projectDir, "build/assembled-src"))
        }

        val generateMetadata = project.tasks.register("generateMetadata", GenerateMetadataTask::class.java) {
            this.loader.set(loader)
            this.rootConfig.set(rootConfig)
            this.versionConfig.set(versionConfig)
            outputDir.set(File(project.projectDir, "build/generated-resources"))
        }

        // Configure sourceSets after evaluation
        project.afterEvaluate {
            val sourceSets = project.extensions.getByType(
                org.gradle.api.tasks.SourceSetContainer::class.java
            )

            sourceSets.named("main").configure {
                java.setSrcDirs(listOf(File(project.projectDir, "build/assembled-src")))

                // Add asset directories (textures, models, etc.)
                assetResolver.resolveAssetDirs(mcVersion, loader).forEach { dir ->
                    resources.srcDir(dir)
                }

                // Add data directories (recipes, tags, etc.)
                assetResolver.resolveDataDirs(mcVersion, loader).forEach { dir ->
                    resources.srcDir(dir)
                }

                // Add generated metadata
                resources.srcDir(File(project.projectDir, "build/generated-resources"))
            }

            sourceSets.named("test").configure {
                java.setSrcDirs(listOf(File(project.projectDir, "build/assembled-src")))
            }

            // Add explicit task dependencies
            project.tasks.named("compileJava").configure {
                dependsOn(assemblePackages)
            }

            project.tasks.named("processResources").configure {
                dependsOn(generateMetadata)
            }
        }

        // Configure JAR output
        project.tasks.named("jar", org.gradle.jvm.tasks.Jar::class.java).configure {
            archiveBaseName.set(rootConfig.mod.id)
            archiveVersion.set(rootConfig.mod.version)
            archiveClassifier.set("$mcVersion-$loader")

            // Exclude test classes from JAR
            exclude("**/*Test.class")

            doLast {
                // Copy to final location: build/{mcVersion}/{loader}.jar
                val outputDir = File(rootDir, "build/$mcVersion")
                outputDir.mkdirs()
                val outputFile = File(outputDir, "$loader.jar")
                archiveFile.get().asFile.copyTo(outputFile, overwrite = true)
                project.logger.lifecycle("Built: ${outputFile.relativeTo(rootDir)}")
            }
        }

        // Configure repositories
        project.repositories.apply {
            mavenCentral()
            maven {
                name = "Fabric"
                url = project.uri("https://maven.fabricmc.net/")
            }
            maven {
                name = "NeoForged"
                url = project.uri("https://maven.neoforged.net/releases/")
            }
            maven {
                name = "MinecraftForge"
                url = project.uri("https://maven.minecraftforge.net/")
            }
            maven {
                name = "Minecraft"
                url = project.uri("https://libraries.minecraft.net/")
            }
            maven {
                name = "Architectury"
                url = project.uri("https://maven.architectury.dev/")
            }
        }

        // Configure dependencies based on loader
        configureDependencies(project, loader, versionConfig)

        // Configure test tasks
        project.tasks.withType(Test::class.java).configureEach {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }

        project.logger.lifecycle("Configured ${project.name}: MC=$mcVersion, Loader=$loader, Java=$requiredJavaVersion")
    }

    private fun applyLoaderPlugin(project: Project, loader: String, versionConfig: config.VersionConfig) {
        // Architectury Loom handles all three loaders via a single plugin
        project.plugins.apply("dev.architectury.loom")

        when (loader) {
            "fabric" -> {
                project.logger.lifecycle("Applied Architectury Loom (Fabric mode)")
            }
            "forge" -> {
                // Architectury Loom configures Forge mode automatically when forge dependency is added
                project.logger.lifecycle("Applied Architectury Loom (Forge mode)")
            }
            "neoforge" -> {
                // Architectury Loom configures NeoForge mode automatically when neoForge dependency is added
                project.logger.lifecycle("Applied Architectury Loom (NeoForge mode)")
            }
            else -> {
                project.logger.warn("Unknown loader: $loader - using default Architectury Loom config")
            }
        }
    }

    private fun configureDependencies(project: Project, loader: String, versionConfig: config.VersionConfig) {
        project.dependencies.apply {
            // Minecraft dependency (common to all loaders via Architectury Loom)
            add("minecraft", "com.mojang:minecraft:${versionConfig.minecraft_version}")

            // Use Mojang official mappings — Architectury Loom automatically remaps for Fabric
            add("mappings", project.extensions.getByType(
                net.fabricmc.loom.api.LoomGradleExtensionAPI::class.java
            ).officialMojangMappings())

            // Architectury API for cross-loader abstractions
            val archApiVersion = versionConfig.architectury_api_version
            if (archApiVersion != null && archApiVersion.isNotEmpty()) {
                val archSuffix = when (loader) {
                    "fabric" -> "fabric"
                    "forge" -> "forge"
                    "neoforge" -> "neoforge"
                    else -> "fabric"
                }
                add("modApi", "dev.architectury:architectury-$archSuffix:$archApiVersion")
            }

            when (loader) {
                "fabric" -> {
                    // Fabric Loader
                    val fabricLoaderVersion = versionConfig.fabric_loader_version ?: "0.15.0"
                    add("modImplementation", "net.fabricmc:fabric-loader:$fabricLoaderVersion")

                    // Fabric API
                    val fabricApiVersion = versionConfig.fabric_api_version
                    if (fabricApiVersion != null && fabricApiVersion.isNotEmpty()) {
                        add("modImplementation", "net.fabricmc.fabric-api:fabric-api:$fabricApiVersion")
                    }
                    project.logger.lifecycle("Configured Fabric dependencies for Minecraft ${versionConfig.minecraft_version}")
                }
                "forge" -> {
                    val forgeVersion = versionConfig.forge_version
                    if (forgeVersion != null && forgeVersion.isNotEmpty()) {
                        add("forge", "net.minecraftforge:forge:$forgeVersion")
                    }
                    project.logger.lifecycle("Configured Forge dependencies for Minecraft ${versionConfig.minecraft_version}")
                }
                "neoforge" -> {
                    val neoforgeVersion = versionConfig.neoforge_version
                    if (neoforgeVersion != null && neoforgeVersion.isNotEmpty()) {
                        add("neoForge", "net.neoforged:neoforge:$neoforgeVersion")
                    }
                    project.logger.lifecycle("Configured NeoForge dependencies for Minecraft ${versionConfig.minecraft_version}")
                }
            }

            // Add testing dependencies
            add("testImplementation", platform("org.junit:junit-bom:5.10.1"))
            add("testImplementation", "org.junit.jupiter:junit-jupiter")
            add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        }
    }
}
