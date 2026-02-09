package dev.dropper.commands.migrate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.migrators.*
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to update mappings versions
 */
class MigrateMappingsCommand : CliktCommand(
    name = "mappings",
    help = "Update to latest mappings"
) {
    private val mappingsVersion by argument(
        name = "VERSION",
        help = "Mappings version to use"
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

        Logger.info("Updating mappings to version: $mappingsVersion")

        // Read config
        val config = configFile.readText()
        val modIdMatch = "id:\\s*\"?([^\"\\s]+)\"?".toRegex().find(config)
        val packageMatch = "package:\\s*\"?([^\"\\s]+)\"?".toRegex().find(config)

        val modId = modIdMatch?.groupValues?.get(1) ?: "mod"
        val packageName = packageMatch?.groupValues?.get(1) ?: "com.example.mod"

        // Create migration context
        val context = MigrationContext(
            projectDir = projectDir,
            modId = modId,
            packageName = packageName,
            force = force,
            dryRun = dryRun,
            params = mapOf(
                "mappingsVersion" to mappingsVersion
            )
        )

        // Plan migration
        val migrator = MappingsMigrator()
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

        // Execute migration
        Logger.info("Executing migration...")
        val result = migrator.executeMigration(plan, dryRun)

        // Show results
        Logger.info(reportGenerator.generateReport(result, plan))

        if (result.success) {
            Logger.success("Mappings updated successfully!")
            Logger.info("Updated to mappings version: $mappingsVersion")
        } else {
            Logger.error("Migration failed with ${result.errors.size} error(s)")
        }
    }
}
