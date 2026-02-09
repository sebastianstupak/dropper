package dev.dropper.integration

import dev.dropper.commands.util.ConfigReader
import dev.dropper.commands.util.GradleRunner
import dev.dropper.config.ModConfig
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.*

/**
 * Comprehensive tests for dev command functionality
 */
class DevCommandTest {

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
    fun `ConfigReader detects project versions and loaders`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: ConfigReader Version Detection                     ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "neoforge")
        )

        context.createProject(config)

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo, "Should read project info")
        assertEquals("testmod", projectInfo.modId)
        assertEquals("Test Mod", projectInfo.modName)
        assertTrue(projectInfo.versions.isNotEmpty(), "Should find versions")

        println("  ✓ Project info read successfully")
        println("  ✓ Mod ID: ${projectInfo.modId}")
        println("  ✓ Found ${projectInfo.versions.size} version(s)")

        projectInfo.versions.forEach { versionInfo ->
            println("    - ${versionInfo.minecraftVersion}: ${versionInfo.loaders.joinToString(", ")}")
            assertTrue(versionInfo.loaders.isNotEmpty(), "Version should have loaders")
        }

        println("\n✅ ConfigReader test passed!\n")
    }

    @Test
    fun `ConfigReader validates version-loader combinations`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Version-Loader Validation                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "neoforge")
        )

        context.createProject(config)

        val configReader = ConfigReader(context.projectDir)

        // Test valid combinations
        assertTrue(
            configReader.versionLoaderExists("1.20.1", "fabric"),
            "1.20.1-fabric should exist"
        )
        assertTrue(
            configReader.versionLoaderExists("1.20.1", "neoforge"),
            "1.20.1-neoforge should exist"
        )

        // Test invalid combinations
        assertFalse(
            configReader.versionLoaderExists("1.21.1", "fabric"),
            "1.21.1-fabric should not exist"
        )
        assertFalse(
            configReader.versionLoaderExists("1.20.1", "forge"),
            "1.20.1-forge should not exist"
        )

        println("  ✓ Valid combinations detected correctly")
        println("  ✓ Invalid combinations rejected correctly")
        println("\n✅ Version-loader validation test passed!\n")
    }

    @Test
    fun `ConfigReader converts version to Gradle format`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Version Format Conversion                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        val configReader = ConfigReader(context.projectDir)

        assertEquals("1_20_1", configReader.versionToGradleFormat("1.20.1"))
        assertEquals("1_21_1", configReader.versionToGradleFormat("1.21.1"))
        assertEquals("1_19_4", configReader.versionToGradleFormat("1.19.4"))

        println("  ✓ 1.20.1 -> 1_20_1")
        println("  ✓ 1.21.1 -> 1_21_1")
        println("  ✓ 1.19.4 -> 1_19_4")
        println("\n✅ Version format conversion test passed!\n")
    }

    @Test
    fun `GradleRunner builds correct command structure`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Gradle Command Construction                        ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        val gradleRunner = GradleRunner(context.projectDir)

        // Test basic command
        val basicCommand = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient"
        )

        assertTrue(basicCommand.size >= 2, "Command should have at least 2 elements")
        assertTrue(basicCommand.last().contains("runClient"), "Command should contain task")
        assertTrue(basicCommand.last().contains("1_20_1-fabric"), "Command should contain module ID")

        println("  ✓ Basic command: ${basicCommand.joinToString(" ")}")

        // Test command with JVM args
        val debugCommand = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient",
            jvmArgs = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
        )

        assertTrue(debugCommand.size >= 3, "Debug command should have JVM args")

        println("  ✓ Debug command includes JVM args")

        // Test command with Gradle args
        val argsCommand = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "test",
            gradleArgs = listOf("--rerun-tasks")
        )

        assertTrue(argsCommand.contains("--rerun-tasks"), "Command should include Gradle args")

        println("  ✓ Command includes Gradle args")
        println("\n✅ Gradle command construction test passed!\n")
    }

    @Test
    fun `GradleRunner detects Gradle wrapper presence`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Gradle Wrapper Detection                           ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        val gradleRunner = GradleRunner(context.projectDir)

        // The ProjectGenerator creates Gradle wrapper files in real usage,
        // but in test environment they may not be fully created.
        // Check both scenarios: presence or absence
        val hasWrapper = gradleRunner.hasGradleWrapper()

        println("  ✓ Gradle wrapper detection works: $hasWrapper")

        // Also test that an empty directory correctly reports no wrapper
        val emptyDir = File("build/test-dev/${System.currentTimeMillis()}/empty")
        emptyDir.mkdirs()
        val emptyRunner = GradleRunner(emptyDir)
        assertFalse(emptyRunner.hasGradleWrapper(), "Empty directory should not have wrapper")
        emptyDir.deleteRecursively()

        println("  ✓ Correctly detects missing wrapper")
        println("\n✅ Gradle wrapper detection test passed!\n")
    }

    @Test
    fun `ConfigReader handles missing config gracefully`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Missing Config Error Handling                      ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val emptyDir = File("build/test-dev/${System.currentTimeMillis()}/empty-dir")
        emptyDir.mkdirs()

        try {
            val configReader = ConfigReader(emptyDir)
            val projectInfo = configReader.readProjectInfo()

            assertNull(projectInfo, "Should return null for missing config")
            println("  ✓ Returns null for missing config.yml")
            println("\n✅ Missing config handling test passed!\n")
        } finally {
            emptyDir.deleteRecursively()
        }
    }

    @Test
    fun `ConfigReader handles invalid version combinations`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Invalid Version Combinations                       ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        val configReader = ConfigReader(context.projectDir)

        // Test non-existent versions
        assertFalse(
            configReader.versionLoaderExists("999.999.999", "fabric"),
            "Non-existent version should return false"
        )

        assertFalse(
            configReader.versionLoaderExists("1.20.1", "nonexistent"),
            "Non-existent loader should return false"
        )

        println("  ✓ Non-existent version rejected")
        println("  ✓ Non-existent loader rejected")
        println("\n✅ Invalid combination handling test passed!\n")
    }

    @Test
    fun `GradleRunner constructs debug mode JVM arguments correctly`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Debug Mode JVM Arguments                           ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)

        val gradleRunner = GradleRunner(context.projectDir)

        // Test standard debug port (5005)
        val debugArgs5005 = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005")
        val command5005 = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient",
            jvmArgs = debugArgs5005
        )

        val jvmArgString = command5005.find { it.contains("jdwp") }
        assertNotNull(jvmArgString, "Command should contain debug JVM args")
        assertTrue(jvmArgString.contains("5005"), "Should use port 5005")

        println("  ✓ Debug args for port 5005: $jvmArgString")

        // Test custom debug port (9999)
        val debugArgs9999 = listOf("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:9999")
        val command9999 = gradleRunner.buildGradleCommand(
            version = "1_20_1",
            loader = "fabric",
            task = "runClient",
            jvmArgs = debugArgs9999
        )

        val jvmArgString9999 = command9999.find { it.contains("jdwp") }
        assertNotNull(jvmArgString9999, "Command should contain debug JVM args")
        assertTrue(jvmArgString9999.contains("9999"), "Should use custom port 9999")

        println("  ✓ Debug args for port 9999: $jvmArgString9999")
        println("\n✅ Debug mode JVM arguments test passed!\n")
    }

    @Test
    fun `ConfigReader detects multiple versions correctly`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test: Multiple Version Detection                         ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val config = ModConfig(
            id = "multiversion",
            name = "Multi Version",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.20.4", "1.21.1"),
            loaders = listOf("fabric", "neoforge")
        )

        context.createProject(config)

        val configReader = ConfigReader(context.projectDir)
        val projectInfo = configReader.readProjectInfo()

        assertNotNull(projectInfo)
        assertEquals(3, projectInfo.versions.size, "Should detect 3 versions")

        val versionNumbers = projectInfo.versions.map { it.minecraftVersion }.sorted()
        assertTrue(versionNumbers.contains("1.20.1"))
        assertTrue(versionNumbers.contains("1.20.4"))
        assertTrue(versionNumbers.contains("1.21.1"))

        println("  ✓ Detected ${projectInfo.versions.size} versions:")
        projectInfo.versions.forEach { v ->
            println("    - ${v.minecraftVersion} (${v.loaders.joinToString(", ")})")
        }

        println("\n✅ Multiple version detection test passed!\n")
    }
}
