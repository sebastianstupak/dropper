package dev.dropper.synchronizers

import dev.dropper.util.Logger
import java.io.File

/**
 * Synchronizes model files (item and block models)
 */
class ModelSynchronizer : Synchronizer {

    override fun sync(
        projectDir: File,
        source: String,
        target: String,
        options: SyncOptions
    ): SyncResult {
        Logger.info("Synchronizing models from $source to $target...")

        val sourceDir = resolveModelsDir(projectDir, source)
        val targetDir = resolveModelsDir(projectDir, target)

        if (!sourceDir.exists()) {
            return SyncResult(
                success = false,
                message = "Source models directory not found: ${sourceDir.absolutePath}"
            )
        }

        val diff = DiffAnalyzer.analyze(sourceDir, targetDir, options.excludePatterns)

        if (options.dryRun) {
            return createDryRunResult(diff, sourceDir, targetDir)
        }

        val copied = mutableListOf<FileChange>()
        val skipped = mutableListOf<FileChange>()
        val conflicts = mutableListOf<FileChange>()

        // Copy missing models
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

        // Update outdated models
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
                else -> {
                    skipped.add(FileChange(
                        sourceFile.absolutePath,
                        targetFile.absolutePath,
                        ChangeType.SKIP,
                        "Conflict: keeping target"
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
            message = "Model sync completed: ${copied.size} copied, ${skipped.size} skipped"
        )
    }

    private fun resolveModelsDir(projectDir: File, path: String): File {
        // Try as version first
        val versionDir = File(projectDir, "versions/$path")
        if (versionDir.exists()) {
            val assetsDir = versionDir.walkTopDown()
                .firstOrNull { it.isDirectory && it.name == "assets" && it.parent.endsWith(path) }

            if (assetsDir != null) {
                val modIdDir = assetsDir.listFiles()?.firstOrNull { it.isDirectory }
                if (modIdDir != null) {
                    return File(modIdDir, "models")
                }
            }
        }

        // Try as asset pack
        val assetPackDir = File(projectDir, "versions/shared/$path")
        if (assetPackDir.exists()) {
            val assetsDir = File(assetPackDir, "assets")
            if (assetsDir.exists()) {
                val modIdDir = assetsDir.listFiles()?.firstOrNull { it.isDirectory }
                if (modIdDir != null) {
                    return File(modIdDir, "models")
                }
            }
        }

        return File(projectDir, path)
    }

    private fun copyFile(source: File, target: File) {
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
    }

    private fun createDryRunResult(diff: DiffResult, sourceDir: File, targetDir: File): SyncResult {
        val changes = mutableListOf<FileChange>()

        (diff.missing + diff.outdated).forEach { sourceFile ->
            val relativePath = sourceFile.relativeTo(sourceDir).path
            val targetFile = File(targetDir, relativePath)
            changes.add(FileChange(
                sourceFile.absolutePath,
                targetFile.absolutePath,
                ChangeType.COPY,
                "Would copy"
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
            message = "Dry run: ${changes.size} models would be synced, ${conflicts.size} conflicts"
        )
    }
}
