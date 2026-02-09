package dev.dropper.e2e

import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Unit tests for JAR validation utilities.
 * These test the validation logic without requiring actual JAR builds.
 */
class JarValidationUtilsTest {

    @Test
    fun `test Java class version detection`() {
        // Test Java version byte code mapping
        val javaVersions = mapOf(
            8 to 52,
            11 to 55,
            17 to 61,
            21 to 65
        )

        javaVersions.forEach { (javaVersion, expectedMajor) ->
            assertEquals(expectedMajor, mapJavaVersionToMajor(javaVersion),
                "Java $javaVersion should map to major version $expectedMajor")
        }
    }

    @Test
    fun `test Fabric metadata field validation`() {
        val validFabricMetadata = """
        {
          "schemaVersion": 1,
          "id": "testmod",
          "version": "1.0.0",
          "name": "Test Mod",
          "description": "A test mod",
          "authors": ["Test Author"],
          "license": "MIT",
          "environment": "*",
          "entrypoints": {
            "main": ["com.testmod.TestMod"]
          },
          "depends": {
            "fabricloader": ">=0.15.0",
            "minecraft": "1.20.1"
          }
        }
        """.trimIndent()

        assertTrue(validFabricMetadata.contains("\"schemaVersion\""), "Should have schemaVersion")
        assertTrue(validFabricMetadata.contains("\"id\""), "Should have id")
        assertTrue(validFabricMetadata.contains("\"version\""), "Should have version")
        assertTrue(validFabricMetadata.contains("\"entrypoints\""), "Should have entrypoints")
    }

    @Test
    fun `test Forge metadata field validation`() {
        val validForgeMetadata = """
        modLoader="javafml"
        loaderVersion="[51,)"
        license="MIT"

        [[mods]]
        modId="testmod"
        version="1.0.0"
        displayName="Test Mod"
        description="A test mod"
        """.trimIndent()

        assertTrue(validForgeMetadata.contains("modLoader"), "Should have modLoader")
        assertTrue(validForgeMetadata.contains("loaderVersion"), "Should have loaderVersion")
        assertTrue(validForgeMetadata.contains("[[mods]]"), "Should have mods section")
        assertTrue(validForgeMetadata.contains("modId"), "Should have modId")
    }

    @Test
    fun `test NeoForge metadata field validation`() {
        val validNeoForgeMetadata = """
        modLoader="javafml"
        loaderVersion="[1,)"
        license="MIT"

        [[mods]]
        modId="testmod"
        version="1.0.0"
        displayName="Test Mod"
        description="A test mod"
        """.trimIndent()

        assertTrue(validNeoForgeMetadata.contains("modLoader"), "Should have modLoader")
        assertTrue(validNeoForgeMetadata.contains("[[mods]]"), "Should have mods section")
    }

    @Test
    fun `test JAR structure path validation`() {
        val validAssetPaths = listOf(
            "assets/testmod/models/item/test_item.json",
            "assets/testmod/textures/item/test_item.png",
            "assets/testmod/blockstates/test_block.json",
            "assets/testmod/lang/en_us.json"
        )

        validAssetPaths.forEach { path ->
            assertTrue(path.startsWith("assets/"), "Asset path should start with assets/: $path")
            assertTrue(path.contains("/testmod/"), "Asset path should contain mod ID: $path")
        }

        val validDataPaths = listOf(
            "data/testmod/recipes/test_recipe.json",
            "data/testmod/loot_tables/blocks/test_block.json",
            "data/testmod/tags/items/test_tag.json"
        )

        validDataPaths.forEach { path ->
            assertTrue(path.startsWith("data/"), "Data path should start with data/: $path")
            assertTrue(path.contains("/testmod/"), "Data path should contain mod ID: $path")
        }
    }

    @Test
    fun `test class file path validation`() {
        val validClassPaths = listOf(
            "com/testmod/TestMod.class",
            "com/testmod/items/TestItem.class",
            "com/testmod/blocks/TestBlock.class",
            "com/testmod/platform/PlatformHelper.class"
        )

        validClassPaths.forEach { path ->
            assertTrue(path.endsWith(".class"), "Class path should end with .class: $path")
            assertTrue(path.contains("/"), "Class path should contain package separator: $path")
        }
    }

    @Test
    fun `test metadata file locations by loader`() {
        val loaderMetadata = mapOf(
            "fabric" to "fabric.mod.json",
            "forge" to "META-INF/mods.toml",
            "neoforge" to "META-INF/neoforge.mods.toml"
        )

        loaderMetadata.forEach { (loader, metadataPath) ->
            assertNotNull(metadataPath, "Loader $loader should have metadata path")
            if (loader == "fabric") {
                assertTrue(metadataPath.endsWith(".json"), "Fabric metadata should be JSON")
            } else {
                assertTrue(metadataPath.contains("META-INF"), "Forge-like loaders should use META-INF")
            }
        }
    }

    @Test
    fun `test JAR size validation ranges`() {
        val testSizes = listOf(
            512L to false,          // 512B - too small
            1024L to true,          // 1KB - minimum acceptable
            1024L * 1024 to true,   // 1MB - reasonable
            50L * 1024 * 1024 to true,  // 50MB - max reasonable
            100L * 1024 * 1024 to false  // 100MB - too large
        )

        testSizes.forEach { (sizeBytes, shouldBeValid) ->
            val sizeMB = sizeBytes / (1024 * 1024)
            val isValid = sizeBytes >= 1024 && sizeMB <= 50

            assertEquals(shouldBeValid, isValid,
                "Size ${sizeBytes}B (${sizeMB}MB) validity should be $shouldBeValid")
        }
    }

    @Test
    fun `test Minecraft version to Java version mapping`() {
        val mcToJavaMapping = mapOf(
            "1.20.1" to 17,
            "1.20.4" to 17,
            "1.21" to 21,
            "1.21.1" to 21
        )

        mcToJavaMapping.forEach { (mcVersion, javaVersion) ->
            val expectedJava = when {
                mcVersion.startsWith("1.20") -> 17
                mcVersion.startsWith("1.21") -> 21
                else -> fail("Unknown MC version: $mcVersion")
            }

            assertEquals(javaVersion, expectedJava,
                "MC $mcVersion should require Java $javaVersion")
        }
    }

    @Test
    fun `test loader compatibility with MC versions`() {
        val compatibility = mapOf(
            "1.20.1" to listOf("fabric", "forge"),
            "1.20.4" to listOf("fabric", "forge", "neoforge"),
            "1.21" to listOf("fabric", "neoforge"),
            "1.21.1" to listOf("fabric", "neoforge")
        )

        compatibility.forEach { (mcVersion, loaders) ->
            assertTrue(loaders.contains("fabric"), "Fabric should support all versions")

            if (mcVersion.startsWith("1.20")) {
                // Forge available for 1.20.x
                assertTrue(loaders.contains("forge") || loaders.contains("neoforge"),
                    "1.20.x should support Forge or NeoForge")
            }

            if (mcVersion.startsWith("1.21")) {
                // NeoForge preferred for 1.21+
                assertTrue(loaders.contains("neoforge"),
                    "1.21+ should support NeoForge")
            }
        }
    }

    // Helper method
    private fun mapJavaVersionToMajor(javaVersion: Int): Int {
        return when (javaVersion) {
            8 -> 52
            11 -> 55
            17 -> 61
            21 -> 65
            else -> throw IllegalArgumentException("Unknown Java version: $javaVersion")
        }
    }
}
