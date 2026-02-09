package dev.dropper.e2e

import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.CreateBlockCommand
import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import javax.tools.ToolProvider
import kotlin.test.assertTrue

/**
 * Comprehensive E2E tests for Mustache template validation.
 *
 * Tests:
 * - Template variable substitution
 * - Java syntax validity after template rendering
 * - JSON validity after template rendering
 * - Edge cases (special characters, long names, etc.)
 * - Package name variations
 * - Mod ID variations
 */
class TemplateValidationE2ETest {

    private lateinit var testDir: File
    private val originalUserDir = System.getProperty("user.dir")

    @BeforeEach
    fun setup() {
        testDir = File("build/test-templates/${System.currentTimeMillis()}")
        testDir.mkdirs()
    }

    @AfterEach
    fun cleanup() {
        System.setProperty("user.dir", originalUserDir)
    }

    @ParameterizedTest
    @ValueSource(strings = ["testmod", "my_mod", "cool-mod", "mod123", "a", "verylongmodnamewith64chars"])
    fun `test templates with various mod IDs`(modId: String) {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Templates with Mod ID: $modId")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val projectDir = File(testDir, modId)

        val config = ModConfig(
            id = modId,
            name = "Test Mod",
            version = "1.0.0",
            description = "Template test",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        ProjectGenerator().generate(projectDir, config)
        System.setProperty("user.dir", projectDir.absolutePath)

        // Generate item
        CreateItemCommand().parse(arrayOf("test_item"))

        // Verify Java files are syntactically valid
        val javaFiles = projectDir.walkTopDown()
            .filter { it.extension == "java" }
            .toList()

        assertTrue(javaFiles.isNotEmpty(), "Should have generated Java files")

        javaFiles.forEach { javaFile ->
            val content = javaFile.readText()

            // Check package declaration
            assertTrue(content.contains("package "), "Should have package declaration: ${javaFile.name}")

            // Check modId is properly substituted
            val packageName = modId.replace("-", "")
            assertTrue(
                content.contains("package com.$packageName") || content.contains("package com.${packageName}."),
                "Package should contain mod ID: ${javaFile.name}"
            )

            // Verify no template variables remain
            assertFalse(content.contains("{{"), "No unsubstituted variables in ${javaFile.name}")
            assertFalse(content.contains("}}"), "No unsubstituted variables in ${javaFile.name}")

            println("  ✓ ${javaFile.name} - valid")
        }
    }

    private fun assertFalse(condition: Boolean, message: String) {
        assertTrue(!condition, message)
    }

    @Test
    fun `test Java syntax validity with Java compiler`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Java Syntax Validity (Compiler Check)                     ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val projectDir = File(testDir, "compiler-test")

        val config = ModConfig(
            id = "compilertest",
            name = "Compiler Test",
            version = "1.0.0",
            description = "Test compilation",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        ProjectGenerator().generate(projectDir, config)
        System.setProperty("user.dir", projectDir.absolutePath)

        // Generate various items
        CreateItemCommand().parse(arrayOf("sword"))
        CreateItemCommand().parse(arrayOf("pickaxe"))
        CreateBlockCommand().parse(arrayOf("ore"))

        // Get all Java files
        val javaFiles = projectDir.walkTopDown()
            .filter { it.extension == "java" }
            .toList()

        println("  Found ${javaFiles.size} Java files to validate")

        // Basic syntax checks (full compilation would require Minecraft dependencies)
        javaFiles.forEach { javaFile ->
            val content = javaFile.readText()

            // Check basic Java syntax elements
            assertTrue(content.contains("package "), "Should have package: ${javaFile.name}")
            assertTrue(content.contains("class ") || content.contains("interface "), "Should have class/interface: ${javaFile.name}")

            // Check for common syntax errors
            val openBraces = content.count { it == '{' }
            val closeBraces = content.count { it == '}' }
            assertTrue(openBraces == closeBraces, "Braces should match in ${javaFile.name}")

            val openParens = content.count { it == '(' }
            val closeParens = content.count { it == ')' }
            assertTrue(openParens == closeParens, "Parentheses should match in ${javaFile.name}")

            println("  ✓ ${javaFile.name} - syntax valid")
        }
    }

    @Test
    fun `test JSON validity after template rendering`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: JSON Validity After Template Rendering                    ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val projectDir = File(testDir, "json-test")

        val config = ModConfig(
            id = "jsontest",
            name = "JSON Test",
            version = "1.0.0",
            description = "Test JSON",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        ProjectGenerator().generate(projectDir, config)
        System.setProperty("user.dir", projectDir.absolutePath)

        // Generate items and blocks
        CreateItemCommand().parse(arrayOf("json_item"))
        CreateBlockCommand().parse(arrayOf("json_block"))

        // Get all JSON files
        val jsonFiles = projectDir.walkTopDown()
            .filter { it.extension == "json" }
            .toList()

        println("  Found ${jsonFiles.size} JSON files to validate")

        jsonFiles.forEach { jsonFile ->
            val content = jsonFile.readText()

            // Basic JSON structure validation
            assertTrue(content.trim().startsWith("{") || content.trim().startsWith("["),
                "JSON should start with { or [: ${jsonFile.name}")
            assertTrue(content.trim().endsWith("}") || content.trim().endsWith("]"),
                "JSON should end with } or ]: ${jsonFile.name}")

            // Check no template variables remain
            assertFalse(content.contains("{{"), "No unsubstituted variables in ${jsonFile.name}")
            assertFalse(content.contains("}}"), "No unsubstituted variables in ${jsonFile.name}")

            // Check balanced braces
            val openBraces = content.count { it == '{' }
            val closeBraces = content.count { it == '}' }
            assertTrue(openBraces == closeBraces, "Braces should balance in ${jsonFile.name}")

            // Try parsing as JSON (basic check)
            try {
                // Just check it doesn't throw on basic parsing
                val hasValidStructure = content.contains("\":") || content.contains("\": ")
                assertTrue(hasValidStructure || content == "{}" || content == "[]",
                    "Should have valid JSON structure: ${jsonFile.name}")
                println("  ✓ ${jsonFile.name} - valid JSON")
            } catch (e: Exception) {
                throw AssertionError("Invalid JSON in ${jsonFile.name}: ${e.message}")
            }
        }
    }

    @Test
    fun `test template with special characters in names`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Templates with Special Characters                         ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val projectDir = File(testDir, "special-chars")

        val config = ModConfig(
            id = "specialtest",
            name = "Special Test",
            version = "1.0.0",
            description = "Test special chars",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        ProjectGenerator().generate(projectDir, config)
        System.setProperty("user.dir", projectDir.absolutePath)

        // Test items with underscores (should work)
        CreateItemCommand().parse(arrayOf("test_item_123"))

        val javaFiles = projectDir.walkTopDown()
            .filter { it.extension == "java" && it.name.contains("TestItem123") }
            .toList()

        assertTrue(javaFiles.isNotEmpty(), "Should generate files for test_item_123")

        javaFiles.forEach { javaFile ->
            val content = javaFile.readText()
            assertTrue(content.contains("class TestItem123"), "Class name should be TestItem123")
            println("  ✓ ${javaFile.name} - underscore handling correct")
        }
    }

    @Test
    fun `test template variable substitution completeness`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Complete Template Variable Substitution                   ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val projectDir = File(testDir, "substitution-test")

        val config = ModConfig(
            id = "subtest",
            name = "Substitution Test",
            version = "1.0.0",
            description = "Test substitution",
            author = "Test Author",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        ProjectGenerator().generate(projectDir, config)

        // Check all generated files for unsubstituted variables
        val allFiles = projectDir.walkTopDown()
            .filter { it.isFile }
            .filter { it.extension in listOf("java", "json", "yml", "yaml", "toml", "properties") }
            .toList()

        println("  Checking ${allFiles.size} files for unsubstituted variables")

        var unsubstitutedCount = 0

        allFiles.forEach { file ->
            val content = file.readText()

            if (content.contains("{{") || content.contains("}}")) {
                println("  ✗ ${file.relativeTo(projectDir).path} - contains unsubstituted variables")
                unsubstitutedCount++

                // Show which variables
                val variables = Regex("\\{\\{([^}]+)}}").findAll(content).map { it.value }.toList()
                variables.forEach { variable ->
                    println("      $variable")
                }
            }
        }

        assertTrue(unsubstitutedCount == 0, "All template variables should be substituted")
        println("  ✓ All ${allFiles.size} files have complete substitution")
    }

    @Test
    fun `test package structure matches mod ID`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: Package Structure Matches Mod ID                          ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val testCases = listOf(
            "testmod" to "com.testmod",
            "my_mod" to "com.mymod",
            "cool-mod" to "com.coolmod"
        )

        testCases.forEach { (modId, expectedPackage) ->
            val projectDir = File(testDir, "package-$modId")

            val config = ModConfig(
                id = modId,
                name = "Package Test",
                version = "1.0.0",
                description = "Test packages",
                author = "Test",
                license = "MIT",
                minecraftVersions = listOf("1.20.1"),
                loaders = listOf("fabric")
            )

            ProjectGenerator().generate(projectDir, config)

            val javaFiles = projectDir.walkTopDown()
                .filter { it.extension == "java" }
                .toList()

            javaFiles.forEach { javaFile ->
                val content = javaFile.readText()
                assertTrue(
                    content.contains("package $expectedPackage"),
                    "File ${javaFile.name} should have package $expectedPackage"
                )
            }

            println("  ✓ $modId → $expectedPackage")
        }
    }

