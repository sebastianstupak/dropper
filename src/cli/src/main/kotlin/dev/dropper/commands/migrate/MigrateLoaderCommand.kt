package dev.dropper.commands.migrate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.migrators.*
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to add support for a new mod loader
 */
class MigrateLoaderCommand : CliktCommand(
    name = "loader",
    help = "Add support for a new mod loader"
) {
    private val loader by argument(
        name = "LOADER",
        help = "Loader to add (fabric, forge, neoforge)"
    )

    private val version by option("--version", "-v", help = "Minecraft version to add loader to (default: all)")

    private val dryRun by option("--dry-run", help = "Preview changes without executing").flag()

    private val force by option("--force", help = "Override safety checks").flag()

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        if (!listOf("fabric", "forge", "neoforge").contains(loader)) {
            Logger.error("Invalid loader: $loader. Supported: fabric, forge, neoforge")
            return
        }

        Logger.info("Adding $loader support to project")

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
                "loader" to loader,
                "version" to (version ?: "")
            )
        )

        // Plan migration
        val migrator = LoaderMigrator()
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
            Logger.success("Loader migration completed successfully!")
            Logger.info("Added $loader support")
        } else {
            Logger.error("Migration failed with ${result.errors.size} error(s)")
        }
    }
}
