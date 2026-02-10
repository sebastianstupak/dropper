package dev.dropper.validator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import java.io.File

/**
 * Validates config.yml metadata
 */
class MetadataValidator : Validator {
    override val name = "Metadata Validator"

    private val yamlMapper = ObjectMapper(YAMLFactory())
    private val validModIdRegex = Regex("^[a-z][a-z0-9_]*$")
    private val validVersionRegex = Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9]+)?$")
    private val validMinecraftVersions = setOf(
        "1.16.5", "1.17.1", "1.18.2", "1.19.2", "1.19.3", "1.19.4",
        "1.20", "1.20.1", "1.20.2", "1.20.3", "1.20.4", "1.20.5", "1.20.6",
        "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4"
    )
    private val validLoaders = setOf("fabric", "forge", "neoforge")

    override fun validate(projectDir: File, options: ValidationOptions): ValidationResult {
        val startTime = System.currentTimeMillis()
        val issues = mutableListOf<ValidationIssue>()
        var filesScanned = 0

        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Missing config.yml",
                    projectDir,
                    null,
                    "Create config.yml in project root"
                )
            )
            return ValidationResult(name, issues, filesScanned, System.currentTimeMillis() - startTime)
        }

        filesScanned++

        try {
            @Suppress("UNCHECKED_CAST")
            val config = yamlMapper.readValue(configFile, Map::class.java) as Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val mod = config["mod"] as? Map<String, Any>

            if (mod == null) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Missing 'mod' section in config.yml",
                        configFile,
                        null,
                        "Add 'mod:' section with required fields"
                    )
                )
                return ValidationResult(name, issues, filesScanned, System.currentTimeMillis() - startTime)
            }

            // Validate required fields
            val requiredFields = listOf("id", "name", "version", "description", "author", "license")
            requiredFields.forEach { field ->
                if (!mod.containsKey(field) || (mod[field] as? String).isNullOrBlank()) {
                    issues.add(
                        ValidationIssue(
                            ValidationSeverity.ERROR,
                            "Missing or empty required field: mod.$field",
                            configFile,
                            null,
                            "Add '$field' under 'mod' section"
                        )
                    )
                }
            }

            // Validate mod ID format
            val modId = mod["id"] as? String
            if (modId != null && !validModIdRegex.matches(modId)) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "Invalid mod ID format: '$modId'",
                        configFile,
                        null,
                        "Mod ID must be lowercase letters, numbers, and underscores only, starting with a letter"
                    )
                )
            }

            // Validate version format
            val version = mod["version"] as? String
            if (version != null && !validVersionRegex.matches(version)) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.WARNING,
                        "Version '$version' does not follow semantic versioning (X.Y.Z)",
                        configFile,
                        null,
                        "Use semantic versioning format: MAJOR.MINOR.PATCH (e.g., 1.0.0)"
                    )
                )
            }

            // Check for minecraft_versions or minecraftVersions
            @Suppress("UNCHECKED_CAST")
            val mcVersions = (config["minecraft_versions"] ?: config["minecraftVersions"]) as? List<String>
            if (mcVersions == null || mcVersions.isEmpty()) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "No Minecraft versions specified",
                        configFile,
                        null,
                        "Add 'minecraft_versions' list to config.yml"
                    )
                )
            } else {
                mcVersions.forEach { version ->
                    if (version !in validMinecraftVersions) {
                        issues.add(
                            ValidationIssue(
                                ValidationSeverity.WARNING,
                                "Unknown Minecraft version: '$version'",
                                configFile,
                                null,
                                "Supported versions: ${validMinecraftVersions.joinToString(", ")}"
                            )
                        )
                    }
                }
            }

            // Validate loaders
            @Suppress("UNCHECKED_CAST")
            val loaders = config["loaders"] as? List<String>
            if (loaders == null || loaders.isEmpty()) {
                issues.add(
                    ValidationIssue(
                        ValidationSeverity.ERROR,
                        "No loaders specified",
                        configFile,
                        null,
                        "Add 'loaders' list to config.yml (fabric, forge, neoforge)"
                    )
                )
            } else {
                loaders.forEach { loader ->
                    if (loader !in validLoaders) {
                        issues.add(
                            ValidationIssue(
                                ValidationSeverity.ERROR,
                                "Invalid loader: '$loader'",
                                configFile,
                                null,
                                "Valid loaders: ${validLoaders.joinToString(", ")}"
                            )
                        )
                    }
                }
            }

            // Validate asset packs
            @Suppress("UNCHECKED_CAST")
            val assetPacks = config["asset_packs"] as? List<Map<String, Any>>
            if (assetPacks != null) {
                val packNames = mutableSetOf<String>()
                assetPacks.forEach { pack ->
                    val packName = pack["name"] as? String
                    if (packName == null) {
                        issues.add(
                            ValidationIssue(
                                ValidationSeverity.ERROR,
                                "Asset pack missing 'name' field",
                                configFile,
                                null,
                                "Add 'name' to asset pack definition"
                            )
                        )
                    } else {
                        if (packName in packNames) {
                            issues.add(
                                ValidationIssue(
                                    ValidationSeverity.ERROR,
                                    "Duplicate asset pack name: '$packName'",
                                    configFile,
                                    null,
                                    "Asset pack names must be unique"
                                )
                            )
                        }
                        packNames.add(packName)

                        // Validate inheritance
                        val inherits = pack["inherits"] as? String
                        if (inherits != null && !packNames.contains(inherits)) {
                            // Check if it references a pack defined later (circular dependency check)
                            val inheritsPack = assetPacks.find { (it["name"] as? String) == inherits }
                            if (inheritsPack != null) {
                                val inheritsInherits = inheritsPack["inherits"] as? String
                                if (inheritsInherits == packName) {
                                    issues.add(
                                        ValidationIssue(
                                            ValidationSeverity.ERROR,
                                            "Circular dependency: '$packName' and '$inherits'",
                                            configFile,
                                            null,
                                            "Remove circular inheritance"
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

        } catch (e: Exception) {
            issues.add(
                ValidationIssue(
                    ValidationSeverity.ERROR,
                    "Failed to parse config.yml: ${e.message}",
                    configFile,
                    null,
                    "Fix YAML syntax errors"
                )
            )
        }

        return ValidationResult(name, issues, filesScanned, System.currentTimeMillis() - startTime)
    }
}
