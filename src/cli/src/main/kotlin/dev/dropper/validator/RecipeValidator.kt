package dev.dropper.validator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Validates recipe JSON files
 */
class RecipeValidator : Validator {
    override val name = "Recipe Validator"

    private val objectMapper = ObjectMapper()
    private val validRecipeTypes = setOf(
        "minecraft:crafting_shaped",
        "minecraft:crafting_shapeless",
        "minecraft:smelting",
        "minecraft:blasting",
        "minecraft:smoking",
        "minecraft:campfire_cooking",
        "minecraft:stonecutting",
        "minecraft:smithing",
        "minecraft:smithing_transform",
        "minecraft:smithing_trim"
    )

    override fun validate(projectDir: File, options: ValidationOptions): ValidationResult {
        val startTime = System.currentTimeMillis()
        val issues = mutableListOf<ValidationIssue>()
        var filesScanned = 0

        // Find all recipe directories
        val versionsDir = File(projectDir, "versions")
        if (!versionsDir.exists()) {
            return ValidationResult(name, emptyList(), 0, System.currentTimeMillis() - startTime)
        }

        val recipeIds = mutableSetOf<String>()
        val recipeFiles = mutableListOf<File>()

        versionsDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "json" && file.path.contains("/recipe/")) {
                recipeFiles.add(file)
            }
        }

        recipeFiles.forEach { file ->
            filesScanned++

            // Get recipe ID from path
            val recipeId = file.nameWithoutExtension
            val fullId = getModIdFromPath(file) + ":" + recipeId

            if (recipeIds.contains(fullId)) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Duplicate recipe ID: $fullId",
                        file,
                        null,
                        "Recipe IDs must be unique across all versions"
                    )
                )
            }
            recipeIds.add(fullId)

            issues.addAll(validateRecipe(file, projectDir))
        }

        return ValidationResult(name, issues, filesScanned, System.currentTimeMillis() - startTime)
    }

    private fun validateRecipe(file: File, projectDir: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        try {
            val json = objectMapper.readValue<Map<String, Any>>(file)

            // Validate type
            val type = json["type"] as? String
            if (type == null) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Recipe missing 'type' field",
                        file,
                        null,
                        "Add 'type' field with valid recipe type"
                    )
                )
                return issues
            }

            if (type !in validRecipeTypes) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Unknown recipe type: '$type'",
                        file,
                        null,
                        "Valid types: ${validRecipeTypes.joinToString(", ")}"
                    )
                )
            }

            // Validate based on type
            when (type) {
                "minecraft:crafting_shaped" -> {
                    issues.addAll(validateShapedRecipe(json, file, projectDir))
                }
                "minecraft:crafting_shapeless" -> {
                    issues.addAll(validateShapelessRecipe(json, file, projectDir))
                }
                "minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking" -> {
                    issues.addAll(validateCookingRecipe(json, file, projectDir))
                }
                "minecraft:stonecutting" -> {
                    issues.addAll(validateStonecuttingRecipe(json, file, projectDir))
                }
            }

        } catch (e: Exception) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Invalid JSON: ${e.message}",
                    file,
                    null,
                    "Fix JSON syntax errors"
                )
            )
        }

        return issues
    }

    private fun validateShapedRecipe(json: Map<String, Any>, file: File, projectDir: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        // Check for pattern
        @Suppress("UNCHECKED_CAST")
        val pattern = json["pattern"] as? List<String>
        if (pattern == null || pattern.isEmpty()) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Shaped recipe missing 'pattern'",
                    file,
                    null,
                    "Add 'pattern' array with crafting grid layout"
                )
            )
        }

        // Check for key
        @Suppress("UNCHECKED_CAST")
        val key = json["key"] as? Map<String, Any>
        if (key == null || key.isEmpty()) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Shaped recipe missing 'key'",
                    file,
                    null,
                    "Add 'key' mapping pattern characters to ingredients"
                )
            )
        } else {
            // Validate ingredients
            key.forEach { (char, ingredient) ->
                issues.addAll(validateIngredient(ingredient, file, projectDir, "key.$char"))
            }
        }

        // Check for result
        val result = json["result"]
        if (result == null) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Recipe missing 'result'",
                    file,
                    null,
                    "Add 'result' with item output"
                )
            )
        } else {
            issues.addAll(validateResult(result, file, projectDir))
        }

        return issues
    }

    private fun validateShapelessRecipe(json: Map<String, Any>, file: File, projectDir: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        // Check for ingredients
        @Suppress("UNCHECKED_CAST")
        val ingredients = json["ingredients"] as? List<Any>
        if (ingredients == null || ingredients.isEmpty()) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Shapeless recipe missing 'ingredients'",
                    file,
                    null,
                    "Add 'ingredients' array with recipe items"
                )
            )
        } else {
            ingredients.forEachIndexed { index, ingredient ->
                issues.addAll(validateIngredient(ingredient, file, projectDir, "ingredients[$index]"))
            }
        }

        // Check for result
        val result = json["result"]
        if (result == null) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Recipe missing 'result'",
                    file,
                    null,
                    "Add 'result' with item output"
                )
            )
        } else {
            issues.addAll(validateResult(result, file, projectDir))
        }

        return issues
    }

    private fun validateCookingRecipe(json: Map<String, Any>, file: File, projectDir: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        // Check for ingredient
        val ingredient = json["ingredient"]
        if (ingredient == null) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Cooking recipe missing 'ingredient'",
                    file,
                    null,
                    "Add 'ingredient' with item to cook"
                )
            )
        } else {
            issues.addAll(validateIngredient(ingredient, file, projectDir, "ingredient"))
        }

        // Check for result
        val result = json["result"]
        if (result == null) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Recipe missing 'result'",
                    file,
                    null,
                    "Add 'result' with item output"
                )
            )
        }

        return issues
    }

    private fun validateStonecuttingRecipe(json: Map<String, Any>, file: File, projectDir: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        // Check for ingredient
        val ingredient = json["ingredient"]
        if (ingredient == null) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Stonecutting recipe missing 'ingredient'",
                    file,
                    null,
                    "Add 'ingredient' with item to cut"
                )
            )
        } else {
            issues.addAll(validateIngredient(ingredient, file, projectDir, "ingredient"))
        }

        // Check for result
        val result = json["result"]
        if (result == null) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Recipe missing 'result'",
                    file,
                    null,
                    "Add 'result' with item output"
                )
            )
        }

        return issues
    }

    private fun validateIngredient(ingredient: Any, file: File, projectDir: File, path: String): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        when (ingredient) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val ingredientMap = ingredient as Map<String, Any>

                // Can be item or tag
                val item = ingredientMap["item"] as? String
                val tag = ingredientMap["tag"] as? String

                if (item == null && tag == null) {
                    issues.add(
                        ValidationIssue(
                            ValidationSeverity.ERROR,
                            "Ingredient at $path must have 'item' or 'tag'",
                            file,
                            null,
                            "Add 'item' or 'tag' field to ingredient"
                        )
                    )
                }

                // Note: We don't validate if items exist because they could be from other mods
                // Tags are validated separately
            }
            is String -> {
                // Simple item string - valid format
            }
            else -> {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Invalid ingredient format at $path",
                        file,
                        null,
                        "Ingredient must be string or object with 'item' or 'tag'"
                    )
                )
            }
        }

        return issues
    }

    private fun validateResult(result: Any, file: File, projectDir: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        when (result) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val resultMap = result as Map<String, Any>

                val item = resultMap["item"] as? String
                if (item == null) {
                    issues.add(
                        ValidationIssue(
                            ValidationSeverity.ERROR,
                            "Result must have 'item' field",
                            file,
                            null,
                            "Add 'item' field to result"
                        )
                    )
                }
            }
            is String -> {
                // Simple item string - valid format
            }
            else -> {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Invalid result format",
                        file,
                        null,
                        "Result must be string or object with 'item'"
                    )
                )
            }
        }

        return issues
    }

    private fun getModIdFromPath(file: File): String {
        // Extract mod ID from path like: .../data/modid/recipe/...
        val path = file.path.replace("\\", "/")
        val dataIndex = path.indexOf("/data/")
        if (dataIndex != -1) {
            val afterData = path.substring(dataIndex + 6)
            val nextSlash = afterData.indexOf("/")
            if (nextSlash != -1) {
                return afterData.substring(0, nextSlash)
            }
        }
        return "unknown"
    }
}
