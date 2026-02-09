package dev.dropper.commands.migrate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.migrators.*
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to migrate project to a new Minecraft version
 */
class MigrateVersionCommand : CliktCommand(
    name = "version",
    help = "Migrate to a new Minecraft version"
) {
    private val targetVersion by argument(
        name = "VERSION",
        help = "Target Minecraft version (e.g., 1.21.1)"
    )

    private val fromVersion by option("--from", "-f", help = "Source version to migrate from")

    private val assetPack by option("--asset-pack", "-p", help = "Asset pack to use")
        .default("v1")

    private val dryRun by option("--dry-run", help = "Preview changes without executing").flag()

    private val autoFix by option("--auto-fix", help = "Automatically fix common issues").flag()

    private val force by option("--force", help = "Override safety checks").flag()

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        Logger.info("Migrating to Minecraft version: $targetVersion")

        // Read config to get mod info
        val config = configFile.readText()
        val modIdMatch = "id:\\s*\"?([^\"\\s]+)\"?".toRegex().find(config)
        val packageMatch = "package:\\s*\"?([^\"\\s]+)\"?".toRegex().find(config)

        val modId = modIdMatch?.groupValues?.get(1) ?: "mod"
        val packageName = packageMatch?.groupValues?.get(1) ?: "com.example.mod"

        // Determine source version
        val sourceVersion = fromVersion ?: findCurrentVersion(projectDir)

        if (sourceVersion.isEmpty() && !force) {
            Logger.error("Could not determine source version. Use --from to specify or --force to continue")
            return
        }

        // Create migration context
        val context = MigrationContext(
            projectDir = projectDir,
            modId = modId,
            packageName = packageName,
            force = force,
            autoFix = autoFix,
            dryRun = dryRun,
            params = mapOf(
                "from" to sourceVersion,
                "to" to targetVersion,
                "assetPack" to assetPack
            )
        )

        // Plan migration
        val migrator = VersionMigrator()
        val plan = migrator.planMigration(context)

        // Show plan
        val reportGenerator = MigrationReport()
        if (dryRun) {
            Logger.info(reportGenerator.generateDryRunReport(plan))
            return
        }

        // Confirm
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
            Logger.success("Migration completed successfully!")
            if (sourceVersion.isNotEmpty()) {
                Logger.info("Migrated from $sourceVersion to $targetVersion")
            }
        } else {
            Logger.error("Migration failed with ${result.errors.size} error(s)")
        }
    }

    private fun findCurrentVersion(projectDir: File): String {
        val versionsDir = File(projectDir, "versions")
        if (!versionsDir.exists()) return ""

        return versionsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.map { it.name.replace("_", ".") }
            ?.maxByOrNull { it }
            ?: ""
    }
}
