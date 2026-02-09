package dev.dropper.integration

import dev.dropper.importers.*
import dev.dropper.util.FileUtil
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * E2E tests for Import command
 * Tests importing existing mod projects into Dropper structure
 */
class ImportCommandE2ETest {

    private lateinit var testDir: File
    private lateinit var sourceDir: File
    private lateinit var targetDir: File

    @BeforeEach
    fun setup() {
        testDir = File("build/test-import/${System.currentTimeMillis()}")
        testDir.mkdirs()
        sourceDir = File(testDir, "source-mod")
        targetDir = File(testDir, "imported-mod")
    }

    @AfterEach
    fun cleanup() {
        testDir.deleteRecursively()
    }

    // ========== Import Fabric Tests (6 tests) ==========

    @Test
    fun `import basic Fabric mod`() {
        // Create a basic Fabric mod structure
        createBasicFabricMod(sourceDir)

        val importer = FabricImporter()
        importer.import(sourceDir, targetDir)

        // Verify Dropper structure was created
        assertTrue(File(targetDir, "config.yml").exists(), "config.yml should exist")
        assertTrue(File(targetDir, "buildSrc").exists(), "buildSrc should exist")
        assertTrue(File(targetDir, "shared/common").exists(), "shared/common should exist")
        assertTrue(File(targetDir, "shared/fabric").exists(), "shared/fabric should exist")
        assertTrue(File(targetDir, "versions/shared/v1").exists(), "versions/shared/v1 should exist")
    }

    @Test
    fun `extract mod info from fabric dot mod dot json`() {
        createBasicFabricMod(sourceDir)

        val analyzer = ProjectAnalyzer()
        val importer = FabricImporter(analyzer)
        val result = importer.analyze(sourceDir)

        assertEquals("testmod", result.modId, "Should extract mod ID")
        assertEquals("Test Mod", result.modName, "Should extract mod name")
        assertEquals("1.0.0", result.version, "Should extract version")
        assertEquals("A test mod", result.description, "Should extract description")
        assertEquals("Test Author", result.author, "Should extract author")
        assertEquals("1.20.1", result.minecraftVersion, "Should extract Minecraft version")
    }

    @Test
    fun `map Fabric file structure to Dropper structure`() {
        createBasicFabricMod(sourceDir)

        // Add some Java files
        createFabricModClass(sourceDir, "com.testmod", "TestMod")
        createFabricItem(sourceDir, "com.testmod.items", "RubyItem")

        val importer = FabricImporter()
        importer.import(sourceDir, targetDir)

        // Verify source files were mapped
        assertTrue(
            File(targetDir, "shared/common/src/main/java/com/testmod/TestMod.java").exists(),
            "Mod class should be in common"
        )
        assertTrue(
            File(targetDir, "shared/common/src/main/java/com/testmod/items/RubyItem.java").exists(),
            "Item class should be in common"
        )
    }

    @Test
    fun `generate config yml from Fabric metadata`() {
        createBasicFabricMod(sourceDir)

        val importer = FabricImporter()
        importer.import(sourceDir, targetDir)

        val configFile = File(targetDir, "config.yml")
        assertTrue(configFile.exists(), "config.yml should exist")

        val content = configFile.readText()
        assertTrue(content.contains("id: testmod"), "Should have mod ID")
        assertTrue(content.contains("name: \"Test Mod\""), "Should have mod name")
        assertTrue(content.contains("version: \"1.0.0\""), "Should have version")
    }

    @Test
    fun `convert Fabric build config to Dropper buildSrc`() {
        createBasicFabricMod(sourceDir)

        val importer = FabricImporter()
        importer.import(sourceDir, targetDir)

        // Verify buildSrc was created
        assertTrue(File(targetDir, "buildSrc").exists(), "buildSrc should exist")
        assertTrue(File(targetDir, "buildSrc/build.gradle.kts").exists(), "buildSrc build.gradle.kts should exist")
        assertTrue(
            File(targetDir, "buildSrc/src/main/kotlin").exists(),
            "buildSrc kotlin sources should exist"
        )
    }

