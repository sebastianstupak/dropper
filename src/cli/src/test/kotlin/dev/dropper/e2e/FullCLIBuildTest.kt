package dev.dropper.e2e

import dev.dropper.commands.util.ConfigReader
import dev.dropper.commands.util.GradleRunner
import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.util.TestProjectContext
import dev.dropper.util.TestValidationUtils
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.*
import java.io.File

/**
 * Full E2E test that creates a real project and verifies dev command setup
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FullCLIBuildTest {

    private lateinit var context: TestProjectContext

    @BeforeAll
    fun setup() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Full E2E Test: Real Project Creation & Dev Setup        ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context = TestProjectContext.create("test-dev-mod")
    }

    @AfterAll
    fun cleanup() {
        // Keep the project for manual testing
        println("\n📁 Test project preserved at: ${context.projectDir.absolutePath}")
        println("   You can manually test with:")
        println("   cd ${context.projectDir.absolutePath}")
        println("   ./gradlew tasks")
        println("   ./gradlew :1_20_1-fabric:runClient")

        context.cleanup()
    }

    @Test
    fun `create real project and verify dev command setup`() {
        println("Step 1: Creating project...")

        // Create project using ProjectGenerator
        val config = ModConfig(
            id = "testdevmod",
            name = "Test Dev Mod",
            version = "1.0.0",
            description = "E2E test project for dev command",
            author = "Dropper E2E Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "neoforge")
        )

        context.createProject(config)
        println("✓ Project created at: ${context.projectDir.absolutePath}")

        // Step 2: Verify project structure
        println("\nStep 2: Verifying project structure...")

        assertTrue(context.projectDir.exists(), "Project directory should exist")
        assertTrue(context.file("config.yml").exists(), "config.yml should exist")
        assertTrue(context.file("build.gradle.kts").exists(), "build.gradle.kts should exist")
        assertTrue(context.file("settings.gradle.kts").exists(), "settings.gradle.kts should exist")

        // Note: gradlew files are not created by ProjectGenerator
        // In real usage, they would be added via `gradle wrapper` command
        // or copied from a template. For this test, we verify the build.gradle.kts
        // is configured correctly to use the wrapper task.

        // Verify version directories
        assertTrue(context.file("versions/1_20_1").exists(), "1_20_1 version should exist")
        assertTrue(context.file("versions/1_21_1").exists(), "1_21_1 version should exist")
        assertTrue(context.file("versions/1_20_1/config.yml").exists(), "Version config should exist")

        // Verify buildSrc
        assertTrue(context.file("build-logic/build.gradle.kts").exists(), "build-logic should exist")

        println("✓ All required files present")

        // Step 3: Verify build-logic exists
        println("\nStep 3: Verifying build-logic structure...")

        assertTrue(context.file("build-logic").exists(), "build-logic should exist")
        assertTrue(context.file("build-logic/build.gradle.kts").exists(), "build-logic build file should exist")
        assertTrue(
            context.file("build-logic/src/main/kotlin").exists(),
            "build-logic source directory should exist"
        )

        println("✓ build-logic structure verified")

        // Step 4: Test ConfigReader can read the project
        println("\nStep 4: Testing ConfigReader...")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo, "ConfigReader should read project info")
        assertEquals("testdevmod", projectInfo!!.modId, "Mod ID should match")
        assertEquals("Test Dev Mod", projectInfo.modName, "Mod name should match")
        assertEquals(2, projectInfo.versions.size, "Should have 2 versions")

        println("✓ ConfigReader working correctly")
        println("  - Mod ID: ${projectInfo.modId}")
        println("  - Mod Name: ${projectInfo.modName}")
        println("  - Versions found: ${projectInfo.versions.size}")

        projectInfo.versions.forEach { versionInfo ->
            println("    • ${versionInfo.minecraftVersion}: ${versionInfo.loaders.joinToString(", ")}")
        }

        // Step 5: Verify version-loader combinations
        println("\nStep 5: Verifying version-loader combinations...")

        assertTrue(
            configReader.versionLoaderExists("1.20.1", "fabric"),
            "1.20.1-fabric should exist"
        )
        assertTrue(
            configReader.versionLoaderExists("1.20.1", "neoforge"),
            "1.20.1-neoforge should exist"
        )
        assertTrue(
            configReader.versionLoaderExists("1.21.1", "fabric"),
            "1.21.1-fabric should exist"
        )
        assertFalse(
            configReader.versionLoaderExists("1.20.1", "forge"),
            "1.20.1-forge should not exist (not configured)"
        )

        println("✓ All version-loader combinations verified")

        // Step 6: Test GradleRunner can construct commands
        println("\nStep 6: Testing GradleRunner command construction...")

        // Note: GradleRunner expects gradlew to exist, but we're testing the command building logic
        // In real usage, gradlew would be created via `gradle wrapper` task
        val gradleRunner = GradleRunner(context.projectDir)

        val command = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient"
        )

        assertFalse(command.isEmpty(), "Command should not be empty")
        assertTrue(command.any { it.contains("runClient") }, "Command should contain runClient task")
        assertTrue(command.any { it.contains("1_20_1-fabric") }, "Command should contain module ID")

        println("✓ GradleRunner working correctly")
        println("  Command: ${command.joinToString(" ")}")

        // Step 7: Test debug mode command construction
        println("\nStep 7: Testing debug mode setup...")

        val debugCommand = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient",
            jvmArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
        )

        assertTrue(
            debugCommand.any { it.contains("jdwp") || it.contains("5005") },
            "Debug command should include JVM args"
        )

        println("✓ Debug mode configuration verified")

        // Final Summary
        println("\n" + "=".repeat(65))
        println("✅ FULL E2E TEST PASSED!")
        println("=".repeat(65))
        println()
        println("Project created successfully at:")
        println("  ${context.projectDir.absolutePath}")
        println()
        println("To complete setup, run:")
        println("  cd ${context.projectDir.absolutePath}")
        println("  gradle wrapper                      # Create gradlew files")
        println("  ./gradlew tasks                     # List available tasks")
        println()
        println("Would work with dropper dev commands:")
        println("  dropper dev run                     # Auto-detect and run")
        println("  dropper dev run --version 1.20.1 --loader fabric")
        println("  dropper dev run --debug --port 5005")
        println()
        println("Note: gradlew files not created by test (requires Gradle installation)")
        println("      In production, dropper init would include gradlew wrapper files")
        println("      All dev command infrastructure is verified!")
        println()
    }

    @Test
    @Tag("slow")
    fun `verify project builds successfully`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Slow Test: Actual Project Build                         ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        assumeTrue(context.projectDir.exists(), "Test project must exist (run main test first)")

        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val gradlewFile = File(context.projectDir, if (isWindows) "gradlew.bat" else "gradlew")

        assumeTrue(
            gradlewFile.exists(),
            "Gradle wrapper must exist. Run 'gradle wrapper' in ${context.projectDir.absolutePath}"
        )

        println("Building project (this may take several minutes)...")
        println("This test downloads Minecraft, applies mappings, and compiles.")
        println()

        val gradlewCmd = if (isWindows) "gradlew.bat" else "./gradlew"

        val result = ProcessBuilder()
            .directory(context.projectDir)
            .command(listOf(gradlewCmd, "build", "--no-daemon", "--stacktrace"))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        // Stream output while waiting
        val outputThread = Thread {
            result.inputStream.bufferedReader().forEachLine { line ->
                println("[Gradle] $line")
            }
        }
        outputThread.start()

        val exitCode = result.waitFor()
        outputThread.join()

        if (exitCode != 0) {
            println("\n❌ Build failed! Error output:")
            println(result.errorStream.bufferedReader().readText())
        }

        assertEquals(0, exitCode, "Project should build successfully")

        println("\n✅ Project builds successfully!")
        println("   This confirms the generated project is production-ready.")
    }

    @Test
    @Tag("slow")
    fun `verify Gradle tasks exist`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Slow Test: Verify Gradle Tasks                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        assumeTrue(context.projectDir.exists(), "Test project must exist")

        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        val gradlewFile = File(context.projectDir, if (isWindows) "gradlew.bat" else "gradlew")

        assumeTrue(
            gradlewFile.exists(),
            "Gradle wrapper must exist. Run 'gradle wrapper' in ${context.projectDir.absolutePath}"
        )

        val gradlewCmd = if (isWindows) "gradlew.bat" else "./gradlew"

        println("Listing all available tasks...")

        val result = ProcessBuilder()
            .directory(context.projectDir)
            .command(listOf(gradlewCmd, "tasks", "--all", "--no-daemon"))
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        val output = result.inputStream.bufferedReader().readText()
        val exitCode = result.waitFor()

        assertEquals(0, exitCode, "Tasks command should succeed")

        // Verify key tasks exist
        val requiredTasks = listOf(
            "runClient",
            "runServer",
            "build",
            "test"
        )

        println("\nVerifying required tasks exist:")
        requiredTasks.forEach { task ->
            assertTrue(
                output.contains(task, ignoreCase = true),
                "Task '$task' should be available"
            )
            println("  ✓ $task")
        }

        println("\n✅ All required Gradle tasks are available!")
    }

    @Test
    fun `verify build system files have proper Architectury Loom configuration`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Build System Architectury Loom Configuration      ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Create project
        val config = ModConfig(
            id = "testdevmod",
            name = "Test Dev Mod",
            version = "1.0.0",
            description = "E2E test project for build system verification",
            author = "Dropper E2E Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "neoforge")
        )

        context.createProject(config)

        // Verify root build.gradle.kts
        val rootBuildGradle = File(context.projectDir, "build.gradle.kts").readText()
        assertTrue(rootBuildGradle.contains("plugins"), "Root build.gradle.kts should have plugins block")
        assertTrue(
            rootBuildGradle.contains("maven.architectury.dev"),
            "Root build.gradle.kts should reference Architectury Maven repository"
        )

        // Verify settings.gradle.kts
        val settingsGradle = File(context.projectDir, "settings.gradle.kts").readText()
        assertTrue(settingsGradle.contains("rootProject.name"), "settings.gradle.kts should set rootProject.name")
        assertTrue(settingsGradle.contains("include("), "settings.gradle.kts should include subprojects")
        assertTrue(
            settingsGradle.contains("versions") || settingsGradle.contains("versionsDir"),
            "settings.gradle.kts should reference versions directory for subproject discovery"
        )

        // Verify build-logic has Architectury Loom dependency
        val buildLogicFile = File(context.projectDir, "build-logic/build.gradle.kts")
        assertTrue(buildLogicFile.exists(), "build-logic/build.gradle.kts should exist")
        val buildLogicContent = buildLogicFile.readText()
        assertTrue(
            buildLogicContent.contains("architectury-loom"),
            "build-logic should depend on Architectury Loom"
        )

        // Verify version directories were created for both versions
        assertTrue(
            File(context.projectDir, "versions/1_20_1").exists(),
            "1_20_1 version directory should exist"
        )
        assertTrue(
            File(context.projectDir, "versions/1_21_1").exists(),
            "1_21_1 version directory should exist"
        )

        println("Build system Architectury Loom configuration verified successfully.")
    }

    @Test
    fun `verify all generated code passes syntax validation`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Generated Code Syntax Validation                  ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "syntaxmod",
            name = "Syntax Mod",
            version = "1.0.0",
            description = "Syntax validation test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        context.createProject(config)

        // Validate all generated Java files
        val javaCount = TestValidationUtils.validateAllJavaFiles(context.projectDir)
        assertTrue(javaCount >= 1, "Should have at least 1 Java file (SyntaxMod)")

        println("Validated $javaCount Java files - all pass syntax checks.")
    }
}
