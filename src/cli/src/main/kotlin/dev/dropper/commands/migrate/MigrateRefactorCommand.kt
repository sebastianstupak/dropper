package dev.dropper.commands.migrate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.migrators.*
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to refactor package names across the project
 */
class MigrateRefactorCommand : CliktCommand(
    name = "refactor",
    help = "Refactor package names"
) {
    private val oldPackage by argument(
        name = "OLD",
        help = "Old package name (e.g., com.old.mod)"
    )

    private val newPackage by argument(
        name = "NEW",
        help = "New package name (e.g., com.new.mod)"
    )

    private val dryRun by option("--dry-run", help = "Preview changes without executing").flag()

    private val force by option("--force", help = "Override safety checks").flag()

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        Logger.info("Refactoring package: $oldPackage → $newPackage")

        // Read config
        val config = configFile.readText()
        val modIdMatch = "id:\\s*\"?([^\"\\s]+)\"?".toRegex().find(config)

        val modId = modIdMatch?.groupValues?.get(1) ?: "mod"

        // Create migration context
        val context = MigrationContext(
            projectDir = projectDir,
            modId = modId,
            packageName = newPackage,
            force = force,
            dryRun = dryRun,
            params = mapOf(
                "oldPackage" to oldPackage,
                "newPackage" to newPackage
            )
        )

        // Plan migration
        val migrator = RefactorMigrator()
        val plan = migrator.planMigration(context)

        // Show plan
        val reportGenerator = MigrationReport()
        if (dryRun) {
            Logger.info(reportGenerator.generateDryRunReport(plan))
            return
        }

        // Confirm warnings
        if (!force && plan.warnings.isNotEmpty()) {
            Logger.warn("Warnings detected:")
            plan.warnings.forEach { Logger.warn("  - $it") }
            Logger.info("Use --force to proceed anyway")
            return
        }

        if (plan.operations.isEmpty()) {
            Logger.warn("No files found to refactor")
            return
        }

        // Execute migration
        Logger.info("Executing migration...")
        val result = migrator.executeMigration(plan, dryRun)

        // Show results
        Logger.info(reportGenerator.generateReport(result, plan))

        if (result.success) {
            Logger.success("Package refactoring completed successfully!")
            Logger.info("Refactored $oldPackage to $newPackage")
        } else {
            Logger.error("Migration failed with ${result.errors.size} error(s)")
        }
    }
}
