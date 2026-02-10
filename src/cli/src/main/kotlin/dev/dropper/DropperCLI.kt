package dev.dropper

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.commands.*
import dev.dropper.commands.clean.*
import dev.dropper.commands.dev.*
import dev.dropper.commands.export.*
import dev.dropper.commands.import_.*
import dev.dropper.commands.list.*
import dev.dropper.commands.package_.*
import dev.dropper.commands.publish.*
import dev.dropper.commands.remove.*
import dev.dropper.commands.rename.*
import dev.dropper.commands.search.*
import dev.dropper.commands.sync.*
import dev.dropper.commands.template.*
import dev.dropper.commands.update.*
import dev.dropper.commands.validate.*

/**
 * Main CLI class for Dropper - Multi-loader Minecraft mod development tool
 */
class DropperCLI : CliktCommand(
    name = "dropper",
    help = "Multi-loader Minecraft mod development tool",
    printHelpOnEmptyArgs = true
) {
    private val version by option("--version", help = "Show version").flag()

    override fun run() {
        if (version) {
            echo(getVersion())
        } else {
            echo("Dropper - Multi-Loader Minecraft Mod Tool")
            echo("Run 'dropper --help' for available commands")
        }
    }

    companion object {
        fun getVersion(): String = "1.0.0"
    }
}

fun main(args: Array<String>) = DropperCLI()
    .subcommands(
        InitCommand(),
        CreateCommand().subcommands(
            CreateItemCommand(),
            CreateBlockCommand(),
            CreateEntityCommand(),
            CreateEnchantmentCommand(),
            CreateBiomeCommand(),
            CreateRecipeCommand(),
            CreateTagCommand(),
            AddVersionCommand(),
            AddAssetPackCommand()
        ),
        ListCommand().subcommands(
            ListItemsCommand(),
            ListBlocksCommand(),
            ListEntitiesCommand(),
            ListRecipesCommand(),
            ListEnchantmentsCommand(),
            ListBiomesCommand(),
            ListTagsCommand(),
            ListAllCommand()
        ),
        ValidateCommand().subcommands(
            ValidateStructureCommand(),
            ValidateAssetsCommand(),
            ValidateMetadataCommand(),
            ValidateRecipesCommand(),
            ValidateLangCommand()
        ),
        RemoveCommand().subcommands(
            RemoveItemCommand(),
            RemoveBlockCommand(),
            RemoveEntityCommand(),
            RemoveRecipeCommand(),
            RemoveEnchantmentCommand(),
            RemoveBiomeCommand(),
            RemoveTagCommand()
        ),
        createRenameCommand(),
        SyncCommand().subcommands(
            SyncLangCommand(),
            SyncAssetsCommand(),
            SyncRecipesCommand(),
            SyncTexturesCommand(),
            SyncModelsCommand(),
            SyncBlockstatesCommand()
        ),
        ExportCommand().subcommands(
            ExportDatapackCommand(),
            ExportResourcepackCommand(),
            ExportAssetsCommand()
        ),
        SearchCommand().subcommands(
            SearchCodeCommand(),
            SearchModelCommand(),
            SearchRecipeCommand(),
            SearchTextureCommand()
        ),
        TemplateCommand().subcommands(
            TemplateListCommand(),
            TemplateCreateCommand(),
            TemplateAddCommand()
        ),
        CleanCommand().subcommands(
            CleanAllCommand(),
            CleanBuildCommand(),
            CleanCacheCommand(),
            CleanGeneratedCommand()
        ),
        PackageCommand().subcommands(
            PackageModrinthCommand(),
            PackageCurseForgeCommand(),
            PackageBundleCommand(),
            PackageUniversalCommand()
        ),
        createMigrateCommand(),
        createPublishCommand(),
        ImportCommand().subcommands(
            ImportFabricCommand(),
            ImportForgeCommand(),
            ImportNeoForgeCommand(),
            ImportConvertCommand()
        ),
        UpdateCommand().subcommands(
            UpdateCheckCommand(),
            UpdateMinecraftCommand(),
            UpdateLoadersCommand(),
            UpdateDependenciesCommand(),
            UpdateApplyCommand()
        ),
        BuildCommand(),
        DevCommand().subcommands(
            DevRunCommand(),
            DevClientCommand(),
            DevServerCommand(),
            DevTestCommand(),
            DevReloadCommand()
        ),
        DocsCommand()
    )
    .main(args)
