package dev.dropper.packagers

import com.google.gson.GsonBuilder
import dev.dropper.util.Logger
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packages mods for CurseForge distribution
 */
class CurseForgePackager : Packager {

    override fun pack(projectDir: File, outputDir: File, options: PackageOptions): File {
        Logger.info("Creating CurseForge package...")

        val packageDir = File(outputDir, "curseforge")
        packageDir.mkdirs()

        val zipFile = File(packageDir, "${options.modId}-${options.modVersion}-curseforge.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            // Add all JAR files
            val jars = collectJars(projectDir, options)
            jars.forEach { jar ->
                addFileToZip(zip, jar, jar.name)
            }

            // Add manifest
            val manifest = generateCurseForgeManifest(options, jars)
            val manifestJson = GsonBuilder().setPrettyPrinting().create().toJson(manifest)
            addStringToZip(zip, "manifest.json", manifestJson)

            // Add README if exists
            val readme = File(projectDir, "README.md")
            if (readme.exists()) {
                addFileToZip(zip, readme, "README.md")
            }

            // Add CHANGELOG if exists
            val changelog = File(projectDir, "CHANGELOG.md")
            if (changelog.exists()) {
                addFileToZip(zip, changelog, "CHANGELOG.md")
            }

            // Add LICENSE if exists
            val license = File(projectDir, "LICENSE")
            if (license.exists()) {
                addFileToZip(zip, license, "LICENSE")
            }

            // Add sources if requested
            if (options.includeSources) {
                val sourceJars = collectJars(projectDir, options, "-sources.jar")
                sourceJars.forEach { jar ->
                    addFileToZip(zip, jar, jar.name)
                }
            }

            // Add javadoc if requested
            if (options.includeJavadoc) {
                val javadocJars = collectJars(projectDir, options, "-javadoc.jar")
                javadocJars.forEach { jar ->
                    addFileToZip(zip, jar, jar.name)
                }
            }
        }

        Logger.success("CurseForge package created: ${zipFile.absolutePath}")
        return zipFile
    }

    private fun generateCurseForgeManifest(options: PackageOptions, jars: List<File>): Map<String, Any> {
        val files = jars.map { jar ->
            mapOf(
                "projectID" to 0,  // Would be filled in by user
                "fileID" to 0,      // Would be filled in by user
                "required" to true
            )
        }

        return mapOf(
            "minecraft" to mapOf<String, Any>(
                "version" to (options.versions.firstOrNull() ?: "1.20.1"),
                "modLoaders" to options.loaders.ifEmpty { listOf("fabric", "forge", "neoforge") }.map { loader ->
                    mapOf<String, Any>("id" to loader, "primary" to true)
                }
            ),
            "manifestType" to "minecraftModpack",
            "manifestVersion" to 1,
            "name" to options.modName,
            "version" to options.modVersion,
            "author" to (options.author ?: ""),
            "files" to files,
            "overrides" to "overrides"
        )
    }

    private fun collectJars(projectDir: File, options: PackageOptions, suffix: String = ".jar"): List<File> {
        val buildDir = File(projectDir, "build")
        if (!buildDir.exists()) {
            return emptyList()
        }

        val jars = mutableListOf<File>()

        // Scan build directory for JARs
        buildDir.walkTopDown().forEach { file ->
            if (file.isFile && file.name.endsWith(suffix) && !file.name.contains("-dev") && !file.name.contains("-shadow")) {
                // Filter by version if specified
                val includeByVersion = options.versions.isEmpty() ||
                    options.versions.any { file.absolutePath.contains(it.replace(".", "_")) }

                // Filter by loader if specified
                val includeByLoader = options.loaders.isEmpty() ||
                    options.loaders.any { file.absolutePath.contains(it) }

                if (includeByVersion && includeByLoader) {
                    jars.add(file)
                }
            }
        }

        return jars
    }

    private fun addFileToZip(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName))
        file.inputStream().use { input ->
            input.copyTo(zip)
        }
        zip.closeEntry()
    }

    private fun addStringToZip(zip: ZipOutputStream, entryName: String, content: String) {
        zip.putNextEntry(ZipEntry(entryName))
        zip.write(content.toByteArray())
        zip.closeEntry()
    }
}
