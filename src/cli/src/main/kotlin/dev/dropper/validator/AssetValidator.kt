package dev.dropper.validator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.io.File

/**
 * Validates asset files (textures, models, blockstates)
 */
class AssetValidator : Validator {
    override val name = "Asset Validator"

    private val objectMapper = ObjectMapper()

    override fun validate(projectDir: File, options: ValidationOptions): ValidationResult {
        val startTime = System.currentTimeMillis()
        val issues = mutableListOf<ValidationIssue>()
        var filesScanned = 0

        val versionsDir = File(projectDir, "versions")
        if (!versionsDir.exists()) {
            return ValidationResult(name, emptyList(), 0, System.currentTimeMillis() - startTime)
        }

        // Find all asset directories
        val assetDirs = findAssetDirectories(versionsDir, options.version)

        assetDirs.forEach { assetDir ->
            // Validate blockstates
            val blockstatesDir = File(assetDir, "blockstates")
            if (blockstatesDir.exists()) {
                blockstatesDir.listFiles()?.forEach { blockstateFile ->
                    filesScanned++
                    issues.addAll(validateBlockstate(blockstateFile, assetDir))
                }
            }

            // Validate item models
            val itemModelsDir = File(assetDir, "models/item")
            if (itemModelsDir.exists()) {
                itemModelsDir.listFiles()?.forEach { modelFile ->
                    filesScanned++
                    issues.addAll(validateItemModel(modelFile, assetDir))
                }
            }

            // Validate block models
            val blockModelsDir = File(assetDir, "models/block")
            if (blockModelsDir.exists()) {
                blockModelsDir.listFiles()?.forEach { modelFile ->
                    filesScanned++
                    issues.addAll(validateBlockModel(modelFile, assetDir))
                }
            }

            // Check for unused textures
            issues.addAll(findUnusedTextures(assetDir))
        }

        return ValidationResult(name, issues, filesScanned, System.currentTimeMillis() - startTime)
    }

    private fun findAssetDirectories(versionsDir: File, versionFilter: String?): List<File> {
        val assetDirs = mutableListOf<File>()

        versionsDir.walkTopDown().forEach { file ->
            if (file.isDirectory && file.name == "assets") {
                val modIdDir = file.listFiles()?.firstOrNull()
                if (modIdDir != null && modIdDir.isDirectory) {
                    // Check if this matches version filter
                    if (versionFilter == null || file.path.contains(versionFilter)) {
                        assetDirs.add(modIdDir)
                    }
                }
            }
        }

        return assetDirs
    }

