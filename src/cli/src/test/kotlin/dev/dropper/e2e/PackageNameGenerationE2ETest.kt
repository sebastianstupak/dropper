package dev.dropper.e2e

import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.CreateBlockCommand
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.File
import kotlin.test.assertTrue

/**
 * E2E tests verifying that package names are correctly sanitized
 * when generating projects and components with various mod IDs
 */
class PackageNameGenerationE2ETest {

    private lateinit var testDir: File
    private val originalUserDir = System.getProperty("user.dir")

    @BeforeEach
    fun setup() {
        testDir = File("build/test-package-generation/${System.currentTimeMillis()}")
        testDir.mkdirs()
    }

    @AfterEach
    fun cleanup() {
        System.setProperty("user.dir", originalUserDir)
        testDir.deleteRecursively()
    }

    @ParameterizedTest
    @CsvSource(
        "my_mod, mymod",
        "cool-mod, coolmod",
        "test_123, test123",
        "super-cool_mod, supercoolmod",
        "my_fancy_mod, myfancymod"
    )
    fun `project generation creates correct package structure`(modId: String, expectedPackageName: String) {
        val projectDir = File(testDir, modId)

        val config = ModConfig(
            id = modId,
            name = "Test Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        ProjectGenerator().generate(projectDir, config)

        // Verify package directory structure
        val packagePath = "com/$expectedPackageName"
        val commonDir = File(projectDir, "shared/common/src/main/java/$packagePath")
        assertTrue(
            commonDir.exists(),
            "Package directory should exist at $packagePath"
        )

        // Verify Services.java has correct package
        val servicesFile = File(projectDir, "shared/common/src/main/java/$packagePath/Services.java")
        assertTrue(servicesFile.exists(), "Services.java should exist")

        val servicesContent = servicesFile.readText()
        assertTrue(
            servicesContent.contains("package com.$expectedPackageName;"),
            "Services.java should have correct package declaration"
        )
    }

    @Test
    fun `item generation creates files in correct package`() {
        val modId = "my_test_mod"
        val expectedPackage = "mytestmod"
        val projectDir = File(testDir, "item-test")

        // Generate project
        val config = ModConfig(
            id = modId,
            name = "Test Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )
        ProjectGenerator().generate(projectDir, config)

        // Set working directory for command
        System.setProperty("user.dir", projectDir.absolutePath)

        // Generate item
        CreateItemCommand().parse(arrayOf("test_item"))

        // Verify item class exists in correct package
        val itemFile = File(projectDir, "shared/common/src/main/java/com/$expectedPackage/items/TestItem.java")
        assertTrue(itemFile.exists(), "Item class should exist in correct package")

        val itemContent = itemFile.readText()
        assertTrue(
            itemContent.contains("package com.$expectedPackage.items;"),
            "Item class should have correct package declaration"
        )

        // Verify loader-specific files
        val fabricFile = File(projectDir, "shared/fabric/src/main/java/com/$expectedPackage/platform/fabric/TestItemFabric.java")
        assertTrue(fabricFile.exists(), "Fabric item registration should exist")

        val fabricContent = fabricFile.readText()
        assertTrue(
            fabricContent.contains("package com.$expectedPackage.platform.fabric;"),
            "Fabric registration should have correct package"
        )
        assertTrue(
            fabricContent.contains("import com.$expectedPackage.items.TestItem;"),
            "Fabric registration should have correct import"
        )
    }

    @Test
    fun `block generation creates files in correct package`() {
        val modId = "cool-block-mod"
        val expectedPackage = "coolblockmod"
        val projectDir = File(testDir, "block-test")

        // Generate project
        val config = ModConfig(
            id = modId,
            name = "Cool Block Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )
        ProjectGenerator().generate(projectDir, config)

        System.setProperty("user.dir", projectDir.absolutePath)

        // Generate block
        CreateBlockCommand().parse(arrayOf("custom_block"))

        // Verify block class
        val blockFile = File(projectDir, "shared/common/src/main/java/com/$expectedPackage/blocks/CustomBlock.java")
        assertTrue(blockFile.exists(), "Block class should exist")

        val blockContent = blockFile.readText()
        assertTrue(
            blockContent.contains("package com.$expectedPackage.blocks;"),
            "Block should have correct package"
        )

        // Verify all loader-specific files have correct packages
        listOf("fabric", "forge", "neoforge").forEach { loader ->
            val loaderFile = File(
                projectDir,
                "shared/$loader/src/main/java/com/$expectedPackage/platform/$loader/CustomBlock${loader.replaceFirstChar { it.uppercase() }}.java"
            )
            assertTrue(loaderFile.exists(), "$loader block registration should exist")

            val loaderContent = loaderFile.readText()
            assertTrue(
                loaderContent.contains("package com.$expectedPackage.platform.$loader;"),
                "$loader registration should have correct package"
            )
        }
    }

    @Test
    fun `multiple components use consistent package names`() {
        val modId = "multi_component-mod"
        val expectedPackage = "multicomponentmod"
        val projectDir = File(testDir, "multi-component")

        val config = ModConfig(
            id = modId,
            name = "Multi Component Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )
        ProjectGenerator().generate(projectDir, config)

        System.setProperty("user.dir", projectDir.absolutePath)

        // Generate multiple components
        CreateItemCommand().parse(arrayOf("sword"))
        CreateItemCommand().parse(arrayOf("pickaxe"))
        CreateBlockCommand().parse(arrayOf("ore"))

        // Verify all components use the same package structure
        val sword = File(projectDir, "shared/common/src/main/java/com/$expectedPackage/items/Sword.java")
        val pickaxe = File(projectDir, "shared/common/src/main/java/com/$expectedPackage/items/Pickaxe.java")
        val ore = File(projectDir, "shared/common/src/main/java/com/$expectedPackage/blocks/Ore.java")

        assertTrue(sword.exists() && pickaxe.exists() && ore.exists(), "All components should exist")

        // Verify all have consistent package names
        listOf(sword, pickaxe).forEach { file ->
            val content = file.readText()
            assertTrue(
                content.contains("package com.$expectedPackage.items;"),
                "${file.name} should have correct package"
            )
        }

        val oreContent = ore.readText()
        assertTrue(
            oreContent.contains("package com.$expectedPackage.blocks;"),
            "Ore block should have correct package"
        )
    }

    @Test
    fun `assets use original mod ID while code uses sanitized package`() {
        val modId = "my_awesome-mod"
        val expectedPackage = "myawesomemod"
        val projectDir = File(testDir, "assets-test")

        val config = ModConfig(
            id = modId,
            name = "Test Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )
        ProjectGenerator().generate(projectDir, config)

        System.setProperty("user.dir", projectDir.absolutePath)
        CreateItemCommand().parse(arrayOf("test_item"))

        // Verify Java code uses sanitized package
        val itemFile = File(projectDir, "shared/common/src/main/java/com/$expectedPackage/items/TestItem.java")
        assertTrue(itemFile.exists())
        assertTrue(itemFile.readText().contains("package com.$expectedPackage.items;"))

        // Verify assets use original mod ID
        val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/item/test_item.json")
        assertTrue(modelFile.exists(), "Asset should use original mod ID: $modId")

        val recipeFile = File(projectDir, "versions/shared/v1/data/$modId/recipes/test_item.json")
        assertTrue(recipeFile.exists(), "Data file should use original mod ID: $modId")
    }

    @Test
    fun `very long mod ID is sanitized correctly`() {
        val modId = "my_super_long_mod_name_with_underscores"
        val expectedPackage = "mysuperlongmodnamewithunderscores"
        val projectDir = File(testDir, "long-id")

        val config = ModConfig(
            id = modId,
            name = "Long Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )
        ProjectGenerator().generate(projectDir, config)

        val packageDir = File(projectDir, "shared/common/src/main/java/com/$expectedPackage")
        assertTrue(packageDir.exists(), "Long package name should work")
    }

    @Test
    fun `mixed hyphens and underscores are both removed`() {
        val modId = "my-cool_test-mod_123"
        val expectedPackage = "mycooltestmod123"
        val projectDir = File(testDir, "mixed-chars")

        val config = ModConfig(
            id = modId,
            name = "Mixed Mod",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )
        ProjectGenerator().generate(projectDir, config)

        val servicesFile = File(projectDir, "shared/common/src/main/java/com/$expectedPackage/Services.java")
        assertTrue(servicesFile.exists())

        val content = servicesFile.readText()
        assertTrue(content.contains("package com.$expectedPackage;"))
        assertTrue(content.contains("import com.$expectedPackage.platform.PlatformHelper;"))
    }

    @Test
    fun `all loader registrations use sanitized package names`() {
        val modId = "test_loader-mod"
        val expectedPackage = "testloadermod"
        val projectDir = File(testDir, "loader-test")

        val config = ModConfig(
            id = modId,
            name = "Loader Test",
            version = "1.0.0",
            description = "Test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )
        ProjectGenerator().generate(projectDir, config)

        System.setProperty("user.dir", projectDir.absolutePath)
        CreateItemCommand().parse(arrayOf("loader_test_item"))

        // Check each loader's registration
        val loaders = mapOf(
            "fabric" to "Fabric",
            "forge" to "Forge",
            "neoforge" to "NeoForge"
        )

        loaders.forEach { (loader, className) ->
            val file = File(
                projectDir,
                "shared/$loader/src/main/java/com/$expectedPackage/platform/$loader/LoaderTestItem$className.java"
            )
            assertTrue(file.exists(), "$loader registration should exist")

            val content = file.readText()
            assertTrue(
                content.contains("package com.$expectedPackage.platform.$loader;"),
                "$loader should use sanitized package"
            )
            assertTrue(
                content.contains("import com.$expectedPackage.items.LoaderTestItem;"),
                "$loader should import from sanitized package"
            )
        }
    }
}
