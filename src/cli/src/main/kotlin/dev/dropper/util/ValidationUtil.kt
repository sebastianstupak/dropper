package dev.dropper.util

import java.io.File

/**
 * Validation utilities for CLI inputs
 * @deprecated Use Validators object instead for new code
 */
object ValidationUtil {

    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val suggestion: String? = null
    )

    /**
     * Validate a name for items, blocks, entities, etc.
     * @deprecated Use Validators.validateComponentName instead
     */
    fun validateName(name: String, type: String = "name"): ValidationResult {
        val result = Validators.validateComponentName(name, type)
        return ValidationResult(
            result.isValid,
            result.errorMessage,
            result.suggestion
        )
    }

    /**
     * Validate mod ID format
     * @deprecated Use Validators.validateModId instead
     */
    fun validateModId(modId: String): ValidationResult {
        val result = Validators.validateModId(modId)
        return ValidationResult(
            result.isValid,
            result.errorMessage,
            result.suggestion
        )
    }

    /**
     * Check if we're in a Dropper project directory
     * @deprecated Use Validators.validateDropperProject instead
     */
    fun validateDropperProject(directory: File = File(System.getProperty("user.dir"))): ValidationResult {
        val result = Validators.validateDropperProject(directory)
        return ValidationResult(
            result.isValid,
            result.errorMessage,
            result.suggestion
        )
    }

    /**
     * Check if a component already exists
     */
    fun checkDuplicate(
        projectDir: File,
        componentType: String,
        componentName: String,
        searchDirs: List<String>
    ): ValidationResult {
        val className = componentName.split('_')
            .joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }

        for (searchDir in searchDirs) {
            val dir = File(projectDir, searchDir)
            if (!dir.exists()) continue

            try {
                dir.walkTopDown().forEach { file ->
                    if (file.isFile && file.extension == "java") {
                        val content = FileUtil.readTextOrNull(file) ?: return@forEach
                        // Check for class definition with exact name match
                        if (content.contains(Regex("class\\s+$className\\s*[\\{:]"))) {
                            return ValidationResult(
                                false,
                                "Warning: $componentType '$componentName' (class $className) already exists",
                                "File: ${file.relativeTo(projectDir).path}\nUse a different name or remove the existing $componentType first"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Logger.debug("Error checking duplicates in $searchDir: ${e.message}")
                continue
            }
        }

        return ValidationResult(true)
    }

    /**
     * Print validation error and exit
     */
    fun exitWithError(result: ValidationResult) {
        if (result.errorMessage != null) {
            Logger.error(result.errorMessage)
            if (result.suggestion != null) {
                Logger.info("Suggestion: ${result.suggestion}")
            }
        }
    }
}

