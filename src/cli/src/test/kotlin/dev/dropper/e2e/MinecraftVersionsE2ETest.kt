package dev.dropper.e2e

import dev.dropper.commands.CreateItemCommand
import dev.dropper.commands.CreateBlockCommand
import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import dev.dropper.util.FileUtil
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import kotlin.test.assertTrue
import java.util.stream.Stream

/**
 * Comprehensive E2E tests for all widely-supported Minecraft versions.
 *
 * Based on 2026 modding ecosystem research:
 * - 1.21.1: Latest stable with NeoForge/Fabric support
 * - 1.20.1: Most extensive mod ecosystem (100,000+ mods on CurseForge)
 * - 1.19.2: Still widely used for major modpacks
 * - 1.18.2: Older but actively supported
 * - 1.16.5: Oldest version with active mod support (Macaw's mods minimum)
 *
 * Sources:
 * - https://www.curseforge.com/minecraft
 * - https://neoforged.net/news/21.0release/
 * - https://www.minecraftforum.net/forums/minecraft-java-edition/discussion/3151716-poll-most-popular-version-for-mods
 * - https://www.curseforge.com/minecraft/mc-mods/macaws-furniture (Macaw's version support)
 */
class MinecraftVersionsE2ETest {

    private lateinit var testDir: File
    private val originalUserDir = System.getProperty("user.dir")

    companion object {
        /**
         * Minecraft version configurations based on loader support in 2026.
         *
         * Forge: Maintenance mode, no official 1.21+ support
         * NeoForge: Active development for 1.20.1+, rapid 1.21.x releases
         * Fabric: Supports all versions from 1.14+
         *
         * Version selection based on:
         * - 1.21.1: Latest with NeoForge/Fabric
         * - 1.20.1: Most popular (100,000+ mods on CurseForge)
         * - 1.19.2: Widely used for modpacks
         * - 1.18.2: Older but actively supported
         * - 1.16.5: Oldest actively maintained (Macaw's mods support this)
         */
        private val VERSION_CONFIGS = mapOf(
            "1.21.1" to LoaderSupport(
                fabric = true,
                neoforge = true,
                forge = false // Forge in maintenance, NeoForge for 1.21+
            ),
            "1.20.1" to LoaderSupport(
                fabric = true,
                neoforge = true,
                forge = true // Last widely-supported Forge version
            ),
            "1.19.2" to LoaderSupport(
                fabric = true,
                neoforge = false, // NeoForge started at 1.20.1
                forge = true
            ),
            "1.18.2" to LoaderSupport(
                fabric = true,
                neoforge = false,
                forge = true
            ),
            "1.16.5" to LoaderSupport(
                fabric = true,
                neoforge = false,
                forge = true // Oldest version with active Macaw's mods support
            )
        )

        @JvmStatic
        fun versionProvider(): Stream<Arguments> {
            return VERSION_CONFIGS.entries.stream()
                .map { (version, support) -> Arguments.of(version, support) }
        }

        @JvmStatic
        fun versionWithLoaderProvider(): Stream<Arguments> {
            return VERSION_CONFIGS.entries.stream()
                .flatMap { (version, support) ->
                    val loaders = mutableListOf<String>()
                    if (support.fabric) loaders.add("fabric")
                    if (support.neoforge) loaders.add("neoforge")
                    if (support.forge) loaders.add("forge")

                    loaders.stream().map { loader ->
                        Arguments.of(version, loader)
                    }
                }
        }
    }

    data class LoaderSupport(
        val fabric: Boolean,
        val neoforge: Boolean,
        val forge: Boolean
    )

    @BeforeEach
    fun setup() {
        testDir = File("build/test-versions/${System.currentTimeMillis()}")
        testDir.mkdirs()
    }

    @AfterEach
    fun cleanup() {
        System.setProperty("user.dir", originalUserDir)
        // Keep artifacts for debugging if needed
        // testDir.deleteRecursively()
    }

