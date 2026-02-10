package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.validate.*
import dev.dropper.util.ValidationException
import dev.dropper.validator.*
import java.io.File

/**
 * Parent command for validation operations
 */
class ValidateCommand : CliktCommand(
    name = "validate",
    help = "Validate project structure, assets, metadata, and more"
) {
    private val fix by option("--fix", help = "Auto-fix issues when possible").flag()
    private val strict by option("--strict", help = "Treat warnings as errors").flag()
    private val version by option("--version", help = "Validate specific version only")
    private val format by option("--format", help = "Output format (text, json)")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))

        if (!projectDir.exists() || !File(projectDir, "config.yml").exists()) {
            echo("Error: Not a Dropper project (missing config.yml)")
            echo("Run this command from your project root directory")
            return
        }

        val options = ValidationOptions(
            strict = strict,
            autoFix = fix,
            version = version,
            format = when (format?.lowercase()) {
                "json" -> OutputFormat.JSON
                else -> OutputFormat.TEXT
            }
        )

        echo("\n╔═══════════════════════════════════════════════════════════════╗")
        echo("║              Dropper Project Validation                       ║")
        echo("╚═══════════════════════════════════════════════════════════════╝\n")
        echo("Project: ${projectDir.name}")
        echo("Options: ${if (strict) "strict " else ""}${if (fix) "auto-fix " else ""}")
        if (version != null) echo("Version filter: $version")

        // Run all validators
        val validators = listOf(
            MetadataValidator(),
            StructureValidator(),
            AssetValidator(),
            RecipeValidator(),
            LangValidator()
        )

        val results = mutableListOf<ValidationResult>()

        validators.forEach { validator ->
            echo("\nRunning ${validator.name}...")
            val result = validator.validate(projectDir, options)
            results.add(result)

            // Auto-fix if requested
            if (fix && result.issues.isNotEmpty()) {
                val fixed = validator.autoFix(projectDir, result.issues)
                if (fixed > 0) {
                    echo("  Auto-fixed $fixed issue(s)")
                }
            }
        }

        // Print summary
        val combined = CombinedValidationResult(results)

        if (options.format == OutputFormat.JSON) {
            echo(formatResultsAsJson(combined))
        } else {
            combined.printSummary()
        }

        // Exit with error code if validation failed
        if (strict && combined.hasWarnings) {
            throw ValidationException("Validation failed (strict mode)")
        } else if (combined.hasErrors) {
            throw ValidationException("Validation failed")
        }
    }

    /**
     * Format validation results as JSON output
     */
    private fun formatResultsAsJson(combined: CombinedValidationResult): String {
        val sb = StringBuilder()
        sb.appendLine("{")
        sb.appendLine("  \"valid\": ${combined.isValid},")
        sb.appendLine("  \"totalErrors\": ${combined.totalErrors},")
        sb.appendLine("  \"totalWarnings\": ${combined.totalWarnings},")
        sb.appendLine("  \"validators\": [")

        combined.results.forEachIndexed { index, result ->
            sb.appendLine("    {")
            sb.appendLine("      \"name\": ${jsonString(result.validatorName)},")
            sb.appendLine("      \"filesScanned\": ${result.filesScanned},")
            sb.appendLine("      \"timeMs\": ${result.timeMs},")
            sb.appendLine("      \"errors\": ${result.errorCount},")
            sb.appendLine("      \"warnings\": ${result.warningCount},")
            sb.appendLine("      \"issues\": [")

            result.issues.forEachIndexed { issueIndex, issue ->
                sb.appendLine("        {")
                sb.appendLine("          \"severity\": ${jsonString(issue.severity.name)},")
                sb.appendLine("          \"message\": ${jsonString(issue.message)},")
                sb.appendLine("          \"file\": ${if (issue.file != null) jsonString(issue.file.path) else "null"},")
                sb.appendLine("          \"line\": ${issue.line ?: "null"},")
                sb.appendLine("          \"suggestion\": ${if (issue.suggestion != null) jsonString(issue.suggestion) else "null"}")
                sb.append("        }")
                if (issueIndex < result.issues.size - 1) sb.appendLine(",") else sb.appendLine()
            }

            sb.appendLine("      ]")
            sb.append("    }")
            if (index < combined.results.size - 1) sb.appendLine(",") else sb.appendLine()
        }

        sb.appendLine("  ]")
        sb.append("}")
        return sb.toString()
    }

    /**
     * Escape a string for JSON output
     */
    private fun jsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
