package dev.dropper.synchronizers

import dev.dropper.util.Logger
import java.io.File

/**
 * Synchronizes all assets (models, textures, blockstates, lang, etc.)
 */
class AssetSynchronizer : Synchronizer {

    override fun sync(
        projectDir: File,
        source: String,
        target: String,
        options: SyncOptions
    ): SyncResult {
        Logger.info("Synchronizing assets from $source to $target...")

        val sourceDir = resolveAssetDir(projectDir, source)
        val targetDir = resolveAssetDir(projectDir, target)

        if (!sourceDir.exists()) {
            return SyncResult(
                success = false,
                message = "Source directory not found: ${sourceDir.absolutePath}"
            )
        }

        // Analyze differences
        val diff = DiffAnalyzer.analyze(sourceDir, targetDir, options.excludePatterns)

        if (options.dryRun) {
            return createDryRunResult(diff, sourceDir, targetDir)
        }

        // Perform sync
        val copied = mutableListOf<FileChange>()
        val merged = mutableListOf<FileChange>()
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

            // Check if it's a lang file - merge instead of overwrite
            if (sourceFile.name.endsWith(".json") && sourceFile.parent.endsWith("lang")) {
                val mergedContent = ConflictResolver.mergeLangFiles(sourceFile, targetFile)
                targetFile.writeText(mergedContent)
                merged.add(FileChange(
                    sourceFile.absolutePath,
                    targetFile.absolutePath,
                    ChangeType.MERGE,
                    "Lang file merged"
                ))
            } else {
                copyFile(sourceFile, targetFile)
                copied.add(FileChange(
                    sourceFile.absolutePath,
                    targetFile.absolutePath,
                    ChangeType.COPY,
                    "Outdated in target"
                ))
            }
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
                    val mergedContent = ConflictResolver.mergeLangFiles(sourceFile, targetFile)
                    targetFile.writeText(mergedContent)
                    merged.add(FileChange(
                        sourceFile.absolutePath,
                        targetFile.absolutePath,
                        ChangeType.MERGE,
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

        // Bidirectional sync if requested
        if (options.bidirectional) {
            val reverseResult = sync(
                projectDir,
                target,
                source,
                options.copy(bidirectional = false) // Prevent infinite recursion
            )
            return SyncResult(
                copied = copied + reverseResult.copied,
                skipped = skipped + reverseResult.skipped,
                conflicts = conflicts + reverseResult.conflicts,
                merged = merged + reverseResult.merged,
                success = true,
                message = "Bidirectional sync completed"
            )
        }

        return SyncResult(
            copied = copied,
            skipped = skipped,
            conflicts = conflicts,
            merged = merged,
            success = true,
            message = "Sync completed: ${copied.size} copied, ${merged.size} merged, ${skipped.size} skipped"
        )
    }

    private fun resolveAssetDir(projectDir: File, path: String): File {
        // Try as version first (e.g., "1.20.1")
        val versionDir = File(projectDir, "versions/$path/assets")
        if (versionDir.exists()) {
            return versionDir
        }

        // Try as asset pack (e.g., "v1")
        val assetPackDir = File(projectDir, "versions/shared/$path/assets")
        if (assetPackDir.exists()) {
            return assetPackDir
        }

        // Return the path as-is if it's absolute or relative
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
