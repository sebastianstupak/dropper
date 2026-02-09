package dev.dropper.integration

import dev.dropper.commands.AddVersionCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.config.ModConfig
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * E2E tests for add version command
 * Tests adding new Minecraft versions to existing projects
 */
class AddVersionCommandTest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("test-add-version")

        // Create initial project with 1.20.1
        val config = ModConfig(
            id = "versiontest",
            name = "Version Test",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "neoforge")
        )

        context.createProject(config)
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    @Test
    fun `add new Minecraft version creates proper structure`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Add Version Command                            ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Add 1.21.1
        println("Adding Minecraft version 1.21.1...")
        context.withProjectDir {
            val command = AddVersionCommand()
            command.parse(arrayOf("1.21.1"))
        }

        // Verify version directory created
        val versionDir = context.file("versions/1_21_1")
        assertTrue(versionDir.exists(), "Version directory should be created")
        println("  ✓ Version directory created")

        // Verify config.yml
        val versionConfig = File(versionDir, "config.yml")
        assertTrue(versionConfig.exists(), "Version config should be created")
        val configContent = versionConfig.readText()
        assertTrue(configContent.contains("minecraft_version: \"1.21.1\""), "Config should have MC version")
        assertTrue(configContent.contains("asset_pack: \"v1\""), "Config should reference asset pack")
        println("  ✓ Version config created")

        // Verify loader directories with src/main/java structure
        val loaders = listOf("fabric", "neoforge")
        loaders.forEach { loader ->
            val loaderDir = File(versionDir, "$loader/src/main/java")
            assertTrue(loaderDir.exists(), "$loader/src/main/java should exist")
            println("  ✓ $loader directory structure created")
        }

        // Verify common directory
        val commonDir = File(versionDir, "common/src/main/java")
        assertTrue(commonDir.exists(), "common/src/main/java should exist")
        println("  ✓ common directory structure created")

        println("\n✅ Add version command test passed!\n")
    }

    @Test
    fun `add version with custom asset pack`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Add Version with Custom Asset Pack             ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Add 1.21.1 with v2 asset pack
        context.withProjectDir {
            val command = AddVersionCommand()
            command.parse(arrayOf("1.21.1", "--asset-pack", "v2"))
        }

        val versionConfig = context.file("versions/1_21_1/config.yml")
        val configContent = versionConfig.readText()
        assertTrue(configContent.contains("asset_pack: \"v2\""), "Config should use v2 asset pack")

        println("  ✓ Custom asset pack configured")
        println("\n✅ Custom asset pack test passed!\n")
    }

    @Test
    fun `add version with custom loaders`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Add Version with Custom Loaders                ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Add 1.21.1 with only fabric
        context.withProjectDir {
            val command = AddVersionCommand()
            command.parse(arrayOf("1.21.1", "--loaders", "fabric"))
        }

        // Verify only fabric directory exists
        assertTrue(
            context.file("versions/1_21_1/fabric").exists(),
            "Fabric directory should exist"
        )
        assertTrue(
            !context.file("versions/1_21_1/neoforge").exists(),
            "NeoForge directory should NOT exist"
        )

        println("  ✓ Only specified loaders created")
        println("\n✅ Custom loaders test passed!\n")
    }

    @Test
    fun `add multiple versions to same project`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Add Multiple Versions                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Add multiple versions
        println("Adding versions 1.20.4, 1.21.1, 1.21.4...")
        context.withProjectDir {
            AddVersionCommand().parse(arrayOf("1.20.4"))
            AddVersionCommand().parse(arrayOf("1.21.1"))
            AddVersionCommand().parse(arrayOf("1.21.4"))
        }

        // Verify all exist
        val versions = listOf("1_20_1", "1_20_4", "1_21_1", "1_21_4")
        versions.forEach { version ->
            assertTrue(
                context.file("versions/$version").exists(),
                "Version $version should exist"
            )
            println("  ✓ Version $version added")
        }

        println("\n✅ Multiple versions test passed!\n")
    }

    @Test
    fun `items created before adding version are available in new version`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: Shared Items Across Versions                   ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Create item in existing version
        println("Creating shared item...")
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("shared_gem"))
        }

        // Verify item is in shared/common (version-agnostic)
        assertTrue(
            context.file("shared/common/src/main/java/com/versiontest/items/SharedGem.java").exists(),
            "Item should be in shared/common"
        )
        println("  ✓ Item created in shared/common")

        // Add new version
        println("\nAdding new version 1.21.1...")
        context.withProjectDir {
            AddVersionCommand().parse(arrayOf("1.21.1"))
        }

        // Verify the item model is in shared asset pack (available to all versions)
        assertTrue(
            context.file("versions/shared/v1/assets/versiontest/models/item/shared_gem.json").exists(),
            "Item model should be in shared asset pack"
        )
        println("  ✓ Item assets in shared asset pack")

        println("\n  ℹ️  The item 'shared_gem' is now available in both 1.20.1 and 1.21.1")
        println("     because it's in shared/common and uses shared asset pack v1")
        println("\n✅ Shared items test passed!\n")
    }

    @Test
    fun `add version creates proper directory structure for IntelliJ`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     E2E Test: IntelliJ Compatibility for New Version         ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            AddVersionCommand().parse(arrayOf("1.21.1"))
        }

        // Verify standard Maven/Gradle structure
        val checks = listOf(
            "versions/1_21_1/common/src/main/java" to "Common source directory",
            "versions/1_21_1/common/src/main/resources" to "Common resources directory",
            "versions/1_21_1/fabric/src/main/java" to "Fabric source directory",
            "versions/1_21_1/fabric/src/main/resources" to "Fabric resources directory",
            "versions/1_21_1/neoforge/src/main/java" to "NeoForge source directory",
            "versions/1_21_1/neoforge/src/main/resources" to "NeoForge resources directory"
        )

        checks.forEach { (path, description) ->
            val dir = context.file(path)
            assertTrue(dir.exists(), "$description should exist: $path")
            println("  ✓ $description")
        }

        println("\n  ✓ All directories use standard Maven/Gradle layout")
        println("  ✓ IntelliJ IDEA will recognize all source directories")
        println("\n✅ IntelliJ compatibility test passed!\n")
    }
}