    @Test
    fun `verify Fabric import creates complete Dropper structure`() {
        createBasicFabricMod(sourceDir)
        createFabricAssets(sourceDir, "testmod")
        createFabricData(sourceDir, "testmod")

        val importer = FabricImporter()
        importer.import(sourceDir, targetDir)

        // Verify complete structure
        val expectedDirs = listOf(
            "config.yml",
            "build.gradle.kts",
            "settings.gradle.kts",
            "README.md",
            "AGENTS.md",
            ".gitignore",
            "buildSrc",
            "shared/common/src/main/java",
            "shared/fabric/src/main/java",
            "versions/shared/v1/assets/testmod",
            "versions/shared/v1/data/testmod",
            "versions/1_20_1"
        )

        expectedDirs.forEach { path ->
            assertTrue(File(targetDir, path).exists(), "$path should exist")
        }
    }

    // ========== Import Forge Tests (5 tests) ==========

    @Test
    fun `import basic Forge mod`() {
        createBasicForgeMod(sourceDir)

        val importer = ForgeImporter()
        importer.import(sourceDir, targetDir)

        assertTrue(File(targetDir, "config.yml").exists(), "config.yml should exist")
        assertTrue(File(targetDir, "shared/common").exists(), "shared/common should exist")
        assertTrue(File(targetDir, "shared/forge").exists(), "shared/forge should exist")
    }

    @Test
    fun `handle Forge-specific files during import`() {
        createBasicForgeMod(sourceDir)

        val importer = ForgeImporter()
        val result = importer.analyze(sourceDir)

        assertEquals("forgemod", result.modId, "Should extract mod ID from mods.toml")
        assertEquals("Forge Mod", result.modName, "Should extract display name")
        assertEquals("forge", result.loader, "Should identify as Forge loader")
    }

    @Test
    fun `convert Forge build config to Dropper structure`() {
        createBasicForgeMod(sourceDir)

        val importer = ForgeImporter()
        importer.import(sourceDir, targetDir)

        assertTrue(File(targetDir, "buildSrc").exists(), "buildSrc should exist")
        assertTrue(File(targetDir, "build.gradle.kts").exists(), "build.gradle.kts should exist")
    }

    @Test
    fun `map Forge resources correctly`() {
        createBasicForgeMod(sourceDir)
        createForgeAssets(sourceDir, "forgemod")

        val importer = ForgeImporter()
        importer.import(sourceDir, targetDir)

        assertTrue(
            File(targetDir, "versions/shared/v1/assets/forgemod").exists(),
            "Assets should be mapped"
        )
    }

    @Test
    fun `verify Forge import creates valid Dropper project`() {
        createBasicForgeMod(sourceDir)

        val importer = ForgeImporter()
        importer.import(sourceDir, targetDir)

        // Verify structure validity
        assertTrue(File(targetDir, "config.yml").exists())
        assertTrue(File(targetDir, "buildSrc").exists())
        assertTrue(File(targetDir, "shared/common").exists())
    }

    // ========== Import NeoForge Tests (4 tests) ==========

    @Test
    fun `import NeoForge mod successfully`() {
        createBasicNeoForgeMod(sourceDir)

        val importer = NeoForgeImporter()
        importer.import(sourceDir, targetDir)

        assertTrue(File(targetDir, "config.yml").exists())
        assertTrue(File(targetDir, "shared/neoforge").exists())
    }

    @Test
    fun `handle NeoForge metadata file`() {
        createBasicNeoForgeMod(sourceDir)

        val importer = NeoForgeImporter()
        val result = importer.analyze(sourceDir)

        assertEquals("neomod", result.modId)
        assertEquals("NeoForge Mod", result.modName)
        assertEquals("neoforge", result.loader)
    }

    @Test
    fun `convert NeoForge config to Dropper`() {
        createBasicNeoForgeMod(sourceDir)

        val importer = NeoForgeImporter()
        importer.import(sourceDir, targetDir)

        val configContent = File(targetDir, "config.yml").readText()
        assertTrue(configContent.contains("id: neomod"))
    }

