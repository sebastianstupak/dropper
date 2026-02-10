package dev.dropper.commands.package_

import com.github.ajalt.clikt.core.CliktCommand
import dev.dropper.util.Logger

/**
 * Create a universal JAR with all loaders merged
 */
class PackageUniversalCommand : CliktCommand(
    name = "universal",
    help = "Create universal JAR with all loaders merged (not yet implemented)"
) {
    override fun run() {
        Logger.error("Universal JAR packaging is not yet implemented.")
        Logger.info("Use 'dropper package bundle' instead.")
    }
}
