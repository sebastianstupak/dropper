package dev.dropper.synchronizers

import dev.dropper.util.Logger
import java.io.File

/**
 * Synchronizes data files (recipes, loot tables, tags, etc.)
 */
class DataSynchronizer : Synchronizer {

    override fun sync(
        projectDir: File,
        source: String,
        target: String,
        options: SyncOptions
    ): SyncResult {
        Logger.info("Synchronizing data files from $source to $target...")

        val sourceDir = resolveDataDir(projectDir, source)
        val targetDir = resolveDataDir(projectDir, target)

        if (!sourceDir.exists()) {
            return SyncResult(
                success = false,
                message = "Source data directory not found: ${sourceDir.absolutePath}"
            )
        }

        // Analyze differences
        val diff = DiffAnalyzer.analyze(sourceDir, targetDir, options.excludePatterns)

        if (options.dryRun) {
            return createDryRunResult(diff, sourceDir, targetDir)
        }

        // Perform sync
        val copied = mutableListOf<FileChange>()
        val skipped = mutableListOf<FileChange>()
        val conflicts = mutableListOf<FileChange>()

        // Copy missing files
        diff.missing.forEach { sourceFile ->
            val relativePath = sourceFile.relativeTo(sourceDir).path
            val targetFile = File(targetDir, relativePath)

            copyFile(sourceFile, targetFile)
            copied.add(FileChange(
                sourceFile.absolutePath,
                targetFile.absolutePath,
                ChangeType.COPY,
                "Missing in target"
            ))
        }

        // Update outdated files
        diff.outdated.forEach { sourceFile ->
            val relativePath = sourceFile.relativeTo(sourceDir).path
            val targetFile = File(targetDir, relativePath)

            copyFile(sourceFile, targetFile)
            copied.add(FileChange(
                sourceFile.absolutePath,
                targetFile.absolutePath,
                ChangeType.COPY,
                "Outdated in target"
            ))
        }

        // Handle conflicts
        val resolutions = ConflictResolver.resolve(diff.conflicts, sourceDir, targetDir, options.force)
        resolutions.forEach { (sourceFile, resolution) ->
            val relativePath = sourceFile.relativeTo(sourceDir).path
            val targetFile = File(targetDir, relativePath)

            when (resolution) {
                ConflictResolution.USE_SOURCE -> {
                    copyFile(sourceFile, targetFile)
                    copied.add(FileChange(
                        sourceFile.absolutePath,
                        targetFile.absolutePath,
                        ChangeType.COPY,
                        "Conflict resolved: use source (--force)"
                    ))
                }
                ConflictResolution.KEEP_TARGET -> {
                    skipped.add(FileChange(
                        sourceFile.absolutePath,
                        targetFile.absolutePath,
                        ChangeType.SKIP,
                        "Conflict: keeping target"
                    ))
                }
                ConflictResolution.MERGE -> {
                    copyFile(sourceFile, targetFile)
                    copied.add(FileChange(
                        sourceFile.absolutePath,
                        targetFile.absolutePath,
                        ChangeType.COPY,
                        "Conflict resolved: merged"
                    ))
                }
            }
        }

        // Log identical files as skipped
        diff.identical.forEach { sourceFile ->
            val relativePath = sourceFile.relativeTo(sourceDir).path
            val targetFile = File(targetDir, relativePath)
            skipped.add(FileChange(
                sourceFile.absolutePath,
                targetFile.absolutePath,
                ChangeType.SKIP,
                "Identical"
            ))
        }

        return SyncResult(
            copied = copied,
            skipped = skipped,
            conflicts = conflicts,
            success = true,
            message = "Data sync completed: ${copied.size} copied, ${skipped.size} skipped"
        )
    }

    private fun resolveDataDir(projectDir: File, path: String): File {
        // Try as version first
        val versionDir = File(projectDir, "versions/$path/data")
        if (versionDir.exists()) {
            return versionDir
        }

        // Try as asset pack
        val assetPackDir = File(projectDir, "versions/shared/$path/data")
        if (assetPackDir.exists()) {
            return assetPackDir
        }

        return File(projectDir, path)
    }

    private fun copyFile(source: File, target: File) {
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
    }

    private fun createDryRunResult(diff: DiffResult, sourceDir: File, targetDir: File): SyncResult {
        val changes = mutableListOf<FileChange>()

        diff.missing.forEach { sourceFile ->
            val relativePath = sourceFile.relativeTo(sourceDir).path
            val targetFile = File(targetDir, relativePath)
            changes.add(FileChange(
                sourceFile.absolutePath,
                targetFile.absolutePath,
                ChangeType.COPY,
                "Would copy (missing)"
            ))
        }

        diff.outdated.forEach { sourceFile ->
            val relativePath = sourceFile.relativeTo(sourceDir).path
            val targetFile = File(targetDir, relativePath)
            changes.add(FileChange(
                sourceFile.absolutePath,
                targetFile.absolutePath,
                ChangeType.COPY,
                "Would copy (outdated)"
            ))
        }

        val conflicts = diff.conflicts.map { sourceFile ->
            val relativePath = sourceFile.relativeTo(sourceDir).path
            val targetFile = File(targetDir, relativePath)
            FileChange(
                sourceFile.absolutePath,
                targetFile.absolutePath,
                ChangeType.CONFLICT,
                "Conflict (use --force to overwrite)"
            )
        }

        return SyncResult(
            copied = changes,
            conflicts = conflicts,
            success = true,
            message = "Dry run: ${changes.size} files would be synced, ${conflicts.size} conflicts"
        )
    }
}
