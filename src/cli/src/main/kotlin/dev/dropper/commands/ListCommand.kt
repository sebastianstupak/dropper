package dev.dropper.commands

import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.list.*
import dev.dropper.indexer.*
import dev.dropper.util.Logger
import java.io.File

/**
 * Parent command for listing mod components
 */
class ListCommand : DropperCommand(
    name = "list",
    help = "List mod components (items, blocks, entities, etc.)"
) {
    private val format by option("--format", "-f", help = "Output format (table, json, csv, tree)").default("table")

    override fun run() {
        val configFile = getConfigFile()

        if (!configFile.exists()) {
            Logger.error("No config.yml found. Are you in a Dropper project directory?")
            return
        }

        // List all components summary
        Logger.info("Scanning project components...")

        val cache = IndexCache.get(projectDir)
        val components = cache ?: indexAllComponents(projectDir)

        if (cache == null) {
            IndexCache.save(projectDir, components)
        }

        val formatter = FormatterFactory.getFormatter(format)

        echo("\n" + "=".repeat(80))
        echo("  PROJECT INVENTORY")
        echo("=".repeat(80) + "\n")

        components.forEach { (type, items) ->
            if (items.isNotEmpty()) {
                echo("  ${type.uppercase()}: ${items.size}")
            }
        }

        echo("\n" + "-".repeat(80))
        echo("Total components: ${components.values.sumOf { it.size }}")
        echo("-".repeat(80) + "\n")

        echo("Run 'dropper list <type>' for detailed listings:")
        echo("  - dropper list items")
        echo("  - dropper list blocks")
        echo("  - dropper list entities")
        echo("  - dropper list recipes")
        echo("  - dropper list enchantments")
        echo("  - dropper list biomes")
        echo("  - dropper list tags")
        echo("  - dropper list all\n")
    }

    private fun indexAllComponents(projectDir: File): Map<String, List<ComponentInfo>> {
        return mapOf(
            "items" to ItemIndexer().index(projectDir),
            "blocks" to BlockIndexer().index(projectDir),
            "entities" to EntityIndexer().index(projectDir),
            "recipes" to RecipeIndexer().index(projectDir),
            "enchantments" to EnchantmentIndexer().index(projectDir),
            "biomes" to BiomeIndexer().index(projectDir),
            "tags" to TagIndexer().index(projectDir)
        )
    }
}

// Standalone main function for testing (commented out to avoid conflicts)
// fun main(args: Array<String>) = ListCommand()
//     .subcommands(
//         ListItemsCommand(),
//         ListBlocksCommand(),
//         ListEntitiesCommand(),
//         ListRecipesCommand(),
//         ListEnchantmentsCommand(),
//         ListBiomesCommand(),
//         ListTagsCommand(),
//         ListAllCommand()
//     )
//     .main(args)
