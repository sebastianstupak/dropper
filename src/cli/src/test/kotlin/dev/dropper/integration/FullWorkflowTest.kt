package dev.dropper.integration

import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateEntityCommand
import dev.dropper.config.ModConfig
import dev.dropper.util.FileUtil
import dev.dropper.util.TestProjectContext
import dev.dropper.util.TestValidationUtils
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import kotlin.test.assertTrue

/**
 * Full end-to-end workflow test that:
 * 1. Runs the actual CLI to generate a project
 * 2. Generates items in the project
 * 3. Builds the project with Gradle
 * 4. Verifies JARs are created
 *
 * This test uses the actual CLI commands (not just generators directly)
 * and verifies the complete workflow works.
 */
class FullWorkflowTest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("test-mod")
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_FULL_BUILD", matches = "true")
    fun `full workflow - generate project and build JARs`() {
        // This test actually builds the generated project with Gradle
        // It's slow (5-10 minutes) so only runs when RUN_FULL_BUILD=true

        // Step 1: Generate project using generators (simulating CLI)
        println("\n═══════════════════════════════════════════════════════════")
        println("FULL E2E WORKFLOW TEST - Including Gradle Build")
        println("═══════════════════════════════════════════════════════════\n")

        println("Step 1: Generating project...")
        generateTestProject()

        // Step 2: Verify project structure
        println("Step 2: Verifying project structure...")
        verifyProjectStructure()

        // Step 3: Copy gradle wrapper (TODO: should be generated)
        println("Step 3: Setting up Gradle wrapper...")
        copyGradleWrapper()

        // Step 4: Build the project with Gradle
        println("Step 4: Building project with Gradle (this takes 5-10 minutes)...")
        buildProject()

        // Step 5: Verify JARs were created
        println("Step 5: Verifying JAR outputs...")
        verifyJars()

        println("\n✓ Full workflow test completed successfully!")
        println("  Generated project builds and produces working JARs!\n")
    }

    private fun copyGradleWrapper() {
        // Copy gradle wrapper from root to generated project
        // TODO: ProjectGenerator should do this
        val gradleDir = File("gradle")
        val gradlewFile = File("gradlew")
        val gradlewBatFile = File("gradlew.bat")

        if (gradleDir.exists()) {
            FileUtil.copyDirectory(gradleDir, context.file("gradle"))
        }
        if (gradlewFile.exists()) {
            gradlewFile.copyTo(context.file("gradlew"), overwrite = true)
            // Make executable on Unix
            if (!System.getProperty("os.name").lowercase().contains("windows")) {
                Runtime.getRuntime().exec(arrayOf("chmod", "+x", context.file("gradlew").absolutePath)).waitFor()
            }
        }
        if (gradlewBatFile.exists()) {
            gradlewBatFile.copyTo(context.file("gradlew.bat"), overwrite = true)
        }
    }

    @Test
    fun `lightweight workflow - generate and verify structure`() {
        // This is the default test that always runs
        // It doesn't build with Gradle (too slow for regular CI)

        println("Generating test project...")
        generateTestProject()

        println("Verifying project structure...")
        verifyProjectStructure()

        println("Verifying build files are valid...")
        verifyBuildFilesValid()

        println("✓ Lightweight workflow test completed successfully!")
    }

    private fun generateTestProject() {
        val config = dev.dropper.config.ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "A test mod for E2E testing",
            author = "Test Author",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "neoforge")
        )

        context.createProject(config)

        assertTrue(context.projectDir.exists(), "Project directory should exist")
    }

    private fun verifyProjectStructure() {
        // Essential files
        val essentialFiles = listOf(
            "config.yml",
            "build.gradle.kts",
            "settings.gradle.kts",
            "gradle.properties",
            ".gitignore",
            "README.md",
            "AGENTS.md"
        )

        essentialFiles.forEach { file ->
            assertTrue(
                context.file( file).exists(),
                "Essential file should exist: $file"
            )
        }

        // Directory structure
        val essentialDirs = listOf(
            "shared/common/src/main/java/com/testmod",
            "shared/neoforge/src/main/java/com/testmod/platform",
            "shared/fabric/src/main/java/com/testmod/platform",
            "versions/shared/v1/assets/testmod",
            "versions/shared/v1/data/testmod",
            "versions/1_20_1",
            "build-logic"
        )

        essentialDirs.forEach { dir ->
            assertTrue(
                context.file( dir).exists(),
                "Essential directory should exist: $dir"
            )
        }

        // Generated Java files (Architectury: no Services.java or PlatformHelper.java)
        val javaFiles = listOf(
            "shared/common/src/main/java/com/testmod/TestMod.java"
        )

        javaFiles.forEach { file ->
            assertTrue(context.file(file).exists(), "Java file should exist: $file")
            assertTrue(context.file(file).readText().contains("package com.testmod"), "Should have correct package")
        }
    }

    private fun verifyBuildFilesValid() {
        // Verify config.yml is valid YAML
        val configContent = context.file("config.yml").readText()
        assertTrue(configContent.contains("id: testmod"), "config.yml should be valid")

        // Verify build.gradle.kts is valid Kotlin
        val buildGradleContent = context.file("build.gradle.kts").readText()
        assertTrue(buildGradleContent.contains("plugins"), "build.gradle.kts should be valid")

        // Verify settings.gradle.kts is valid
        val settingsContent = context.file("settings.gradle.kts").readText()
        assertTrue(settingsContent.contains("rootProject.name"), "settings.gradle.kts should be valid")

        // Verify AGENTS.md was generated with content
        val agentsContent = context.file("AGENTS.md").readText()
        assertTrue(agentsContent.contains("# Project Structure Guide"), "AGENTS.md should have content")
        assertTrue(agentsContent.contains("fabricmc.net"), "AGENTS.md should have Fabric docs link")
        assertTrue(agentsContent.contains("neoforged.net"), "AGENTS.md should have NeoForge docs link")
    }

    private fun buildProject() {
        // This method actually runs Gradle build on the generated project
        val gradlewCmd = if (System.getProperty("os.name").lowercase().contains("windows")) {
            context.file("gradlew.bat").absolutePath
        } else {
            context.file("gradlew").absolutePath
        }

        // Make gradlew executable on Unix
        if (!System.getProperty("os.name").lowercase().contains("windows")) {
            Runtime.getRuntime().exec(arrayOf("chmod", "+x", gradlewCmd)).waitFor()
        }

        // Run build (this will take several minutes)
        println("Running: $gradlewCmd build")
        println("(This may take 5-10 minutes as Gradle downloads dependencies...)")

        val process = ProcessBuilder(gradlewCmd, "build", "--no-daemon", "--console=plain")
            .directory(context.projectDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        // Capture output
        val output = process.inputStream.bufferedReader().readText()
        val errors = process.errorStream.bufferedReader().readText()

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            println("=== Gradle Output ===")
            println(output)
            println("=== Gradle Errors ===")
            println(errors)
            throw AssertionError("Gradle build failed with exit code $exitCode")
        }

        println("✓ Gradle build completed successfully")
    }

    private fun verifyJars() {
        // Check that JARs were created
        val expectedJars = listOf(
            "build/1_20_1/fabric.jar",
            "build/1_20_1/neoforge.jar"
        )

        expectedJars.forEach { jarPath ->
            val jarFile = context.file( jarPath)
            assertTrue(jarFile.exists(), "JAR should exist: $jarPath")
            assertTrue(jarFile.length() > 0, "JAR should not be empty: $jarPath")
            println("✓ Created: $jarPath (${jarFile.length() / 1024}KB)")
        }
    }

    @Test
    fun `verify gradle wrapper can be generated`() {
        // TODO: Implement gradle wrapper generation in ProjectGenerator
        // For now, just verify the build-logic directory exists
        generateTestProject()

        val buildLogicDir = context.file( "build-logic")
        assertTrue(buildLogicDir.exists(), "build-logic directory should exist")
        assertTrue(buildLogicDir.isDirectory, "build-logic should be a directory")

        // Note: Gradle wrapper generation will be added in a future update
        // For now, users can run: gradle wrapper
        println("Note: Gradle wrapper generation not yet implemented")
        println("Users can run 'gradle wrapper' in the generated project")
    }

    @Test
    fun `verify generated project has valid package structure`() {
        generateTestProject()

        // Verify main mod class
        val modClassFile = context.file( "shared/common/src/main/java/com/testmod/TestMod.java")
        val modClassContent = modClassFile.readText()

        assertTrue(modClassContent.contains("package com.testmod;"), "Should have correct package")
        assertTrue(modClassContent.contains("class TestMod"), "Should have TestMod class")
        assertTrue(modClassContent.contains("MOD_ID"), "Should have MOD_ID constant")

        // Verify per-loader entry points exist (Architectury replaces Services/PlatformHelper)
        assertTrue(
            context.file("shared/fabric/src/main/java/com/testmod/platform").exists(),
            "Fabric platform directory should exist"
        )
        assertTrue(
            context.file("shared/neoforge/src/main/java/com/testmod/platform").exists(),
            "NeoForge platform directory should exist"
        )
    }

    @Test
    fun `verify asset pack configuration is correct`() {
        generateTestProject()

        val assetPackConfig = context.file( "versions/shared/v1/config.yml")
        assertTrue(assetPackConfig.exists(), "Asset pack config should exist")

        val content = assetPackConfig.readText()
        assertTrue(content.contains("asset_pack:"), "Should have asset_pack section")
        assertTrue(content.contains("version: \"v1\""), "Should have version")
        assertTrue(content.contains("1.20.1"), "Should include MC version")
    }

    @Test
    fun `verify complete file tree after generating items blocks and entities`() {
        generateTestProject()

        // Generate components
        CreateItemCommand().parse(arrayOf("ruby_gem"))
        CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))
        CreateEntityCommand().parse(arrayOf("ruby_golem"))

        // Verify item files exist (Architectury: registry in common)
        assertTrue(
            context.file("shared/common/src/main/java/com/testmod/items/RubyGem.java").exists(),
            "Common item class should exist"
        )
        assertTrue(
            context.file("shared/common/src/main/java/com/testmod/registry/ModItems.java").exists(),
            "ModItems registry should exist"
        )

        // Verify block files
        assertTrue(
            context.file("shared/common/src/main/java/com/testmod/blocks/RubyOre.java").exists(),
            "Common block class should exist"
        )
        assertTrue(
            context.file("shared/common/src/main/java/com/testmod/registry/ModBlocks.java").exists(),
            "ModBlocks registry should exist"
        )

        // Verify entity files
        assertTrue(
            context.file("shared/common/src/main/java/com/testmod/entities/RubyGolem.java").exists(),
            "Common entity class should exist"
        )
        assertTrue(
            context.file("shared/common/src/main/java/com/testmod/registry/ModEntities.java").exists(),
            "ModEntities registry should exist"
        )

        // Verify assets
        assertTrue(
            context.file("versions/shared/v1/assets/testmod/models/item/ruby_gem.json").exists(),
            "Item model should exist"
        )
        assertTrue(
            context.file("versions/shared/v1/assets/testmod/blockstates/ruby_ore.json").exists(),
            "Block blockstate should exist"
        )
        assertTrue(
            context.file("versions/shared/v1/data/testmod/loot_tables/blocks/ruby_ore.json").exists(),
            "Block loot table should exist"
        )

        // Validate all generated Java files
        val javaCount = TestValidationUtils.validateAllJavaFiles(context.projectDir)
        assertTrue(javaCount > 0, "Should have validated Java files")

        // Validate all generated JSON files
        val jsonCount = TestValidationUtils.validateAllJsonFiles(context.projectDir)
        assertTrue(jsonCount > 0, "Should have validated JSON files")

        println("Verified complete file tree: $javaCount Java files, $jsonCount JSON files")
    }

    @Test
    fun `verify build gradle has proper Architectury Loom configuration`() {
        generateTestProject()

        // Root build.gradle.kts
        val rootBuildGradle = context.file("build.gradle.kts").readText()
        assertTrue(
            rootBuildGradle.contains("maven.architectury.dev"),
            "Root build.gradle.kts should reference Architectury Maven"
        )
        assertTrue(
            rootBuildGradle.contains("subprojects"),
            "Root build.gradle.kts should configure subprojects"
        )

        // build-logic has Architectury Loom as a dependency
        val buildLogicGradle = context.file("build-logic/build.gradle.kts").readText()
        assertTrue(
            buildLogicGradle.contains("architectury-loom"),
            "build-logic should have Architectury Loom dependency"
        )
        assertTrue(
            buildLogicGradle.contains("maven.architectury.dev"),
            "build-logic should have Architectury Maven repository"
        )
    }

    @Test
    fun `verify settings gradle discovers version-loader subprojects`() {
        generateTestProject()

        val settingsContent = context.file("settings.gradle.kts").readText()

        // settings.gradle.kts should dynamically include subprojects
        assertTrue(
            settingsContent.contains("rootProject.name"),
            "settings.gradle.kts should set rootProject.name"
        )
        assertTrue(
            settingsContent.contains("\"testmod\""),
            "rootProject.name should be set to the mod ID"
        )
        assertTrue(
            settingsContent.contains("versionsDir") || settingsContent.contains("versions"),
            "settings.gradle.kts should reference versions directory"
        )
        assertTrue(
            settingsContent.contains("include("),
            "settings.gradle.kts should include subprojects dynamically"
        )
        assertTrue(
            settingsContent.contains("loaders") || settingsContent.contains("fabric"),
            "settings.gradle.kts should reference loaders"
        )
    }

    @Test
    fun `verify all generated Java files pass syntax validation`() {
        generateTestProject()

        // Generate items and blocks to have more files to validate
        CreateItemCommand().parse(arrayOf("test_sword", "--type", "tool"))
        CreateBlockCommand().parse(arrayOf("test_block"))

        val javaFiles = context.projectDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()

        assertTrue(javaFiles.size >= 5, "Should have at least 5 Java files")

        javaFiles.forEach { file ->
            val content = file.readText()
            val relativePath = file.relativeTo(context.projectDir).path

            // Syntax validation
            TestValidationUtils.assertValidJavaSyntax(content, relativePath)
            TestValidationUtils.assertClassNameMatchesFile(content, file.name)
            TestValidationUtils.assertPackageMatchesPath(content, file.absolutePath, context.projectDir.absolutePath)
        }

        println("All ${javaFiles.size} Java files pass syntax validation")
    }
}
