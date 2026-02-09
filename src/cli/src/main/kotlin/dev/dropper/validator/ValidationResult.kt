package dev.dropper.validator

import java.io.File

/**
 * Severity level for validation issues
 */
enum class ValidationSeverity {
    ERROR,   // Must be fixed - project is broken
    WARNING  // Should be fixed - potential issues
}

/**
 * A single validation issue
 */
data class ValidationIssue(
    val severity: ValidationSeverity,
    val message: String,
    val file: File? = null,
    val line: Int? = null,
    val suggestion: String? = null
) {
    override fun toString(): String {
        val prefix = when (severity) {
            ValidationSeverity.ERROR -> "[ERROR]"
            ValidationSeverity.WARNING -> "[WARN]"
        }

        val location = when {
            file != null && line != null -> " (${file.path}:$line)"
            file != null -> " (${file.path})"
            else -> ""
        }

        val suggestionText = suggestion?.let { "\n  Suggestion: $it" } ?: ""

        return "$prefix $message$location$suggestionText"
    }
}

/**
 * Result of a validation operation
 */
data class ValidationResult(
    val validatorName: String,
    val issues: List<ValidationIssue> = emptyList(),
    val filesScanned: Int = 0,
    val timeMs: Long = 0
) {
    val hasErrors: Boolean get() = issues.any { it.severity == ValidationSeverity.ERROR }
    val hasWarnings: Boolean get() = issues.any { it.severity == ValidationSeverity.WARNING }
    val errorCount: Int get() = issues.count { it.severity == ValidationSeverity.ERROR }
    val warningCount: Int get() = issues.count { it.severity == ValidationSeverity.WARNING }
    val isValid: Boolean get() = !hasErrors

    fun printSummary() {
        println("\n$validatorName:")
        println("  Files scanned: $filesScanned")
        println("  Time: ${timeMs}ms")
        println("  Errors: $errorCount")
        println("  Warnings: $warningCount")

        if (issues.isNotEmpty()) {
            println("\nIssues found:")
            issues.forEach { issue ->
                println("  $issue")
            }
        }
    }
}

/**
 * Combined results from multiple validators
 */
data class CombinedValidationResult(
    val results: List<ValidationResult>
) {
    val hasErrors: Boolean get() = results.any { it.hasErrors }
    val hasWarnings: Boolean get() = results.any { it.hasWarnings }
    val totalErrors: Int get() = results.sumOf { it.errorCount }
    val totalWarnings: Int get() = results.sumOf { it.warningCount }
    val isValid: Boolean get() = !hasErrors

    fun printSummary() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║                   Validation Summary                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝")

        results.forEach { it.printSummary() }

        println("\n" + "═".repeat(63))
        println("Total - Errors: $totalErrors, Warnings: $totalWarnings")
        println("═".repeat(63))

        if (isValid) {
            println("\n✓ Validation passed!")
        } else {
            println("\n✗ Validation failed with $totalErrors error(s)")
        }
    }
}
