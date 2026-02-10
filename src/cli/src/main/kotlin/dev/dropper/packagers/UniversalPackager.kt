package dev.dropper.packagers

import dev.dropper.util.Logger
import java.io.File

/**
 * Universal JAR packager (not yet implemented)
 *
 * This would merge all loaders into a single JAR using jar merging/shading.
 * This is a complex task that requires:
 * - JAR merging logic
 * - Service provider configuration merging
 * - Resource deduplication
 * - Shade plugin integration
 *
 * For now, this is a placeholder that throws an informative error.
 */
class UniversalPackager : Packager {

    override fun pack(projectDir: File, outputDir: File, options: PackageOptions): File {
        Logger.error("Universal JAR packaging is not yet implemented")
        Logger.info("Creating a universal JAR requires:")
        Logger.info("  - JAR merging and shading")
        Logger.info("  - Service provider configuration merging")
        Logger.info("  - Resource deduplication")
        Logger.info("  - Complex classloading strategies")
        Logger.info("")
        Logger.info("For now, please use 'dropper package bundle' to create a bundle")
        Logger.info("with all loaders, or package each loader separately.")

        return outputDir
    }
}
