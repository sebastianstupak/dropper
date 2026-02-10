package dev.dropper.commands.search

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.searchers.RecipeSearcher
import dev.dropper.util.Logger
import java.io.File

/**
 * Search for recipes in the project
 */
class SearchRecipeCommand : CliktCommand(
    name = "recipe",
    help = "Find recipes in the project"
) {
    private val query by argument(help = "Search query")
    private val details by option("--details", "-d", help = "Show recipe details").flag()
    private val limit by option("--limit", "-l", help = "Limit number of results").default("10")
    private val type by option("--type", "-t", help = "Filter by recipe type (crafting, smelting, etc.)")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.warn("No config.yml found. Searching in current directory.")
        }

        val searcher = RecipeSearcher()
        val results = searcher.search(projectDir, query, details, limit.toInt())

        if (type != null) {
            Logger.info("Filtering recipes by type: $type")
        }

        if (results.isEmpty()) {
            Logger.warn("No recipes found matching: $query")
        } else {
            Logger.success("Found ${results.size} recipe(s):")
            results.forEach { result ->
                println("  ${result.name} (${result.type})")
                if (details && result.details != null) {
                    println("    ${result.details}")
                }
            }
        }
    }
}