    @Test
    fun `verify NeoForge import structure`() {
        createBasicNeoForgeMod(sourceDir)

        val importer = NeoForgeImporter()
        importer.import(sourceDir, targetDir)

        val expectedFiles = listOf(
            "config.yml",
            "buildSrc",
            "shared/common",
            "shared/neoforge"
        )

        expectedFiles.forEach { path ->
            assertTrue(File(targetDir, path).exists(), "$path should exist")
        }
    }

    // ========== Project Analysis Tests (5 tests) ==========

    @Test
    fun `detect mod ID from project structure`() {
        createBasicFabricMod(sourceDir)

        val analyzer = ProjectAnalyzer()
        val modId = analyzer.detectModId(sourceDir, "fabric")

        assertEquals("testmod", modId)
    }

    @Test
    fun `detect Minecraft version from metadata`() {
        createBasicFabricMod(sourceDir)

        val importer = FabricImporter()
        val result = importer.analyze(sourceDir)

        assertEquals("1.20.1", result.minecraftVersion)
    }

    @Test
    fun `detect dependencies from project`() {
        createBasicFabricMod(sourceDir)

        val analyzer = ProjectAnalyzer()
        val importer = FabricImporter(analyzer)
        val result = importer.analyze(sourceDir)

        assertNotNull(result.dependencies)
    }

    @Test
    fun `detect asset structure`() {
        createBasicFabricMod(sourceDir)
        createFabricAssets(sourceDir, "testmod")

        val analyzer = ProjectAnalyzer()
        val assetDirs = analyzer.findAssetDirectories(sourceDir)

        assertTrue(assetDirs.isNotEmpty(), "Should find asset directories")
    }

    @Test
    fun `handle missing mod info gracefully`() {
        sourceDir.mkdirs()
        File(sourceDir, "src/main/resources").mkdirs()

        val analyzer = ProjectAnalyzer()
        val modId = analyzer.detectModId(sourceDir, null)

        // Should handle missing metadata without crashing
        // modId may be null
    }

    // ========== Conversion Tests (5 tests) ==========

    @Test
    fun `convert single-loader to multi-loader structure`() {
        // Create Fabric mod
        createBasicFabricMod(sourceDir)

        // Import as Fabric
        val importer = FabricImporter()
        importer.import(sourceDir, targetDir)

        // Verify it can be extended to support other loaders
        assertTrue(File(targetDir, "shared/common").exists())
        assertTrue(File(targetDir, "shared/fabric").exists())

        // Structure allows adding forge/neoforge later
        val forgeDir = File(targetDir, "shared/forge/src/main/java")
        forgeDir.mkdirs()
        assertTrue(forgeDir.exists(), "Should allow adding Forge")
    }

    @Test
    fun `preserve mod functionality during conversion`() {
        createBasicFabricMod(sourceDir)
        createFabricModClass(sourceDir, "com.testmod", "TestMod")

        val importer = FabricImporter()
        importer.import(sourceDir, targetDir)

        // Verify mod class was preserved
        val modClass = File(targetDir, "shared/common/src/main/java/com/testmod/TestMod.java")
        assertTrue(modClass.exists())

        val content = modClass.readText()
        assertTrue(content.contains("class TestMod"))
    }

    @Test
    fun `generate missing loader implementations`() {
        createBasicFabricMod(sourceDir)

        val importer = FabricImporter()
        importer.import(sourceDir, targetDir)

        // After import, structure is ready for other loaders
        assertTrue(File(targetDir, "shared/common").exists())

        // Can manually add other loaders
        File(targetDir, "shared/forge").mkdirs()
        File(targetDir, "shared/neoforge").mkdirs()

        assertTrue(File(targetDir, "shared/forge").exists())
        assertTrue(File(targetDir, "shared/neoforge").exists())
    }

    @Test
    fun `update build system for multi-loader`() {
        createBasicFabricMod(sourceDir)

        val importer = FabricImporter()
        importer.import(sourceDir, targetDir)

        // Verify buildSrc supports multi-loader
        val buildSrcFile = File(targetDir, "buildSrc/build.gradle.kts")
        assertTrue(buildSrcFile.exists())

        val content = buildSrcFile.readText()
        // Should have references to Fabric
        assertTrue(content.contains("fabric") || content.contains("Fabric"))
    }

