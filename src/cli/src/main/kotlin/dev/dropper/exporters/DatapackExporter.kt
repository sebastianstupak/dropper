package dev.dropper.exporters

import dev.dropper.util.Logger
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Exports datapacks from a Dropper project
 */
class DatapackExporter : Exporter {

    var packFormat: Int? = null

    override fun export(projectDir: File, packName: String, outputDir: File): File? {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) {
            Logger.error("No config.yml found in project")
            return null
        }

        val modId = extractModId(configFile)
        if (modId == null) {
            Logger.error("Could not extract mod ID from config.yml")
            return null
        }

        val description = extractDescription(configFile) ?: "A Minecraft datapack"

        // Resolve output directory relative to projectDir
        val resolvedOutputDir = resolveOutputDir(projectDir, outputDir)
        resolvedOutputDir.mkdirs()

        val format = packFormat ?: getDefaultPackFormat(packName)
        val zipFileName = "${modId}_datapack_${packName}.zip"
        val zipFile = File(resolvedOutputDir, zipFileName)

        try {
            ZipOutputStream(zipFile.outputStream()).use { zos ->
                // Write pack.mcmeta
                val packMcmeta = buildPackMcmeta(format, description)
                zos.putNextEntry(ZipEntry("pack.mcmeta"))
                zos.write(packMcmeta.toByteArray())
                zos.closeEntry()

                // Collect data files from versions/shared/v1/data/
                val dataSourceDir = File(projectDir, "versions/shared/v1/data")
                if (dataSourceDir.exists() && dataSourceDir.isDirectory) {
                    addDirectoryToZip(zos, dataSourceDir, "data")
                } else {
                    // Create an empty data directory entry with the mod ID namespace
                    zos.putNextEntry(ZipEntry("data/$modId/"))
                    zos.closeEntry()
                }
            }

            Logger.info("Created datapack ZIP: ${zipFile.absolutePath}")
            return zipFile
        } catch (e: Exception) {
            Logger.error("Failed to create datapack ZIP: ${e.message}")
            return null
        }
    }

    private fun buildPackMcmeta(packFormat: Int, description: String): String {
        val escapedDescription = description
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return """{
  "pack" : {
    "pack_format" : $packFormat,
    "description" : "$escapedDescription"
  }
}"""
    }

    private fun addDirectoryToZip(zos: ZipOutputStream, sourceDir: File, zipPath: String) {
        val files = sourceDir.listFiles() ?: return
        for (file in files) {
            val entryPath = "$zipPath/${file.name}"
            if (file.isDirectory) {
                zos.putNextEntry(ZipEntry("$entryPath/"))
                zos.closeEntry()
                addDirectoryToZip(zos, file, entryPath)
            } else {
                zos.putNextEntry(ZipEntry(entryPath))
                file.inputStream().use { input ->
                    input.copyTo(zos)
                }
                zos.closeEntry()
            }
        }
    }
}