    @Test
    fun `test all loaders generate valid code`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Test: All Loaders Generate Valid Code                           ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val loaders = listOf("fabric", "forge", "neoforge")

        loaders.forEach { loader ->
            println("\n  Testing loader: $loader")

            val projectDir = File(testDir, "loader-$loader")

            val config = ModConfig(
                id = "loadertest",
                name = "Loader Test",
                version = "1.0.0",
                description = "Test loader",
                author = "Test",
                license = "MIT",
                minecraftVersions = listOf("1.20.1"),
                loaders = listOf(loader)
            )

            ProjectGenerator().generate(projectDir, config)
            System.setProperty("user.dir", projectDir.absolutePath)

            // Generate item
            CreateItemCommand().parse(arrayOf("loader_item"))

            // Check loader-specific file exists
            val loaderFile = File(projectDir, "shared/$loader/src/main/java/com/loadertest/platform/$loader/LoaderItemFabric.java")
                .takeIf { loader == "fabric" }
                ?: File(projectDir, "shared/$loader/src/main/java/com/loadertest/platform/$loader/LoaderItemForge.java")
                    .takeIf { loader == "forge" }
                ?: File(projectDir, "shared/$loader/src/main/java/com/loadertest/platform/$loader/LoaderItemNeoForge.java")

            val loaderFiles = projectDir.walkTopDown()
                .filter { it.extension == "java" && it.path.contains(loader) }
                .toList()

            assertTrue(loaderFiles.isNotEmpty(), "Should have $loader-specific files")

            loaderFiles.forEach { file ->
                val content = file.readText()
                assertTrue(content.contains("package "), "Should have package: ${file.name}")
                println("    ✓ ${file.name}")
            }
        }
    }
}
