package dev.dropper.importers

import dev.dropper.util.FileUtil
import java.io.File

/**
 * Maps files from source project to Dropper structure
 */
class FileMapper {

    /**
     * Map source files to Dropper shared/common structure
     */
    fun mapCommonSources(
        sourceFiles: List<File>,
        sourcePath: String,
        targetDir: File,
        basePackage: String
    ) {
        sourceFiles.forEach { sourceFile ->
            // Calculate relative path from source root
            val relativePath = sourceFile.toRelativeString(File(sourcePath))

            // Determine target path in shared/common
            val targetFile = File(targetDir, "shared/common/src/main/java/$relativePath")

            // Copy file
            FileUtil.writeText(targetFile, sourceFile.readText())
        }
    }

    /**
     * Map assets to Dropper versions/shared/v1 structure
     */
    fun mapAssets(
        assetDirs: List<File>,
        targetDir: File,
        modId: String
    ) {
        assetDirs.forEach { assetDir ->
            val targetAssetDir = File(targetDir, "versions/shared/v1/assets/${assetDir.name}")

            if (assetDir.exists() && assetDir.isDirectory) {
                FileUtil.copyDirectory(assetDir, targetAssetDir)
            }
        }
    }

    /**
     * Map data to Dropper versions/shared/v1 structure
     */
    fun mapData(
        dataDirs: List<File>,
        targetDir: File,
        modId: String
    ) {
        dataDirs.forEach { dataDir ->
            val targetDataDir = File(targetDir, "versions/shared/v1/data/${dataDir.name}")

            if (dataDir.exists() && dataDir.isDirectory) {
                FileUtil.copyDirectory(dataDir, targetDataDir)
            }
        }
    }

    /**
     * Categorize source files by type (common, loader-specific, platform)
     */
    fun categorizeSourceFiles(sourceFiles: List<File>, loader: String): CategorizedSources {
        val common = mutableListOf<File>()
        val loaderSpecific = mutableListOf<File>()
        val platform = mutableListOf<File>()

        sourceFiles.forEach { file ->
            val content = file.readText()
            when {
                // Platform helper implementations
                content.contains("implements PlatformHelper") ||
                content.contains("extends PlatformHelper") -> {
                    platform.add(file)
                }
                // Loader-specific code
                isLoaderSpecific(content, loader) -> {
                    loaderSpecific.add(file)
                }
                // Common code
                else -> {
                    common.add(file)
                }
            }
        }

        return CategorizedSources(common, loaderSpecific, platform)
    }

    private fun isLoaderSpecific(content: String, loader: String): Boolean {
        return when (loader) {
            "fabric" -> content.contains("net.fabricmc") ||
                       content.contains("FabricMod") ||
                       content.contains("ModInitializer")
            "forge" -> content.contains("net.minecraftforge") ||
                      content.contains("@Mod") ||
                      content.contains("ForgeEvent")
            "neoforge" -> content.contains("net.neoforged") ||
                         content.contains("@Mod") ||
                         content.contains("NeoForge")
            else -> false
        }
    }
}

/**
 * Categorized source files
 */
data class CategorizedSources(
    val common: List<File>,
    val loaderSpecific: List<File>,
    val platform: List<File>
)
