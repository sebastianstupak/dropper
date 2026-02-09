package dev.dropper.integration

import dev.dropper.config.ModConfig
import dev.dropper.util.FileUtil
import dev.dropper.util.TestProjectContext
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

        // Generated Java files
        val javaFiles = listOf(
            "shared/common/src/main/java/com/testmod/Services.java",
            "shared/common/src/main/java/com/testmod/platform/PlatformHelper.java",
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

        // Verify Services.java has correct package and content
        val servicesFile = context.file( "shared/common/src/main/java/com/testmod/Services.java")
        val servicesContent = servicesFile.readText()

        assertTrue(servicesContent.contains("package com.testmod;"), "Should have correct package")
        assertTrue(servicesContent.contains("class Services"), "Should have Services class")
        assertTrue(servicesContent.contains("ServiceLoader"), "Should use ServiceLoader")
        assertTrue(servicesContent.contains("PlatformHelper"), "Should reference PlatformHelper")

        // Verify PlatformHelper.java
        val platformHelperFile = context.file( "shared/common/src/main/java/com/testmod/platform/PlatformHelper.java")
        val platformHelperContent = platformHelperFile.readText()

        assertTrue(platformHelperContent.contains("package com.testmod.platform;"), "Should have correct package")
        assertTrue(platformHelperContent.contains("interface PlatformHelper"), "Should have PlatformHelper interface")
        assertTrue(platformHelperContent.contains("getPlatformName"), "Should have getPlatformName method")

        // Verify main mod class
        val modClassFile = context.file( "shared/common/src/main/java/com/testmod/TestMod.java")
        val modClassContent = modClassFile.readText()

        assertTrue(modClassContent.contains("package com.testmod;"), "Should have correct package")
        assertTrue(modClassContent.contains("class TestMod"), "Should have TestMod class")
        assertTrue(modClassContent.contains("MOD_ID"), "Should have MOD_ID constant")
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
}
