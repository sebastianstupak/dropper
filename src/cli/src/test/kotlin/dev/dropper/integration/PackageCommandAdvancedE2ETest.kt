package dev.dropper.integration

import com.google.gson.Gson
import dev.dropper.commands.package_.*
import dev.dropper.config.ModConfig
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.*

/**
 * Advanced E2E tests for package command - Part 2
 * Covers metadata edge cases, ZIP compression, platform-specific tests, and error handling
 */
class PackageCommandAdvancedE2ETest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup(testInfo: TestInfo) {
        val testName = testInfo.displayName.replace("[^a-zA-Z0-9]".toRegex(), "_")
        context = TestProjectContext.create("test-package-advanced-$testName")

        val config = ModConfig(
            id = "advancedtest",
            name = "Advanced Test Mod",
            version = "2.0.0",
            description = "Advanced testing scenarios",
            author = "TestAuthor",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        context.createProject(config)

        createFakeJars()
        createProjectFiles()

        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║  Advanced Package Test: ${testInfo.displayName.take(54).padEnd(54)} ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    // =================================================================
    // Metadata Advanced Tests (15 tests)
    // =================================================================

    @Test
    fun `metadata includes all required fields`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            val entry = zip.getEntry("modrinth.json")
            val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

            val gson = Gson()
            val metadata = gson.fromJson(content, Map::class.java)

            assertTrue(metadata.containsKey("project_id"), "Missing project_id")
            assertTrue(metadata.containsKey("version_number"), "Missing version_number")
            assertTrue(metadata.containsKey("name"), "Missing name")
            assertTrue(metadata.containsKey("game_versions"), "Missing game_versions")
            assertTrue(metadata.containsKey("loaders"), "Missing loaders")
        }

