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

        println("\n" + "=".repeat(80))
        println("  PROJECT INVENTORY")
        println("=".repeat(80) + "\n")

        components.forEach { (type, items) ->
            if (items.isNotEmpty()) {
                println("  ${type.uppercase()}: ${items.size}")
            }
        }

        println("\n" + "-".repeat(80))
        println("Total components: ${components.values.sumOf { it.size }}")
        println("-".repeat(80) + "\n")

        println("Run 'dropper list <type>' for detailed listings:")
        println("  - dropper list items")
        println("  - dropper list blocks")
        println("  - dropper list entities")
        println("  - dropper list recipes")
        println("  - dropper list enchantments")
        println("  - dropper list biomes")
        println("  - dropper list tags")
        println("  - dropper list all\n")
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
