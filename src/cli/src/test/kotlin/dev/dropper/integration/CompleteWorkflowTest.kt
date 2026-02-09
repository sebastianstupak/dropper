package dev.dropper.integration

import dev.dropper.commands.BuildCommand
import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.InitCommand
import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * Complete E2E workflow tests for all Dropper commands
 * Tests: init, create item, create block, build with proper src/main/java structure
 */
class CompleteWorkflowTest {

    private lateinit var testProjectDir: File
    private val originalUserDir = System.getProperty("user.dir")

    @BeforeEach
    fun setup() {
        testProjectDir = File("build/test-workflow/${System.currentTimeMillis()}/test-mod")
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
    fun `complete workflow - init, create items and blocks, verify structure`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Complete Workflow                              ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Step 1: Initialize project
        println("Step 1: Initializing project...")
        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "Test mod for E2E workflow",
            author = "Test Author",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)
        System.setProperty("user.dir", testProjectDir.absolutePath)

        println("  ✓ Project initialized")

        // Verify root config
        val configFile = File(testProjectDir, "config.yml")
        assertTrue(configFile.exists(), "config.yml should exist")
        println("  ✓ config.yml created")

        // Verify shared directory structure with src/main/java
        verifyProperJavaStructure()
        println("  ✓ Proper src/main/java structure created")

        // Step 2: Create items
        println("\nStep 2: Creating items...")
        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        CreateItemCommand().parse(arrayOf("ruby_sword", "--type", "tool"))
        println("  ✓ Items created")

