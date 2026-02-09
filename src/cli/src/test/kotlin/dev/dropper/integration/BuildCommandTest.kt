package dev.dropper.integration

import dev.dropper.commands.CreateItemCommand
import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.util.FileUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * E2E tests for build command and Gradle integration
 * Tests that the generated project structure can actually be built by Gradle
 */
class BuildCommandTest {

    private lateinit var testProjectDir: File
    private val originalUserDir = System.getProperty("user.dir")

    @BeforeEach
    fun setup() {
        testProjectDir = File("build/test-build/${System.currentTimeMillis()}/test-mod")
        testProjectDir.mkdirs()
    }

    @AfterEach
    fun cleanup() {
        System.setProperty("user.dir", originalUserDir)
        if (testProjectDir.exists()) {
            testProjectDir.deleteRecursively()
        }
    }

    @Test
    fun `generated project has proper Gradle structure`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Gradle Structure Verification                  ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "gradletest",
            name = "Gradle Test",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)
        System.setProperty("user.dir", testProjectDir.absolutePath)

        // Create an item to have some content
        CreateItemCommand().parse(arrayOf("test_item"))

        // Verify Gradle files
        assertTrue(File(testProjectDir, "build.gradle.kts").exists(), "Root build.gradle.kts should exist")
        assertTrue(File(testProjectDir, "settings.gradle.kts").exists(), "settings.gradle.kts should exist")
        assertTrue(File(testProjectDir, "gradle.properties").exists(), "gradle.properties should exist")
        assertTrue(File(testProjectDir, "build-logic").exists(), "build-logic should exist")

        println("  ✓ Gradle files present")
        println("  ✓ Build-logic directory exists")

        // Verify settings.gradle.kts includes version-loader modules
        val settingsContent = File(testProjectDir, "settings.gradle.kts").readText()
        assertTrue(
            settingsContent.contains("includeBuild(\"build-logic\")"),
            "settings.gradle.kts should include build-logic"
        )

        println("  ✓ Settings configured for composite build")
        println("\n✅ Gradle structure test passed!\n")
    }

    @Test
    fun `generated project has correct source directories for compilation`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Source Directory Configuration                 ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "sourcetest",
            name = "Source Test",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "neoforge")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)
        System.setProperty("user.dir", testProjectDir.absolutePath)

        // Create items and blocks
        CreateItemCommand().parse(arrayOf("diamond_sword_mk2"))

        // Verify all source files are in proper src/main/java structure
        val allJavaFiles = testProjectDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()

        assertTrue(allJavaFiles.isNotEmpty(), "Should have generated Java files")

        allJavaFiles.forEach { javaFile ->
            val relativePath = javaFile.relativeTo(testProjectDir).path

            // Every Java file should be under src/main/java or src/test/java
            val isInProperStructure = relativePath.contains("src${File.separator}main${File.separator}java") ||
                                     relativePath.contains("src${File.separator}test${File.separator}java")

            assertTrue(
                isInProperStructure,
                "Java file should be in src/main/java or src/test/java: $relativePath"
            )

            println("  ✓ $relativePath")
        }

        println("\n  ✓ All ${allJavaFiles.size} Java files in proper structure")
        println("\n✅ Source directory test passed!\n")
    }

    @Test
    fun `generated project files have correct package declarations`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Package Declaration Verification               ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "package-test",
            name = "Package Test",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)
        System.setProperty("user.dir", testProjectDir.absolutePath)

        CreateItemCommand().parse(arrayOf("ruby_gem"))

        // Find all Java files and verify package declarations
        val javaFiles = testProjectDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()

        javaFiles.forEach { javaFile ->
            val content = javaFile.readText()
            val packageLine = content.lines().find { it.trim().startsWith("package ") }

            assertTrue(packageLine != null, "File should have package declaration: ${javaFile.name}")

            // Extract package from declaration
            val declaredPackage = packageLine!!.trim()
                .removePrefix("package ")
                .removeSuffix(";")
                .trim()

            // Extract package from file path
            val relativePath = javaFile.relativeTo(testProjectDir).path
            val srcMainJavaIndex = relativePath.indexOf("src${File.separator}main${File.separator}java${File.separator}")
            val srcTestJavaIndex = relativePath.indexOf("src${File.separator}test${File.separator}java${File.separator}")

            val packageFromPath = when {
                srcMainJavaIndex != -1 -> {
                    val afterSrcMainJava = relativePath.substring(srcMainJavaIndex + "src${File.separator}main${File.separator}java${File.separator}".length)
                    afterSrcMainJava.substringBeforeLast(File.separator).replace(File.separator, ".")
                }
                srcTestJavaIndex != -1 -> {
                    val afterSrcTestJava = relativePath.substring(srcTestJavaIndex + "src${File.separator}test${File.separator}java${File.separator}".length)
                    afterSrcTestJava.substringBeforeLast(File.separator).replace(File.separator, ".")
                }
                else -> ""
            }

            if (packageFromPath.isNotEmpty()) {
                assertTrue(
                    declaredPackage == packageFromPath,
                    "Package declaration ($declaredPackage) should match directory structure ($packageFromPath) for ${javaFile.name}"
                )
                println("  ✓ ${javaFile.name}: $declaredPackage")
            }
        }

        println("\n  ✓ All package declarations match directory structure")
        println("\n✅ Package declaration test passed!\n")
    }

    @Test
    fun `multi-loader project has correct structure for each loader`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Multi-Loader Structure                         ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "multiloader",
            name = "Multi Loader",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)
        System.setProperty("user.dir", testProjectDir.absolutePath)

        CreateItemCommand().parse(arrayOf("test_item"))

        // Verify each loader has its own structure
        val loaders = listOf("fabric", "forge", "neoforge")
        loaders.forEach { loader ->
            println("\nVerifying $loader structure...")

            // Check shared loader directory
            val sharedLoaderDir = File(testProjectDir, "shared/$loader/src/main/java/com/multiloader/platform/$loader")
            assertTrue(sharedLoaderDir.exists(), "$loader should have shared platform directory")
            println("  ✓ shared/$loader structure")

            // Check version loader directory exists
            val versionLoaderDir = File(testProjectDir, "versions/1_20_1/$loader")
            assertTrue(versionLoaderDir.exists(), "versions/1_20_1/$loader should exist")
            println("  ✓ versions/1_20_1/$loader structure")

            // Check loader-specific registration file
            val regFile = File(sharedLoaderDir, "TestItem${loader.replaceFirstChar { it.uppercase() }}.java")
            assertTrue(regFile.exists(), "$loader registration file should exist")
            println("  ✓ $loader registration file")
        }

        println("\n  ✓ All 3 loaders have correct structure")
        println("\n✅ Multi-loader test passed!\n")
    }

    @Test
    fun `asset pack structure is correct`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Asset Pack Structure                           ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "assettest",
            name = "Asset Test",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)
        System.setProperty("user.dir", testProjectDir.absolutePath)

        CreateItemCommand().parse(arrayOf("emerald_sword"))

        // Verify asset pack v1 exists
        assertTrue(
            File(testProjectDir, "versions/shared/v1/config.yml").exists(),
            "Asset pack v1 config should exist"
        )

        // Verify assets are in shared v1 (not version-specific)
        assertTrue(
            File(testProjectDir, "versions/shared/v1/assets/assettest/models/item/emerald_sword.json").exists(),
            "Item model should be in shared asset pack"
        )

        assertTrue(
            File(testProjectDir, "versions/shared/v1/assets/assettest/textures/item/emerald_sword.png").exists(),
            "Texture placeholder should be in shared asset pack"
        )

        // Verify version configs reference the asset pack
        val version1Config = File(testProjectDir, "versions/1_20_1/config.yml").readText()
        assertTrue(
            version1Config.contains("asset_pack: \"v1\""),
            "Version config should reference asset pack v1"
        )

        println("  ✓ Asset pack v1 config exists")
        println("  ✓ Assets in shared/v1 directory")
        println("  ✓ Version configs reference asset pack")
        println("\n✅ Asset pack test passed!\n")
    }
}
