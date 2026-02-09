package dev.dropper.packagers

import com.google.gson.GsonBuilder
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packages mods for Modrinth distribution
 */
class ModrinthPackager : Packager {

    override fun pack(projectDir: File, outputDir: File, options: PackageOptions): File {
        Logger.info("Creating Modrinth package...")

        val packageDir = File(outputDir, "modrinth")
        packageDir.mkdirs()

        val zipFile = File(packageDir, "${options.modId}-${options.modVersion}-modrinth.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            // Add all JAR files
            val jars = collectJars(projectDir, options)
            jars.forEach { jar ->
                addFileToZip(zip, jar, jar.name)
            }

            // Add metadata
            val metadata = generateModrinthMetadata(options, jars)
            val metadataJson = GsonBuilder().setPrettyPrinting().create().toJson(metadata)
            addStringToZip(zip, "modrinth.json", metadataJson)

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

            // Add icon if exists
            val icon = findIcon(projectDir)
            if (icon != null) {
                addFileToZip(zip, icon, "icon.png")
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

        Logger.success("Modrinth package created: ${zipFile.absolutePath}")
        return zipFile
    }

    private fun generateModrinthMetadata(options: PackageOptions, jars: List<File>): Map<String, Any> {
        val files = jars.map { jar ->
            mapOf(
                "path" to jar.name,
                "file_type" to "mod"
            )
        }

        return mapOf(
            "project_id" to options.modId,
            "version_number" to options.modVersion,
            "name" to options.modName,
            "description" to (options.description ?: ""),
            "files" to files,
            "loaders" to options.loaders.ifEmpty { listOf("fabric", "forge", "neoforge") },
            "game_versions" to options.versions.ifEmpty { listOf("1.20.1") }
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

    private fun findIcon(projectDir: File): File? {
        val possibleNames = listOf("icon.png", "logo.png", "icon.jpg", "logo.jpg")
        for (name in possibleNames) {
            val file = File(projectDir, name)
            if (file.exists()) {
                return file
            }
        }
        return null
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
