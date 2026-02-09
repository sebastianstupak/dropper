package dev.dropper.commands.list

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.indexer.FormatterFactory
import dev.dropper.indexer.IndexCache
import dev.dropper.indexer.ItemIndexer
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * List all items in the project
 */
class ListItemsCommand : CliktCommand(
    name = "items",
    help = "List all items"
) {
    private val format by option("--format", "-f", help = "Output format (table, json, csv, tree)").default("table")
    private val search by option("--search", "-s", help = "Search by name")
    private val version by option("--version", "-v", help = "Filter by version")
    private val loader by option("--loader", "-l", help = "Filter by loader")
    private val export by option("--export", "-e", help = "Export to file")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        // Get cached or fresh index
        val cache = IndexCache.get(projectDir)
        var items = cache?.get("items") ?: ItemIndexer().index(projectDir)

        if (cache == null) {
            val components = mapOf("items" to items)
            IndexCache.save(projectDir, components)
        }

        // Apply filters
        if (search != null) {
            items = items.filter { it.name.contains(search!!, ignoreCase = true) }
        }

        if (version != null) {
            items = items.filter { it.versions.contains(version) }
        }

        if (loader != null) {
            items = items.filter { it.loaders.contains(loader) }
        }

        // Format output
        val formatter = FormatterFactory.getFormatter(format)
        val output = formatter.format(items, "item")

        // Export or print
        if (export != null) {
            val exportFile = File(export!!)
            FileUtil.writeText(exportFile, output)
            Logger.success("Exported ${items.size} items to ${exportFile.absolutePath}")
        } else {
            echo(output)
        }
    }
}
