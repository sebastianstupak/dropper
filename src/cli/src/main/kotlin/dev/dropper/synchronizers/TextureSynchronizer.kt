package dev.dropper.synchronizers

import dev.dropper.util.Logger
import java.io.File

/**
 * Synchronizes texture files
 */
class TextureSynchronizer : Synchronizer {

    override fun sync(
        projectDir: File,
        source: String,
        target: String,
        options: SyncOptions
    ): SyncResult {
        Logger.info("Synchronizing textures from $source to $target...")

        val sourceDir = resolveTexturesDir(projectDir, source)
        val targetDir = resolveTexturesDir(projectDir, target)

        if (!sourceDir.exists()) {
            return SyncResult(
                success = false,
                message = "Source textures directory not found: ${sourceDir.absolutePath}"
            )
        }

        // Only sync image files
        val texturePatterns = options.excludePatterns.toMutableList()

        val diff = DiffAnalyzer.analyze(sourceDir, targetDir, texturePatterns)

        if (options.dryRun) {
            return createDryRunResult(diff, sourceDir, targetDir)
        }

        val copied = mutableListOf<FileChange>()
        val skipped = mutableListOf<FileChange>()
        val conflicts = mutableListOf<FileChange>()

        // Copy missing textures
        diff.missing.forEach { sourceFile ->
            if (isTextureFile(sourceFile)) {
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
        }

        // Update outdated textures
        diff.outdated.forEach { sourceFile ->
            if (isTextureFile(sourceFile)) {
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
        }

        // Handle conflicts
        val resolutions = ConflictResolver.resolve(
            diff.conflicts.filter { isTextureFile(it) },
            sourceDir,
            targetDir,
            options.force
        )
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
            if (isTextureFile(sourceFile)) {
                val relativePath = sourceFile.relativeTo(sourceDir).path
                val targetFile = File(targetDir, relativePath)
                skipped.add(FileChange(
                    sourceFile.absolutePath,
                    targetFile.absolutePath,
                    ChangeType.SKIP,
                    "Identical"
                ))
            }
        }

        return SyncResult(
            copied = copied,
            skipped = skipped,
            conflicts = conflicts,
            success = true,
            message = "Texture sync completed: ${copied.size} copied, ${skipped.size} skipped"
        )
    }

    private fun resolveTexturesDir(projectDir: File, path: String): File {
        // Try as version first
        val versionDir = File(projectDir, "versions/$path")
        if (versionDir.exists()) {
            val assetsDir = versionDir.walkTopDown()
                .firstOrNull { it.isDirectory && it.name == "assets" && it.parent.endsWith(path) }

            if (assetsDir != null) {
                val modIdDir = assetsDir.listFiles()?.firstOrNull { it.isDirectory }
                if (modIdDir != null) {
                    return File(modIdDir, "textures")
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
                    return File(modIdDir, "textures")
                }
            }
        }

        return File(projectDir, path)
    }

    private fun isTextureFile(file: File): Boolean {
        val extension = file.extension.lowercase()
        return extension in listOf("png", "jpg", "jpeg", "gif", "bmp", "tga")
    }

    private fun copyFile(source: File, target: File) {
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
    }

    private fun createDryRunResult(diff: DiffResult, sourceDir: File, targetDir: File): SyncResult {
        val changes = mutableListOf<FileChange>()

        (diff.missing + diff.outdated).forEach { sourceFile ->
            if (isTextureFile(sourceFile)) {
                val relativePath = sourceFile.relativeTo(sourceDir).path
                val targetFile = File(targetDir, relativePath)
                changes.add(FileChange(
                    sourceFile.absolutePath,
                    targetFile.absolutePath,
                    ChangeType.COPY,
                    "Would copy"
                ))
            }
        }

        val conflicts = diff.conflicts.filter { isTextureFile(it) }.map { sourceFile ->
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
            message = "Dry run: ${changes.size} textures would be synced, ${conflicts.size} conflicts"
        )
    }
}
