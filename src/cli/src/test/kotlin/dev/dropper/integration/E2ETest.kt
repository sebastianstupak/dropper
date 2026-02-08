package dev.dropper.integration

import dev.dropper.config.ModConfig
import dev.dropper.generator.ItemGenerator
import dev.dropper.generator.ProjectGenerator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue

/**
 * End-to-end tests for Dropper CLI
 */
class E2ETest {

    private lateinit var testDir: File

    @BeforeEach
    fun setup() {
        // Create temporary test directory
        testDir = File("build/test-projects/${System.currentTimeMillis()}")
        testDir.mkdirs()
    }

    @AfterEach
    fun cleanup() {
        // Clean up test directory
        testDir.deleteRecursively()
    }

    @Test
    fun `test complete project generation workflow`() {
        val projectDir = File(testDir, "test-mod")

        // Create mod config
        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "A test mod",
            author = "Test Author",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "neoforge")
        )

        // Generate project
        val generator = ProjectGenerator()
        generator.generate(projectDir, config)

        // Verify project structure
        assertTrue(projectDir.exists(), "Project directory should exist")
        assertTrue(File(projectDir, "config.yml").exists(), "config.yml should exist")
        assertTrue(File(projectDir, "build.gradle.kts").exists(), "build.gradle.kts should exist")
        assertTrue(File(projectDir, "settings.gradle.kts").exists(), "settings.gradle.kts should exist")
        assertTrue(File(projectDir, "README.md").exists(), "README.md should exist")
        assertTrue(File(projectDir, ".gitignore").exists(), ".gitignore should exist")
        assertTrue(File(projectDir, "gradle.properties").exists(), "gradle.properties should exist")

        // Verify build-logic
        assertTrue(File(projectDir, "build-logic").exists(), "build-logic should exist")
        assertTrue(File(projectDir, "build-logic/build.gradle.kts").exists(), "build-logic build.gradle.kts should exist")
        assertTrue(File(projectDir, "build-logic/settings.gradle.kts").exists(), "build-logic settings.gradle.kts should exist")

        // Verify shared code structure
        assertTrue(File(projectDir, "shared/common/src/main/java/com/testmod").exists(), "Shared common source should exist")
        assertTrue(File(projectDir, "shared/neoforge/src/main/java/com/testmod/platform").exists(), "NeoForge platform should exist")
        assertTrue(File(projectDir, "shared/fabric/src/main/java/com/testmod/platform").exists(), "Fabric platform should exist")

        // Verify generated Java files
        assertTrue(File(projectDir, "shared/common/src/main/java/com/testmod/Services.java").exists(), "Services.java should exist")
        assertTrue(File(projectDir, "shared/common/src/main/java/com/testmod/platform/PlatformHelper.java").exists(), "PlatformHelper.java should exist")
        assertTrue(File(projectDir, "shared/common/src/main/java/com/testmod/TestMod.java").exists(), "Main mod class should exist")

        // Verify version structure
        assertTrue(File(projectDir, "versions/shared/v1/config.yml").exists(), "Asset pack v1 config should exist")
        assertTrue(File(projectDir, "versions/1_20_1/config.yml").exists(), "1.20.1 version config should exist")
        assertTrue(File(projectDir, "versions/1_21_1/config.yml").exists(), "1.21.1 version config should exist")

        // Verify asset directories
        assertTrue(File(projectDir, "versions/shared/v1/assets/testmod").exists(), "Assets directory should exist")
        assertTrue(File(projectDir, "versions/shared/v1/data/testmod").exists(), "Data directory should exist")
    }

    @Test
    fun `test item generation in generated project`() {
        val projectDir = File(testDir, "test-mod-with-items")

        // First generate the project
        val config = ModConfig(
            id = "itemmod",
            name = "Item Mod",
            version = "1.0.0",
            description = "A mod with items",
            author = "Test Author",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        val projectGenerator = ProjectGenerator()
        projectGenerator.generate(projectDir, config)

        // Now generate an item
        val itemGenerator = ItemGenerator()
        itemGenerator.generate(projectDir, "ruby_sword", "com.itemmod", "itemmod")

        // Verify item files were created
        assertTrue(
            File(projectDir, "shared/common/src/main/java/com/itemmod/items/RubySword.java").exists(),
            "RubySword.java should exist"
        )
        assertTrue(
            File(projectDir, "versions/shared/v1/assets/itemmod/models/item/ruby_sword.json").exists(),
            "Item model should exist"
        )
        assertTrue(
            File(projectDir, "versions/shared/v1/assets/itemmod/textures/item/ruby_sword.png").exists(),
            "Item texture placeholder should exist"
        )
        assertTrue(
            File(projectDir, "versions/shared/v1/data/itemmod/recipes/ruby_sword.json").exists(),
            "Item recipe should exist"
        )

        // Verify Java class content
        val javaContent = File(projectDir, "shared/common/src/main/java/com/itemmod/items/RubySword.java").readText()
        assertTrue(javaContent.contains("class RubySword"), "Java class should be named RubySword")
        assertTrue(javaContent.contains("package com.itemmod.items"), "Package should be correct")

        // Verify model content
        val modelContent = File(projectDir, "versions/shared/v1/assets/itemmod/models/item/ruby_sword.json").readText()
        assertTrue(modelContent.contains("item/generated"), "Model should have generated parent")
        assertTrue(modelContent.contains("itemmod:item/ruby_sword"), "Model should reference correct texture")

        // Verify recipe content
        val recipeContent = File(projectDir, "versions/shared/v1/data/itemmod/recipes/ruby_sword.json").readText()
        assertTrue(recipeContent.contains("minecraft:crafting_shaped"), "Recipe should be shaped crafting")
        assertTrue(recipeContent.contains("itemmod:ruby_sword"), "Recipe should produce correct item")
    }

    @Test
    fun `test config yml content is correct`() {
        val projectDir = File(testDir, "config-test")

        val config = ModConfig(
            id = "awesome-mod",
            name = "Awesome Mod",
            version = "2.0.0",
            description = "An awesome test mod",
            author = "Awesome Author",
            license = "Apache-2.0",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        val generator = ProjectGenerator()
        generator.generate(projectDir, config)

        val configContent = File(projectDir, "config.yml").readText()

        assertTrue(configContent.contains("id: awesome-mod"), "Config should have correct ID")
        assertTrue(configContent.contains("name: \"Awesome Mod\""), "Config should have correct name")
        assertTrue(configContent.contains("version: \"2.0.0\""), "Config should have correct version")
        assertTrue(configContent.contains("description: \"An awesome test mod\""), "Config should have correct description")
        assertTrue(configContent.contains("author: \"Awesome Author\""), "Config should have correct author")
        assertTrue(configContent.contains("license: \"Apache-2.0\""), "Config should have correct license")
    }

    @Test
    fun `test gitignore contains necessary entries`() {
        val projectDir = File(testDir, "gitignore-test")

        val config = ModConfig(
            id = "gitmod",
            name = "Git Mod",
            version = "1.0.0",
            description = "Test gitignore",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        val generator = ProjectGenerator()
        generator.generate(projectDir, config)

        val gitignoreContent = File(projectDir, ".gitignore").readText()

        assertTrue(gitignoreContent.contains(".gradle/"), "Should ignore .gradle")
        assertTrue(gitignoreContent.contains("build/"), "Should ignore build")
        assertTrue(gitignoreContent.contains("build-temp/"), "Should ignore build-temp")
        assertTrue(gitignoreContent.contains(".idea/"), "Should ignore .idea")
        assertTrue(gitignoreContent.contains("*.iml"), "Should ignore .iml files")
    }

    @Test
    fun `test multiple minecraft versions create correct structure`() {
        val projectDir = File(testDir, "multi-version-test")

        val config = ModConfig(
            id = "multimod",
            name = "Multi Mod",
            version = "1.0.0",
            description = "Multi-version test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.20.4", "1.21.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        val generator = ProjectGenerator()
        generator.generate(projectDir, config)

        // Verify all version directories exist
        assertTrue(File(projectDir, "versions/1_20_1").exists(), "1.20.1 directory should exist")
        assertTrue(File(projectDir, "versions/1_20_4").exists(), "1.20.4 directory should exist")
        assertTrue(File(projectDir, "versions/1_21_1").exists(), "1.21.1 directory should exist")

        // Verify each version has config
        assertTrue(File(projectDir, "versions/1_20_1/config.yml").exists(), "1.20.1 config should exist")
        assertTrue(File(projectDir, "versions/1_20_4/config.yml").exists(), "1.20.4 config should exist")
        assertTrue(File(projectDir, "versions/1_21_1/config.yml").exists(), "1.21.1 config should exist")

        // Verify loader directories in each version
        listOf("1_20_1", "1_20_4", "1_21_1").forEach { version ->
            assertTrue(File(projectDir, "versions/$version/fabric").exists(), "$version fabric should exist")
            assertTrue(File(projectDir, "versions/$version/forge").exists(), "$version forge should exist")
            assertTrue(File(projectDir, "versions/$version/neoforge").exists(), "$version neoforge should exist")
        }
    }
}
