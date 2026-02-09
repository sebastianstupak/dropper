package dev.dropper.synchronizers

import dev.dropper.util.Logger
import java.io.File

/**
 * Synchronizes language files with intelligent merging
 */
class LangSynchronizer : Synchronizer {

    override fun sync(
        projectDir: File,
        source: String,
        target: String,
        options: SyncOptions
    ): SyncResult {
        Logger.info("Synchronizing lang files from $source to $target...")

        val sourceDir = resolveLangDir(projectDir, source)
        val targetDir = resolveLangDir(projectDir, target)

        if (!sourceDir.exists()) {
            return SyncResult(
                success = false,
                message = "Source lang directory not found: ${sourceDir.absolutePath}"
            )
        }

        // Find all lang files
        val langFiles = sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .filterNot { shouldExclude(it, sourceDir, options.excludePatterns) }
            .toList()

        if (options.dryRun) {
            return createDryRunResult(langFiles, sourceDir, targetDir)
        }

        val merged = mutableListOf<FileChange>()
        val copied = mutableListOf<FileChange>()
        val skipped = mutableListOf<FileChange>()

        langFiles.forEach { sourceFile ->
            val relativePath = sourceFile.relativeTo(sourceDir).path
            val targetFile = File(targetDir, relativePath)

            if (!targetFile.exists()) {
                // Copy new lang file
                copyFile(sourceFile, targetFile)
                copied.add(FileChange(
                    sourceFile.absolutePath,
                    targetFile.absolutePath,
                    ChangeType.COPY,
                    "New lang file"
                ))
            } else {
                // Merge existing lang file
                val mergedContent = ConflictResolver.mergeLangFiles(sourceFile, targetFile)
                val originalContent = targetFile.readText()

                if (mergedContent != originalContent) {
                    targetFile.writeText(mergedContent)
                    merged.add(FileChange(
                        sourceFile.absolutePath,
                        targetFile.absolutePath,
                        ChangeType.MERGE,
                        "Lang file merged"
                    ))
                } else {
                    skipped.add(FileChange(
                        sourceFile.absolutePath,
                        targetFile.absolutePath,
                        ChangeType.SKIP,
                        "Already up-to-date"
                    ))
                }
            }
        }

        return SyncResult(
            copied = copied,
            merged = merged,
            skipped = skipped,
            success = true,
            message = "Lang sync completed: ${copied.size} copied, ${merged.size} merged, ${skipped.size} skipped"
        )
    }

    private fun resolveLangDir(projectDir: File, path: String): File {
        // Try as version first
        val versionDir = File(projectDir, "versions/$path")
        if (versionDir.exists()) {
            // Find the first modid assets directory
            val assetsDir = versionDir.walkTopDown()
                .firstOrNull { it.isDirectory && it.name == "assets" && it.parent.endsWith(path) }

            if (assetsDir != null) {
                val modIdDir = assetsDir.listFiles()?.firstOrNull { it.isDirectory }
                if (modIdDir != null) {
                    return File(modIdDir, "lang")
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
                    return File(modIdDir, "lang")
                }
            }
        }

        return File(projectDir, path)
    }

    private fun shouldExclude(file: File, baseDir: File, patterns: List<String>): Boolean {
        val relativePath = file.relativeTo(baseDir).path.replace('\\', '/')
        return patterns.any { pattern ->
            val regex = globToRegex(pattern)
            regex.matches(relativePath)
        }
    }

    private fun globToRegex(pattern: String): Regex {
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return Regex(regexPattern)
    }

    private fun copyFile(source: File, target: File) {
        target.parentFile?.mkdirs()
        source.copyTo(target, overwrite = true)
    }

    private fun createDryRunResult(langFiles: List<File>, sourceDir: File, targetDir: File): SyncResult {
        val changes = mutableListOf<FileChange>()

        langFiles.forEach { sourceFile ->
            val relativePath = sourceFile.relativeTo(sourceDir).path
            val targetFile = File(targetDir, relativePath)

            if (!targetFile.exists()) {
                changes.add(FileChange(
                    sourceFile.absolutePath,
                    targetFile.absolutePath,
                    ChangeType.COPY,
                    "Would copy"
                ))
            } else {
                changes.add(FileChange(
                    sourceFile.absolutePath,
                    targetFile.absolutePath,
                    ChangeType.MERGE,
                    "Would merge"
                ))
            }
        }

        return SyncResult(
            copied = changes.filter { it.changeType == ChangeType.COPY },
            merged = changes.filter { it.changeType == ChangeType.MERGE },
            success = true,
            message = "Dry run: ${changes.size} lang files would be synced"
        )
    }
}
