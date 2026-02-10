package dev.dropper.integration

import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.config.ModConfig
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for updateMainModInit() behavior.
 *
 * Verifies that creating items/blocks/entities correctly updates the main mod
 * class with registry init() calls and imports.
 */
class RegistryInitTest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("registry-init-test")

        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "Test project for registry init",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        context.createProject(config)
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    /**
     * The main mod class file path. ProjectGenerator creates a class named
     * after the mod name with spaces/hyphens/underscores stripped.
     * For name="Test Mod" -> class "TestMod" in package com.testmod.
     */
    private fun getMainModFile(): File {
        return context.file("shared/common/src/main/java/com/testmod/TestMod.java")
    }

    @Test
    fun `first item creation adds ModItems init to main mod class`() {
        // Verify main mod class exists before item creation
        val mainModFile = getMainModFile()
        assertTrue(mainModFile.exists(), "Main mod class should exist after project generation")

        val contentBefore = mainModFile.readText()
        assertTrue(
            !contentBefore.contains("ModItems.init();"),
            "Main mod class should not contain ModItems.init() before any item is created"
        )

        // Create an item
        context.withProjectDir {
            val command = CreateItemCommand()
            command.projectDir = context.projectDir
            command.parse(arrayOf("ruby_sword", "--type", "tool"))
        }

        // Verify main mod class now contains ModItems.init()
        val contentAfter = mainModFile.readText()
        assertTrue(
            contentAfter.contains("ModItems.init();"),
            "Main mod class should contain ModItems.init(); after creating an item"
        )

        // Verify the import was added
        assertTrue(
            contentAfter.contains("import com.testmod.registry.ModItems;"),
            "Main mod class should have import for ModItems"
        )
    }

    @Test
    fun `second item does not duplicate the init call`() {
        // Create first item
        context.withProjectDir {
            val command = CreateItemCommand()
            command.projectDir = context.projectDir
            command.parse(arrayOf("ruby_sword", "--type", "tool"))
        }

        // Create second item
        context.withProjectDir {
            val command = CreateItemCommand()
            command.projectDir = context.projectDir
            command.parse(arrayOf("emerald_gem", "--type", "basic"))
        }

        // Read main mod class
        val content = getMainModFile().readText()

        // Count occurrences of ModItems.init()
        val initCallCount = Regex(Regex.escape("ModItems.init();")).findAll(content).count()
        assertEquals(
            1, initCallCount,
            "ModItems.init(); should appear exactly once in the main mod class, but appeared $initCallCount times"
        )

        // Count occurrences of the import
        val importCount = Regex(Regex.escape("import com.testmod.registry.ModItems;")).findAll(content).count()
        assertEquals(
            1, importCount,
            "ModItems import should appear exactly once, but appeared $importCount times"
        )
    }

    @Test
    fun `items and blocks both add their init calls`() {
        // Create an item
        context.withProjectDir {
            val command = CreateItemCommand()
            command.projectDir = context.projectDir
            command.parse(arrayOf("ruby_sword", "--type", "tool"))
        }

        // Create a block
        context.withProjectDir {
            val command = CreateBlockCommand()
            command.projectDir = context.projectDir
            command.parse(arrayOf("ruby_ore", "--type", "ore"))
        }

        // Read main mod class
        val content = getMainModFile().readText()

        // Verify both init calls are present
        assertTrue(
            content.contains("ModItems.init();"),
            "Main mod class should contain ModItems.init(); after creating an item"
        )
        assertTrue(
            content.contains("ModBlocks.init();"),
            "Main mod class should contain ModBlocks.init(); after creating a block"
        )

        // Verify both imports are present
        assertTrue(
            content.contains("import com.testmod.registry.ModItems;"),
            "Main mod class should have import for ModItems"
        )
        assertTrue(
            content.contains("import com.testmod.registry.ModBlocks;"),
            "Main mod class should have import for ModBlocks"
        )

        // Verify each init call appears exactly once
        val itemsInitCount = Regex(Regex.escape("ModItems.init();")).findAll(content).count()
        assertEquals(
            1, itemsInitCount,
            "ModItems.init(); should appear exactly once"
        )

        val blocksInitCount = Regex(Regex.escape("ModBlocks.init();")).findAll(content).count()
        assertEquals(
            1, blocksInitCount,
            "ModBlocks.init(); should appear exactly once"
        )
    }
}
