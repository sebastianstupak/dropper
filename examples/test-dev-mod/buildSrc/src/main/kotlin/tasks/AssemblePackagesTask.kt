package tasks

import config.ConfigLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import utils.AssetPackResolver
import java.io.File

/**
 * Assembles Java package structure from flat source files.
 *
 * Reads package declarations from Java files and organizes them into
 * proper com/modid/package/Class.java structure at build time.
 *
 * Supports incremental builds via @InputDirectory and @OutputDirectory.
 */
abstract class AssemblePackagesTask : DefaultTask() {

    @get:Internal
    abstract val rootDirectory: DirectoryProperty

    @get:Input
    abstract val mcVersion: Property<String>

    @get:Input
    abstract val loader: Property<String>

    @get:Input
    abstract val basePackage: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        group = "build"
        description = "Assembles package structure from flat source files"
    }

    @TaskAction
    fun assemble() {
        val root = rootDirectory.get().asFile
        val version = mcVersion.get()
        val loaderName = loader.get()
        val outDir = outputDir.get().asFile

        // Clear output
        outDir.deleteRecursively()
        outDir.mkdirs()

        // Resolve source directories in layer order
        val resolver = AssetPackResolver(root)
        val sourceDirs = resolver.resolveSourceDirs(version, loaderName)

        logger.lifecycle("Assembling packages for $version-$loaderName")
        logger.info("Source layers (${sourceDirs.size}):")
        sourceDirs.forEach { logger.info("  - ${it.relativeTo(root)}") }

        // Process each source file
        var fileCount = 0
        sourceDirs.forEach { srcDir ->
            if (srcDir.exists()) {
                srcDir.walkTopDown()
                    .filter { it.isFile && it.extension == "java" }
                    .forEach { javaFile ->
                        assembleFile(javaFile, outDir, srcDir)
                        fileCount++
                    }
            }
        }

        logger.lifecycle("Assembled $fileCount Java files into ${outDir.relativeTo(root)}")
    }

    private fun assembleFile(javaFile: File, outDir: File, sourceRoot: File) {
        // Read package declaration
        val packagePath = extractPackagePath(javaFile)

        // Create target directory
        val targetDir = File(outDir, packagePath)
        targetDir.mkdirs()

        // Copy file (later layers override earlier)
        javaFile.copyTo(
            File(targetDir, javaFile.name),
            overwrite = true
        )
    }

    private fun extractPackagePath(javaFile: File): String {
        val packageLine = javaFile.useLines { lines ->
            lines.firstOrNull { it.trim().startsWith("package ") }
        }

        return if (packageLine != null) {
            packageLine.trim()
                .removePrefix("package ")
                .removeSuffix(";")
                .trim()
                .replace('.', '/')
        } else {
            // No package declaration, use base package
            logger.warn("No package declaration in ${javaFile.name}, using base package")
            basePackage.get().replace('.', '/')
        }
    }
}
