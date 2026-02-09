package dev.dropper.commands.search

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.searchers.CodeSearcher
import dev.dropper.util.Logger
import java.io.File

/**
 * Search in Java source code
 */
class SearchCodeCommand : CliktCommand(
    name = "code",
    help = "Search in Java source code"
) {
    private val query by argument(help = "Search query or regex pattern")
    private val regex by option("--regex", "-r", help = "Use regex pattern").flag()
    private val limit by option("--limit", "-l", help = "Limit number of results").default("20")

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        val searcher = CodeSearcher()
        val results = searcher.search(projectDir, query, regex, limit.toInt())

        if (results.isEmpty()) {
            Logger.warn("No code matches found for: $query")
        } else {
            Logger.success("Found ${results.size} match(es) in code:")
            results.forEach { result ->
                println("  ${result.file}:${result.lineNumber}")
                println("    ${result.line.trim()}")
            }
        }
    }
}
