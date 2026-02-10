package dev.dropper.commands.export

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.DropperCommand
import dev.dropper.exporters.DatapackExporter
import dev.dropper.util.Logger
import java.io.File

/**
 * Export project data as a Minecraft datapack
 */
class ExportDatapackCommand : DropperCommand(
    name = "datapack",
    help = "Export as a Minecraft datapack"
) {
    private val version by argument(help = "Minecraft version (e.g., 1.20.1)")
    private val output by option("--output", "-o", help = "Output directory").default("build/datapacks")
    private val packFormat by option("--pack-format", "-f", help = "Pack format version")

    override fun run() {
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        Logger.info("Exporting datapack for version $version...")

        val exporter = DatapackExporter()
        packFormat?.let { exporter.packFormat = it.toIntOrNull() }
        val outputFile = exporter.export(projectDir, version, File(output))

        if (outputFile != null) {
            Logger.success("Datapack exported to: ${outputFile.absolutePath}")
        } else {
            Logger.error("Failed to export datapack")
        }
    }
}
