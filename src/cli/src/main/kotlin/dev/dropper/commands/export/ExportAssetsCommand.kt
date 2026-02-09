package dev.dropper.commands.export

import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.DropperCommand
import dev.dropper.exporters.AssetExporter
import dev.dropper.util.Logger
import java.io.File

/**
 * Export specific asset pack
 */
class ExportAssetsCommand : DropperCommand(
    name = "assets",
    help = "Export specific asset pack"
) {
    private val pack by argument(help = "Asset pack name (e.g., v1)")
    private val output by option("--output", "-o", help = "Output directory").default("build/assets")

    override fun run() {
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        Logger.info("Exporting asset pack: $pack...")

        val exporter = AssetExporter()
        val outputDir = exporter.export(projectDir, pack, File(output))

        if (outputDir != null) {
            Logger.success("Assets exported to: ${outputDir.absolutePath}")
        } else {
            Logger.error("Failed to export assets")
        }
    }
}
