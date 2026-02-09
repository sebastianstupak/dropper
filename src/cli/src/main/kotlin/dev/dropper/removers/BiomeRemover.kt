package dev.dropper.removers

import dev.dropper.util.Logger
import java.io.File

/**
 * Removes biomes
 */
class BiomeRemover : ComponentRemover {

    override fun findRelatedFiles(projectDir: File, componentName: String, modId: String): List<File> {
        val files = mutableListOf<File>()

        // Biome JSON
        files.add(File(projectDir, "versions/shared/v1/data/$modId/worldgen/biome/$componentName.json"))

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
        val errors = mutableListOf<String>()

        if (relatedFiles.isEmpty()) {
            return RemovalResult(
                success = false,
                errors = listOf("Biome '$componentName' not found")
            )
        }

        if (options.dryRun) {
            Logger.info("DRY RUN: Would remove the following files:")
            relatedFiles.forEach { file ->
                Logger.info("  - ${file.relativeTo(projectDir).path}")
            }
            return RemovalResult(success = true, filesRemoved = relatedFiles)
        }

        if (options.createBackup) {
            createBackup(projectDir, componentName, relatedFiles)
        }

        val removedFiles = mutableListOf<File>()
        val removedDirs = mutableListOf<File>()

        relatedFiles.forEach { file ->
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
            errors = errors
        )
    }

    private fun createBackup(projectDir: File, componentName: String, files: List<File>) {
        val backupDir = File(projectDir, ".dropper/backups/${System.currentTimeMillis()}_biome_$componentName")
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

        dirsToCheck.forEach { dir ->
            if (dir.exists() && dir.isDirectory && (dir.listFiles()?.isEmpty() == true)) {
                val relativePath = dir.relativeTo(projectDir).path
                if (dir.delete()) {
                    removedDirs.add(dir)
                    Logger.info("Removed empty directory: $relativePath")
                }
            }
        }

        return removedDirs
    }
}
