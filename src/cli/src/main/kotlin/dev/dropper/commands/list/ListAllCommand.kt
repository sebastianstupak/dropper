package dev.dropper.commands.list

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.indexer.*
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * List all components in the project
 */
class ListAllCommand : CliktCommand(
    name = "all",
    help = "List all components (complete inventory)"
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

        Logger.info("Indexing all components...")

        // Get cached or fresh index
        val cache = IndexCache.get(projectDir)
        var components = cache ?: indexAllComponents(projectDir)

        if (cache == null) {
            IndexCache.save(projectDir, components)
            Logger.success("Index cached for faster future lookups")
        } else {
            Logger.info("Using cached index")
        }

        // Apply filters to all component types
        if (search != null || version != null || loader != null) {
            components = components.mapValues { (_, items) ->
                var filtered = items

                if (search != null) {
                    filtered = filtered.filter { it.name.contains(search!!, ignoreCase = true) }
                }

                if (version != null) {
                    filtered = filtered.filter { it.versions.contains(version) }
                }

                if (loader != null) {
                    filtered = filtered.filter { it.loaders.contains(loader) }
                }

                filtered
            }
        }

        val formatter = FormatterFactory.getFormatter(format)
        val output = StringBuilder()

        output.append("\n")
        output.append("=".repeat(80))
        output.append("\n")
        output.append("  COMPLETE PROJECT INVENTORY\n")
        output.append("=".repeat(80))
        output.append("\n")

        components.forEach { (type, items) ->
            if (items.isNotEmpty()) {
                output.append("\n")
                output.append(formatter.format(items, type))
            }
        }

        val totalComponents = components.values.sumOf { it.size }
        output.append("\n")
        output.append("=".repeat(80))
        output.append("\n")
        output.append("  Total Components: $totalComponents\n")
        output.append("=".repeat(80))
        output.append("\n")

        // Export or print
        if (export != null) {
            val exportFile = File(export!!)
            FileUtil.writeText(exportFile, output.toString())
            Logger.success("Exported complete inventory to ${exportFile.absolutePath}")
        } else {
            echo(output.toString())
        }
    }

    private fun indexAllComponents(projectDir: File): Map<String, List<ComponentInfo>> {
        return mapOf(
            "items" to ItemIndexer().index(projectDir),
            "blocks" to BlockIndexer().index(projectDir),
            "entities" to EntityIndexer().index(projectDir),
            "recipes" to RecipeIndexer().index(projectDir),
            "enchantments" to EnchantmentIndexer().index(projectDir),
            "biomes" to BiomeIndexer().index(projectDir),
            "tags" to TagIndexer().index(projectDir)
        )
    }
}
