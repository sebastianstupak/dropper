package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Generate documentation JSON for the website
 */
class DocsCommand : CliktCommand(
    name = "docs",
    help = "Generate documentation JSON for the website"
) {
    private val output by option("--output", "-o", help = "Output file path")
        .default("docs.json")

    override fun run() {
        Logger.info("Generating documentation...")

        val docs = buildDocumentation()
        val json = serializeToJson(docs)

        val outputFile = File(output)
        FileUtil.writeText(outputFile, json)

        Logger.success("Documentation generated: $output")
    }

    private fun buildDocumentation(): DocumentationSchema {
        return DocumentationSchema(
            version = "1.0.0",
            commands = listOf(
                CommandDoc(
                    name = "init",
                    description = "Initialize a new multi-loader Minecraft mod project",
                    usage = "dropper init <PROJECT_NAME>",
                    arguments = listOf(
                        ArgumentDoc("PROJECT_NAME", "Name of the project directory to create", required = true)
                    ),
                    options = listOf(
                        OptionDoc("--name", "Mod display name", required = false),
                        OptionDoc("--id", "Mod ID (lowercase, no spaces)", required = false),
                        OptionDoc("--author", "Mod author", required = false),
                        OptionDoc("--description", "Mod description", required = false),
                        OptionDoc("--license", "License (default: MIT)", required = false),
                        OptionDoc("--versions", "Minecraft versions (comma-separated)", required = false),
                        OptionDoc("--loaders", "Mod loaders (default: fabric,forge,neoforge)", required = false)
                    ),
                    examples = listOf(
                        "dropper init my-mod",
                        "dropper init my-mod --name \"My Awesome Mod\" --author \"YourName\"",
                        "dropper init my-mod --versions 1.20.1,1.21.1 --loaders fabric,neoforge"
                    )
                ),
                CommandDoc(
                    name = "create",
                    description = "Create new mod components (items, blocks, etc.)",
                    usage = "dropper create <TYPE> <NAME>",
                    arguments = listOf(),
                    options = listOf(),
                    examples = listOf(
                        "dropper create item ruby_sword",
                        "dropper create block custom_ore"
                    ),
                    subcommands = listOf(
                        CommandDoc(
                            name = "item",
                            description = "Create a new item with registration code and assets",
                            usage = "dropper create item <NAME>",
                            arguments = listOf(
                                ArgumentDoc("NAME", "Item name in snake_case (e.g., ruby_sword)", required = true)
                            ),
                            options = listOf(
                                OptionDoc("--type, -t", "Item type: basic, tool, food (default: basic)", required = false),
                                OptionDoc("--recipe, -r", "Generate recipe (default: true)", required = false)
                            ),
                            examples = listOf(
                                "dropper create item ruby_sword",
                                "dropper create item ruby_sword --type tool",
                                "dropper create item golden_apple --type food --recipe true"
                            )
                        ),
                        CommandDoc(
                            name = "block",
                            description = "Create a new block with registration code and assets",
                            usage = "dropper create block <NAME>",
                            arguments = listOf(
                                ArgumentDoc("NAME", "Block name in snake_case (e.g., custom_ore)", required = true)
                            ),
                            options = listOf(
                                OptionDoc("--type, -t", "Block type (default: basic)", required = false)
                            ),
                            examples = listOf(
                                "dropper create block custom_ore",
                                "dropper create block magic_block --type basic"
                            )
                        )
                    )
                ),
                CommandDoc(
                    name = "build",
                    description = "Build your mod for all configured loaders and versions",
                    usage = "dropper build [OPTIONS]",
                    arguments = listOf(),
                    options = listOf(
                        OptionDoc("--version", "Build for specific Minecraft version", required = false),
                        OptionDoc("--loader", "Build for specific loader (fabric, forge, neoforge)", required = false),
                        OptionDoc("--all", "Build for all versions and loaders (default)", required = false)
                    ),
                    examples = listOf(
                        "dropper build",
                        "dropper build --version 1.20.1",
                        "dropper build --loader fabric",
                        "dropper build --version 1.20.1 --loader neoforge"
                    )
                ),
                CommandDoc(
                    name = "generate",
                    description = "Generate project files from templates",
                    usage = "dropper generate",
                    arguments = listOf(),
                    options = listOf(),
                    examples = listOf(
                        "dropper generate"
                    )
                )
            )
        )
    }

    private fun serializeToJson(docs: DocumentationSchema): String {
        // Manual JSON serialization (no dependencies needed for native image)
        val sb = StringBuilder()
        sb.append("{\n")
        sb.append("  \"version\": \"${docs.version}\",\n")
        sb.append("  \"commands\": [\n")

        docs.commands.forEachIndexed { index, cmd ->
            sb.append(commandToJson(cmd, indent = 4))
            if (index < docs.commands.size - 1) sb.append(",")
            sb.append("\n")
        }

        sb.append("  ]\n")
        sb.append("}")
        return sb.toString()
    }

    private fun commandToJson(cmd: CommandDoc, indent: Int): String {
        val pad = " ".repeat(indent)
        val sb = StringBuilder()

        sb.append("$pad{\n")
        sb.append("$pad  \"name\": \"${cmd.name}\",\n")
        sb.append("$pad  \"description\": \"${escapeJson(cmd.description)}\",\n")
        sb.append("$pad  \"usage\": \"${escapeJson(cmd.usage)}\",\n")

        // Arguments
        sb.append("$pad  \"arguments\": [\n")
        cmd.arguments.forEachIndexed { i, arg ->
            sb.append("$pad    {\"name\": \"${arg.name}\", \"description\": \"${escapeJson(arg.description)}\", \"required\": ${arg.required}}")
            if (i < cmd.arguments.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("$pad  ],\n")

        // Options
        sb.append("$pad  \"options\": [\n")
        cmd.options.forEachIndexed { i, opt ->
            sb.append("$pad    {\"name\": \"${opt.name}\", \"description\": \"${escapeJson(opt.description)}\", \"required\": ${opt.required}}")
            if (i < cmd.options.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("$pad  ],\n")

        // Examples
        sb.append("$pad  \"examples\": [\n")
        cmd.examples.forEachIndexed { i, ex ->
            sb.append("$pad    \"${escapeJson(ex)}\"")
            if (i < cmd.examples.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("$pad  ]")

        // Subcommands
        if (cmd.subcommands.isNotEmpty()) {
            sb.append(",\n$pad  \"subcommands\": [\n")
            cmd.subcommands.forEachIndexed { i, sub ->
                sb.append(commandToJson(sub, indent + 4))
                if (i < cmd.subcommands.size - 1) sb.append(",")
                sb.append("\n")
            }
            sb.append("$pad  ]\n")
        } else {
            sb.append("\n")
        }

        sb.append("$pad}")
        return sb.toString()
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}

// Data classes for documentation
data class DocumentationSchema(
    val version: String,
    val commands: List<CommandDoc>
)

data class CommandDoc(
    val name: String,
    val description: String,
    val usage: String,
    val arguments: List<ArgumentDoc>,
    val options: List<OptionDoc>,
    val examples: List<String>,
    val subcommands: List<CommandDoc> = emptyList()
)

data class ArgumentDoc(
    val name: String,
    val description: String,
    val required: Boolean
)

data class OptionDoc(
    val name: String,
    val description: String,
    val required: Boolean
)
