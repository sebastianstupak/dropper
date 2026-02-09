package dev.dropper.packagers

import dev.dropper.util.Logger
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Packages all mod versions and loaders into a single bundle
 */
class BundlePackager : Packager {

    override fun pack(projectDir: File, outputDir: File, options: PackageOptions): File {
        Logger.info("Creating bundle package...")

        val packageDir = File(outputDir, "bundle")
        packageDir.mkdirs()

        val zipFile = File(packageDir, "${options.modId}-${options.modVersion}-bundle.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            // Add all JAR files organized by version/loader
            val jars = collectJars(projectDir, options)
            jars.forEach { jar ->
                // Determine version and loader from path
                val relativePath = jar.relativeTo(File(projectDir, "build")).path
                addFileToZip(zip, jar, relativePath)
            }

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

            // Generate bundle info
            val bundleInfo = generateBundleInfo(options, jars)
            addStringToZip(zip, "BUNDLE_INFO.txt", bundleInfo)

            // Add sources if requested
            if (options.includeSources) {
                val sourceJars = collectJars(projectDir, options, "-sources.jar")
                sourceJars.forEach { jar ->
                    val relativePath = jar.relativeTo(File(projectDir, "build")).path
                    addFileToZip(zip, jar, relativePath)
                }
            }

            // Add javadoc if requested
            if (options.includeJavadoc) {
                val javadocJars = collectJars(projectDir, options, "-javadoc.jar")
                javadocJars.forEach { jar ->
                    val relativePath = jar.relativeTo(File(projectDir, "build")).path
                    addFileToZip(zip, jar, relativePath)
                }
            }
        }

        Logger.success("Bundle package created: ${zipFile.absolutePath}")
        return zipFile
    }

    private fun generateBundleInfo(options: PackageOptions, jars: List<File>): String {
        val sb = StringBuilder()
        sb.appendLine("${options.modName} v${options.modVersion}")
        sb.appendLine("=" + "=".repeat(options.modName.length + options.modVersion.length + 2))
        sb.appendLine()

        if (options.description != null) {
            sb.appendLine(options.description)
            sb.appendLine()
        }

        if (options.author != null) {
            sb.appendLine("Author: ${options.author}")
        }

        if (options.license != null) {
            sb.appendLine("License: ${options.license}")
        }

        sb.appendLine()
        sb.appendLine("Included Files:")
        sb.appendLine("---------------")

        jars.forEach { jar ->
            sb.appendLine("  - ${jar.name}")
        }

        sb.appendLine()
        sb.appendLine("This bundle contains all versions and loaders for ${options.modName}.")
        sb.appendLine("Please install the appropriate JAR file for your Minecraft version and mod loader.")

        return sb.toString()
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

        return jars.sortedBy { it.name }
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
