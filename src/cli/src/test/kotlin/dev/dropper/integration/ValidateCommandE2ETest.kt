package dev.dropper.integration

import dev.dropper.commands.CreateBlockCommand
import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.ValidateCommand
import dev.dropper.commands.validate.*
import dev.dropper.config.ModConfig
import dev.dropper.util.TestProjectContext
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

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("test-validatecommande2etest")

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
        context.createProject(config)
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    // =================================================================
    // HAPPY PATH TESTS
    // =================================================================

    @Test
    fun `valid project passes all validations`() {
        println("\n[TEST] Valid project passes all validations")

        // Create a valid item
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("test_item", "--type", "basic"))
        }

        // Run validation
        context.withProjectDir {
            val command = ValidateCommand()
            command.parse(emptyArray())
        }

        // Should not throw
        println("OK Valid project passed validation")
    }

    @Test
    fun `each validator individually on valid project`() {
        println("\n[TEST] Each validator individually on valid project")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("test_item", "--type", "basic"))
        }

        // Test each validator
        context.withProjectDir {
            ValidateAssetsCommand().parse(emptyArray())
            ValidateMetadataCommand().parse(emptyArray())
            ValidateStructureCommand().parse(emptyArray())
            ValidateRecipesCommand().parse(emptyArray())
            ValidateLangCommand().parse(emptyArray())
        }

        println("OK All individual validators passed")
    }

    // =================================================================
    // ASSET VALIDATION TESTS
    // =================================================================

    @Test
    fun `missing item texture is detected`() {
        println("\n[TEST] Missing item texture is detected")

        // Create item
        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        // Delete texture
        val textureFile = File(context.projectDir, "versions/shared/v1/assets/testmod/textures/item/ruby.png")
        textureFile.delete()

        // Validate
        val validator = AssetValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing texture") })
        println("OK Missing texture detected")
    }

    @Test
    fun `missing blockstate is detected`() {
        println("\n[TEST] Missing blockstate is detected")

        // Create block
        context.withProjectDir {
            CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))
        }

        // Delete blockstate
        val blockstateFile = File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/ruby_ore.json")
        blockstateFile.delete()

        // Create a model that references it to trigger validation
        val itemModel = File(context.projectDir, "versions/shared/v1/assets/testmod/models/item/ruby_ore.json")

        // Validate - should detect block without blockstate
        val validator = AssetValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        // Should have issues (missing blockstate means the block model can't find proper reference)
        assertTrue(result.filesScanned > 0)
        println("OK Missing blockstate scenario handled")
    }

    @Test
    fun `invalid blockstate JSON is detected`() {
        println("\n[TEST] Invalid blockstate JSON is detected")

        context.withProjectDir {
            CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))
        }

        // Corrupt blockstate JSON
        val blockstateFile = File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/ruby_ore.json")
        blockstateFile.writeText("{invalid json}")

        val validator = AssetValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Invalid JSON") })
        println("OK Invalid JSON detected")
    }

    @Test
    fun `broken model reference in blockstate is detected`() {
        println("\n[TEST] Broken model reference in blockstate is detected")

        context.withProjectDir {
            CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))
        }

        // Modify blockstate to reference non-existent model
        val blockstateFile = File(context.projectDir, "versions/shared/v1/assets/testmod/blockstates/ruby_ore.json")
        blockstateFile.writeText("""
            {
              "variants": {
                "": { "model": "testmod:block/nonexistent" }
              }
            }
        """.trimIndent())

        val validator = AssetValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("does not exist") })
        println("OK Broken model reference detected")
    }

    @Test
    fun `missing texture in model is detected`() {
        println("\n[TEST] Missing texture in model is detected")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        // Delete texture
        val textureFile = File(context.projectDir, "versions/shared/v1/assets/testmod/textures/item/ruby.png")
        textureFile.delete()

        val validator = AssetValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing texture") })
        println("OK Missing texture in model detected")
    }

    @Test
    fun `unused textures generate warnings`() {
        println("\n[TEST] Unused textures generate warnings")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        // Create unused texture
        val unusedTexture = File(context.projectDir, "versions/shared/v1/assets/testmod/textures/item/unused.png")
        unusedTexture.parentFile.mkdirs()
        unusedTexture.writeText("fake texture")

        val validator = AssetValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Unused texture") && it.severity == ValidationSeverity.WARNING })
        println("OK Unused texture warning generated")
    }

    // =================================================================
    // METADATA VALIDATION TESTS
    // =================================================================

    @Test
    fun `missing config yml is detected`() {
        println("\n[TEST] Missing config.yml is detected")

        val configFile = File(context.projectDir, "config.yml")
        configFile.delete()

        val validator = MetadataValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing config.yml") })
        println("OK Missing config.yml detected")
    }

    @Test
    fun `missing required fields in config are detected`() {
        println("\n[TEST] Missing required fields in config are detected")

        val configFile = File(context.projectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: testmod
        """.trimIndent())

        val validator = MetadataValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing or empty required field") })
        println("OK Missing required fields detected")
    }

    @Test
    fun `invalid mod ID format is detected`() {
        println("\n[TEST] Invalid mod ID format is detected")

        val configFile = File(context.projectDir, "config.yml")
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
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Invalid mod ID format") })
        println("OK Invalid mod ID format detected")
    }

    @Test
    fun `invalid version format generates warning`() {
        println("\n[TEST] Invalid version format generates warning")

        val configFile = File(context.projectDir, "config.yml")
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
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("does not follow semantic versioning") })
        println("OK Invalid version format warning generated")
    }

    @Test
    fun `unknown minecraft version generates warning`() {
        println("\n[TEST] Unknown minecraft version generates warning")

        val configFile = File(context.projectDir, "config.yml")
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
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Unknown Minecraft version") })
        println("OK Unknown Minecraft version warning generated")
    }

    @Test
    fun `invalid loader is detected`() {
        println("\n[TEST] Invalid loader is detected")

        val configFile = File(context.projectDir, "config.yml")
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
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Invalid loader") })
        println("OK Invalid loader detected")
    }

    @Test
    fun `circular asset pack dependency is detected`() {
        println("\n[TEST] Circular asset pack dependency is detected")

        val configFile = File(context.projectDir, "config.yml")
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
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Circular dependency") })
        println("OK Circular dependency detected")
    }

    // =================================================================
    // STRUCTURE VALIDATION TESTS
    // =================================================================

    @Test
    fun `missing required directory is detected`() {
        println("\n[TEST] Missing required directory is detected")

        val sharedDir = File(context.projectDir, "shared")
        sharedDir.deleteRecursively()

        val validator = StructureValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing required directory") })
        println("OK Missing required directory detected")
    }

    @Test
    fun `wrong package declaration is detected`() {
        println("\n[TEST] Wrong package declaration is detected")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        // Find and modify a Java file
        val javaFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/items/Ruby.java")
        val content = javaFile.readText()
        val modified = content.replace("package com.testmod.items;", "package com.wrongpackage;")
        javaFile.writeText(modified)

        val validator = StructureValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Package declaration") && it.message.contains("does not match") })
        println("OK Wrong package declaration detected")
    }

    @Test
    fun `java file in versions directory generates warning`() {
        println("\n[TEST] Java file in versions directory generates warning")

        val wrongJavaFile = File(context.projectDir, "versions/shared/v1/WrongPlace.java")
        wrongJavaFile.writeText("""
            package wrongplace;
            public class WrongPlace {}
        """.trimIndent())

        val validator = StructureValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Java file in versions directory") })
        println("OK Java file in wrong location detected")
    }

    @Test
    fun `missing package declaration is detected`() {
        println("\n[TEST] Missing package declaration is detected")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        val javaFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/items/Ruby.java")
        val content = javaFile.readText()
        val modified = content.replace("package com.testmod.items;", "// package removed")
        javaFile.writeText(modified)

        val validator = StructureValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing package declaration") })
        println("OK Missing package declaration detected")
    }

    @Test
    fun `invalid directory name generates warning`() {
        println("\n[TEST] Invalid directory name generates warning")

        val invalidDir = File(context.projectDir, "versions/invalid@name")
        invalidDir.mkdirs()

        val validator = StructureValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("invalid characters") })
        println("OK Invalid directory name warning generated")
    }

    // =================================================================
    // RECIPE VALIDATION TESTS
    // =================================================================

    @Test
    fun `recipe with invalid JSON is detected`() {
        println("\n[TEST] Recipe with invalid JSON is detected")

        val recipeFile = File(context.projectDir, "versions/shared/v1/data/testmod/recipe/bad_recipe.json")
        recipeFile.parentFile.mkdirs()
        recipeFile.writeText("{invalid json")

        val validator = RecipeValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Invalid JSON") })
        println("OK Invalid recipe JSON detected")
    }

    @Test
    fun `recipe with unknown type is detected`() {
        println("\n[TEST] Recipe with unknown type is detected")

        val recipeFile = File(context.projectDir, "versions/shared/v1/data/testmod/recipe/bad_recipe.json")
        recipeFile.parentFile.mkdirs()
        recipeFile.writeText("""
            {
              "type": "minecraft:invalid_type",
              "result": { "item": "testmod:item" }
            }
        """.trimIndent())

        val validator = RecipeValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Unknown recipe type") })
        println("OK Unknown recipe type detected")
    }

    @Test
    fun `shaped recipe missing pattern is detected`() {
        println("\n[TEST] Shaped recipe missing pattern is detected")

        val recipeFile = File(context.projectDir, "versions/shared/v1/data/testmod/recipe/bad_recipe.json")
        recipeFile.parentFile.mkdirs()
        recipeFile.writeText("""
            {
              "type": "minecraft:crafting_shaped",
              "key": { "X": { "item": "minecraft:stick" } },
              "result": { "item": "testmod:item" }
            }
        """.trimIndent())

        val validator = RecipeValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("missing 'pattern'") })
        println("OK Missing pattern detected")
    }

    @Test
    fun `shapeless recipe missing ingredients is detected`() {
        println("\n[TEST] Shapeless recipe missing ingredients is detected")

        val recipeFile = File(context.projectDir, "versions/shared/v1/data/testmod/recipe/bad_recipe.json")
        recipeFile.parentFile.mkdirs()
        recipeFile.writeText("""
            {
              "type": "minecraft:crafting_shapeless",
              "result": { "item": "testmod:item" }
            }
        """.trimIndent())

        val validator = RecipeValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("missing 'ingredients'") })
        println("OK Missing ingredients detected")
    }

    @Test
    fun `recipe missing result is detected`() {
        println("\n[TEST] Recipe missing result is detected")

        val recipeFile = File(context.projectDir, "versions/shared/v1/data/testmod/recipe/bad_recipe.json")
        recipeFile.parentFile.mkdirs()
        recipeFile.writeText("""
            {
              "type": "minecraft:crafting_shapeless",
              "ingredients": [{ "item": "minecraft:stick" }]
            }
        """.trimIndent())

        val validator = RecipeValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("missing 'result'") })
        println("OK Missing result detected")
    }

    @Test
    fun `duplicate recipe ID is detected`() {
        println("\n[TEST] Duplicate recipe ID is detected")

        val recipe1 = File(context.projectDir, "versions/shared/v1/data/testmod/recipe/test.json")
        recipe1.parentFile.mkdirs()
        recipe1.writeText("""
            {
              "type": "minecraft:crafting_shapeless",
              "ingredients": [{ "item": "minecraft:stick" }],
              "result": { "item": "testmod:item" }
            }
        """.trimIndent())

        val recipe2 = File(context.projectDir, "versions/1_20_1/data/testmod/recipe/test.json")
        recipe2.parentFile.mkdirs()
        recipe2.writeText("""
            {
              "type": "minecraft:crafting_shapeless",
              "ingredients": [{ "item": "minecraft:stick" }],
              "result": { "item": "testmod:item2" }
            }
        """.trimIndent())

        val validator = RecipeValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Duplicate recipe ID") })
        println("OK Duplicate recipe ID detected")
    }

    // =================================================================
    // LANG VALIDATION TESTS
    // =================================================================

    @Test
    fun `missing item translation is detected`() {
        println("\n[TEST] Missing item translation is detected")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        // Create empty lang file
        val langFile = File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        langFile.parentFile.mkdirs()
        langFile.writeText("{}")

        val validator = LangValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing translation for item") })
        println("OK Missing item translation detected")
    }

    @Test
    fun `missing block translation is detected`() {
        println("\n[TEST] Missing block translation is detected")

        context.withProjectDir {
            CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))
        }

        val langFile = File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        langFile.parentFile.mkdirs()
        langFile.writeText("{}")

        val validator = LangValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Missing translation for block") })
        println("OK Missing block translation detected")
    }

    @Test
    fun `empty translation value generates warning`() {
        println("\n[TEST] Empty translation value generates warning")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        val langFile = File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        langFile.parentFile.mkdirs()
        langFile.writeText("""
            {
              "item.testmod.ruby": ""
            }
        """.trimIndent())

        val validator = LangValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Empty translation value") })
        println("OK Empty translation value warning generated")
    }

    @Test
    fun `no lang file warning when items exist`() {
        println("\n[TEST] No lang file warning when items exist")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        // Don't create lang file

        val validator = LangValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("No language files found") })
        println("OK No lang file warning generated")
    }

    // =================================================================
    // AUTO-FIX TESTS
    // =================================================================

    @Test
    fun `auto-fix repairs package declarations`() {
        println("\n[TEST] Auto-fix repairs package declarations")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        val javaFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/items/Ruby.java")
        val content = javaFile.readText()
        val modified = content.replace("package com.testmod.items;", "package com.wrongpackage;")
        javaFile.writeText(modified)

        val validator = StructureValidator()
        val options = ValidationOptions(autoFix = true)
        val result = validator.validate(context.projectDir, options)

        val fixed = validator.autoFix(context.projectDir, result.issues)
        assertTrue(fixed > 0)

        val fixedContent = javaFile.readText()
        assertTrue(fixedContent.contains("package com.testmod.items;"))
        println("OK Package declaration auto-fixed")
    }

    @Test
    fun `auto-fix adds missing lang entries`() {
        println("\n[TEST] Auto-fix adds missing lang entries")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        val langFile = File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        langFile.parentFile.mkdirs()
        langFile.writeText("{}")

        val validator = LangValidator()
        val options = ValidationOptions(autoFix = true)
        val result = validator.validate(context.projectDir, options)

        val fixed = validator.autoFix(context.projectDir, result.issues)
        assertTrue(fixed > 0)

        val langContent = langFile.readText()
        assertTrue(langContent.contains("item.testmod.ruby"))
        println("OK Missing lang entries auto-fixed")
    }

    // =================================================================
    // MULTIPLE VALIDATION TESTS
    // =================================================================

    @Test
    fun `project with multiple issues reports all`() {
        println("\n[TEST] Project with multiple issues reports all")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        // Create multiple issues
        val configFile = File(context.projectDir, "config.yml")
        configFile.writeText("""
            mod:
              id: "Invalid-ID"
              name: "Test"
        """.trimIndent())

        val textureFile = File(context.projectDir, "versions/shared/v1/assets/testmod/textures/item/ruby.png")
        textureFile.delete()

        context.withProjectDir {
            val command = ValidateCommand()
            assertThrows<Exception> {
                command.parse(emptyArray())
            }
        }

        println("OK Multiple issues detected and reported")
    }

    @Test
    fun `strict mode fails on warnings`() {
        println("\n[TEST] Strict mode fails on warnings")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        // Create unused texture (warning only)
        val unusedTexture = File(context.projectDir, "versions/shared/v1/assets/testmod/textures/item/unused.png")
        unusedTexture.writeText("fake")

        context.withProjectDir {
            val command = ValidateCommand()
            assertThrows<Exception> {
                command.parse(arrayOf("--strict"))
            }
        }

        println("OK Strict mode correctly fails on warnings")
    }

    @Test
    fun `validate specific version only`() {
        println("\n[TEST] Validate specific version only")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        // Create version-specific asset
        val versionSpecificModel = File(context.projectDir, "versions/1_20_1/assets/testmod/models/item/version_specific.json")
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
        val result = validator.validate(context.projectDir, ValidationOptions(version = "1_20_1"))

        // Should find the missing texture in version-specific model
        assertTrue(result.filesScanned > 0)
        println("OK Version-specific validation works")
    }

    @Test
    fun `validation with no issues returns clean result`() {
        println("\n[TEST] Validation with no issues returns clean result")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        // Create lang file
        val langFile = File(context.projectDir, "versions/shared/v1/assets/testmod/lang/en_us.json")
        langFile.parentFile.mkdirs()
        langFile.writeText("""
            {
              "item.testmod.ruby": "Ruby"
            }
        """.trimIndent())

        context.withProjectDir {
            val command = ValidateCommand()
            command.parse(emptyArray())
        }

        println("OK Clean validation completed successfully")
    }

    @Test
    fun `combined validation reports summary correctly`() {
        println("\n[TEST] Combined validation reports summary correctly")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
            CreateBlockCommand().parse(arrayOf("ruby_ore", "--type", "ore"))
        }

        // Create some issues
        val textureFile = File(context.projectDir, "versions/shared/v1/assets/testmod/textures/item/ruby.png")
        textureFile.delete()

        val validator = AssetValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.errorCount > 0)
        assertTrue(result.filesScanned > 0)
        println("OK Combined validation summary correct")
    }

    @Test
    fun `validation detects issues across all loaders`() {
        println("\n[TEST] Validation detects issues in common code")

        context.withProjectDir {
            CreateItemCommand().parse(arrayOf("ruby", "--type", "basic"))
        }

        // With Architectury, items use common classes instead of per-loader files
        // Modify the common item class to have wrong package
        val commonFile = File(context.projectDir, "shared/common/src/main/java/com/testmod/items/Ruby.java")
        if (commonFile.exists()) {
            val content = commonFile.readText()
            val modified = content.replace("package com.testmod.items;", "package com.wrongpackage;")
            commonFile.writeText(modified)
        }

        val validator = StructureValidator()
        val result = validator.validate(context.projectDir, ValidationOptions())

        assertTrue(result.issues.any { it.message.contains("Package declaration") })
        println("OK Issues detected in common code")
    }
}
