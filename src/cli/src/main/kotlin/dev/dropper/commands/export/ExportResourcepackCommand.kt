package dev.dropper.commands.export

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.DropperCommand
import dev.dropper.exporters.ResourcepackExporter
import dev.dropper.util.Logger
import java.io.File

/**
 * Export project assets as a Minecraft resource pack
 */
class ExportResourcepackCommand : DropperCommand(
    name = "resourcepack",
    help = "Export as a Minecraft resource pack"
) {
    private val output by option("--output", "-o", help = "Output directory").default("build/resourcepacks")
    private val packFormat by option("--pack-format", "-f", help = "Pack format version")

    override fun run() {
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        Logger.info("Exporting resource pack...")

        val exporter = ResourcepackExporter()
        packFormat?.let { exporter.packFormat = it.toIntOrNull() }
        val outputFile = exporter.export(projectDir, "v1", File(output))

        if (outputFile != null) {
            Logger.success("Resource pack exported to: ${outputFile.absolutePath}")
        } else {
            Logger.error("Failed to export resource pack")
        }
    }
}
