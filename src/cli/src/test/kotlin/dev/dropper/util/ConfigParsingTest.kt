package dev.dropper.util

import dev.dropper.commands.DropperCommand
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for extractMinecraftVersions() in DropperCommand.
 *
 * Since extractMinecraftVersions is a protected method on the abstract
 * DropperCommand, we expose it via a minimal test subclass.
 */
class ConfigParsingTest {

    private lateinit var tempDir: File

    /**
     * Minimal concrete subclass that exposes the protected method for testing.
     */
    private class TestDropperCommand : DropperCommand(name = "test", help = "test") {
        override fun run() {}

        fun testExtractMinecraftVersions(configFile: File): List<String> {
            return extractMinecraftVersions(configFile)
        }
    }

    private val command = TestDropperCommand()

    @BeforeEach
    fun setup() {
        tempDir = File("build/test-projects/${System.currentTimeMillis()}/config-parsing-test")
        tempDir.mkdirs()
    }

    @AfterEach
    fun cleanup() {
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `standard multi-version config extracts correctly`() {
        val configFile = File(tempDir, "config.yml")
        configFile.writeText(
            "minecraft_versions:\n" +
            "  - \"1.20.1\"\n" +
            "  - \"1.21.1\"\n"
        )

        val versions = command.testExtractMinecraftVersions(configFile)

        assertEquals(2, versions.size, "Should extract exactly 2 versions")
        assertEquals("1.20.1", versions[0])
        assertEquals("1.21.1", versions[1])
    }

    @Test
    fun `single version config extracts correctly`() {
        val configFile = File(tempDir, "config.yml")
        configFile.writeText(
            "minecraft_versions:\n" +
            "  - \"1.20.1\"\n"
        )

        val versions = command.testExtractMinecraftVersions(configFile)

        assertEquals(1, versions.size, "Should extract exactly 1 version")
        assertEquals("1.20.1", versions[0])
    }

    @Test
    fun `unquoted versions extract correctly`() {
        val configFile = File(tempDir, "config.yml")
        configFile.writeText(
            "minecraft_versions:\n" +
            "  - 1.20.1\n"
        )

        val versions = command.testExtractMinecraftVersions(configFile)

        assertEquals(1, versions.size, "Should extract exactly 1 version")
        assertEquals("1.20.1", versions[0])
    }

    @Test
    fun `empty config returns empty list`() {
        val configFile = File(tempDir, "config.yml")
        configFile.writeText(
            "mod:\n" +
            "  id: testmod\n" +
            "  name: Test Mod\n"
        )

        val versions = command.testExtractMinecraftVersions(configFile)

        assertTrue(versions.isEmpty(), "Should return empty list when minecraft_versions is missing")
    }

    @Test
    fun `minecraft_versions key with no items returns empty list`() {
        val configFile = File(tempDir, "config.yml")
        configFile.writeText(
            "minecraft_versions:\n" +
            "loaders:\n" +
            "  - fabric\n"
        )

        val versions = command.testExtractMinecraftVersions(configFile)

        assertTrue(versions.isEmpty(), "Should return empty list when minecraft_versions has no items")
    }

    @Test
    fun `config with other keys before and after minecraft_versions works correctly`() {
        val configFile = File(tempDir, "config.yml")
        configFile.writeText(
            "mod:\n" +
            "  id: testmod\n" +
            "  name: Test Mod\n" +
            "  version: 1.0.0\n" +
            "minecraft_versions:\n" +
            "  - \"1.20.1\"\n" +
            "  - \"1.21.1\"\n" +
            "  - \"1.21.4\"\n" +
            "loaders:\n" +
            "  - fabric\n" +
            "  - forge\n"
        )

        val versions = command.testExtractMinecraftVersions(configFile)

        assertEquals(3, versions.size, "Should extract exactly 3 versions")
        assertEquals("1.20.1", versions[0])
        assertEquals("1.21.1", versions[1])
        assertEquals("1.21.4", versions[2])
    }

    @Test
    fun `mixed quoted and unquoted versions extract correctly`() {
        val configFile = File(tempDir, "config.yml")
        configFile.writeText(
            "minecraft_versions:\n" +
            "  - \"1.20.1\"\n" +
            "  - 1.21.1\n" +
            "  - \"1.21.4\"\n"
        )

        val versions = command.testExtractMinecraftVersions(configFile)

        assertEquals(3, versions.size, "Should extract exactly 3 versions")
        assertEquals("1.20.1", versions[0])
        assertEquals("1.21.1", versions[1])
        assertEquals("1.21.4", versions[2])
    }
}
