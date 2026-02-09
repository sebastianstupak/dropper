package dev.dropper.exporters

import java.io.File

/**
 * Exports datapacks from a Dropper project
 */
class DatapackExporter : Exporter {
    override fun export(projectDir: File, packName: String, outputDir: File): File? {
        // TODO: Implement datapack export
        println("Datapack export not yet implemented")
        return null
    }
}
