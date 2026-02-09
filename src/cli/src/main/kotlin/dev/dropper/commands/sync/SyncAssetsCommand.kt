package dev.dropper.commands.sync

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.dropper.synchronizers.AssetSynchronizer
import dev.dropper.synchronizers.SyncOptions
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to sync all assets between versions or asset packs
 */
class SyncAssetsCommand : CliktCommand(
    name = "assets",
    help = "Sync all assets (models, textures, blockstates, lang, etc.)"
) {
    private val from by option("--from", help = "Source version or asset pack (e.g., v1, 1.20.1)").required()
    private val to by option("--to", help = "Target version or asset pack (e.g., v2, 1.21.1)").required()
    private val dryRun by option("--dry-run", help = "Preview changes without applying them").flag()
    private val force by option("--force", help = "Overwrite existing files in conflicts").flag()
    private val bidirectional by option("--bidirectional", help = "Sync both directions").flag()
    private val exclude by option("--exclude", help = "Exclude files matching pattern (can be used multiple times)").multiple()

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        Logger.info("Syncing assets: $from -> $to")
        if (dryRun) {
            Logger.warn("DRY RUN MODE: No changes will be made")
        }

        val options = SyncOptions(
            dryRun = dryRun,
            force = force,
            bidirectional = bidirectional,
            excludePatterns = exclude
        )

        val synchronizer = AssetSynchronizer()
        val result = synchronizer.sync(projectDir, from, to, options)

        if (!result.success) {
            Logger.error(result.message)
            return
        }

        // Display results
        if (dryRun) {
            Logger.info("\nDRY RUN RESULTS:")
        } else {
            Logger.info("\nSYNC RESULTS:")
        }

        if (result.copied.isNotEmpty()) {
            Logger.info("\n${if (dryRun) "Would copy" else "Copied"} ${result.copied.size} file(s):")
            result.copied.take(10).forEach { change ->
                Logger.info("  - ${File(change.sourcePath).name} (${change.reason})")
            }
            if (result.copied.size > 10) {
                Logger.info("  ... and ${result.copied.size - 10} more")
            }
        }

        if (result.merged.isNotEmpty()) {
            Logger.info("\n${if (dryRun) "Would merge" else "Merged"} ${result.merged.size} file(s):")
            result.merged.forEach { change ->
                Logger.info("  - ${File(change.sourcePath).name} (${change.reason})")
            }
        }

        if (result.skipped.isNotEmpty()) {
            Logger.info("\n${if (dryRun) "Would skip" else "Skipped"} ${result.skipped.size} file(s) (already up-to-date)")
        }

        if (result.conflicts.isNotEmpty()) {
            Logger.warn("\nConflicts detected (${result.conflicts.size} file(s)):")
            result.conflicts.forEach { change ->
                Logger.warn("  - ${File(change.sourcePath).name}")
            }
            if (!force) {
                Logger.info("\nUse --force to overwrite conflicting files")
            }
        }

        if (!dryRun) {
            Logger.success("\n${result.message}")
        } else {
            Logger.info("\n${result.message}")
            Logger.info("Run without --dry-run to apply changes")
        }
    }
}
