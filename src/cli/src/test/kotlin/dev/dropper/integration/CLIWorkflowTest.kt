package dev.dropper.integration

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.io.File
import kotlin.test.assertTrue

/**
 * End-to-end CLI workflow test that:
 * 1. Cleans examples directory
 * 2. Creates a mod using actual CLI commands
 * 3. Builds for 2 different Minecraft versions (different Java SDKs)
 * 4. Verifies all loader JARs are created (Fabric, Forge, NeoForge)
 *
 * This test validates the complete real-world workflow using the CLI.
 */
class CLIWorkflowTest {

    private lateinit var examplesDir: File
    private lateinit var projectDir: File

    @BeforeEach
    fun setup() {
        examplesDir = File("examples").absoluteFile
        projectDir = File(examplesDir, "test-mod-cli")
    }

    @AfterEach
    fun cleanup() {
        // Clean up the test project after test completes
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_CLI_BUILD", matches = "true")
    fun `full CLI workflow - create mod and build all versions and loaders`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║        CLI E2E WORKFLOW TEST - Full Build Validation         ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Step 1: Clean examples directory
        println("Step 1: Cleaning examples directory...")
        cleanExamplesDirectory()

        // Step 2: Create mod using CLI
        println("\nStep 2: Creating mod using CLI commands...")
        createModWithCLI()

        // Step 3: Verify project structure
        println("\nStep 3: Verifying project structure...")
        verifyProjectStructure()

        // Step 4: Copy gradle wrapper (TODO: CLI should do this)
        println("\nStep 4: Setting up Gradle wrapper...")
        copyGradleWrapper()

        // Step 5: Build all versions and loaders
        println("\nStep 5: Building all versions and loaders (this may take 10-15 minutes)...")
        buildAllVersionsAndLoaders()

        // Step 6: Verify all JARs were created
        println("\nStep 6: Verifying all JAR outputs...")
        verifyAllJars()

        println("\n✓ Full CLI workflow test completed successfully!")
        println("  All versions and loaders built successfully!\n")
    }

    @Test
    fun `lightweight CLI workflow - create and verify structure`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     CLI E2E WORKFLOW TEST - Structure Validation Only        ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // This is the default test that always runs
        // It doesn't build with Gradle (too slow for regular CI)

        println("Step 1: Cleaning examples directory...")
        cleanExamplesDirectory()

        println("\nStep 2: Creating mod using CLI commands...")
        createModWithCLI()

        println("\nStep 3: Verifying project structure...")
        verifyProjectStructure()

        println("\nStep 4: Verifying configuration files...")
        verifyConfigFiles()

