package dev.dropper.packagers

import com.google.gson.GsonBuilder
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
            val buildDir = File(projectDir, "build")
            val jars = collectJars(projectDir, options)
            jars.forEach { jar ->
                // Use relative path from build dir to avoid duplicate entries across versions
                val entryName = if (buildDir.exists() && jar.startsWith(buildDir)) {
                    jar.relativeTo(buildDir).path.replace('\\', '/')
                } else {
                    jar.name
                }
                addFileToZip(zip, jar, entryName)
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

            // Add icon if exists
            val icon = findIcon(projectDir)
            if (icon != null) {
                addFileToZip(zip, icon, "icon.png")
            }

            // Add sources if requested
            if (options.includeSources) {
                val sourceJars = collectJars(projectDir, options, "-sources.jar")
                sourceJars.forEach { jar ->
                    val entryName = if (buildDir.exists() && jar.startsWith(buildDir)) {
                        jar.relativeTo(buildDir).path.replace('\\', '/')
                    } else {
                        jar.name
                    }
                    addFileToZip(zip, jar, entryName)
                }
            }

            // Add javadoc if requested
            if (options.includeJavadoc) {
                val javadocJars = collectJars(projectDir, options, "-javadoc.jar")
                javadocJars.forEach { jar ->
                    val entryName = if (buildDir.exists() && jar.startsWith(buildDir)) {
                        jar.relativeTo(buildDir).path.replace('\\', '/')
                    } else {
                        jar.name
                    }
                    addFileToZip(zip, jar, entryName)
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
        val isDefaultSuffix = suffix == ".jar"

        // Scan build directory for JARs
        buildDir.walkTopDown().forEach { file ->
            if (file.isFile && file.name.endsWith(suffix) && !file.name.contains("-dev") && !file.name.contains("-shadow")) {
                // When collecting regular JARs, exclude sources and javadoc
                if (isDefaultSuffix && (file.name.contains("-sources.jar") || file.name.contains("-javadoc.jar"))) {
                    return@forEach
                }

                // Filter by version if specified
                val includeByVersion = options.versions.isEmpty() ||
                    options.versions.any { file.absolutePath.contains(it.replace(".", "_")) }

                // Filter by loader if specified (use path separator matching to avoid "forge" matching "neoforge")
                val normalizedPath = file.absolutePath.replace('\\', '/')
                val includeByLoader = options.loaders.isEmpty() ||
                    options.loaders.any { loader -> normalizedPath.contains("/$loader/") || normalizedPath.contains("/$loader-") }

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
