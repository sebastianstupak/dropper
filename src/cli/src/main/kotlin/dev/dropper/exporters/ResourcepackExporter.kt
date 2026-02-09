package dev.dropper.exporters

import java.io.File

/**
 * Exports resource packs from a Dropper project
 */
class ResourcepackExporter : Exporter {
    override fun export(projectDir: File, packName: String, outputDir: File): File? {
        // TODO: Implement resourcepack export
        println("Resourcepack export not yet implemented")
        return null
    }
}