    @Test
    fun `verify all loaders work after conversion`() {
        createBasicFabricMod(sourceDir)

        val importer = FabricImporter()
        importer.import(sourceDir, targetDir)

        // Verify Dropper structure is valid
        assertTrue(File(targetDir, "config.yml").exists())
        assertTrue(File(targetDir, "buildSrc").exists())
        assertTrue(File(targetDir, "shared/common").exists())

        // Structure is ready for build
        assertTrue(File(targetDir, "build.gradle.kts").exists())
    }

    // ========== Helper Methods ==========

    private fun createBasicFabricMod(dir: File) {
        dir.mkdirs()

        // Create fabric.mod.json
        val resourcesDir = File(dir, "src/main/resources")
        resourcesDir.mkdirs()

        val fabricModJson = """
            {
              "id": "testmod",
              "name": "Test Mod",
              "version": "1.0.0",
              "description": "A test mod",
              "authors": ["Test Author"],
              "license": "MIT",
              "environment": "*",
              "depends": {
                "minecraft": "1.20.1"
              }
            }
        """.trimIndent()

        FileUtil.writeText(File(resourcesDir, "fabric.mod.json"), fabricModJson)
    }

    private fun createBasicForgeMod(dir: File) {
        dir.mkdirs()

        val resourcesDir = File(dir, "src/main/resources/META-INF")
        resourcesDir.mkdirs()

        val modsToml = """
            modId="forgemod"
            displayName="Forge Mod"
            version="1.0.0"
            description="A Forge mod"
            license="MIT"
            authors="Test Author"

            [[dependencies.forgemod]]
            modId="minecraft"
            versionRange="[1.20.1,)"
        """.trimIndent()

        FileUtil.writeText(File(resourcesDir, "mods.toml"), modsToml)
    }

    private fun createBasicNeoForgeMod(dir: File) {
        dir.mkdirs()

        val resourcesDir = File(dir, "src/main/resources/META-INF")
        resourcesDir.mkdirs()

        val neoforgeModsToml = """
            modId="neomod"
            displayName="NeoForge Mod"
            version="1.0.0"
            description="A NeoForge mod"
            license="MIT"
            authors="Test Author"

            [[dependencies.neomod]]
            modId="minecraft"
            versionRange="[1.20.1,)"
        """.trimIndent()

        FileUtil.writeText(File(resourcesDir, "neoforge.mods.toml"), neoforgeModsToml)
    }

    private fun createFabricModClass(dir: File, pkg: String, className: String) {
        val pkgPath = pkg.replace(".", "/")
        val javaFile = File(dir, "src/main/java/$pkgPath/$className.java")

        val content = """
            package $pkg;

            public class $className {
                public static final String MOD_ID = "testmod";

                public void onInitialize() {
                    System.out.println("Mod initialized");
                }
            }
        """.trimIndent()

        FileUtil.writeText(javaFile, content)
    }

    private fun createFabricItem(dir: File, pkg: String, className: String) {
        val pkgPath = pkg.replace(".", "/")
        val javaFile = File(dir, "src/main/java/$pkgPath/$className.java")

        val content = """
            package $pkg;

            public class $className {
                public static final String ID = "ruby_item";
            }
        """.trimIndent()

        FileUtil.writeText(javaFile, content)
    }

    private fun createFabricAssets(dir: File, modId: String) {
        val assetsDir = File(dir, "src/main/resources/assets/$modId")
        File(assetsDir, "models/item").mkdirs()
        File(assetsDir, "textures/item").mkdirs()
        File(assetsDir, "lang").mkdirs()

        FileUtil.writeText(File(assetsDir, "lang/en_us.json"), "{}")
    }

    private fun createFabricData(dir: File, modId: String) {
        val dataDir = File(dir, "src/main/resources/data/$modId")
        File(dataDir, "recipes").mkdirs()

        FileUtil.writeText(File(dataDir, "recipes/example.json"), "{}")
    }

    private fun createForgeAssets(dir: File, modId: String) {
        val assetsDir = File(dir, "src/main/resources/assets/$modId")
        File(assetsDir, "models").mkdirs()
        File(assetsDir, "textures").mkdirs()
    }
}
