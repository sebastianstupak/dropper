package dev.dropper.commands.search

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.searchers.TextureSearcher
import dev.dropper.util.Logger
import java.io.File

/**
 * Search for textures in the project
 */
class SearchTextureCommand : CliktCommand(
    name = "texture",
    help = "Find textures in the project"
) {
    private val query by argument(help = "Search query")
    private val fuzzy by option("--fuzzy", "-f", help = "Enable fuzzy matching").flag()
    private val limit by option("--limit", "-l", help = "Limit number of results").default("10")
    private val resolution by option("--resolution", help = "Filter by texture resolution (e.g., 16x16, 32x32)")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.warn("No config.yml found. Searching in current directory.")
        }

        val searcher = TextureSearcher()
        val results = searcher.search(projectDir, query, fuzzy, limit.toInt())

        if (resolution != null) {
            Logger.info("Filtering textures by resolution: $resolution")
        }

        if (results.isEmpty()) {
            Logger.warn("No textures found matching: $query")
        } else {
            Logger.success("Found ${results.size} texture(s):")
            results.forEach { result ->
                println("  ${result.relativePath} (${result.size} bytes)")
            }
        }
    }
}
