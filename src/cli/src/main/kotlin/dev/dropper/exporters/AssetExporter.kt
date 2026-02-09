package dev.dropper.exporters

import java.io.File

/**
 * Exports asset packs from a Dropper project
 */
class AssetExporter : Exporter {
    override fun export(projectDir: File, packName: String, outputDir: File): File? {
        // TODO: Implement asset export
        println("Asset export not yet implemented")
        return null
    }
}