    @ParameterizedTest
    @MethodSource("versionProvider")
    fun `test project generation for each Minecraft version`(mcVersion: String, loaderSupport: LoaderSupport) {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Testing Minecraft $mcVersion - Project Generation               ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val projectDir = File(testDir, "test-mc-${mcVersion.replace(".", "_")}")
        val loaders = mutableListOf<String>()
        if (loaderSupport.fabric) loaders.add("fabric")
        if (loaderSupport.neoforge) loaders.add("neoforge")
        if (loaderSupport.forge) loaders.add("forge")

        println("  Supported loaders: ${loaders.joinToString(", ")}")

        val config = ModConfig(
            id = "testmod",
            name = "Test Mod $mcVersion",
            version = "1.0.0",
            description = "Test mod for Minecraft $mcVersion",
            author = "E2E Test",
            license = "MIT",
            minecraftVersions = listOf(mcVersion),
            loaders = loaders
        )

        val generator = ProjectGenerator()
        generator.generate(projectDir, config)

        // Verify project structure
        verifyProjectStructure(projectDir, mcVersion, loaders)

        println("  ✅ Project generation successful for MC $mcVersion")
    }

    @ParameterizedTest
    @MethodSource("versionWithLoaderProvider")
    fun `test item generation for each version and loader combination`(mcVersion: String, loader: String) {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Testing MC $mcVersion - $loader - Item Generation              ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val projectDir = File(testDir, "item-test-${mcVersion.replace(".", "_")}-$loader")

        val config = ModConfig(
            id = "itemtest",
            name = "Item Test",
            version = "1.0.0",
            description = "Item test for MC $mcVersion",
            author = "E2E Test",
            license = "MIT",
            minecraftVersions = listOf(mcVersion),
            loaders = listOf(loader)
        )

        val generator = ProjectGenerator()
        generator.generate(projectDir, config)
        System.setProperty("user.dir", projectDir.absolutePath)

        // Generate test items
        val itemName = "ruby_sword"
        CreateItemCommand().parse(arrayOf(itemName))

        // Verify item files were created
        verifyItemGeneration(projectDir, itemName, mcVersion, loader)

        println("  ✅ Item generation successful for MC $mcVersion ($loader)")
    }

    @ParameterizedTest
    @MethodSource("versionWithLoaderProvider")
    fun `test block generation for each version and loader combination`(mcVersion: String, loader: String) {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Testing MC $mcVersion - $loader - Block Generation             ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val projectDir = File(testDir, "block-test-${mcVersion.replace(".", "_")}-$loader")

        val config = ModConfig(
            id = "blocktest",
            name = "Block Test",
            version = "1.0.0",
            description = "Block test for MC $mcVersion",
            author = "E2E Test",
            license = "MIT",
            minecraftVersions = listOf(mcVersion),
            loaders = listOf(loader)
        )

        val generator = ProjectGenerator()
        generator.generate(projectDir, config)
        System.setProperty("user.dir", projectDir.absolutePath)

        // Generate test block
        val blockName = "ruby_ore"
        CreateBlockCommand().parse(arrayOf(blockName))

        // Verify block files were created
        verifyBlockGeneration(projectDir, blockName, mcVersion, loader)

        println("  ✅ Block generation successful for MC $mcVersion ($loader)")
    }

