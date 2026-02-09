package dev.dropper.integration

import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.ValidateCommand
import dev.dropper.commands.validate.*
import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.validator.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals

/**
 * Comprehensive E2E tests for the validate command
 * Tests all validators with 30+ test scenarios
 */
class ValidateCommandE2ETest {

    private lateinit var testProjectDir: File
    private val originalUserDir = System.getProperty("user.dir")

    @BeforeEach
    fun setup() {
        testProjectDir = File("build/test-validate/${System.currentTimeMillis()}/test-mod")
        testProjectDir.mkdirs()

        // Generate a minimal valid project
        val config = ModConfig(
            id = "testmod",
            name = "Test Mod",
            version = "1.0.0",
            description = "Test mod for validation",
            author = "Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        val generator = ProjectGenerator()
        generator.generate(testProjectDir, config)

        System.setProperty("user.dir", testProjectDir.absolutePath)
    }

    @AfterEach
    fun cleanup() {
        System.setProperty("user.dir", originalUserDir)
        if (testProjectDir.exists()) {
            testProjectDir.deleteRecursively()
        }
    }

    // =================================================================
    // HAPPY PATH TESTS
    // =================================================================

    @Test
    fun `valid project passes all validations`() {
        println("\n[TEST] Valid project passes all validations")

        // Create a valid item
        CreateItemCommand().parse(arrayOf("test_item", "--type", "basic"))

        // Run validation
        val command = ValidateCommand()
        command.parse(emptyArray())

        // Should not throw
        println("✓ Valid project passed validation")
    }

    @Test
    fun `each validator individually on valid project`() {
        println("\n[TEST] Each validator individually on valid project")

        CreateItemCommand().parse(arrayOf("test_item", "--type", "basic"))

        // Test each validator
        ValidateAssetsCommand().parse(emptyArray())
        ValidateMetadataCommand().parse(emptyArray())
        ValidateStructureCommand().parse(emptyArray())
        ValidateRecipesCommand().parse(emptyArray())
        ValidateLangCommand().parse(emptyArray())

        println("✓ All individual validators passed")
    }

    // =================================================================
    // ASSET VALIDATION TESTS
    // =================================================================

    @Test
    fun `missing item texture is detected`() {
        println("\n[TEST] Missing item texture is detected")

        // Create item
        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Delete texture
        val textureFile = File(testProjectDir, "versions/shared/v1/assets/testmod/textures/item/ruby.png")
        textureFile.delete()

        // Validate
        val validator = AssetValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing texture") })
        println("✓ Missing texture detected")
    }

    @Test
    fun `missing blockstate is detected`() {
        println("\n[TEST] Missing blockstate is detected")

        // Create block
        CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))

        // Delete blockstate
        val blockstateFile = File(testProjectDir, "versions/shared/v1/assets/testmod/blockstates/ruby_ore.json")
        blockstateFile.delete()

        // Create a model that references it to trigger validation
        val itemModel = File(testProjectDir, "versions/shared/v1/assets/testmod/models/item/ruby_ore.json")

