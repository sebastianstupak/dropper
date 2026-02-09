package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand

/**
 * Parent command for searching mod content
 */
class SearchCommand : CliktCommand(
    name = "search",
    help = "Search for components, assets, and code"
) {
    override fun run() = Unit
}