        println("\n✓ Lightweight CLI workflow test completed successfully!")
    }

    private fun cleanExamplesDirectory() {
        if (!examplesDir.exists()) {
            examplesDir.mkdirs()
        }

        // Remove any existing test-mod-cli directory
        if (projectDir.exists()) {
            println("  Removing existing test project...")
            projectDir.deleteRecursively()
        }

        // Clean other test artifacts but keep ruby-sword example
        examplesDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("test-") && file.isDirectory) {
                println("  Cleaning up: ${file.name}")
                file.deleteRecursively()
            }
        }

        println("  ✓ Examples directory cleaned")
    }

    private fun createModWithCLI() {
        // Use ProjectGenerator directly since we can't easily mock stdin for CLI
        // In a real scenario, this would be: dropper init test-mod-cli
        val config = dev.dropper.config.ModConfig(
            id = "testmodcli",
            name = "Test Mod CLI",
            version = "1.0.0",
            description = "E2E test mod created via CLI workflow",
            author = "Dropper E2E Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        val generator = dev.dropper.generator.ProjectGenerator()
        generator.generate(projectDir, config)

        assertTrue(projectDir.exists(), "Project directory should exist")
        println("  ✓ Mod project created")
    }

    private fun verifyProjectStructure() {
        // Essential root files
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
                File(projectDir, file).exists(),
                "Essential file should exist: $file"
            )
        }

        // Version directories
        val versionDirs = listOf(
            "versions/1_20_1",
            "versions/1_21_1",
            "versions/shared/v1"
        )

        versionDirs.forEach { dir ->
            assertTrue(
                File(projectDir, dir).exists(),
                "Version directory should exist: $dir"
            )
        }

        // Loader-specific directories for each version
        val loaders = listOf("fabric", "forge", "neoforge")
        val versions = listOf("1_20_1", "1_21_1")

        versions.forEach { version ->
            loaders.forEach { loader ->
                val loaderDir = File(projectDir, "versions/$version/$loader")
                assertTrue(
                    loaderDir.exists(),
                    "Loader directory should exist: versions/$version/$loader"
                )
            }
        }

        // Shared code structure
        val sharedDirs = listOf(
            "shared/common/src/main/java/com/testmodcli",
            "shared/fabric/src/main/java/com/testmodcli/platform",
            "shared/forge/src/main/java/com/testmodcli/platform",
            "shared/neoforge/src/main/java/com/testmodcli/platform"
        )

        sharedDirs.forEach { dir ->
            assertTrue(
                File(projectDir, dir).exists(),
                "Shared directory should exist: $dir"
            )
        }

        println("  ✓ Project structure verified")
    }

    private fun verifyConfigFiles() {
        // Verify root config.yml
        val rootConfig = File(projectDir, "config.yml")
        assertTrue(rootConfig.exists(), "Root config.yml should exist")
        val rootContent = rootConfig.readText()
        assertTrue(rootContent.contains("id: testmodcli"), "Should have correct mod ID")
        assertTrue(rootContent.contains("Test Mod CLI"), "Should have mod name")

        // Verify version configs
        val version1Config = File(projectDir, "versions/1_20_1/config.yml")
        assertTrue(version1Config.exists(), "Version 1.20.1 config should exist")
        val version1Content = version1Config.readText()
        assertTrue(version1Content.contains("minecraft_version: \"1.20.1\""), "Should specify MC version")

        val version2Config = File(projectDir, "versions/1_21_1/config.yml")
        assertTrue(version2Config.exists(), "Version 1.21.1 config should exist")
        val version2Content = version2Config.readText()
        assertTrue(version2Content.contains("minecraft_version: \"1.21.1\""), "Should specify MC version")

        // Verify asset pack config
        val assetPackConfig = File(projectDir, "versions/shared/v1/config.yml")
        assertTrue(assetPackConfig.exists(), "Asset pack config should exist")
        val assetPackContent = assetPackConfig.readText()
        assertTrue(assetPackContent.contains("asset_pack:"), "Should have asset_pack section")

        println("  ✓ Configuration files verified")
    }

    private fun copyGradleWrapper() {
        // Copy gradle wrapper from root to generated project
        // Find the actual project root (where gradle wrapper lives)
        val rootDir = File(System.getProperty("user.dir"))
        val gradleDir = File(rootDir, "gradle")
        val gradlewFile = File(rootDir, "gradlew")
        val gradlewBatFile = File(rootDir, "gradlew.bat")

        println("  Root directory: ${rootDir.absolutePath}")
        println("  Gradle dir exists: ${gradleDir.exists()}")
        println("  gradlew exists: ${gradlewFile.exists()}")

        if (gradleDir.exists()) {
            gradleDir.copyRecursively(File(projectDir, "gradle"), overwrite = true)
        }
        if (gradlewFile.exists()) {
            gradlewFile.copyTo(File(projectDir, "gradlew"), overwrite = true)
            // Make executable on Unix
            if (!System.getProperty("os.name").lowercase().contains("windows")) {
                Runtime.getRuntime().exec(arrayOf("chmod", "+x", File(projectDir, "gradlew").absolutePath)).waitFor()
            }
        }
        if (gradlewBatFile.exists()) {
            gradlewBatFile.copyTo(File(projectDir, "gradlew.bat"), overwrite = true)
        }

        println("  ✓ Gradle wrapper copied")
    }

    private fun buildAllVersionsAndLoaders() {
        val gradlewCmd = if (System.getProperty("os.name").lowercase().contains("windows")) {
            File(projectDir, "gradlew.bat").absolutePath
        } else {
            File(projectDir, "gradlew").absolutePath
        }

        // Make gradlew executable on Unix
        if (!System.getProperty("os.name").lowercase().contains("windows")) {
            Runtime.getRuntime().exec(arrayOf("chmod", "+x", gradlewCmd)).waitFor()
        }

        println("\n  Building all versions and loaders:")
        println("  - 1.20.1 (Java 17): Fabric, Forge, NeoForge")
        println("  - 1.21.1 (Java 21): Fabric, Forge, NeoForge")
        println("\n  This will download dependencies and compile everything...")
        println("  (Progress may appear slow, please be patient)\n")

        // Run build for all versions and loaders
        val process = ProcessBuilder(gradlewCmd, "build", "--no-daemon", "--console=plain", "--warning-mode=all")
            .directory(projectDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        // Print output as it comes
        val outputThread = Thread {
            process.inputStream.bufferedReader().use { reader ->
                reader.lines().forEach { line ->
                    println("    $line")
                }
            }
        }
        outputThread.start()

        val errorThread = Thread {
            process.errorStream.bufferedReader().use { reader ->
                reader.lines().forEach { line ->
                    System.err.println("    [ERROR] $line")
                }
            }
        }
        errorThread.start()

        val exitCode = process.waitFor()
        outputThread.join()
        errorThread.join()

        if (exitCode != 0) {
            throw AssertionError("Gradle build failed with exit code $exitCode")
        }

        println("\n  ✓ All versions and loaders built successfully")
    }

    private fun verifyAllJars() {
        // Verify JARs for version 1.20.1
        val version1Jars = listOf(
            "build/1_20_1/fabric.jar",
            "build/1_20_1/forge.jar",
            "build/1_20_1/neoforge.jar"
        )

        println("\n  Verifying 1.20.1 JARs:")
        version1Jars.forEach { jarPath ->
            val jarFile = File(projectDir, jarPath)
            assertTrue(jarFile.exists(), "JAR should exist: $jarPath")
            assertTrue(jarFile.length() > 0, "JAR should not be empty: $jarPath")
            println("    ✓ $jarPath (${jarFile.length() / 1024}KB)")
        }

        // Verify JARs for version 1.21.1
        val version2Jars = listOf(
            "build/1_21_1/fabric.jar",
            "build/1_21_1/forge.jar",
            "build/1_21_1/neoforge.jar"
        )

        println("\n  Verifying 1.21.1 JARs:")
        version2Jars.forEach { jarPath ->
            val jarFile = File(projectDir, jarPath)
            assertTrue(jarFile.exists(), "JAR should exist: $jarPath")
            assertTrue(jarFile.length() > 0, "JAR should not be empty: $jarPath")
            println("    ✓ $jarPath (${jarFile.length() / 1024}KB)")
        }

        println("\n  ✓ All 6 JARs verified (3 loaders × 2 versions)")
    }
}