    private fun validateBlockstate(file: File, assetDir: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        // Validate JSON syntax
        try {
            val json = objectMapper.readValue<Map<String, Any>>(file)

            // Check for variants or multipart
            if (!json.containsKey("variants") && !json.containsKey("multipart")) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Blockstate must have 'variants' or 'multipart'",
                        file,
                        null,
                        "Add either 'variants' or 'multipart' key to blockstate"
                    )
                )
            }

            // Validate model references
            if (json.containsKey("variants")) {
                @Suppress("UNCHECKED_CAST")
                val variants = json["variants"] as? Map<String, Any>
                variants?.values?.forEach { variant ->
                    val models = when (variant) {
                        is Map<*, *> -> listOf(variant)
                        is List<*> -> variant as List<Map<*, *>>
                        else -> emptyList()
                    }

                    models.forEach { model ->
                        val modelPath = (model as? Map<*, *>)?.get("model") as? String
                        if (modelPath != null) {
                            issues.addAll(validateModelReference(modelPath, assetDir, file))
                        }
                    }
                }
            }

            // Check for matching texture
            val blockName = file.nameWithoutExtension
            val textureFile = File(assetDir, "textures/block/$blockName.png")
            if (!textureFile.exists()) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.WARNING,
                        "Missing texture for block '$blockName'",
                        file,
                        null,
                        "Create texture at: ${textureFile.path}"
                    )
                )
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

    private fun validateItemModel(file: File, assetDir: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        try {
            val json = objectMapper.readValue<Map<String, Any>>(file)

            // Validate parent
            val parent = json["parent"] as? String
            if (parent != null && parent.startsWith(assetDir.name + ":")) {
                // Custom model reference
                issues.addAll(validateModelReference(parent, assetDir, file))
            }

            // Validate textures
            @Suppress("UNCHECKED_CAST")
            val textures = json["textures"] as? Map<String, String>
            textures?.forEach { (key, texturePath) ->
                if (texturePath.contains(":")) {
                    val parts = texturePath.split(":")
                    if (parts.size == 2 && parts[0] == assetDir.name) {
                        val textureFile = File(assetDir, "textures/${parts[1]}.png")
                        if (!textureFile.exists()) {
                            issues.add(
                                ValidationIssue(
                                    ValidationSeverity.ERROR,
                                    "Missing texture '$texturePath' in model",
                                    file,
                                    null,
                                    "Create texture at: ${textureFile.path}"
                                )
                            )
                        }
                    }
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

    private fun validateBlockModel(file: File, assetDir: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        try {
            val json = objectMapper.readValue<Map<String, Any>>(file)

            // Validate textures
            @Suppress("UNCHECKED_CAST")
            val textures = json["textures"] as? Map<String, String>
            textures?.forEach { (key, texturePath) ->
                if (texturePath.contains(":")) {
                    val parts = texturePath.split(":")
                    if (parts.size == 2 && parts[0] == assetDir.name) {
                        val textureFile = File(assetDir, "textures/${parts[1]}.png")
                        if (!textureFile.exists()) {
                            issues.add(
                                ValidationIssue(
                                    ValidationSeverity.ERROR,
                                    "Missing texture '$texturePath' in model",
                                    file,
                                    null,
                                    "Create texture at: ${textureFile.path}"
                                )
                            )
                        }
                    }
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

    private fun validateModelReference(modelPath: String, assetDir: File, sourceFile: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()

        if (modelPath.contains(":")) {
            val parts = modelPath.split(":")
            if (parts.size == 2 && parts[0] == assetDir.name) {
                val modelFile = File(assetDir, "models/${parts[1]}.json")
                if (!modelFile.exists()) {
                    issues.add(
                        ValidationIssue(
                            ValidationSeverity.ERROR,
                            "Referenced model '$modelPath' does not exist",
                            sourceFile,
                            null,
                            "Create model at: ${modelFile.path}"
                        )
                    )
                }
            }
        }

        return issues
    }

    private fun findUnusedTextures(assetDir: File): List<ValidationIssue> {
        val issues = mutableListOf<ValidationIssue>()
        val texturesDir = File(assetDir, "textures")

        if (!texturesDir.exists()) {
            return issues
        }

        // Collect all texture references from models
        val referencedTextures = mutableSetOf<String>()

        // Scan item models
        val itemModelsDir = File(assetDir, "models/item")
        if (itemModelsDir.exists()) {
            itemModelsDir.listFiles()?.forEach { file ->
                try {
                    val json = objectMapper.readValue<Map<String, Any>>(file)
                    @Suppress("UNCHECKED_CAST")
                    val textures = json["textures"] as? Map<String, String>
                    textures?.values?.forEach { path ->
                        if (path.contains(":")) {
                            val textureName = path.split(":")[1]
                            referencedTextures.add(textureName)
                        }
                    }
                } catch (e: Exception) {
                    // Skip invalid files
                }
            }
        }

        // Scan block models
        val blockModelsDir = File(assetDir, "models/block")
        if (blockModelsDir.exists()) {
            blockModelsDir.listFiles()?.forEach { file ->
                try {
                    val json = objectMapper.readValue<Map<String, Any>>(file)
                    @Suppress("UNCHECKED_CAST")
                    val textures = json["textures"] as? Map<String, String>
                    textures?.values?.forEach { path ->
                        if (path.contains(":")) {
                            val textureName = path.split(":")[1]
                            referencedTextures.add(textureName)
                        }
                    }
                } catch (e: Exception) {
                    // Skip invalid files
                }
            }
        }

        // Find unused textures
        texturesDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "png") {
                val relativePath = file.relativeTo(texturesDir).path.replace("\\", "/").removeSuffix(".png")
                if (!referencedTextures.contains(relativePath) && file.name != "PLACEHOLDER.txt") {
                    issues.add(
                        ValidationIssue(
                            ValidationSeverity.WARNING,
                            "Unused texture: $relativePath",
                            file,
                            null,
                            "Remove texture or reference it in a model"
                        )
                    )
                }
            }
        }

        return issues
    }
}
