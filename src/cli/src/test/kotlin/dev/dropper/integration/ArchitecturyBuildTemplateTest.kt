package dev.dropper.integration

import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Tests that generated build templates use Architectury Loom correctly.
 */
class ArchitecturyBuildTemplateTest {

    @TempDir
    lateinit var tempDir: File

    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        projectDir = File(tempDir, "test-mod")
        val config = ModConfig(
            id = "testmod",
            name = "TestMod",
            version = "1.0.0",
            description = "Test mod",
            author = "test",
            license = "MIT",
            minecraftVersions = listOf("1.21.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )
        ProjectGenerator().generate(projectDir, config)
    }

    @Test
    fun `buildSrc uses architectury loom dependency`() {
        val buildGradle = File(projectDir, "buildSrc/build.gradle.kts")
        assertTrue(buildGradle.exists(), "buildSrc/build.gradle.kts should exist")

        val content = buildGradle.readText()
        assertTrue(content.contains("architectury-loom"), "Should depend on architectury-loom")
        assertFalse(content.contains("fabric-loom"), "Should NOT depend on raw fabric-loom")
        assertFalse(content.contains("ForgeGradle"), "Should NOT depend on ForgeGradle")
    }

    @Test
    fun `buildSrc has architectury maven repository`() {
        val buildGradle = File(projectDir, "buildSrc/build.gradle.kts")
        val content = buildGradle.readText()
        assertTrue(content.contains("maven.architectury.dev"), "Should have Architectury Maven repo")
    }

    @Test
    fun `build-logic uses architectury loom dependency`() {
        val buildGradle = File(projectDir, "build-logic/build.gradle.kts")
        assertTrue(buildGradle.exists(), "build-logic/build.gradle.kts should exist")

        val content = buildGradle.readText()
        assertTrue(content.contains("architectury-loom"), "Should depend on architectury-loom")
        assertFalse(content.contains("fabric-loom"), "Should NOT depend on raw fabric-loom")
    }

    @Test
    fun `root build gradle has architectury maven`() {
        val buildGradle = File(projectDir, "build.gradle.kts")
        assertTrue(buildGradle.exists(), "build.gradle.kts should exist")

        val content = buildGradle.readText()
        assertTrue(content.contains("maven.architectury.dev"), "Should have Architectury Maven repo")
    }

    @Test
    fun `version config includes architectury api version`() {
        val versionConfig = File(projectDir, "versions/1_21_1/config.yml")
        assertTrue(versionConfig.exists(), "Version config should exist")

        val content = versionConfig.readText()
        assertTrue(content.contains("architectury_api_version"), "Should include architectury_api_version")
    }

    @Test
    fun `version config has correct java version for 1_21`() {
        val versionConfig = File(projectDir, "versions/1_21_1/config.yml")
        val content = versionConfig.readText()
        assertTrue(content.contains("java_version: 21"), "1.21.x should require Java 21")
    }

    @Test
    fun `ModLoaderPlugin uses architectury loom`() {
        val modLoaderPlugin = File(projectDir, "buildSrc/src/main/kotlin/ModLoaderPlugin.kt")
        assertTrue(modLoaderPlugin.exists(), "ModLoaderPlugin.kt should exist")

        val content = modLoaderPlugin.readText()
        assertTrue(content.contains("dev.architectury.loom"), "Should apply architectury loom plugin")
        assertTrue(content.contains("maven.architectury.dev"), "Should have Architectury Maven repo")
        assertTrue(content.contains("officialMojangMappings"), "Should use Mojang mappings")
    }

    @Test
    fun `ConfigModels includes architectury api version field`() {
        val configModels = File(projectDir, "buildSrc/src/main/kotlin/config/ConfigModels.kt")
        assertTrue(configModels.exists(), "ConfigModels.kt should exist")

        val content = configModels.readText()
        assertTrue(content.contains("architectury_api_version"), "Should have architectury_api_version field")
    }

    @Test
    fun `generated project has per-loader entry points`() {
        val fabricEntry = File(projectDir, "shared/fabric/src/main/java/com/testmod/platform/fabric/TestModFabric.java")
        val forgeEntry = File(projectDir, "shared/forge/src/main/java/com/testmod/platform/forge/TestModForge.java")
        val neoforgeEntry = File(projectDir, "shared/neoforge/src/main/java/com/testmod/platform/neoforge/TestModNeoForge.java")

        assertTrue(fabricEntry.exists(), "Fabric entry point should exist")
        assertTrue(forgeEntry.exists(), "Forge entry point should exist")
        assertTrue(neoforgeEntry.exists(), "NeoForge entry point should exist")

        val fabricContent = fabricEntry.readText()
        assertTrue(fabricContent.contains("ModInitializer"), "Fabric entry should implement ModInitializer")
        assertTrue(fabricContent.contains("TestMod.init()"), "Fabric entry should call common init()")

        val forgeContent = forgeEntry.readText()
        assertTrue(forgeContent.contains("@Mod"), "Forge entry should have @Mod annotation")
    }

    @Test
    fun `generated project does NOT have Services or PlatformHelper`() {
        val servicesFile = File(projectDir, "shared/common/src/main/java/com/testmod/Services.java")
        val platformHelperFile = File(projectDir, "shared/common/src/main/java/com/testmod/platform/PlatformHelper.java")

        assertFalse(servicesFile.exists(), "Services.java should NOT exist (replaced by Architectury)")
        assertFalse(platformHelperFile.exists(), "PlatformHelper.java should NOT exist (replaced by Architectury)")
    }
}
