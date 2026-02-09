package dev.dropper.removers

import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Removes items and all related files
 */
class ItemRemover : ComponentRemover {

    override fun findRelatedFiles(projectDir: File, componentName: String, modId: String): List<File> {
        val files = mutableListOf<File>()
        val className = toClassName(componentName)

        // Common item class
        files.add(File(projectDir, "shared/common/src/main/java/com/$modId/items/$className.java"))

        // Loader-specific registrations
        files.add(File(projectDir, "shared/fabric/src/main/java/com/$modId/platform/fabric/${className}Fabric.java"))
        files.add(File(projectDir, "shared/forge/src/main/java/com/$modId/platform/forge/${className}Forge.java"))
        files.add(File(projectDir, "shared/neoforge/src/main/java/com/$modId/platform/neoforge/${className}NeoForge.java"))

        // Item model
        files.add(File(projectDir, "versions/shared/v1/assets/$modId/models/item/$componentName.json"))

        // Item texture
        files.add(File(projectDir, "versions/shared/v1/assets/$modId/textures/item/$componentName.png"))

        // Recipe (optional, may not exist)
        files.add(File(projectDir, "versions/shared/v1/data/$modId/recipe/$componentName.json"))

        return files.filter { it.exists() }
    }

    override fun findDependencies(projectDir: File, componentName: String, modId: String): List<Dependency> {
        return DependencyAnalyzer.findAllDependencies(projectDir, componentName, modId)
    }

    override fun remove(
        projectDir: File,
        componentName: String,
        modId: String,
        options: RemovalOptions
    ): RemovalResult {
        val relatedFiles = findRelatedFiles(projectDir, componentName, modId)
        val dependencies = findDependencies(projectDir, componentName, modId)
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        // Check for dependencies
        if (dependencies.isNotEmpty() && !options.force) {
            warnings.add("Item '$componentName' is referenced by ${dependencies.size} other component(s):")
            dependencies.forEach { dep ->
                warnings.add("  - ${dep.description}")
            }
            warnings.add("Use --force to remove anyway")
            return RemovalResult(
                success = false,
                warnings = warnings,
                errors = listOf("Cannot remove item with dependencies (use --force to override)")
            )
        }

        if (relatedFiles.isEmpty()) {
            return RemovalResult(
                success = false,
                errors = listOf("Item '$componentName' not found")
            )
        }

        // Dry run mode
        if (options.dryRun) {
            Logger.info("DRY RUN: Would remove the following files:")
            relatedFiles.forEach { file ->
                Logger.info("  - ${file.relativeTo(projectDir).path}")
            }
            if (dependencies.isNotEmpty()) {
                Logger.warn("Warning: ${dependencies.size} dependencies found")
                dependencies.forEach { dep ->
                    Logger.warn("  - ${dep.description}")
                }
            }
            return RemovalResult(success = true, filesRemoved = relatedFiles, warnings = warnings)
        }

        // Create backup if requested
        if (options.createBackup) {
            createBackup(projectDir, componentName, relatedFiles)
        }

        // Filter files if only removing code (keepAssets)
        val filesToRemove = if (options.keepAssets) {
            relatedFiles.filter { !it.path.contains("/assets/") && !it.path.contains("/textures/") }
        } else {
            relatedFiles
        }

        // Remove files
        val removedFiles = mutableListOf<File>()
        val removedDirs = mutableListOf<File>()

        filesToRemove.forEach { file ->
            try {
                if (file.exists() && file.delete()) {
                    removedFiles.add(file)
                    Logger.info("Removed: ${file.relativeTo(projectDir).path}")
                }
            } catch (e: Exception) {
                errors.add("Failed to remove ${file.name}: ${e.message}")
            }
        }

        // Clean up empty directories
        val emptyDirs = cleanupEmptyDirectories(projectDir, removedFiles)
        removedDirs.addAll(emptyDirs)

        return RemovalResult(
            success = errors.isEmpty(),
            filesRemoved = removedFiles,
            directoriesRemoved = removedDirs,
            warnings = warnings,
            errors = errors
        )
    }

    private fun toClassName(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { it.capitalize() }
    }

    private fun createBackup(projectDir: File, componentName: String, files: List<File>) {
        val backupDir = File(projectDir, ".dropper/backups/${System.currentTimeMillis()}_$componentName")
        backupDir.mkdirs()

        files.forEach { file ->
            if (file.exists()) {
                val relativePath = file.relativeTo(projectDir)
                val backupFile = File(backupDir, relativePath.path)
                backupFile.parentFile?.mkdirs()
                file.copyTo(backupFile, overwrite = true)
            }
        }

        Logger.info("Backup created at: ${backupDir.relativeTo(projectDir).path}")
    }

    private fun cleanupEmptyDirectories(projectDir: File, removedFiles: List<File>): List<File> {
        val removedDirs = mutableListOf<File>()
        val dirsToCheck = removedFiles.mapNotNull { it.parentFile }.distinct()

        dirsToCheck.sortedByDescending { it.path.length }.forEach { dir ->
            if (dir.exists() && dir.isDirectory && (dir.listFiles()?.isEmpty() == true)) {
                // Don't remove important base directories
                val relativePath = dir.relativeTo(projectDir).path
                if (!isImportantDirectory(relativePath)) {
                    if (dir.delete()) {
                        removedDirs.add(dir)
                        Logger.info("Removed empty directory: $relativePath")
                    }
                }
            }
        }

        return removedDirs
    }

    private fun isImportantDirectory(path: String): Boolean {
        val importantPaths = listOf(
            "shared",
            "shared/common",
            "shared/fabric",
            "shared/forge",
            "shared/neoforge",
            "versions",
            "versions/shared",
            "versions/shared/v1"
        )
        return importantPaths.any { path.startsWith(it) && path == it }
    }
}
