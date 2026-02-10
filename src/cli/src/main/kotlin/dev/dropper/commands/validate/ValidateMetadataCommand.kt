package dev.dropper.commands.validate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.ValidationException
import dev.dropper.validator.*
import java.io.File

/**
 * Validate config.yml metadata
 */
class ValidateMetadataCommand : CliktCommand(
    name = "metadata",
    help = "Validate config.yml consistency and format"
) {
    private val fix by option("--fix", help = "Auto-fix issues when possible").flag()
    private val strict by option("--strict", help = "Treat warnings as errors").flag()

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))

        if (!projectDir.exists()) {
            echo("Error: Project directory does not exist")
            return
        }

        val options = ValidationOptions(strict = strict, autoFix = fix)

        echo("\n╔═══════════════════════════════════════════════════════════════╗")
        echo("║                  Metadata Validation                          ║")
        echo("╚═══════════════════════════════════════════════════════════════╝\n")

        val validator = MetadataValidator()
        val result = validator.validate(projectDir, options)

        if (fix && result.issues.isNotEmpty()) {
            val fixed = validator.autoFix(projectDir, result.issues)
            if (fixed > 0) {
                echo("Auto-fixed $fixed issue(s)\n")
            }
        }

        result.printSummary()

        if (strict && result.hasWarnings) {
            throw ValidationException("Validation failed (strict mode)")
        } else if (result.hasErrors) {
            throw ValidationException("Validation failed")
        }
    }
}
