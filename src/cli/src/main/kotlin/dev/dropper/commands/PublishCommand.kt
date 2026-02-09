package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.dropper.commands.publish.*

/**
 * Main publish command with subcommands for different platforms
 */
class PublishCommand : CliktCommand(
    name = "publish",
    help = "Publish mod to distribution platforms"
) {
    override fun run() {
        echo("Use 'dropper publish <platform>' to publish your mod")
        echo("Available platforms: modrinth, curseforge, github, all")
    }
}

/**
 * Create publish command with all subcommands
 */
fun createPublishCommand(): CliktCommand {
    return PublishCommand().subcommands(
        PublishModrinthCommand(),
        PublishCurseForgeCommand(),
        PublishGitHubCommand(),
        PublishAllCommand()
    )
}
