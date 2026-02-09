package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand

/**
 * Parent command for syncing assets and data across versions
 */
class SyncCommand : CliktCommand(
    name = "sync",
    help = "Synchronize assets and data across versions and asset packs"
) {
    override fun run() = Unit
}
