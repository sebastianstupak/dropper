package dev.dropper.commands.sync

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import dev.dropper.synchronizers.BlockstateSynchronizer
import dev.dropper.synchronizers.SyncOptions
import dev.dropper.util.Logger
import java.io.File

/**
 * Command to sync blockstate files only
 */
class SyncBlockstatesCommand : CliktCommand(
    name = "blockstates",
    help = "Sync blockstate files only"
) {
    private val from by option("--from", help = "Source version or asset pack").required()
    private val to by option("--to", help = "Target version or asset pack").required()
    private val dryRun by option("--dry-run", help = "Preview changes without applying them").flag()
    private val force by option("--force", help = "Overwrite existing files in conflicts").flag()
    private val exclude by option("--exclude", help = "Exclude files matching pattern").multiple()

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        Logger.info("Syncing blockstates: $from -> $to")
        if (dryRun) {
            Logger.warn("DRY RUN MODE: No changes will be made")
        }

        val options = SyncOptions(
            dryRun = dryRun,
            force = force,
            excludePatterns = exclude
        )

        val synchronizer = BlockstateSynchronizer()
        val result = synchronizer.sync(projectDir, from, to, options)

        if (!result.success) {
            Logger.error(result.message)
            return
        }

        // Display results
        if (result.copied.isNotEmpty()) {
            Logger.info("\n${if (dryRun) "Would copy" else "Copied"} ${result.copied.size} blockstate(s):")
            result.copied.take(10).forEach { change ->
                Logger.info("  - ${File(change.sourcePath).name}")
            }
            if (result.copied.size > 10) {
                Logger.info("  ... and ${result.copied.size - 10} more")
            }
        }

        if (result.skipped.isNotEmpty()) {
            Logger.info("\n${if (dryRun) "Would skip" else "Skipped"} ${result.skipped.size} blockstate(s)")
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
