import config.ConfigLoader
import org.gradle.api.tasks.testing.Test
import tasks.AssemblePackagesTask
import tasks.GenerateMetadataTask
import utils.AssetPackResolver

plugins {
    java
}

// Extract version and loader from project name (e.g., "1_20_1-neoforge")
val projectParts = project.name.split("-")
require(projectParts.size == 2) { "Project name must be format: VERSION-LOADER (e.g., 1_20_1-neoforge)" }

val mcVersion = projectParts[0]
val loader = projectParts[1]

// Get root directory (parent of build-temp)
val rootDir = project.projectDir.parentFile.parentFile

// Load configurations
val rootConfig = ConfigLoader.loadRootConfig(rootDir)
val versionConfig = ConfigLoader.loadVersionConfig(rootDir, mcVersion)
val assetResolver = AssetPackResolver(rootDir)

// Set project properties
group = "com.${rootConfig.mod.id.replace("-", "")}"
version = rootConfig.mod.version

// Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(versionConfig.java_version))
    }
}

// Set up source assembly
val assemblePackages = tasks.register<AssemblePackagesTask>("assemblePackages") {
    rootDirectory.set(rootDir)
    mcVersion.set(mcVersion)
    this.loader.set(loader)
    basePackage.set("com.${rootConfig.mod.id.replace("-", "")}")
    outputDir.set(project.layout.buildDirectory.dir("assembled-src"))
}

// Set up metadata generation
val generateMetadata = tasks.register<GenerateMetadataTask>("generateMetadata") {
    this.loader.set(loader)
    this.rootConfig.set(rootConfig)
    this.versionConfig.set(versionConfig)
    outputDir.set(project.layout.buildDirectory.dir("generated-resources"))
}

// Configure sourceSets
sourceSets {
    main {
        java {
            // Use assembled sources
            setSrcDirs(listOf(assemblePackages.get().outputDir))
        }

        resources {
            // Asset pack inheritance chain (virtual overlay)
            assetResolver.resolveAssetDirs(mcVersion, loader).forEach {
                srcDir(it)
            }

            // Data pack inheritance chain
            assetResolver.resolveDataDirs(mcVersion, loader).forEach {
                srcDir(it)
            }

            // Generated metadata
            srcDir(generateMetadata.get().outputDir)
        }
    }

    test {
        java {
            // Tests are included in assembled sources (filtered by *Test.java)
            setSrcDirs(listOf(assemblePackages.get().outputDir))
        }
    }
}

// Task dependencies
tasks.named("compileJava") {
    dependsOn(assemblePackages)
}

tasks.named("processResources") {
    dependsOn(generateMetadata)
}

// Filter test sources
tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Configure JAR output
tasks.named<Jar>("jar") {
    archiveBaseName.set(rootConfig.mod.id)
    archiveVersion.set(rootConfig.mod.version)
    archiveClassifier.set("$mcVersion-$loader")

    // Exclude test classes
    exclude("**/*Test.class")

    doLast {
        // Copy to final location
        val outputDir = File(rootDir, "build/$mcVersion")
        outputDir.mkdirs()

        val outputFile = File(outputDir, "$loader.jar")
        archiveFile.get().asFile.copyTo(outputFile, overwrite = true)

        logger.lifecycle("Built: ${outputFile.relativeTo(rootDir)}")
    }
}

// Add dependencies
repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.minecraftforge.net/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    // Testing
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Log configuration for debugging
logger.lifecycle("Configured project ${project.name}:")
logger.lifecycle("  MC Version: $mcVersion")
logger.lifecycle("  Loader: $loader")
logger.lifecycle("  Asset Pack: ${versionConfig.asset_pack}")
logger.lifecycle("  Java: ${versionConfig.java_version}")