    @Test
    fun `test multi-version project with all supported versions`() {
        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  Testing Multi-Version Project (All Versions)                    ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val projectDir = File(testDir, "multi-version-all")
        val allVersions = VERSION_CONFIGS.keys.toList()

        val config = ModConfig(
            id = "multiversion",
            name = "Multi Version Mod",
            version = "1.0.0",
            description = "Mod supporting all major versions",
            author = "E2E Test",
            license = "MIT",
            minecraftVersions = allVersions,
            loaders = listOf("fabric") // Use Fabric as it supports all versions
        )

        val generator = ProjectGenerator()
        generator.generate(projectDir, config)
        System.setProperty("user.dir", projectDir.absolutePath)

        // Generate content
        CreateItemCommand().parse(arrayOf("universal_gem"))

        // Verify all version directories exist
        allVersions.forEach { version ->
            val versionDir = File(projectDir, "versions/${version.replace(".", "_")}")
            assertTrue(versionDir.exists(), "Version directory should exist for $version")
            println("  ✓ Version $version directory created")
        }

        // Verify shared assets
        val sharedAssetDir = File(projectDir, "versions/shared/v1")
        assertTrue(sharedAssetDir.exists(), "Shared asset pack should exist")

        println("  ✅ Multi-version project test successful")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_FULL_BUILD", matches = "true")
    fun `test full build for 1_20_1 Fabric`() {
        // This test actually runs Gradle build for the most popular version
        // Only runs when RUN_FULL_BUILD=true (CI or manual testing)

        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  FULL BUILD TEST: MC 1.20.1 Fabric (Most Popular)                ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val projectDir = File(testDir, "full-build-1-20-1")

        val config = ModConfig(
            id = "fullbuild",
            name = "Full Build Test",
            version = "1.0.0",
            description = "Full build test for MC 1.20.1",
            author = "E2E Test",
            license = "MIT",
            minecraftVersions = listOf("1.20.1"),
            loaders = listOf("fabric")
        )

        val generator = ProjectGenerator()
        generator.generate(projectDir, config)
        System.setProperty("user.dir", projectDir.absolutePath)

        // Generate some content
        CreateItemCommand().parse(arrayOf("test_item"))
        CreateBlockCommand().parse(arrayOf("test_block"))

        // Copy gradle wrapper
        copyGradleWrapper(projectDir)

        // Run build
        println("\n  Building project (this may take 5-10 minutes)...")
        runGradleBuild(projectDir)

        // Verify JAR was created
        val jarFile = File(projectDir, "build/1_20_1/fabric.jar")
        assertTrue(jarFile.exists(), "JAR should be created")
        assertTrue(jarFile.length() > 0, "JAR should not be empty")

        println("  ✅ Full build test successful - JAR created: ${jarFile.length() / 1024}KB")
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "RUN_FULL_BUILD", matches = "true")
    fun `test full build for 1_21_1 NeoForge`() {
        // Test latest version with NeoForge

        println("\n╔══════════════════════════════════════════════════════════════════╗")
        println("║  FULL BUILD TEST: MC 1.21.1 NeoForge (Latest)                    ")
        println("╚══════════════════════════════════════════════════════════════════╝")

        val projectDir = File(testDir, "full-build-1-21-1")

        val config = ModConfig(
            id = "neoforgefull",
            name = "NeoForge Full Build",
            version = "1.0.0",
            description = "Full build test for MC 1.21.1 NeoForge",
            author = "E2E Test",
            license = "MIT",
            minecraftVersions = listOf("1.21.1"),
            loaders = listOf("neoforge")
        )

        val generator = ProjectGenerator()
        generator.generate(projectDir, config)
        System.setProperty("user.dir", projectDir.absolutePath)

        CreateItemCommand().parse(arrayOf("neoforge_gem"))

        copyGradleWrapper(projectDir)

        println("\n  Building NeoForge project (this may take 5-10 minutes)...")
        runGradleBuild(projectDir)

        val jarFile = File(projectDir, "build/1_21_1/neoforge.jar")
        assertTrue(jarFile.exists(), "NeoForge JAR should be created")
        assertTrue(jarFile.length() > 0, "JAR should not be empty")

        println("  ✅ NeoForge full build test successful - JAR created: ${jarFile.length() / 1024}KB")
    }

    private fun verifyProjectStructure(projectDir: File, mcVersion: String, loaders: List<String>) {
        // Essential files
        assertTrue(File(projectDir, "config.yml").exists(), "config.yml should exist")
        assertTrue(File(projectDir, "build.gradle.kts").exists(), "build.gradle.kts should exist")
        assertTrue(File(projectDir, "settings.gradle.kts").exists(), "settings.gradle.kts should exist")
        assertTrue(File(projectDir, "AGENTS.md").exists(), "AGENTS.md should exist")
        println("  ✓ Essential files present")

        // Version directory
        val versionDir = File(projectDir, "versions/${mcVersion.replace(".", "_")}")
        assertTrue(versionDir.exists(), "Version directory should exist for $mcVersion")
        println("  ✓ Version directory: ${mcVersion.replace(".", "_")}")

        // Version config
        val versionConfig = File(versionDir, "config.yml")
        assertTrue(versionConfig.exists(), "Version config should exist")
        val versionConfigContent = versionConfig.readText()
        assertTrue(
            versionConfigContent.contains("minecraft_version: \"$mcVersion\""),
            "Version config should specify MC version"
        )
        println("  ✓ Version config valid")

        // Loader directories
        loaders.forEach { loader ->
            val loaderDir = File(versionDir, loader)
            assertTrue(loaderDir.exists(), "Loader directory should exist: $loader")
            println("  ✓ Loader directory: $loader")
        }

        // Shared asset pack
        val sharedAssetPack = File(projectDir, "versions/shared/v1")
        assertTrue(sharedAssetPack.exists(), "Shared asset pack should exist")
        println("  ✓ Shared asset pack present")
    }

    private fun verifyItemGeneration(projectDir: File, itemName: String, @Suppress("UNUSED_PARAMETER") mcVersion: String, @Suppress("UNUSED_PARAMETER") loader: String) {
        val modId = "itemtest"

        // Check Java class exists
        val itemClass = File(projectDir, "shared/common/src/main/java/com/$modId/items/${itemName.toCamelCase()}.java")
        assertTrue(itemClass.exists(), "Item class should exist: $itemName")

        val itemContent = itemClass.readText()
        assertTrue(itemContent.contains("class ${itemName.toCamelCase()}"), "Should have correct class name")
        println("  ✓ Item class: ${itemName.toCamelCase()}.java")

        // Check model JSON
        val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/item/$itemName.json")
        assertTrue(modelFile.exists(), "Item model should exist")
        println("  ✓ Item model: $itemName.json")

        // Check texture placeholder
        val textureFile = File(projectDir, "versions/shared/v1/assets/$modId/textures/item/$itemName.png")
        assertTrue(textureFile.exists(), "Texture placeholder should exist")
        println("  ✓ Texture: $itemName.png")
    }

    private fun verifyBlockGeneration(projectDir: File, blockName: String, @Suppress("UNUSED_PARAMETER") mcVersion: String, @Suppress("UNUSED_PARAMETER") loader: String) {
        val modId = "blocktest"

        // Check Java class
        val blockClass = File(projectDir, "shared/common/src/main/java/com/$modId/blocks/${blockName.toCamelCase()}.java")
        assertTrue(blockClass.exists(), "Block class should exist: $blockName")
        println("  ✓ Block class: ${blockName.toCamelCase()}.java")

        // Check blockstate JSON
        val blockstateFile = File(projectDir, "versions/shared/v1/assets/$modId/blockstates/$blockName.json")
        assertTrue(blockstateFile.exists(), "Blockstate should exist")
        println("  ✓ Blockstate: $blockName.json")

        // Check block model
        val blockModelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/block/$blockName.json")
        assertTrue(blockModelFile.exists(), "Block model should exist")
        println("  ✓ Block model: $blockName.json")

        // Check item model
        val itemModelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/item/$blockName.json")
        assertTrue(itemModelFile.exists(), "Item model should exist")
        println("  ✓ Item model: $blockName.json")
    }

    private fun String.toCamelCase(): String {
        return split("_")
            .joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun copyGradleWrapper(projectDir: File) {
        val rootDir = File(".").absoluteFile.parentFile.parentFile.parentFile
        val gradleDir = File(rootDir, "gradle")
        val gradlewFile = File(rootDir, "gradlew")
        val gradlewBatFile = File(rootDir, "gradlew.bat")

        if (gradleDir.exists()) {
            FileUtil.copyDirectory(gradleDir, File(projectDir, "gradle"))
        }
        if (gradlewFile.exists()) {
            gradlewFile.copyTo(File(projectDir, "gradlew"), overwrite = true)
            if (!System.getProperty("os.name").lowercase().contains("windows")) {
                Runtime.getRuntime().exec(arrayOf("chmod", "+x", File(projectDir, "gradlew").absolutePath)).waitFor()
            }
        }
        if (gradlewBatFile.exists()) {
            gradlewBatFile.copyTo(File(projectDir, "gradlew.bat"), overwrite = true)
        }
    }

    private fun runGradleBuild(projectDir: File) {
        val gradlewCmd = if (System.getProperty("os.name").lowercase().contains("windows")) {
            File(projectDir, "gradlew.bat").absolutePath
        } else {
            File(projectDir, "gradlew").absolutePath
        }

        if (!System.getProperty("os.name").lowercase().contains("windows")) {
            Runtime.getRuntime().exec(arrayOf("chmod", "+x", gradlewCmd)).waitFor()
        }

        val process = ProcessBuilder(gradlewCmd, "build", "--no-daemon", "--console=plain")
            .directory(projectDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val errors = process.errorStream.bufferedReader().readText()

        val exitCode = process.waitFor()

        if (exitCode != 0) {
            println("=== Gradle Output ===")
            println(output)
            println("=== Gradle Errors ===")
            println(errors)
            throw AssertionError("Gradle build failed with exit code $exitCode")
        }

        println("  ✓ Gradle build completed successfully")
    }
}
