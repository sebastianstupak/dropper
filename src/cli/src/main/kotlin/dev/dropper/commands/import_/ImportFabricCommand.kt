package dev.dropper.commands.import_

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.importers.FabricImporter
import dev.dropper.util.Logger
import java.io.File

/**
 * Import Fabric mod command
 */
class ImportFabricCommand : CliktCommand(
    name = "fabric",
    help = "Import a Fabric mod project"
) {
    private val sourcePath by argument(
        name = "SOURCE",
        help = "Path to existing Fabric mod project"
    )

    private val targetPath by option(
        "--target", "-t",
        help = "Target directory for imported project (default: source directory name)"
    )

    override fun run() {
        val source = File(sourcePath).absoluteFile

        if (!source.exists()) {
            Logger.error("Source directory does not exist: ${source.absolutePath}")
            return
        }

        val target = if (targetPath != null) {
            File(targetPath!!).absoluteFile
        } else {
            File(File(".").absolutePath, source.name + "-dropper")
        }

        if (target.exists()) {
            Logger.error("Target directory already exists: ${target.absolutePath}")
            return
        }

        try {
            val importer = FabricImporter()
            importer.import(source, target)
        } catch (e: Exception) {
            Logger.error("Failed to import Fabric mod: ${e.message}")
        }
    }
}
