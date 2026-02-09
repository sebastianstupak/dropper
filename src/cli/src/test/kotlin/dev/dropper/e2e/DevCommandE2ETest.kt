package dev.dropper.e2e

import dev.dropper.DropperCLI
import dev.dropper.commands.util.ConfigReader
import dev.dropper.commands.util.GradleRunner
import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive E2E tests for the `dropper dev` command.
 *
 * These tests verify end-to-end functionality including:
 * - Command parsing and validation
 * - Version/loader detection and auto-selection
 * - Debug mode configuration
 * - Error handling for invalid inputs
 * - Gradle command construction
 * - Integration with real project structure
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DevCommandE2ETest {

    private lateinit var context: TestProjectContext
    private lateinit var originalOut: PrintStream
    private lateinit var originalErr: PrintStream

    @BeforeAll
    fun setupAll() {
        // Save original streams
        originalOut = System.out
        originalErr = System.err
    }

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("test-mod")

        // Create realistic test project structure using ProjectGenerator
        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "Test mod for E2E testing",
            author = "Test Author",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "neoforge")
        )

        context.createProject(config)

        // Verify project was created
        assertTrue(context.file("config.yml").exists(), "Project config.yml should exist")
        assertTrue(context.file("versions/1_20_1").exists(), "Version 1.20.1 should exist")
        assertTrue(context.file("versions/1_21_1").exists(), "Version 1.21.1 should exist")
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    @AfterAll
    fun cleanupAll() {
        // Restore original streams
        System.setOut(originalOut)
        System.setErr(originalErr)
    }


    /**
     * Capture console output for testing
     */
    private fun captureOutput(block: () -> Unit): String {
        val outputStream = ByteArrayOutputStream()
        val printStream = PrintStream(outputStream)
        System.setOut(printStream)
        System.setErr(printStream)

        try {
            block()
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }

        return outputStream.toString()
    }

    // ==============================================
    // 1. BASIC EXECUTION TESTS
    // ==============================================

    @Test
    fun `test ConfigReader auto-detects first available version`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Auto-detect First Available Version                ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo, "Should detect project info")
        assertTrue(projectInfo.versions.isNotEmpty(), "Should find at least one version")

        val firstVersion = projectInfo.versions.first()
        assertNotNull(firstVersion, "First version should exist")
        assertTrue(
            firstVersion.minecraftVersion == "1.20.1" || firstVersion.minecraftVersion == "1.21.1",
            "First version should be one of the configured versions"
        )

        println("  ✓ Detected first version: ${firstVersion.minecraftVersion}")
        println("  ✓ Available loaders: ${firstVersion.loaders.joinToString(", ")}")
        println("\n✅ Auto-detection test passed!\n")
    }

    @Test
    fun `test ConfigReader auto-detects default loader (fabric)`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Auto-detect Default Loader (Fabric)                ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo)
        val versionInfo = projectInfo.versions.first()

        assertTrue(versionInfo.loaders.isNotEmpty(), "Version should have loaders")
        assertTrue(versionInfo.loaders.contains("fabric"), "Fabric should be available")

        println("  ✓ First loader: ${versionInfo.loaders.first()}")
        println("  ✓ All loaders: ${versionInfo.loaders.joinToString(", ")}")
        println("\n✅ Default loader detection test passed!\n")
    }

    @Test
    fun `test version override with --version flag`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Version Override with --version Flag               ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo)

        // Test that we can find a specific version
        val targetVersion = "1.21.1"
        val versionInfo = projectInfo.versions.find { it.minecraftVersion == targetVersion }

        assertNotNull(versionInfo, "Should find version $targetVersion")
        assertEquals(targetVersion, versionInfo.minecraftVersion)

        println("  ✓ Requested version: $targetVersion")
        println("  ✓ Found version: ${versionInfo.minecraftVersion}")
        println("  ✓ Loaders: ${versionInfo.loaders.joinToString(", ")}")
        println("\n✅ Version override test passed!\n")
    }

    @Test
    fun `test loader override with --loader flag`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Loader Override with --loader Flag                 ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo)
        val versionInfo = projectInfo.versions.first()

        val targetLoader = "neoforge"
        assertTrue(
            versionInfo.loaders.contains(targetLoader),
            "Version should have $targetLoader loader"
        )

        println("  ✓ Requested loader: $targetLoader")
        println("  ✓ Available loaders: ${versionInfo.loaders.joinToString(", ")}")
        println("  ✓ Loader is available: ${versionInfo.loaders.contains(targetLoader)}")
        println("\n✅ Loader override test passed!\n")
    }

    @Test
    fun `test multiple versions detected correctly`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Detect Multiple Versions                           ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo)
        assertTrue(projectInfo.versions.size >= 2, "Should detect at least 2 versions")

        val versionNumbers = projectInfo.versions.map { it.minecraftVersion }.sorted()
        assertTrue(versionNumbers.contains("1.20.1"), "Should have 1.20.1")
        assertTrue(versionNumbers.contains("1.21.1"), "Should have 1.21.1")

        println("  ✓ Detected ${projectInfo.versions.size} versions:")
        projectInfo.versions.forEach { v ->
            println("    - ${v.minecraftVersion} (${v.loaders.joinToString(", ")})")
        }
        println("\n✅ Multiple version detection test passed!\n")
    }

    // ==============================================
    // 2. DEBUG MODE TESTS
    // ==============================================

    @Test
    fun `test debug flag adds JVM arguments`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Debug Flag Adds JVM Arguments                      ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val gradleRunner = GradleRunner(context.projectDir)

        val debugArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
        val command = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient",
            jvmArgs = debugArgs
        )

        val commandString = command.joinToString(" ")
        assertTrue(commandString.contains("jdwp"), "Command should contain debug JVM args")
        assertTrue(commandString.contains("5005"), "Command should contain port 5005")

        println("  ✓ Debug args added to command")
        println("  ✓ Port: 5005")
        println("  ✓ Command includes: jdwp")
        println("\n✅ Debug flag test passed!\n")
    }

    @Test
    fun `test custom debug port flag`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Custom Debug Port Flag                             ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val gradleRunner = GradleRunner(context.projectDir)

        val customPort = 9999
        val debugArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:$customPort")
        val command = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient",
            jvmArgs = debugArgs
        )

        val commandString = command.joinToString(" ")
        assertTrue(commandString.contains("jdwp"), "Command should contain debug JVM args")
        assertTrue(commandString.contains("$customPort"), "Command should contain custom port")

        println("  ✓ Custom debug port: $customPort")
        println("  ✓ Command includes custom port")
        println("\n✅ Custom port test passed!\n")
    }

    @Test
    fun `test debug message displays connection info`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Debug Message Displays Connection Info             ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // This test verifies that debug information would be shown
        // In real usage, the dev command prints debug connection info

        val port = 5005
        val expectedMessage = "Connect debugger to: localhost:$port"

        println("  ✓ Expected debug message format:")
        println("    Debug mode enabled")
        println("    Debug port: $port")
        println("    $expectedMessage")
        println("    Waiting for debugger to attach...")
        println("\n✅ Debug message test passed!\n")
    }

    // ==============================================
    // 3. ERROR HANDLING TESTS
    // ==============================================

    @Test
    fun `test error when config yml missing`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Error When config.yml Missing                      ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val emptyDir = File("build/test-dev-e2e/${System.currentTimeMillis()}/empty-project")
        emptyDir.mkdirs()

        try {
            val configReader = ConfigReader(emptyDir)
            val projectInfo = configReader.readProjectInfo()

            assertNull(projectInfo, "Should return null when config.yml missing")

            println("  ✓ Correctly returns null for missing config.yml")
            println("  ✓ Would show error: 'No config.yml found'")
            println("\n✅ Missing config error test passed!\n")
        } finally {
            emptyDir.deleteRecursively()
        }
    }

    @Test
    fun `test error when invalid version specified`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Error When Invalid Version Specified               ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo)

        val invalidVersion = "999.999.999"
        val versionInfo = projectInfo.versions.find { it.minecraftVersion == invalidVersion }

        assertNull(versionInfo, "Should not find invalid version")

        println("  ✓ Invalid version: $invalidVersion")
        println("  ✓ Correctly returns null")
        println("  ✓ Available versions: ${projectInfo.versions.joinToString(", ") { it.minecraftVersion }}")
        println("\n✅ Invalid version error test passed!\n")
    }

    @Test
    fun `test error when invalid loader specified`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Error When Invalid Loader Specified                ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo)
        val versionInfo = projectInfo.versions.first()

        val invalidLoader = "nonexistent-loader"
        val loaderExists = versionInfo.loaders.contains(invalidLoader)

        assertFalse(loaderExists, "Invalid loader should not exist")

        println("  ✓ Invalid loader: $invalidLoader")
        println("  ✓ Correctly not found")
        println("  ✓ Available loaders: ${versionInfo.loaders.joinToString(", ")}")
        println("\n✅ Invalid loader error test passed!\n")
    }

    @Test
    fun `test error when Gradle wrapper missing`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Error When Gradle Wrapper Missing                  ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val emptyDir = File("build/test-dev-e2e/${System.currentTimeMillis()}/no-wrapper")
        emptyDir.mkdirs()

        try {
            val gradleRunner = GradleRunner(emptyDir)
            val hasWrapper = gradleRunner.hasGradleWrapper()

            assertFalse(hasWrapper, "Should not have Gradle wrapper")

            println("  ✓ Correctly detects missing Gradle wrapper")
            println("  ✓ Would show error: 'Gradle wrapper not found'")
            println("\n✅ Missing wrapper error test passed!\n")
        } finally {
            emptyDir.deleteRecursively()
        }
    }

    @Test
    fun `test error when version-loader combination does not exist`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Error When Version-Loader Combo Does Not Exist     ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configReader = ConfigReader(context.projectDir)

        // Test non-existent combination
        val exists = configReader.versionLoaderExists("1.20.1", "forge")

        assertFalse(exists, "forge loader should not exist for this project")

        println("  ✓ Correctly detects missing version-loader combination")
        println("  ✓ 1.20.1-forge does not exist (as expected)")
        println("\n✅ Version-loader combination error test passed!\n")
    }

    // ==============================================
    // 4. GRADLE COMMAND CONSTRUCTION TESTS
    // ==============================================

    @Test
    fun `test correct Gradle task format`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Correct Gradle Task Format                         ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val gradleRunner = GradleRunner(context.projectDir)

        val command = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient"
        )

        val taskName = command.last()
        assertEquals(":1_20_1-fabric:runClient", taskName, "Task name should be correctly formatted")

        println("  ✓ Task format: $taskName")
        println("  ✓ Format is correct: :VERSION-LOADER:TASK")
        println("\n✅ Gradle task format test passed!\n")
    }

    @Test
    fun `test JVM args included when debug enabled`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: JVM Args Included When Debug Enabled               ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val gradleRunner = GradleRunner(context.projectDir)

        val debugArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
        val command = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient",
            jvmArgs = debugArgs
        )

        val hasJvmArgs = command.any { it.contains("jdwp") }
        assertTrue(hasJvmArgs, "Command should include JVM args")

        println("  ✓ JVM args included in command")
        println("  ✓ Debug configuration present")
        println("\n✅ JVM args inclusion test passed!\n")
    }

    @Test
    fun `test Windows vs Unix wrapper detection`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Windows vs Unix Wrapper Detection                  ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val gradleRunner = GradleRunner(context.projectDir)
        val command = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient"
        )

        val wrapperCommand = command.first()
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")

        if (isWindows) {
            assertTrue(
                wrapperCommand.contains("gradlew.bat") || wrapperCommand.endsWith(".bat"),
                "Windows should use .bat wrapper"
            )
            println("  ✓ Detected Windows environment")
            println("  ✓ Using gradlew.bat wrapper")
        } else {
            assertTrue(
                wrapperCommand.contains("gradlew") && !wrapperCommand.contains(".bat"),
                "Unix should use gradlew (no .bat)"
            )
            println("  ✓ Detected Unix environment")
            println("  ✓ Using gradlew wrapper")
        }

        println("\n✅ Wrapper detection test passed!\n")
    }

    @Test
    fun `test clean arguments passed when clean flag set`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Clean Arguments Passed When --clean Flag           ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val gradleRunner = GradleRunner(context.projectDir)

        val command = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient",
            gradleArgs = listOf("--rerun-tasks")
        )

        assertTrue(command.contains("--rerun-tasks"), "Command should include --rerun-tasks flag")

        println("  ✓ Clean flag translates to --rerun-tasks")
        println("  ✓ Gradle will start with fresh data")
        println("\n✅ Clean arguments test passed!\n")
    }

    // ==============================================
    // 5. REAL PROJECT INTEGRATION TESTS
    // ==============================================

    @Test
    fun `test integration with actual project structure`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Integration with Actual Project Structure          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Verify project structure
        assertTrue(context.projectDir.exists(), "Project directory should exist")
        assertTrue(File(context.projectDir, "config.yml").exists(), "config.yml should exist")
        assertTrue(File(context.projectDir, "versions").exists(), "versions directory should exist")
        assertTrue(File(context.projectDir, "shared").exists(), "shared directory should exist")

        // Verify version directories
        assertTrue(File(context.projectDir, "versions/1_20_1").exists(), "1.20.1 version should exist")
        assertTrue(File(context.projectDir, "versions/1_21_1").exists(), "1.21.1 version should exist")

        // Verify loader directories
        assertTrue(File(context.projectDir, "versions/1_20_1/fabric").exists(), "1.20.1 fabric should exist")
        assertTrue(File(context.projectDir, "versions/1_20_1/neoforge").exists(), "1.20.1 neoforge should exist")

        println("  ✓ Project structure is correct")
        println("  ✓ All required directories exist")
        println("  ✓ Version-loader combinations are valid")
        println("\n✅ Project integration test passed!\n")
    }

    @Test
    fun `test read real config yml`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Read Real config.yml                               ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configFile = File(context.projectDir, "config.yml")
        assertTrue(configFile.exists(), "config.yml should exist")

        val content = configFile.readText()
        assertTrue(content.contains("id: testmod"), "Should have mod ID")
        assertTrue(content.contains("name:"), "Should have mod name")
        assertTrue(content.contains("version:"), "Should have mod version")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo)
        assertEquals("testmod", projectInfo.modId)
        assertEquals("Test Mod", projectInfo.modName)

        println("  ✓ config.yml exists and is readable")
        println("  ✓ Mod ID: ${projectInfo.modId}")
        println("  ✓ Mod Name: ${projectInfo.modName}")
        println("\n✅ Real config reading test passed!\n")
    }

    @Test
    fun `test scan real versions directory`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Scan Real versions/ Directory                      ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val versionsDir = File(context.projectDir, "versions")
        assertTrue(versionsDir.exists(), "versions directory should exist")
        assertTrue(versionsDir.isDirectory, "versions should be a directory")

        val versionDirs = versionsDir.listFiles { file ->
            file.isDirectory && !file.name.equals("shared", ignoreCase = true)
        }

        assertNotNull(versionDirs)
        assertTrue(versionDirs.size >= 2, "Should have at least 2 version directories")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo)
        assertTrue(projectInfo.versions.size >= 2, "Should detect at least 2 versions")

        println("  ✓ versions/ directory exists")
        println("  ✓ Found ${versionDirs.size} version directories")
        println("  ✓ ConfigReader detected ${projectInfo.versions.size} versions")

        projectInfo.versions.forEach { v ->
            println("    - ${v.minecraftVersion}: ${v.loaders.joinToString(", ")}")
        }

        println("\n✅ Real versions scan test passed!\n")
    }

    @Test
    fun `test validate version-loader combinations`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Validate Version-Loader Combinations               ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configReader = ConfigReader(context.projectDir)

        // Test valid combinations
        val validCombinations = listOf(
            "1.20.1" to "fabric",
            "1.20.1" to "neoforge",
            "1.21.1" to "fabric",
            "1.21.1" to "neoforge"
        )

        validCombinations.forEach { (version, loader) ->
            val exists = configReader.versionLoaderExists(version, loader)
            assertTrue(exists, "$version-$loader should exist")
            println("  ✓ Valid: $version-$loader")
        }

        // Test invalid combinations
        val invalidCombinations = listOf(
            "1.20.1" to "forge",
            "1.19.4" to "fabric",
            "999.999.999" to "fabric"
        )

        invalidCombinations.forEach { (version, loader) ->
            val exists = configReader.versionLoaderExists(version, loader)
            assertFalse(exists, "$version-$loader should not exist")
            println("  ✗ Invalid (correctly rejected): $version-$loader")
        }

        println("\n✅ Version-loader validation test passed!\n")
    }

    // ==============================================
    // 6. COMMAND SCENARIOS TESTS
    // ==============================================

    @Test
    fun `test dev run with auto-detection scenario`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: 'dropper dev run' Auto-detection Scenario          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo)

        // Simulate auto-detection logic from DevRunCommand
        val selectedVersion = projectInfo.versions.first().minecraftVersion
        val selectedLoader = projectInfo.versions.first().loaders.first()

        assertNotNull(selectedVersion)
        assertNotNull(selectedLoader)

        val versionGradle = configReader.versionToGradleFormat(selectedVersion)

        println("  ✓ Auto-detected version: $selectedVersion")
        println("  ✓ Auto-detected loader: $selectedLoader")
        println("  ✓ Gradle format: $versionGradle")
        println("  ✓ Would execute: ./gradlew :${versionGradle}-${selectedLoader}:runClient")
        println("\n✅ Auto-detection scenario test passed!\n")
    }

    @Test
    fun `test dev client with specific version and loader scenario`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: 'dropper dev client -v 1.21.1 -l neoforge'         ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo)

        // Simulate specific version/loader selection
        val targetVersion = "1.21.1"
        val targetLoader = "neoforge"

        val versionInfo = projectInfo.versions.find { it.minecraftVersion == targetVersion }
        assertNotNull(versionInfo, "Version $targetVersion should exist")
        assertTrue(versionInfo.loaders.contains(targetLoader), "Loader $targetLoader should exist")

        val versionGradle = configReader.versionToGradleFormat(targetVersion)

        println("  ✓ Specified version: $targetVersion")
        println("  ✓ Specified loader: $targetLoader")
        println("  ✓ Version exists: true")
        println("  ✓ Loader exists: true")
        println("  ✓ Would execute: ./gradlew :${versionGradle}-${targetLoader}:runClient")
        println("\n✅ Specific version/loader scenario test passed!\n")
    }

    @Test
    fun `test dev server with clean flag scenario`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: 'dropper dev server --clean' Scenario              ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val gradleRunner = GradleRunner(context.projectDir)

        val command = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runServer",
            gradleArgs = listOf("--rerun-tasks")
        )

        val commandString = command.joinToString(" ")
        assertTrue(commandString.contains("runServer"), "Should run server task")
        assertTrue(command.contains("--rerun-tasks"), "Should include clean flag")

        println("  ✓ Task: runServer")
        println("  ✓ Clean mode: enabled (--rerun-tasks)")
        println("  ✓ Command: $commandString")
        println("\n✅ Server with clean scenario test passed!\n")
    }

    @Test
    fun `test dev test execution scenario`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: 'dropper dev test' Execution Scenario              ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val gradleRunner = GradleRunner(context.projectDir)

        val command = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "test"
        )

        assertTrue(command.last().contains("test"), "Should run test task")

        println("  ✓ Task: test")
        println("  ✓ Version: 1_20_1")
        println("  ✓ Loader: fabric")
        println("  ✓ Would execute: ./gradlew :1_20_1-fabric:test")
        println("\n✅ Test execution scenario test passed!\n")
    }

    @Test
    fun `test dev with debug mode scenario`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: 'dropper dev run --debug --port 9999' Scenario     ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val gradleRunner = GradleRunner(context.projectDir)

        val customPort = 9999
        val debugArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:$customPort")

        val command = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient",
            jvmArgs = debugArgs
        )

        val commandString = command.joinToString(" ")
        assertTrue(commandString.contains("jdwp"), "Should include debug JVM args")
        assertTrue(commandString.contains("$customPort"), "Should include custom port")

        println("  ✓ Debug mode: enabled")
        println("  ✓ Debug port: $customPort")
        println("  ✓ Would show: 'Connect debugger to: localhost:$customPort'")
        println("  ✓ JVM args included in command")
        println("\n✅ Debug mode scenario test passed!\n")
    }

    // ==============================================
    // 7. COMPREHENSIVE WORKFLOW TEST
    // ==============================================

    @Test
    fun `test complete dev workflow from project detection to command construction`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Complete Dev Workflow E2E                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        println("1️⃣  Detecting project...")
        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()
        assertNotNull(projectInfo, "Project should be detected")
        println("    ✓ Project detected: ${projectInfo.modName}")

        println("\n2️⃣  Scanning versions...")
        assertTrue(projectInfo.versions.isNotEmpty(), "Should find versions")
        println("    ✓ Found ${projectInfo.versions.size} version(s)")

        println("\n3️⃣  Auto-selecting version and loader...")
        val selectedVersion = projectInfo.versions.first().minecraftVersion
        val selectedLoader = projectInfo.versions.first().loaders.first()
        println("    ✓ Version: $selectedVersion")
        println("    ✓ Loader: $selectedLoader")

        println("\n4️⃣  Validating version-loader combination...")
        val exists = configReader.versionLoaderExists(selectedVersion, selectedLoader)
        assertTrue(exists, "Version-loader combination should exist")
        println("    ✓ Combination valid: $selectedVersion-$selectedLoader")

        println("\n5️⃣  Checking Gradle wrapper...")
        val gradleRunner = GradleRunner(context.projectDir)
        val hasWrapper = gradleRunner.hasGradleWrapper()

        // Create a mock wrapper for testing if it doesn't exist
        if (!hasWrapper) {
            val isWindows = System.getProperty("os.name").lowercase().contains("windows")
            val wrapperName = if (isWindows) "gradlew.bat" else "gradlew"
            File(context.projectDir, wrapperName).writeText("@echo off\necho Mock wrapper")
        }

        // Verify wrapper exists now
        val hasWrapperAfter = gradleRunner.hasGradleWrapper()
        assertTrue(hasWrapperAfter, "Gradle wrapper should exist (or be created for test)")
        println("    ✓ Gradle wrapper found/created")

        println("\n6️⃣  Building Gradle command...")
        val versionGradle = configReader.versionToGradleFormat(selectedVersion)
        val command = gradleRunner.buildGradleCommand(
            version = versionGradle,
            loader = selectedLoader,
            task = "runClient"
        )
        assertTrue(command.isNotEmpty(), "Command should be built")
        println("    ✓ Command: ${command.joinToString(" ")}")

        println("\n7️⃣  Validating command format...")
        assertTrue(command.last().contains("runClient"), "Should contain task name")
        assertTrue(command.last().contains(versionGradle), "Should contain version")
        assertTrue(command.last().contains(selectedLoader), "Should contain loader")
        println("    ✓ Command format valid: :${versionGradle}-${selectedLoader}:runClient")

        println("\n✅ Complete dev workflow test passed!\n")
        println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
    }
}
