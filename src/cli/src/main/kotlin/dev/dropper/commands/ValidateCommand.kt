package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.validate.*
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
            // TODO: Print JSON output
            echo("\nJSON output not yet implemented")
        } else {
            combined.printSummary()
        }

        // Exit with error code if validation failed
        if (strict && combined.hasWarnings) {
            throw Exception("Validation failed (strict mode)")
        } else if (combined.hasErrors) {
            throw Exception("Validation failed")
        }
    }
}
