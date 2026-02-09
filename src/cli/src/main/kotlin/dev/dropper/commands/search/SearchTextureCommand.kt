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

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val searcher = TextureSearcher()
        val results = searcher.search(projectDir, query, fuzzy, limit.toInt())

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