        // Step 3: Create blocks
        println("\nStep 3: Creating blocks...")
        CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))
        CreateBlockCommand().parse(arrayOf("ruby_block", "--type", "basic"))
        println("  ✓ Blocks created")

        // Step 4: Verify all files have proper structure
        println("\nStep 4: Verifying file structure...")
        verifyItemStructure("ruby", "Ruby")
        verifyItemStructure("ruby_sword", "RubySword")
        verifyBlockStructure("ruby_ore", "RubyOre")
        verifyBlockStructure("ruby_block", "RubyBlock")
        println("  ✓ All files verified")

        // Step 5: Verify src/main/java structure for all modules
        println("\nStep 5: Verifying module structure...")
        verifyModuleStructure()
        println("  ✓ Module structure verified")

        println("\n✅ Complete workflow test passed!")
        println("  - Project initialized with proper structure")
        println("  - 2 items created (ruby, ruby_sword)")
        println("  - 2 blocks created (ruby_ore, ruby_block)")
        println("  - All files use src/main/java structure")
        println("  - Ready for IDE import\n")
    }

    @Test
    fun `init command creates proper directory structure`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Init Command                                   ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "mymod",
            name = "My Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "neoforge")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)

        // Verify shared/common structure
        assertTrue(
            File(testProjectDir, "shared/common/src/main/java/com/mymod").exists(),
            "shared/common should have src/main/java/com/mymod"
        )
        assertTrue(
            File(testProjectDir, "shared/common/src/test/java/com/mymod").exists(),
            "shared/common should have src/test/java/com/mymod"
        )

        // Verify shared/fabric structure
        assertTrue(
            File(testProjectDir, "shared/fabric/src/main/java/com/mymod/platform").exists(),
            "shared/fabric should have src/main/java/com/mymod/platform"
        )

        // Verify shared/neoforge structure
        assertTrue(
            File(testProjectDir, "shared/neoforge/src/main/java/com/mymod/platform").exists(),
            "shared/neoforge should have src/main/java/com/mymod/platform"
        )

        // Verify generated files exist
        assertTrue(
            File(testProjectDir, "shared/common/src/main/java/com/mymod/Services.java").exists(),
            "Services.java should be generated"
        )
        assertTrue(
            File(testProjectDir, "shared/common/src/main/java/com/mymod/platform/PlatformHelper.java").exists(),
            "PlatformHelper.java should be generated"
        )

        // Verify versions directory
        assertTrue(
            File(testProjectDir, "versions/shared/v1/config.yml").exists(),
            "Asset pack config should exist"
        )
        assertTrue(
            File(testProjectDir, "versions/1_20_1/config.yml").exists(),
            "Version config should exist"
        )

        println("  ✓ All directory structure verified")
        println("  ✓ Proper src/main/java structure")
        println("  ✓ Generated files exist")
        println("\n✅ Init command test passed!\n")
    }

    @Test
    fun `verify all generated files use proper package structure`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Package Structure Verification                 ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Create project
        val config = ModConfig(
            id = "packagetest",
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

        // Create item
        CreateItemCommand().parse(arrayOf("test_item"))

        // Verify package declaration in generated files
        val itemFile = File(testProjectDir, "shared/common/src/main/java/com/packagetest/items/TestItem.java")
        assertTrue(itemFile.exists(), "Item file should exist")

        val itemContent = itemFile.readText()
        assertTrue(
            itemContent.contains("package com.packagetest.items;"),
            "Item should have correct package declaration"
        )

        // Verify file is in correct directory matching package
        val expectedPath = "shared/common/src/main/java/com/packagetest/items/TestItem.java"
        assertTrue(
            itemFile.path.endsWith(expectedPath.replace("/", File.separator)),
            "File should be in directory matching package: $expectedPath"
        )

        println("  ✓ Package declarations correct")
        println("  ✓ File paths match package structure")
        println("\n✅ Package structure test passed!\n")
    }

    @Test
    fun `create item in multi-version project generates version-agnostic code`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Multi-Version Item Creation                    ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "multiversion",
            name = "Multi Version",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.20.4", "1.21.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)
        System.setProperty("user.dir", testProjectDir.absolutePath)

        CreateItemCommand().parse(arrayOf("shared_item"))

        // Verify item is created in shared/common (not version-specific)
        val itemFile = File(testProjectDir, "shared/common/src/main/java/com/multiversion/items/SharedItem.java")
        assertTrue(itemFile.exists(), "Item should be in shared/common")

        // Verify loader registrations exist for all loaders
        val loaders = listOf("fabric", "forge", "neoforge")
        loaders.forEach { loader ->
            val regFile = File(
                testProjectDir,
                "shared/$loader/src/main/java/com/multiversion/platform/$loader/SharedItem${loader.capitalize()}.java"
            )
            assertTrue(regFile.exists(), "$loader registration should exist")
        }

        // Verify assets are in shared v1 (version-agnostic)
        val modelFile = File(testProjectDir, "versions/shared/v1/assets/multiversion/models/item/shared_item.json")
        assertTrue(modelFile.exists(), "Model should be in shared asset pack")

        println("  ✓ Item created in shared/common (version-agnostic)")
        println("  ✓ All loader registrations created")
        println("  ✓ Assets in shared asset pack")
        println("\n✅ Multi-version test passed!\n")
    }

    @Test
    fun `verify IntelliJ IDEA compatibility of generated structure`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: IntelliJ IDEA Compatibility                    ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "idetest",
            name = "IDE Test",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)

        // Check for standard Maven/Gradle conventions
        val checks = listOf(
            "shared/common/src/main/java" to "Production source directory",
            "shared/common/src/test/java" to "Test source directory",
            "shared/fabric/src/main/java" to "Fabric source directory",
            "build.gradle.kts" to "Gradle build file",
            "settings.gradle.kts" to "Gradle settings file",
            "gradle.properties" to "Gradle properties"
        )

        checks.forEach { (path, description) ->
            val file = File(testProjectDir, path)
            assertTrue(file.exists(), "$description should exist at: $path")
            println("  ✓ $description")
        }

        // Verify NO flat file structure (old approach)
        val commonDir = File(testProjectDir, "shared/common")
        val javaFiles = commonDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()

        javaFiles.forEach { javaFile ->
            val relativePath = javaFile.relativeTo(testProjectDir).path
            assertTrue(
                relativePath.contains("src${File.separator}main${File.separator}java") ||
                relativePath.contains("src${File.separator}test${File.separator}java"),
                "Java file should be under src/main/java or src/test/java: $relativePath"
            )
        }

        println("  ✓ All Java files in proper src/main/java structure")
        println("  ✓ No flat files found")
        println("\n✅ IntelliJ IDEA compatibility test passed!\n")
    }

    private fun verifyProperJavaStructure() {
        val requiredDirs = listOf(
            "shared/common/src/main/java/com/testmod",
            "shared/common/src/test/java/com/testmod",
            "shared/fabric/src/main/java/com/testmod/platform",
            "shared/forge/src/main/java/com/testmod/platform",
            "shared/neoforge/src/main/java/com/testmod/platform"
        )

        requiredDirs.forEach { path ->
            val dir = File(testProjectDir, path)
            assertTrue(dir.exists(), "Directory should exist: $path")
        }
    }

    private fun verifyItemStructure(itemName: String, className: String) {
        // Common item
        assertTrue(
            File(testProjectDir, "shared/common/src/main/java/com/testmod/items/$className.java").exists(),
            "$className should exist in shared/common"
        )

        // Loader registrations
        listOf("fabric", "forge", "neoforge").forEach { loader ->
            val loaderClass = "${className}${loader.capitalize()}"
            assertTrue(
                File(testProjectDir, "shared/$loader/src/main/java/com/testmod/platform/$loader/$loaderClass.java").exists(),
                "$loaderClass should exist in shared/$loader"
            )
        }

        // Assets
        assertTrue(
            File(testProjectDir, "versions/shared/v1/assets/testmod/models/item/$itemName.json").exists(),
            "Item model should exist"
        )
    }

    private fun verifyBlockStructure(blockName: String, className: String) {
        // Common block
        assertTrue(
            File(testProjectDir, "shared/common/src/main/java/com/testmod/blocks/$className.java").exists(),
            "$className should exist in shared/common"
        )

        // Loader registrations
        listOf("fabric", "forge", "neoforge").forEach { loader ->
            val loaderClass = "${className}${loader.capitalize()}"
            assertTrue(
                File(testProjectDir, "shared/$loader/src/main/java/com/testmod/platform/$loader/$loaderClass.java").exists(),
                "$loaderClass should exist in shared/$loader"
            )
        }

        // Assets
        assertTrue(
            File(testProjectDir, "versions/shared/v1/assets/testmod/blockstates/$blockName.json").exists(),
            "Blockstate should exist"
        )
        assertTrue(
            File(testProjectDir, "versions/shared/v1/assets/testmod/models/block/$blockName.json").exists(),
            "Block model should exist"
        )
        assertTrue(
            File(testProjectDir, "versions/shared/v1/assets/testmod/models/item/$blockName.json").exists(),
            "Item model should exist"
        )
    }

    private fun verifyModuleStructure() {
        // Verify each module has proper structure
        val modules = listOf(
            "shared/common",
            "shared/fabric",
            "shared/forge",
            "shared/neoforge"
        )

        modules.forEach { module ->
            val srcMainJava = File(testProjectDir, "$module/src/main/java")
            assertTrue(srcMainJava.exists(), "$module should have src/main/java")

            // Verify at least one .java file exists in proper structure
            val javaFiles = srcMainJava.walkTopDown()
                .filter { it.isFile && it.extension == "java" }
                .toList()

            assertTrue(javaFiles.isNotEmpty(), "$module should have Java files in src/main/java")
        }
    }
}
