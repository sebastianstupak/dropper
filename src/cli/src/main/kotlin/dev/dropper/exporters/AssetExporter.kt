package dev.dropper.exporters

import dev.dropper.util.Logger
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Exports asset packs from a Dropper project
 */
class AssetExporter : Exporter {

    override fun export(projectDir: File, packName: String, outputDir: File): File? {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) {
            Logger.error("No config.yml found in project")
            return null
        }

        // Resolve output directory relative to projectDir
        val resolvedOutputDir = resolveOutputDir(projectDir, outputDir)

        // The export target is outputDir/packName (e.g., build/exported-assets/v1)
        val exportDir = File(resolvedOutputDir, packName)
        exportDir.mkdirs()

        // Source asset pack directory
        val sourceDir = File(projectDir, "versions/shared/$packName")
        if (!sourceDir.exists()) {
            Logger.warn("Asset pack directory not found: versions/shared/$packName")
            // Still create the export directory structure
            File(exportDir, "assets").mkdirs()
            return exportDir
        }

        try {
            // Copy all contents from the asset pack source to the export directory
            copyDirectoryRecursive(sourceDir, exportDir)

            Logger.info("Exported asset pack '$packName' to: ${exportDir.absolutePath}")
            return exportDir
        } catch (e: Exception) {
            Logger.error("Failed to export asset pack: ${e.message}")
            return null
        }
    }

    private fun copyDirectoryRecursive(source: File, target: File) {
        if (source.isDirectory) {
            target.mkdirs()
            val children = source.listFiles() ?: return
            for (child in children) {
                val targetChild = File(target, child.name)
                if (child.isDirectory) {
                    copyDirectoryRecursive(child, targetChild)
                } else {
                    targetChild.parentFile?.mkdirs()
                    Files.copy(
                        child.toPath(),
                        targetChild.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                }
            }
        } else {
            target.parentFile?.mkdirs()
            Files.copy(
                source.toPath(),
                target.toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
        }
    }
}