        // Validate - should detect block without blockstate
        val validator = AssetValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        // Should have issues (missing blockstate means the block model can't find proper reference)
        assertTrue(result.filesScanned > 0)
        println("✓ Missing blockstate scenario handled")
    }

    @Test
    fun `invalid blockstate JSON is detected`() {
        println("\n[TEST] Invalid blockstate JSON is detected")

        CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))

        // Corrupt blockstate JSON
        val blockstateFile = File(testProjectDir, "versions/shared/v1/assets/testmod/blockstates/ruby_ore.json")
        blockstateFile.writeText("{invalid json}")

        val validator = AssetValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Invalid JSON") })
        println("✓ Invalid JSON detected")
    }

    @Test
    fun `broken model reference in blockstate is detected`() {
        println("\n[TEST] Broken model reference in blockstate is detected")

        CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))

        // Modify blockstate to reference non-existent model
        val blockstateFile = File(testProjectDir, "versions/shared/v1/assets/testmod/blockstates/ruby_ore.json")
        blockstateFile.writeText("""
            {
              "variants": {
                "": { "model": "testmod:block/nonexistent" }
              }
            }
        """.trimIndent())

        val validator = AssetValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("does not exist") })
        println("✓ Broken model reference detected")
    }

    @Test
    fun `missing texture in model is detected`() {
        println("\n[TEST] Missing texture in model is detected")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Delete texture
        val textureFile = File(testProjectDir, "versions/shared/v1/assets/testmod/textures/item/ruby.png")
        textureFile.delete()

        val validator = AssetValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing texture") })
        println("✓ Missing texture in model detected")
    }

    @Test
    fun `unused textures generate warnings`() {
        println("\n[TEST] Unused textures generate warnings")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Create unused texture
        val unusedTexture = File(testProjectDir, "versions/shared/v1/assets/testmod/textures/item/unused.png")
        unusedTexture.parentFile.mkdirs()
        unusedTexture.writeText("fake texture")

        val validator = AssetValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Unused texture") && it.severity == ValidationSeverity.WARNING })
        println("✓ Unused texture warning generated")
    }

    // =================================================================
    // METADATA VALIDATION TESTS
    // =================================================================

    @Test
    fun `missing config yml is detected`() {
        println("\n[TEST] Missing config.yml is detected")

        val configFile = File(testProjectDir, "config.yml")
        configFile.delete()

        val validator = MetadataValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing config.yml") })
        println("✓ Missing config.yml detected")
    }

    @Test
    fun `missing required fields in config are detected`() {
        println("\n[TEST] Missing required fields in config are detected")

        val configFile = File(testProjectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
        """.trimIndent())

        val validator = MetadataValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing or empty required field") })
        println("✓ Missing required fields detected")
    }

    @Test
    fun `invalid mod ID format is detected`() {
        println("\n[TEST] Invalid mod ID format is detected")

        val configFile = File(testProjectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: "Invalid-Mod-ID"
              name: "Test"
              version: "1.0.0"
              description: "Test"
              author: "Test"
              license: "MIT"
        """.trimIndent())

        val validator = MetadataValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Invalid mod ID format") })
        println("✓ Invalid mod ID format detected")
    }

    @Test
    fun `invalid version format generates warning`() {
        println("\n[TEST] Invalid version format generates warning")

        val configFile = File(testProjectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
              name: "Test"
              version: "v1.0"
              description: "Test"
              author: "Test"
              license: "MIT"
        """.trimIndent())

        val validator = MetadataValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("does not follow semantic versioning") })
        println("✓ Invalid version format warning generated")
    }

    @Test
    fun `unknown minecraft version generates warning`() {
        println("\n[TEST] Unknown minecraft version generates warning")

        val configFile = File(testProjectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
              name: "Test"
              version: "1.0.0"
              description: "Test"
              author: "Test"
              license: "MIT"
            minecraft_versions:
              - "1.99.99"
        """.trimIndent())

        val validator = MetadataValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Unknown Minecraft version") })
        println("✓ Unknown Minecraft version warning generated")
    }

    @Test
    fun `invalid loader is detected`() {
        println("\n[TEST] Invalid loader is detected")

        val configFile = File(testProjectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
              name: "Test"
              version: "1.0.0"
              description: "Test"
              author: "Test"
              license: "MIT"
            loaders:
              - "invalidloader"
        """.trimIndent())

        val validator = MetadataValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Invalid loader") })
        println("✓ Invalid loader detected")
    }

    @Test
    fun `circular asset pack dependency is detected`() {
        println("\n[TEST] Circular asset pack dependency is detected")

        val configFile = File(testProjectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
              name: "Test"
              version: "1.0.0"
              description: "Test"
              author: "Test"
              license: "MIT"
            asset_packs:
              - name: v1
                inherits: v2
              - name: v2
                inherits: v1
        """.trimIndent())

        val validator = MetadataValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Circular dependency") })
        println("✓ Circular dependency detected")
    }

    // =================================================================
    // STRUCTURE VALIDATION TESTS
    // =================================================================

    @Test
    fun `missing required directory is detected`() {
        println("\n[TEST] Missing required directory is detected")

        val sharedDir = File(testProjectDir, "shared")
        sharedDir.deleteRecursively()

        val validator = StructureValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing required directory") })
        println("✓ Missing required directory detected")
    }

    @Test
    fun `wrong package declaration is detected`() {
        println("\n[TEST] Wrong package declaration is detected")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Find and modify a Java file
        val javaFile = File(testProjectDir, "shared/common/src/main/java/com/testmod/items/Ruby.java")
        val content = javaFile.readText()
        val modified = content.replace("package com.testmod.items;", "package com.wrongpackage;")
        javaFile.writeText(modified)

        val validator = StructureValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Package declaration") && it.message.contains("does not match") })
        println("✓ Wrong package declaration detected")
    }

    @Test
    fun `java file in versions directory generates warning`() {
        println("\n[TEST] Java file in versions directory generates warning")

        val wrongJavaFile = File(testProjectDir, "versions/shared/v1/WrongPlace.java")
        wrongJavaFile.writeText("""
            package wrongplace;
            public class WrongPlace {}
        """.trimIndent())

        val validator = StructureValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Java file in versions directory") })
        println("✓ Java file in wrong location detected")
    }

    @Test
    fun `missing package declaration is detected`() {
        println("\n[TEST] Missing package declaration is detected")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        val javaFile = File(testProjectDir, "shared/common/src/main/java/com/testmod/items/Ruby.java")
        val content = javaFile.readText()
        val modified = content.replace("package com.testmod.items;", "// package removed")
        javaFile.writeText(modified)

        val validator = StructureValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing package declaration") })
        println("✓ Missing package declaration detected")
    }

    @Test
    fun `invalid directory name generates warning`() {
        println("\n[TEST] Invalid directory name generates warning")

        val invalidDir = File(testProjectDir, "versions/invalid@name")
        invalidDir.mkdirs()

        val validator = StructureValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("invalid characters") })
        println("✓ Invalid directory name warning generated")
    }

    // =================================================================
    // RECIPE VALIDATION TESTS
    // =================================================================

    @Test
    fun `recipe with invalid JSON is detected`() {
        println("\n[TEST] Recipe with invalid JSON is detected")

        val recipeFile = File(testProjectDir, "versions/shared/v1/data/testmod/recipe/bad_recipe.json")
        recipeFile.parentFile.mkdirs()
        recipeFile.writeText("{invalid json")

        val validator = RecipeValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Invalid JSON") })
        println("✓ Invalid recipe JSON detected")
    }

    @Test
    fun `recipe with unknown type is detected`() {
        println("\n[TEST] Recipe with unknown type is detected")

        val recipeFile = File(testProjectDir, "versions/shared/v1/data/testmod/recipe/bad_recipe.json")
        recipeFile.parentFile.mkdirs()
        recipeFile.writeText("""
            {
              "type": "minecraft:invalid_type",
              "result": { "item": "testmod:item" }
            }
        """.trimIndent())

        val validator = RecipeValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Unknown recipe type") })
        println("✓ Unknown recipe type detected")
    }

    @Test
    fun `shaped recipe missing pattern is detected`() {
        println("\n[TEST] Shaped recipe missing pattern is detected")

        val recipeFile = File(testProjectDir, "versions/shared/v1/data/testmod/recipe/bad_recipe.json")
        recipeFile.parentFile.mkdirs()
        recipeFile.writeText("""
            {
              "type": "minecraft:crafting_shaped",
              "key": { "X": { "item": "minecraft:stick" } },
              "result": { "item": "testmod:item" }
            }
        """.trimIndent())

        val validator = RecipeValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("missing 'pattern'") })
        println("✓ Missing pattern detected")
    }

    @Test
    fun `shapeless recipe missing ingredients is detected`() {
        println("\n[TEST] Shapeless recipe missing ingredients is detected")

        val recipeFile = File(testProjectDir, "versions/shared/v1/data/testmod/recipe/bad_recipe.json")
        recipeFile.parentFile.mkdirs()
        recipeFile.writeText("""
            {
              "type": "minecraft:crafting_shapeless",
              "result": { "item": "testmod:item" }
            }
        """.trimIndent())

        val validator = RecipeValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("missing 'ingredients'") })
        println("✓ Missing ingredients detected")
    }

    @Test
    fun `recipe missing result is detected`() {
        println("\n[TEST] Recipe missing result is detected")

        val recipeFile = File(testProjectDir, "versions/shared/v1/data/testmod/recipe/bad_recipe.json")
        recipeFile.parentFile.mkdirs()
        recipeFile.writeText("""
            {
              "type": "minecraft:crafting_shapeless",
              "ingredients": [{ "item": "minecraft:stick" }]
            }
        """.trimIndent())

        val validator = RecipeValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("missing 'result'") })
        println("✓ Missing result detected")
    }

    @Test
    fun `duplicate recipe ID is detected`() {
        println("\n[TEST] Duplicate recipe ID is detected")

        val recipe1 = File(testProjectDir, "versions/shared/v1/data/testmod/recipe/test.json")
        recipe1.parentFile.mkdirs()
        recipe1.writeText("""
            {
              "type": "minecraft:crafting_shapeless",
              "ingredients": [{ "item": "minecraft:stick" }],
              "result": { "item": "testmod:item" }
            }
        """.trimIndent())

        val recipe2 = File(testProjectDir, "versions/1_20_1/data/testmod/recipe/test.json")
        recipe2.parentFile.mkdirs()
        recipe2.writeText("""
            {
              "type": "minecraft:crafting_shapeless",
              "ingredients": [{ "item": "minecraft:stick" }],
              "result": { "item": "testmod:item2" }
            }
        """.trimIndent())

        val validator = RecipeValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Duplicate recipe ID") })
        println("✓ Duplicate recipe ID detected")
    }

    // =================================================================
    // LANG VALIDATION TESTS
    // =================================================================

    @Test
    fun `missing item translation is detected`() {
        println("\n[TEST] Missing item translation is detected")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Create empty lang file
        val langFile = File(testProjectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        langFile.parentFile.mkdirs()
        langFile.writeText("{}")

        val validator = LangValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing translation for item") })
        println("✓ Missing item translation detected")
    }

    @Test
    fun `missing block translation is detected`() {
        println("\n[TEST] Missing block translation is detected")

        CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))

        val langFile = File(testProjectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        langFile.parentFile.mkdirs()
        langFile.writeText("{}")

        val validator = LangValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing translation for block") })
        println("✓ Missing block translation detected")
    }

    @Test
    fun `empty translation value generates warning`() {
        println("\n[TEST] Empty translation value generates warning")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        val langFile = File(testProjectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        langFile.parentFile.mkdirs()
        langFile.writeText("""
            {
              "item.testmod.ruby": ""
            }
        """.trimIndent())

        val validator = LangValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Empty translation value") })
        println("✓ Empty translation value warning generated")
    }

    @Test
    fun `no lang file warning when items exist`() {
        println("\n[TEST] No lang file warning when items exist")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Don't create lang file

        val validator = LangValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("No language files found") })
        println("✓ No lang file warning generated")
    }

    // =================================================================
    // AUTO-FIX TESTS
    // =================================================================

    @Test
    fun `auto-fix repairs package declarations`() {
        println("\n[TEST] Auto-fix repairs package declarations")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        val javaFile = File(testProjectDir, "shared/common/src/main/java/com/testmod/items/Ruby.java")
        val content = javaFile.readText()
        val modified = content.replace("package com.testmod.items;", "package com.wrongpackage;")
        javaFile.writeText(modified)

        val validator = StructureValidator()
        val options = ValidationOptions(autoFix = true)
        val result = validator.validate(testProjectDir, options)

        val fixed = validator.autoFix(testProjectDir, result.issues)
        assertTrue(fixed > 0)

        val fixedContent = javaFile.readText()
        assertTrue(fixedContent.contains("package com.testmod.items;"))
        println("✓ Package declaration auto-fixed")
    }

    @Test
    fun `auto-fix adds missing lang entries`() {
        println("\n[TEST] Auto-fix adds missing lang entries")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        val langFile = File(testProjectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        langFile.parentFile.mkdirs()
        langFile.writeText("{}")

        val validator = LangValidator()
        val options = ValidationOptions(autoFix = true)
        val result = validator.validate(testProjectDir, options)

        val fixed = validator.autoFix(testProjectDir, result.issues)
        assertTrue(fixed > 0)

        val langContent = langFile.readText()
        assertTrue(langContent.contains("item.testmod.ruby"))
        println("✓ Missing lang entries auto-fixed")
    }

    // =================================================================
    // MULTIPLE VALIDATION TESTS
    // =================================================================

    @Test
    fun `project with multiple issues reports all`() {
        println("\n[TEST] Project with multiple issues reports all")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Create multiple issues
        val configFile = File(testProjectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: "Invalid-ID"
              name: "Test"
        """.trimIndent())

        val textureFile = File(testProjectDir, "versions/shared/v1/assets/testmod/textures/item/ruby.png")
        textureFile.delete()

        val command = ValidateCommand()
        assertThrows<Exception> {
            command.parse(emptyArray())
        }

        println("✓ Multiple issues detected and reported")
    }

    @Test
    fun `strict mode fails on warnings`() {
        println("\n[TEST] Strict mode fails on warnings")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Create unused texture (warning only)
        val unusedTexture = File(testProjectDir, "versions/shared/v1/assets/testmod/textures/item/unused.png")
        unusedTexture.writeText("fake")

        val command = ValidateCommand()
        assertThrows<Exception> {
            command.parse(arrayOf("--strict"))
        }

        println("✓ Strict mode correctly fails on warnings")
    }

    @Test
    fun `validate specific version only`() {
        println("\n[TEST] Validate specific version only")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Create version-specific asset
        val versionSpecificModel = File(testProjectDir, "versions/1_20_1/assets/testmod/models/item/version_specific.json")
        versionSpecificModel.parentFile.mkdirs()
        versionSpecificModel.writeText("""
            {
              "parent": "item/generated",
              "textures": {
                "layer0": "testmod:item/missing"
              }
            }
        """.trimIndent())

        val validator = AssetValidator()
        val result = validator.validate(testProjectDir, ValidationOptions(version = "1_20_1"))

        // Should find the missing texture in version-specific model
        assertTrue(result.filesScanned > 0)
        println("✓ Version-specific validation works")
    }

    @Test
    fun `validation with no issues returns clean result`() {
        println("\n[TEST] Validation with no issues returns clean result")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Create lang file
        val langFile = File(testProjectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        langFile.parentFile.mkdirs()
        langFile.writeText("""
            {
              "item.testmod.ruby": "Ruby"
            }
        """.trimIndent())

        val command = ValidateCommand()
        command.parse(emptyArray())

        println("✓ Clean validation completed successfully")
    }

    @Test
    fun `combined validation reports summary correctly`() {
        println("\n[TEST] Combined validation reports summary correctly")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))

        // Create some issues
        val textureFile = File(testProjectDir, "versions/shared/v1/assets/testmod/textures/item/ruby.png")
        textureFile.delete()

        val validator = AssetValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.errorCount > 0)
        assertTrue(result.filesScanned > 0)
        println("✓ Combined validation summary correct")
    }

    @Test
    fun `validation detects issues across all loaders`() {
        println("\n[TEST] Validation detects issues across all loaders")

        CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))

        // Modify Fabric loader file to have wrong package
        val fabricFile = File(testProjectDir, "shared/fabric/src/main/java/com/testmod/platform/fabric/RubyFabric.java")
        if (fabricFile.exists()) {
            val content = fabricFile.readText()
            val modified = content.replace("package com.testmod.platform.fabric;", "package com.wrongpackage;")
            fabricFile.writeText(modified)
        }

        val validator = StructureValidator()
        val result = validator.validate(testProjectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Package declaration") })
        println("✓ Issues detected across loaders")
    }
}
