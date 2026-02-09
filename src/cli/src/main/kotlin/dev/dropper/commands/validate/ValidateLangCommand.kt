package dev.dropper.commands.validate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.validator.*
import java.io.File

/**
 * Validate language files (translations)
 */
class ValidateLangCommand : CliktCommand(
    name = "lang",
    help = "Validate language files and find missing translations"
) {
    private val fix by option("--fix", help = "Auto-fix issues when possible").flag()
    private val strict by option("--strict", help = "Treat warnings as errors").flag()
    private val version by option("--version", help = "Validate specific version only")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))

        if (!projectDir.exists() || !File(projectDir, "config.yml").exists()) {
            echo("Error: Not a Dropper project (missing config.yml)")
            return
        }

        val options = ValidationOptions(strict = strict, autoFix = fix, version = version)

        echo("\n╔═══════════════════════════════════════════════════════════════╗")
        echo("║                Language File Validation                       ║")
        echo("╚═══════════════════════════════════════════════════════════════╝\n")

        val validator = LangValidator()
        val result = validator.validate(projectDir, options)

        if (fix && result.issues.isNotEmpty()) {
            val fixed = validator.autoFix(projectDir, result.issues)
            if (fixed > 0) {
                echo("Auto-fixed $fixed issue(s)\n")
            }
        }

        result.printSummary()

        if (strict && result.hasWarnings) {
            throw Exception("Validation failed (strict mode)")
        } else if (result.hasErrors) {
            throw Exception("Validation failed")
        }
    }
}