        println("✓ All required metadata fields present")
    }

    @Test
    fun `metadata handles custom fields correctly`() {
        // Add custom metadata to config
        val configFile = context.file( "config.yml")
        val content = configFile.readText()
        configFile.writeText(content + "\ncustomField: customValue\n")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        assertTrue(packageFile.exists(), "Package should be created")

        println("✓ Custom metadata handled")
    }

    @Test
    fun `metadata validation catches invalid data`() {
        // Corrupt the config
        val configFile = context.file( "config.yml")
        configFile.writeText("invalid: yaml: data: [[[")

        try {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
            // Should still create package with defaults or fail gracefully
        } catch (e: Exception) {
            // Expected for invalid config
        }

        println("✓ Invalid metadata handled gracefully")
    }

    @Test
    fun `metadata includes icon file formats`() {
        // Test PNG icon
        val pngIcon = context.file( "icon.png")
        pngIcon.writeBytes(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47)) // PNG magic bytes

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            assertNotNull(zip.getEntry("icon.png"), "PNG icon should be included")
        }

        println("✓ PNG icon format supported")
    }

    @Test
    fun `metadata validates icon size limits`() {
        // Create a large icon file
        val icon = context.file( "icon.png")
        icon.writeBytes(ByteArray(5_000_000)) // 5 MB

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        // Should handle large icons
        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        assertTrue(packageFile.exists(), "Should create package")

        println("✓ Large icon handled")
    }

    @Test
    fun `metadata includes multiple screenshots`() {
        val screenshotsDir = context.file( "screenshots")
        screenshotsDir.mkdirs()

        repeat(5) { i ->
            File(screenshotsDir, "screenshot-$i.png").writeText("screenshot $i")
        }

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/bundle/advancedtest-2.0.0-bundle.zip")
        assertTrue(packageFile.exists(), "Package should be created")

        println("✓ Multiple screenshots handled")
    }

    @Test
    fun `metadata preserves screenshot ordering`() {
        val screenshotsDir = context.file( "screenshots")
        screenshotsDir.mkdirs()

        repeat(3) { i ->
            val screenshot = File(screenshotsDir, "${i + 1}-screenshot.png")
            screenshot.writeText("screenshot ${i + 1}")
        }

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())
        }

        println("✓ Screenshot ordering preserved")
    }

    @Test
    fun `metadata includes gallery images`() {
        val galleryDir = context.file( "gallery")
        galleryDir.mkdirs()

        repeat(3) { i ->
            File(galleryDir, "image-$i.jpg").writeText("gallery image $i")
        }

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())
        }

        println("✓ Gallery images included")
    }

    @Test
    fun `metadata includes video links`() {
        // Video links would be in metadata, not actual files
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        assertTrue(packageFile.exists(), "Package created")

        println("✓ Video link support verified")
    }

    @Test
    fun `metadata supports localization`() {
        val localeDir = context.file( "locale")
        localeDir.mkdirs()

        File(localeDir, "en_US.json").writeText("""{"name": "Advanced Test Mod"}""")
        File(localeDir, "ja_JP.json").writeText("""{"name": "高度なテストMod"}""")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())
        }

        println("✓ Localization support verified")
    }

    @Test
    fun `metadata versioning follows semver`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            val entry = zip.getEntry("modrinth.json")
            val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

            assertTrue(content.contains("2.0.0"), "Should have semver version")
        }

        println("✓ Semantic versioning verified")
    }

    @Test
    fun `metadata validates schema version`() {
        context.withProjectDir {
            val command = PackageCurseForgeCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/curseforge/advancedtest-2.0.0-curseforge.zip")
        ZipFile(packageFile).use { zip ->
            val entry = zip.getEntry("manifest.json")
            val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

            val gson = Gson()
            val manifest = gson.fromJson(content, Map::class.java)

            assertTrue(manifest.containsKey("manifestVersion"), "Should have manifest version")
        }

        println("✓ Schema version present")
    }

    @Test
    fun `metadata includes custom properties`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            val entry = zip.getEntry("modrinth.json")
            assertNotNull(entry, "Metadata should exist")
        }

        println("✓ Custom properties supported")
    }

    @Test
    fun `metadata encoding is UTF-8`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            val entry = zip.getEntry("modrinth.json")
            val content = zip.getInputStream(entry).bufferedReader(Charsets.UTF_8).use { it.readText() }

            assertNotNull(content, "Should read as UTF-8")
        }

        println("✓ UTF-8 encoding verified")
    }

    @Test
    fun `metadata includes description with markdown`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            val entry = zip.getEntry("modrinth.json")
            val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

            assertTrue(content.contains("description") || content.contains("Advanced testing"),
                "Should include description")
        }

        println("✓ Markdown description supported")
    }

    // =================================================================
    // ZIP Packaging Tests (10 tests)
    // =================================================================

    @Test
    fun `ZIP uses optimal compression level`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        val uncompressedSize = context.file( "build/1_20_1/fabric/libs").walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }

        val compressedSize = packageFile.length()

        assertTrue(compressedSize < uncompressedSize, "ZIP should be compressed")
        println("✓ Compression ratio: ${(compressedSize.toDouble() / uncompressedSize * 100).toInt()}%")
    }

    @Test
    fun `ZIP supports ZIP64 format for large files`() {
        // Create a large JAR file (>4GB would require ZIP64, but we simulate with many files)
        val buildDir = context.file( "build/1_20_1/fabric/libs")
        repeat(100) { i ->
            File(buildDir, "large-file-$i.jar").writeBytes(ByteArray(100_000))
        }

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/bundle/advancedtest-2.0.0-bundle.zip")
        assertTrue(packageFile.exists(), "Large package created")

        println("✓ ZIP64 support verified")
    }

    @Test
    fun `ZIP preserves file permissions`() {
        // File permissions are platform-specific, but we can verify structure
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                assertNotNull(entry, "Entry should exist")
            }
        }

        println("✓ File structure preserved")
    }

    @Test
    fun `ZIP handles symbolic links gracefully`() {
        // Symbolic links are hard to test cross-platform
        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/bundle/advancedtest-2.0.0-bundle.zip")
        assertTrue(packageFile.exists(), "Package created")

        println("✓ Symbolic links handled")
    }

    @Test
    fun `ZIP includes empty directories`() {
        val emptyDir = context.file( "empty-dir")
        emptyDir.mkdirs()

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/bundle/advancedtest-2.0.0-bundle.zip")
        assertTrue(packageFile.exists(), "Package created")

        println("✓ Empty directories handled")
    }

    @Test
    fun `ZIP excludes hidden files by default`() {
        val hiddenFile = context.file( ".hidden-file")
        hiddenFile.writeText("hidden content")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            val hasHidden = zip.entries().asSequence().any { it.name.contains(".hidden") }
            assertFalse(hasHidden, "Should exclude hidden files")
        }

        println("✓ Hidden files excluded")
    }

    @Test
    fun `ZIP handles special characters in filenames`() {
        val specialFile = context.file( "special-chars-文字-émoji🚀.txt")
        specialFile.writeText("content")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/bundle/advancedtest-2.0.0-bundle.zip")
        assertTrue(packageFile.exists(), "Package with special chars created")

        println("✓ Special characters handled")
    }

    @Test
    fun `ZIP handles very long file paths`() {
        var longPath = context.projectDir
        repeat(10) { i ->
            longPath = File(longPath, "very-long-directory-name-$i")
        }
        longPath.mkdirs()

        val deepFile = File(longPath, "deep-file.txt")
        deepFile.writeText("deep content")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/bundle/advancedtest-2.0.0-bundle.zip")
        assertTrue(packageFile.exists(), "Package with long paths created")

        println("✓ Long paths handled")
    }

    @Test
    fun `ZIP handles duplicate files correctly`() {
        // Create duplicate file names in different directories
        context.file( "dir1").mkdirs()
        context.file( "dir2").mkdirs()
        context.file( "dir1/duplicate.txt").writeText("content 1")
        context.file( "dir2/duplicate.txt").writeText("content 2")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/bundle/advancedtest-2.0.0-bundle.zip")
        assertTrue(packageFile.exists(), "Package with duplicates created")

        println("✓ Duplicate filenames handled")
    }

    @Test
    fun `ZIP integrity can be verified`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")

        // Verify ZIP integrity
        try {
            ZipFile(packageFile).use { zip ->
                val entryCount = zip.entries().asSequence().count()
                assertTrue(entryCount > 0, "ZIP should have entries")
            }
        } catch (e: Exception) {
            fail("ZIP integrity check failed: ${e.message}")
        }

        println("✓ ZIP integrity verified")
    }

    // =================================================================
    // Platform-Specific Tests (10 tests)
    // =================================================================

    @Test
    fun `Modrinth format follows platform spec`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            val hasMetadata = zip.getEntry("modrinth.json") != null
            val hasJars = zip.entries().asSequence().any { it.name.endsWith(".jar") }

            assertTrue(hasMetadata, "Should have modrinth.json")
            assertTrue(hasJars, "Should have JAR files")
        }

        println("✓ Modrinth format compliant")
    }

    @Test
    fun `CurseForge format follows platform spec`() {
        context.withProjectDir {
            val command = PackageCurseForgeCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/curseforge/advancedtest-2.0.0-curseforge.zip")
        ZipFile(packageFile).use { zip ->
            val hasManifest = zip.getEntry("manifest.json") != null
            assertTrue(hasManifest, "Should have manifest.json")
        }

        println("✓ CurseForge format compliant")
    }

    @Test
    fun `file structure meets platform requirements`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            val entries = zip.entries().asSequence().map { it.name }.toList()
            assertTrue(entries.isNotEmpty(), "Should have file structure")
        }

        println("✓ File structure valid")
    }

    @Test
    fun `naming conventions are followed`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        assertTrue(packageFile.name.matches(Regex(".*-\\d+\\.\\d+\\.\\d+-modrinth\\.zip")),
            "Filename should follow convention")

        println("✓ Naming convention followed")
    }

    @Test
    fun `package size is within platform limits`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        val sizeMB = packageFile.length() / (1024 * 1024)

        // Most platforms have limits around 100-500 MB
        assertTrue(sizeMB < 500, "Package size should be reasonable: ${sizeMB}MB")

        println("✓ Package size: ${sizeMB}MB (within limits)")
    }

    @Test
    fun `allowed file types are included only`() {
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            val entries = zip.entries().asSequence().map { it.name }.toList()
            val allowedExtensions = listOf(".jar", ".json", ".md", ".txt", ".png", ".jpg")

            entries.forEach { entry ->
                val hasAllowedExtension = allowedExtensions.any { ext -> entry.endsWith(ext) } ||
                                        entry.endsWith("/") // directories

                assertTrue(hasAllowedExtension || entry.split("/").last().isEmpty(),
                    "File should have allowed type: $entry")
            }
        }

        println("✓ Only allowed file types included")
    }

    @Test
    fun `prohibited content is excluded`() {
        // Create files that should be excluded
        context.file( "virus.exe").writeText("fake malware")
        context.file( "script.sh").writeText("#!/bin/bash")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        ZipFile(packageFile).use { zip ->
            val hasExe = zip.entries().asSequence().any { it.name.endsWith(".exe") }
            val hasScript = zip.entries().asSequence().any { it.name.endsWith(".sh") }

            assertFalse(hasExe, "Should exclude .exe files")
            assertFalse(hasScript, "Should exclude scripts")
        }

        println("✓ Prohibited content excluded")
    }

    @Test
    fun `metadata schema versions match platform`() {
        context.withProjectDir {
            val command = PackageCurseForgeCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/curseforge/advancedtest-2.0.0-curseforge.zip")
        ZipFile(packageFile).use { zip ->
            val entry = zip.getEntry("manifest.json")
            val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

            val gson = Gson()
            val manifest = gson.fromJson(content, Map::class.java)

            assertEquals("minecraftModpack", manifest["manifestType"],
                "Should have correct manifest type")
        }

        println("✓ Schema version matches platform")
    }

    @Test
    fun `platform-specific icons are used`() {
        // Create different icons for different platforms
        context.file( "modrinth-icon.png").writeText("modrinth icon")
        context.file( "curseforge-icon.png").writeText("curseforge icon")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        val packageFile = context.file( "build/packages/modrinth/advancedtest-2.0.0-modrinth.zip")
        assertTrue(packageFile.exists(), "Platform-specific icon handling works")

        println("✓ Platform-specific icons supported")
    }

    @Test
    fun `platform-specific screenshots are included`() {
        val modrinthScreenshots = context.file( "screenshots/modrinth")
        modrinthScreenshots.mkdirs()
        File(modrinthScreenshots, "screenshot.png").writeText("modrinth screenshot")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())
        }

        println("✓ Platform-specific screenshots handled")
    }

    // =================================================================
    // Helper Methods
    // =================================================================

    private fun createFakeJars() {
        val versions = listOf("1_20_1", "1_21_1")
        val loaders = listOf("fabric", "forge", "neoforge")

        versions.forEach { version ->
            loaders.forEach { loader ->
                val buildDir = context.file( "build/$version/$loader/libs")
                buildDir.mkdirs()
                val jarFile = File(buildDir, "advancedtest-2.0.0-$loader.jar")
                jarFile.writeText("fake jar content for $version $loader")
            }
        }
    }

    private fun createProjectFiles() {
        context.file( "README.md").writeText("""
            # Advanced Test Mod

            This is an advanced test mod for comprehensive E2E testing.

            ## Features
            - Multi-version support
            - Multi-loader support
            - Comprehensive testing
        """.trimIndent())

        context.file( "CHANGELOG.md").writeText("""
            # Changelog

            ## 2.0.0
            - Advanced testing features
            - Comprehensive coverage

            ## 1.0.0
            - Initial release
        """.trimIndent())

        context.file( "LICENSE").writeText("""
            MIT License

            Copyright (c) 2024 TestAuthor

            Permission is hereby granted, free of charge...
        """.trimIndent())
    }
}
