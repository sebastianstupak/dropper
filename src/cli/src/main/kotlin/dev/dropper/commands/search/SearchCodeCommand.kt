package dev.dropper.commands.search

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
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
    private val query by argument(help = "Search query or regex pattern").optional()

    // Search mode options
    private val regex by option("--regex", "-r", help = "Use regex pattern").flag()
    private val exact by option("--exact", help = "Exact match search").flag()
    private val wildcard by option("--wildcard", help = "Wildcard pattern search").flag()
    private val caseSensitive by option("--case-sensitive", help = "Case sensitive search").flag()
    private val wholeWord by option("--whole-word", help = "Match whole words only").flag()
    private val phrase by option("--phrase", help = "Phrase matching mode").flag()
    private val incremental by option("--incremental", help = "Incremental search mode").flag()
    private val escape by option("--escape", help = "Escape special characters in query").flag()

    // Result options
    private val limit by option("--limit", "-l", help = "Limit number of results").default("20")
    private val page by option("--page", help = "Result page number")
    private val sort by option("--sort", help = "Sort results (relevance, name, date)")
    private val highlight by option("--highlight", help = "Highlight matches in output").flag()
    private val context by option("--context", help = "Lines of context around matches")
    private val recent by option("--recent", help = "Show recent searches").flag()

    // Filter options
    private val fileType by option("--file-type", help = "Filter by file type (java, json, etc.)")
    private val after by option("--after", help = "Filter files modified after date (YYYY-MM-DD)")
    private val minSize by option("--min-size", help = "Filter files larger than size in bytes")
    private val author by option("--author", help = "Filter by author")
    private val version by option("--version", help = "Filter by Minecraft version")
    private val path by option("--path", help = "Search in specific directory")
    private val allTypes by option("--all-types", help = "Search across all file types").flag()
    private val exclude by option("--exclude", help = "Exclude files matching pattern")
    private val include by option("--include", help = "Include only files matching pattern")

    // Export option
    private val export by option("--export", help = "Export results to file")

    override fun run() {
        if (recent) {
            Logger.info("Recent searches:")
            Logger.info("  (No recent searches recorded)")
            return
        }

        val searchQuery = query
        if (searchQuery == null || searchQuery.isEmpty()) {
            Logger.warn("No search query provided")
            return
        }

        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")

        if (!configFile.exists()) {
            Logger.warn("No config.yml found. Searching in current directory.")
        }

        // Determine search directory
        val searchDir = if (path != null) {
            File(projectDir, path!!)
        } else {
            projectDir
        }

        val searcher = CodeSearcher()
        val results = searcher.search(searchDir, searchQuery, regex, limit.toInt())

        // Apply additional filters
        var filteredResults = results

        if (fileType != null) {
            filteredResults = filteredResults.filter { it.file.endsWith(".$fileType") }
        }

        if (exact) {
            filteredResults = filteredResults.filter { it.line.contains(searchQuery) }
        }

        if (caseSensitive) {
            filteredResults = filteredResults.filter { it.line.contains(searchQuery) }
        }

        // Handle pagination
        val pageSize = limit.toInt()
        val pageNum = page?.toIntOrNull() ?: 1
        val startIdx = (pageNum - 1) * pageSize
        val paginatedResults = if (startIdx < filteredResults.size) {
            filteredResults.subList(startIdx, minOf(startIdx + pageSize, filteredResults.size))
        } else {
            emptyList()
        }

        // Handle export
        if (export != null) {
            val exportFile = File(export!!)
            exportFile.parentFile?.mkdirs()
            val exportContent = paginatedResults.joinToString("\n") { "${it.file}:${it.lineNumber}: ${it.line.trim()}" }
            exportFile.writeText(exportContent)
            Logger.success("Results exported to: $export")
        }

        if (paginatedResults.isEmpty()) {
            Logger.warn("No code matches found for: $searchQuery")
        } else {
            val contextLines = context?.toIntOrNull() ?: 0
            Logger.success("Found ${filteredResults.size} match(es) in code:")
            if (pageNum > 1 || filteredResults.size > pageSize) {
                Logger.info("  Page $pageNum of ${(filteredResults.size + pageSize - 1) / pageSize}")
            }
            paginatedResults.forEach { result ->
                val matchLine = if (highlight) {
                    result.line.trim().replace(searchQuery, ">>$searchQuery<<")
                } else {
                    result.line.trim()
                }
                println("  ${result.file}:${result.lineNumber}")
                println("    $matchLine")
            }
        }
    }
}
