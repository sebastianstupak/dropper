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

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val searcher = RecipeSearcher()
        val results = searcher.search(projectDir, query, details, limit.toInt())

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
