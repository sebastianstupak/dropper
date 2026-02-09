package dev.dropper.validator

import java.io.File

/**
 * Base interface for all validators
 */
interface Validator {
    /**
     * The name of this validator
     */
    val name: String

    /**
     * Validate the project
     *
     * @param projectDir The root directory of the project
     * @param options Validation options
     * @return ValidationResult with any issues found
     */
    fun validate(projectDir: File, options: ValidationOptions = ValidationOptions()): ValidationResult

    /**
     * Auto-fix issues if possible
     *
     * @param projectDir The root directory of the project
     * @param issues The issues to fix
     * @return Number of issues fixed
     */
    fun autoFix(projectDir: File, issues: List<ValidationIssue>): Int = 0
}

/**
 * Options for validation
 */
data class ValidationOptions(
    val strict: Boolean = false,        // Treat warnings as errors
    val autoFix: Boolean = false,       // Auto-fix issues when possible
    val version: String? = null,        // Validate specific version only
    val format: OutputFormat = OutputFormat.TEXT
)

/**
 * Output format for validation results
 */
enum class OutputFormat {
    TEXT,
    JSON
}
