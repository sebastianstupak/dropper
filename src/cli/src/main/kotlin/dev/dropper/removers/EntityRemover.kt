package dev.dropper.removers

import dev.dropper.util.Logger
import java.io.File

/**
 * Removes entities and all related files
 */
class EntityRemover : ComponentRemover {

    override fun findRelatedFiles(projectDir: File, componentName: String, modId: String): List<File> {
        val files = mutableListOf<File>()
        val className = toClassName(componentName)

        // Common entity class
        files.add(File(projectDir, "shared/common/src/main/java/com/$modId/entities/$className.java"))

        // Loader-specific registrations
        files.add(File(projectDir, "shared/fabric/src/main/java/com/$modId/platform/fabric/${className}Fabric.java"))
        files.add(File(projectDir, "shared/forge/src/main/java/com/$modId/platform/forge/${className}Forge.java"))
        files.add(File(projectDir, "shared/neoforge/src/main/java/com/$modId/platform/neoforge/${className}NeoForge.java"))

        // Entity renderer (if exists)
        files.add(File(projectDir, "shared/common/src/main/java/com/$modId/entities/renderer/${className}Renderer.java"))

        // Entity model (if exists)
        files.add(File(projectDir, "shared/common/src/main/java/com/$modId/entities/model/${className}Model.java"))

        // Entity textures
        files.add(File(projectDir, "versions/shared/v1/assets/$modId/textures/entity/$componentName.png"))

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

        if (dependencies.isNotEmpty() && !options.force) {
            warnings.add("Entity '$componentName' is referenced by ${dependencies.size} other component(s):")
            dependencies.forEach { dep ->
                warnings.add("  - ${dep.description}")
            }
            warnings.add("Use --force to remove anyway")
            return RemovalResult(
                success = false,
                warnings = warnings,
                errors = listOf("Cannot remove entity with dependencies (use --force to override)")
            )
        }

        if (relatedFiles.isEmpty()) {
            return RemovalResult(
                success = false,
                errors = listOf("Entity '$componentName' not found")
            )
        }

        if (options.dryRun) {
            Logger.info("DRY RUN: Would remove the following files:")
            relatedFiles.forEach { file ->
                Logger.info("  - ${file.relativeTo(projectDir).path}")
            }
            return RemovalResult(success = true, filesRemoved = relatedFiles, warnings = warnings)
        }

        if (options.createBackup) {
            createBackup(projectDir, componentName, relatedFiles)
        }

        val filesToRemove = if (options.keepAssets) {
            relatedFiles.filter { !it.path.contains("/assets/") && !it.path.contains("/textures/") }
        } else {
            relatedFiles
        }

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
