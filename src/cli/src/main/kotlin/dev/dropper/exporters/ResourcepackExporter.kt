package dev.dropper.exporters

import dev.dropper.util.Logger
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Exports resource packs from a Dropper project
 */
class ResourcepackExporter : Exporter {

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

        val description = extractDescription(configFile) ?: "A Minecraft resource pack"

        // Resolve output directory relative to projectDir
        val resolvedOutputDir = resolveOutputDir(projectDir, outputDir)
        resolvedOutputDir.mkdirs()

        val format = packFormat ?: getDefaultResourcePackFormat("1.20.1")
        val zipFileName = "${modId}_resourcepack.zip"
        val zipFile = File(resolvedOutputDir, zipFileName)

        try {
            ZipOutputStream(zipFile.outputStream()).use { zos ->
                // Write pack.mcmeta
                val packMcmeta = buildPackMcmeta(format, description)
                zos.putNextEntry(ZipEntry("pack.mcmeta"))
                zos.write(packMcmeta.toByteArray())
                zos.closeEntry()

                // Collect asset files from versions/shared/v1/assets/
                val assetsSourceDir = File(projectDir, "versions/shared/v1/assets")
                if (assetsSourceDir.exists() && assetsSourceDir.isDirectory) {
                    addDirectoryToZip(zos, assetsSourceDir, "assets")
                } else {
                    // Create an empty assets directory entry with the mod ID namespace
                    zos.putNextEntry(ZipEntry("assets/$modId/"))
                    zos.closeEntry()
                }
            }

            Logger.info("Created resourcepack ZIP: ${zipFile.absolutePath}")
            return zipFile
        } catch (e: Exception) {
            Logger.error("Failed to create resourcepack ZIP: ${e.message}")
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
