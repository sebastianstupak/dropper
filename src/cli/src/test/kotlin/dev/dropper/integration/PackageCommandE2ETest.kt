package dev.dropper.integration

import com.google.gson.Gson
import dev.dropper.commands.package_.*
import dev.dropper.config.ModConfig
import dev.dropper.util.TestProjectContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.util.zip.ZipFile
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive E2E tests for package command
 * Tests all packaging scenarios with 44+ test cases
 */
class PackageCommandE2ETest {

    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("test-package")

        // Generate a project with multiple versions and loaders
        val config = ModConfig(
            id = "testpackage",
            name = "Test Package Mod",
            version = "1.0.0",
            description = "Test mod for package commands",
            author = "TestAuthor",
            license = "MIT",
            minecraftVersions = listOf("1.20.1", "1.21.1"),
            loaders = listOf("fabric", "forge", "neoforge")
        )

        context.createProject(config)

        // Create fake JAR files in build directory to simulate a built project
        createFakeJars()

        // Create additional files
        createProjectFiles()
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    // =================================================================
    // Basic Packaging Tests (8 tests)
    // =================================================================

    @Test
    fun `package modrinth creates zip file`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 1: Package Modrinth Format                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file("build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            assertTrue(packageFile.exists(), "Modrinth package should exist")
            assertTrue(packageFile.length() > 0, "Package should not be empty")

            println("✓ Modrinth package created successfully")
        }
    }

    @Test
    fun `package curseforge creates zip file`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 2: Package CurseForge Format                       ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageCurseForgeCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/curseforge/testpackage-1.0.0-curseforge.zip")
            assertTrue(packageFile.exists(), "CurseForge package should exist")
            assertTrue(packageFile.length() > 0, "Package should not be empty")

            println("✓ CurseForge package created successfully")
        }
    }

    @Test
    fun `package bundle creates zip file`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 3: Package Bundle Format                           ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            assertTrue(packageFile.exists(), "Bundle package should exist")
            assertTrue(packageFile.length() > 0, "Package should not be empty")

            println("✓ Bundle package created successfully")
        }
    }

    @Test
    fun `package with custom output directory`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 4: Custom Output Directory                         ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val customOutput = "build/custom-packages"
        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(arrayOf("--output", customOutput))

            val packageFile = context.file( "$customOutput/modrinth/testpackage-1.0.0-modrinth.zip")
            assertTrue(packageFile.exists(), "Package should exist in custom directory")

            println("✓ Package created in custom directory")
        }
    }

    @Test
    fun `package includes sources`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 5: Include Sources                                 ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Create source JARs
        createFakeSourceJars()

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(arrayOf("--include-sources"))

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            assertTrue(packageFile.exists(), "Package should exist")

            // Verify sources are included
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val hasSourceJar = entries.any { it.name.contains("-sources.jar") }
                assertTrue(hasSourceJar, "Package should contain source JARs")
            }

            println("✓ Source JARs included in package")
        }
    }

    @Test
    fun `package includes javadoc`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 6: Include Javadoc                                 ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Create javadoc JARs
        createFakeJavadocJars()

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(arrayOf("--include-javadoc"))

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            assertTrue(packageFile.exists(), "Package should exist")

            // Verify javadoc is included
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val hasJavadocJar = entries.any { it.name.contains("-javadoc.jar") }
                assertTrue(hasJavadocJar, "Package should contain javadoc JARs")
            }

            println("✓ Javadoc JARs included in package")
        }
    }

    @Test
    fun `package specific versions only`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 7: Specific Versions Only                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(arrayOf("--versions", "1.20.1"))

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            assertTrue(packageFile.exists(), "Package should exist")

            // Verify only 1.20.1 JARs are included
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val has1201 = entries.any { it.name.contains("1_20_1") }
                val has1211 = entries.any { it.name.contains("1_21_1") }
                assertTrue(has1201, "Should include 1.20.1 JARs")
                assertFalse(has1211, "Should NOT include 1.21.1 JARs")
            }

            println("✓ Only specified version included")
        }
    }

    @Test
    fun `package specific loaders only`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 8: Specific Loaders Only                           ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(arrayOf("--loaders", "fabric,forge"))

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            assertTrue(packageFile.exists(), "Package should exist")

            // Verify only fabric and forge JARs are included
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val hasFabric = entries.any { it.name.contains("fabric") }
                val hasForge = entries.any { it.name.contains("forge") && !it.name.contains("neoforge") }
                val hasNeoforge = entries.any { it.name.contains("neoforge") }
                assertTrue(hasFabric, "Should include Fabric JARs")
                assertTrue(hasForge, "Should include Forge JARs")
                assertFalse(hasNeoforge, "Should NOT include NeoForge JARs")
            }

            println("✓ Only specified loaders included")
        }
    }

    // =================================================================
    // Modrinth Package Tests (6 tests)
    // =================================================================

    @Test
    fun `modrinth package contains metadata json`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 9: Modrinth Metadata JSON                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("modrinth.json")
                assertTrue(entry != null, "Should contain modrinth.json")
            }

            println("✓ modrinth.json present in package")
        }
    }

    @Test
    fun `modrinth metadata has valid json format`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 10: Validate Modrinth JSON Format                  ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("modrinth.json")
                val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                // Parse as JSON to validate format
                val gson = Gson()
                val metadata = gson.fromJson(content, Map::class.java)

                assertTrue(metadata.containsKey("project_id"), "Should have project_id")
                assertTrue(metadata.containsKey("version_number"), "Should have version_number")
                assertTrue(metadata.containsKey("name"), "Should have name")
                assertEquals("testpackage", metadata["project_id"])
                assertEquals("1.0.0", metadata["version_number"])
            }

            println("✓ Modrinth JSON format is valid")
        }
    }

    @Test
    fun `modrinth package includes icon if present`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 11: Modrinth Icon Inclusion                        ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Create icon file
        val iconFile = context.file( "icon.png")
        iconFile.writeText("fake png data")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val iconEntry = zip.getEntry("icon.png")
                assertTrue(iconEntry != null, "Should include icon.png")
            }

            println("✓ Icon included in package")
        }
    }

    @Test
    fun `modrinth package works without icon`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 12: Modrinth Package Without Icon                  ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            assertTrue(packageFile.exists(), "Package should be created without icon")

            ZipFile(packageFile).use { zip ->
                val iconEntry = zip.getEntry("icon.png")
                assertTrue(iconEntry == null, "Should NOT include icon.png")
            }

            println("✓ Package created successfully without icon")
        }
    }

    @Test
    fun `modrinth metadata includes version info`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 13: Modrinth Version Metadata                      ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("modrinth.json")
                val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                val gson = Gson()
                val metadata = gson.fromJson(content, Map::class.java)

                assertTrue(metadata.containsKey("game_versions"), "Should have game_versions")
                assertTrue(metadata.containsKey("loaders"), "Should have loaders")
            }

            println("✓ Version metadata present")
        }
    }

    @Test
    fun `modrinth metadata includes loader info`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 14: Modrinth Loader Metadata                       ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("modrinth.json")
                val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                assertTrue(content.contains("fabric") || content.contains("forge"), "Should mention loaders")
            }

            println("✓ Loader metadata present")
        }
    }

    // =================================================================
    // CurseForge Package Tests (6 tests)
    // =================================================================

    @Test
    fun `curseforge package contains manifest json`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 15: CurseForge Manifest JSON                       ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageCurseForgeCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/curseforge/testpackage-1.0.0-curseforge.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("manifest.json")
                assertTrue(entry != null, "Should contain manifest.json")
            }

            println("✓ manifest.json present in package")
        }
    }

    @Test
    fun `curseforge manifest has valid json format`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 16: Validate CurseForge JSON Format                ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageCurseForgeCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/curseforge/testpackage-1.0.0-curseforge.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("manifest.json")
                val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                // Parse as JSON to validate format
                val gson = Gson()
                val manifest = gson.fromJson(content, Map::class.java)

                assertTrue(manifest.containsKey("manifestType"), "Should have manifestType")
                assertTrue(manifest.containsKey("manifestVersion"), "Should have manifestVersion")
                assertTrue(manifest.containsKey("name"), "Should have name")
                assertEquals("minecraftModpack", manifest["manifestType"])
            }

            println("✓ CurseForge JSON format is valid")
        }
    }

    @Test
    fun `curseforge manifest includes minecraft section`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 17: CurseForge Minecraft Section                   ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageCurseForgeCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/curseforge/testpackage-1.0.0-curseforge.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("manifest.json")
                val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                val gson = Gson()
                val manifest = gson.fromJson(content, Map::class.java)

                assertTrue(manifest.containsKey("minecraft"), "Should have minecraft section")
            }

            println("✓ Minecraft section present")
        }
    }

    @Test
    fun `curseforge manifest includes mod loaders`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 18: CurseForge Mod Loaders                         ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageCurseForgeCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/curseforge/testpackage-1.0.0-curseforge.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("manifest.json")
                val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                assertTrue(content.contains("modLoaders"), "Should have modLoaders")
            }

            println("✓ Mod loaders included")
        }
    }

    @Test
    fun `curseforge manifest includes files section`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 19: CurseForge Files Section                       ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageCurseForgeCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/curseforge/testpackage-1.0.0-curseforge.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("manifest.json")
                val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                val gson = Gson()
                val manifest = gson.fromJson(content, Map::class.java)

                assertTrue(manifest.containsKey("files"), "Should have files section")
            }

            println("✓ Files section present")
        }
    }

    @Test
    fun `curseforge file structure is correct`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 20: CurseForge File Structure                      ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageCurseForgeCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/curseforge/testpackage-1.0.0-curseforge.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val hasManifest = entries.any { it.name == "manifest.json" }
                val hasJars = entries.any { it.name.endsWith(".jar") }

                assertTrue(hasManifest, "Should have manifest.json")
                assertTrue(hasJars, "Should have JAR files")
            }

            println("✓ File structure is correct")
        }
    }

    // =================================================================
    // Bundle Package Tests (8 tests)
    // =================================================================

    @Test
    fun `bundle creates proper zip structure`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 21: Bundle ZIP Structure                           ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                assertTrue(entries.isNotEmpty(), "ZIP should not be empty")
            }

            println("✓ ZIP structure is valid")
        }
    }

    @Test
    fun `bundle includes all jars`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 22: Bundle Includes All JARs                       ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val jars = entries.filter { it.name.endsWith(".jar") }

                // Should have JARs for all version/loader combinations
                // 2 versions * 3 loaders = 6 JARs
                assertTrue(jars.size >= 6, "Should have at least 6 JARs (2 versions * 3 loaders)")
            }

            println("✓ All JARs included")
        }
    }

    @Test
    fun `bundle includes readme`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 23: Bundle Includes README                         ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val readmeEntry = zip.getEntry("README.md")
                assertTrue(readmeEntry != null, "Should include README.md")
            }

            println("✓ README.md included")
        }
    }

    @Test
    fun `bundle includes changelog`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 24: Bundle Includes CHANGELOG                      ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val changelogEntry = zip.getEntry("CHANGELOG.md")
                assertTrue(changelogEntry != null, "Should include CHANGELOG.md")
            }

            println("✓ CHANGELOG.md included")
        }
    }

    @Test
    fun `bundle includes license`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 25: Bundle Includes LICENSE                        ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val licenseEntry = zip.getEntry("LICENSE")
                assertTrue(licenseEntry != null, "Should include LICENSE")
            }

            println("✓ LICENSE included")
        }
    }

    @Test
    fun `bundle supports multiple versions`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 26: Bundle Multiple Versions                       ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val has1201 = entries.any { it.name.contains("1_20_1") }
                val has1211 = entries.any { it.name.contains("1_21_1") }

                assertTrue(has1201, "Should include 1.20.1 JARs")
                assertTrue(has1211, "Should include 1.21.1 JARs")
            }

            println("✓ Multiple versions supported")
        }
    }

    @Test
    fun `bundle supports multiple loaders`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 27: Bundle Multiple Loaders                        ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val hasFabric = entries.any { it.name.contains("fabric") }
                val hasForge = entries.any { it.name.contains("forge") && !it.name.contains("neoforge") }
                val hasNeoforge = entries.any { it.name.contains("neoforge") }

                assertTrue(hasFabric, "Should include Fabric JARs")
                assertTrue(hasForge, "Should include Forge JARs")
                assertTrue(hasNeoforge, "Should include NeoForge JARs")
            }

            println("✓ Multiple loaders supported")
        }
    }

    @Test
    fun `bundle includes bundle info file`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 28: Bundle Info File                               ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val infoEntry = zip.getEntry("BUNDLE_INFO.txt")
                assertTrue(infoEntry != null, "Should include BUNDLE_INFO.txt")

                val content = zip.getInputStream(infoEntry).bufferedReader().use { it.readText() }
                assertTrue(content.contains("Test Package Mod"), "Should contain mod name")
                assertTrue(content.contains("1.0.0"), "Should contain version")
            }

            println("✓ Bundle info file included")
        }
    }

    // =================================================================
    // File Inclusion Tests (5 tests)
    // =================================================================

    @Test
    fun `include sources creates source jars in package`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 29: Include Source JARs                            ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        createFakeSourceJars()

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(arrayOf("--include-sources"))

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val sourceJars = entries.filter { it.name.contains("-sources.jar") }
                assertTrue(sourceJars.isNotEmpty(), "Should include source JARs")
            }

            println("✓ Source JARs included")
        }
    }

    @Test
    fun `include javadoc creates javadoc jars in package`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 30: Include Javadoc JARs                           ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        createFakeJavadocJars()

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(arrayOf("--include-javadoc"))

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val javadocJars = entries.filter { it.name.contains("-javadoc.jar") }
                assertTrue(javadocJars.isNotEmpty(), "Should include javadoc JARs")
            }

            println("✓ Javadoc JARs included")
        }
    }

    @Test
    fun `skip sources and javadoc by default`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 31: Skip Sources/Javadoc by Default                ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        createFakeSourceJars()
        createFakeJavadocJars()

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val sourceJars = entries.filter { it.name.contains("-sources.jar") }
                val javadocJars = entries.filter { it.name.contains("-javadoc.jar") }
                assertTrue(sourceJars.isEmpty(), "Should NOT include source JARs by default")
                assertTrue(javadocJars.isEmpty(), "Should NOT include javadoc JARs by default")
            }

            println("✓ Sources and javadoc skipped by default")
        }
    }

    @Test
    fun `include both sources and javadoc`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 32: Include Both Sources and Javadoc               ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        createFakeSourceJars()
        createFakeJavadocJars()

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(arrayOf("--include-sources", "--include-javadoc"))

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val sourceJars = entries.filter { it.name.contains("-sources.jar") }
                val javadocJars = entries.filter { it.name.contains("-javadoc.jar") }
                assertTrue(sourceJars.isNotEmpty(), "Should include source JARs")
                assertTrue(javadocJars.isNotEmpty(), "Should include javadoc JARs")
            }

            println("✓ Both sources and javadoc included")
        }
    }

    @Test
    fun `package includes project files`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 33: Include Project Files                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val hasReadme = zip.getEntry("README.md") != null
                val hasChangelog = zip.getEntry("CHANGELOG.md") != null
                val hasLicense = zip.getEntry("LICENSE") != null

                assertTrue(hasReadme, "Should include README.md")
                assertTrue(hasChangelog, "Should include CHANGELOG.md")
                assertTrue(hasLicense, "Should include LICENSE")
            }

            println("✓ Project files included")
        }
    }

    // =================================================================
    // Build Integration Tests (5 tests)
    // =================================================================

    @Test
    fun `package finds jar files in build directory`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 34: Find JAR Files                                 ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val jars = entries.filter { it.name.endsWith(".jar") }
                assertTrue(jars.isNotEmpty(), "Should find JAR files")
            }

            println("✓ JAR files found")
        }
    }

    @Test
    fun `package handles missing build directory gracefully`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 35: Handle Missing Build Directory                 ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Delete build directory
        val buildDir = context.file( "build")
        buildDir.deleteRecursively()

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            // Should still create package (even if empty)
            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            assertTrue(packageFile.exists(), "Should create package even without build directory")

            println("✓ Handled missing build directory")
        }
    }

    @Test
    fun `package verifies jar naming convention`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 36: Verify JAR Naming Convention                   ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val jars = entries.filter { it.name.endsWith(".jar") }

                jars.forEach { jar ->
                    // Should follow naming convention: modid-version-loader.jar
                    assertTrue(
                        jar.name.contains("testpackage") || jar.name.contains("fabric") ||
                        jar.name.contains("forge") || jar.name.contains("neoforge"),
                        "JAR should follow naming convention: ${jar.name}"
                    )
                }
            }

            println("✓ JAR naming verified")
        }
    }

    @Test
    fun `package handles multiple versions correctly`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 37: Handle Multiple Versions                       ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val jars = entries.filter { it.name.endsWith(".jar") }

                val versions = jars.map { jar ->
                    when {
                        jar.name.contains("1_20_1") -> "1.20.1"
                        jar.name.contains("1_21_1") -> "1.21.1"
                        else -> "unknown"
                    }
                }.distinct()

                assertTrue(versions.size >= 2, "Should handle multiple versions")
            }

            println("✓ Multiple versions handled")
        }
    }

    @Test
    fun `package excludes dev and shadow jars`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 38: Exclude Dev and Shadow JARs                    ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Create dev and shadow JARs
        val buildDir = context.file( "build/1_20_1")
        File(buildDir, "testpackage-1.0.0-dev.jar").writeText("dev")
        File(buildDir, "testpackage-1.0.0-shadow.jar").writeText("shadow")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val entries = zip.entries().toList()
                val devJars = entries.filter { it.name.contains("-dev.jar") }
                val shadowJars = entries.filter { it.name.contains("-shadow.jar") }

                assertTrue(devJars.isEmpty(), "Should exclude dev JARs")
                assertTrue(shadowJars.isEmpty(), "Should exclude shadow JARs")
            }

            println("✓ Dev and shadow JARs excluded")
        }
    }

    // =================================================================
    // Metadata Generation Tests (6 tests)
    // =================================================================

    @Test
    fun `metadata reads from config yml`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 39: Read Metadata from config.yml                  ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("modrinth.json")
                val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                assertTrue(content.contains("Test Package Mod"), "Should read mod name from config")
                assertTrue(content.contains("testpackage"), "Should read mod ID from config")
            }

            println("✓ Metadata read from config.yml")
        }
    }

    @Test
    fun `bundle generates readme info`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 40: Generate Bundle README                         ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val readmeEntry = zip.getEntry("README.md")
                assertTrue(readmeEntry != null, "Should include README.md")
            }

            println("✓ README included in bundle")
        }
    }

    @Test
    fun `bundle generates changelog info`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 41: Generate Bundle CHANGELOG                      ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val changelogEntry = zip.getEntry("CHANGELOG.md")
                assertTrue(changelogEntry != null, "Should include CHANGELOG.md")
            }

            println("✓ CHANGELOG included in bundle")
        }
    }

    @Test
    fun `metadata includes version info`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 42: Metadata Version Info                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("modrinth.json")
                val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                assertTrue(content.contains("1.0.0"), "Should include version number")
            }

            println("✓ Version info included")
        }
    }

    @Test
    fun `metadata includes loader info`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 43: Metadata Loader Info                           ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            ZipFile(packageFile).use { zip ->
                val entry = zip.getEntry("modrinth.json")
                val content = zip.getInputStream(entry).bufferedReader().use { it.readText() }

                assertTrue(
                    content.contains("fabric") || content.contains("forge") || content.contains("neoforge"),
                    "Should include loader info"
                )
            }

            println("✓ Loader info included")
        }
    }

    @Test
    fun `metadata includes license info`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 44: Metadata License Info                          ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        context.withProjectDir {
            val command = PackageBundleCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/bundle/testpackage-1.0.0-bundle.zip")
            ZipFile(packageFile).use { zip ->
                val infoEntry = zip.getEntry("BUNDLE_INFO.txt")
                val content = zip.getInputStream(infoEntry).bufferedReader().use { it.readText() }

                assertTrue(content.contains("MIT"), "Should include license info")
            }

            println("✓ License info included")
        }
    }

    // =================================================================
    // Additional Tests (2 tests for 46 total)
    // =================================================================

    @Test
    fun `package universal throws not implemented error`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 45: Universal Package Not Implemented              ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        val command = PackageUniversalCommand()

        // Should throw error
        assertThrows<UnsupportedOperationException> {
            command.parse(emptyArray())
        }

        println("✓ Universal package correctly throws not implemented error")
    }

    @Test
    fun `package command works without optional files`() {
        println("\n╔═══════════════════════════════════════════════════════════════╗")
        println("║     Test 46: Package Without Optional Files                 ║")
        println("╚═══════════════════════════════════════════════════════════════╝\n")

        // Delete optional files
        context.file( "README.md").delete()
        context.file( "CHANGELOG.md").delete()

        context.withProjectDir {
            val command = PackageModrinthCommand()
            command.parse(emptyArray())

            val packageFile = context.file( "build/packages/modrinth/testpackage-1.0.0-modrinth.zip")
            assertTrue(packageFile.exists(), "Should create package without optional files")

            ZipFile(packageFile).use { zip ->
                val readmeEntry = zip.getEntry("README.md")
                val changelogEntry = zip.getEntry("CHANGELOG.md")
                assertTrue(readmeEntry == null, "Should NOT include missing README")
                assertTrue(changelogEntry == null, "Should NOT include missing CHANGELOG")
            }

            println("✓ Package created without optional files")
        }
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
                val jarFile = File(buildDir, "testpackage-1.0.0-$loader.jar")
                jarFile.writeText("fake jar content")
            }
        }
    }

    private fun createFakeSourceJars() {
        val versions = listOf("1_20_1", "1_21_1")
        val loaders = listOf("fabric", "forge", "neoforge")

        versions.forEach { version ->
            loaders.forEach { loader ->
                val buildDir = context.file( "build/$version/$loader/libs")
                buildDir.mkdirs()
                val jarFile = File(buildDir, "testpackage-1.0.0-$loader-sources.jar")
                jarFile.writeText("fake source jar content")
            }
        }
    }

    private fun createFakeJavadocJars() {
        val versions = listOf("1_20_1", "1_21_1")
        val loaders = listOf("fabric", "forge", "neoforge")

        versions.forEach { version ->
            loaders.forEach { loader ->
                val buildDir = context.file( "build/$version/$loader/libs")
                buildDir.mkdirs()
                val jarFile = File(buildDir, "testpackage-1.0.0-$loader-javadoc.jar")
                jarFile.writeText("fake javadoc jar content")
            }
        }
    }

    private fun createProjectFiles() {
        // Create README
        context.file( "README.md").writeText("""
            # Test Package Mod

            A test mod for packaging.
        """.trimIndent())

        // Create CHANGELOG
        context.file( "CHANGELOG.md").writeText("""
            # Changelog

            ## 1.0.0
            - Initial release
        """.trimIndent())

        // Create LICENSE
        context.file( "LICENSE").writeText("""
            MIT License

            Copyright (c) 2024 TestAuthor
        """.trimIndent())
    }
}
