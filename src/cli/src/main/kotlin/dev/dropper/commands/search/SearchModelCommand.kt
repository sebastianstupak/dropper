package dev.dropper.commands.search

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.searchers.ModelSearcher
import dev.dropper.util.Logger
import java.io.File

/**
 * Search for models in the project
 */
class SearchModelCommand : CliktCommand(
    name = "model",
    help = "Find models in the project"
) {
    private val query by argument(help = "Search query")
    private val preview by option("--preview", "-p", help = "Show model content preview").flag()
    private val limit by option("--limit", "-l", help = "Limit number of results").default("10")
    private val type by option("--type", "-t", help = "Filter by model type (item, block, entity)")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.warn("No config.yml found. Searching in current directory.")
        }

        val searcher = ModelSearcher()
        val results = searcher.search(projectDir, query, preview, limit.toInt())

        if (type != null) {
            Logger.info("Filtering models by type: $type")
        }

        if (results.isEmpty()) {
            Logger.warn("No models found matching: $query")
        } else {
            Logger.success("Found ${results.size} model(s):")
            results.forEach { result ->
                println("  ${result.relativePath}")
                if (preview && result.preview != null) {
                    println("    Preview: ${result.preview}")
                }
            }
        }
    }
}
