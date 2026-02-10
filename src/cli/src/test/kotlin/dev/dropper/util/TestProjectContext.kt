package dev.dropper.util

import dev.dropper.config.ModConfig
import dev.dropper.generator.ProjectGenerator
import java.io.File

/**
 * Test utility to create and manage test project contexts.
 * Sets user.dir to the project directory so commands using the default
 * projectDir (System.getProperty("user.dir")) find the correct project.
 */
class TestProjectContext(val projectDir: File) {
    private val originalUserDir = System.getProperty("user.dir")

    /**
     * Execute a block of code with the project directory as the current working directory
     * This temporarily modifies user.dir but restores it immediately after
     */
    fun <T> withProjectDir(block: () -> T): T {
        val currentUserDir = System.getProperty("user.dir")
        return try {
            System.setProperty("user.dir", projectDir.absolutePath)
            block()
        } finally {
            System.setProperty("user.dir", currentUserDir)
        }
    }

    /**
     * Activate this context by setting user.dir to the project directory.
     * Commands that default to System.getProperty("user.dir") will then
     * resolve to this project directory.
     */
    fun activate() {
        System.setProperty("user.dir", projectDir.absolutePath)
    }

    /**
     * Create a test project with the given configuration
     */
    fun createProject(config: ModConfig) {
        ProjectGenerator().generate(projectDir, config)
        activate()
    }

    /**
     * Create a default test project
     */
    fun createDefaultProject(
        id: String = "testmod",
        name: String = "Test Mod",
        minecraftVersions: List<String> = listOf("1.20.1"),
        loaders: List<String> = listOf("fabric")
    ) {
        val config = ModConfig(
            id = id,
            name = name,
            version = "1.0.0",
            description = "Test project",
            author = "Test",
            license = "MIT",
            minecraftVersions = minecraftVersions,
            loaders = loaders
        )
        createProject(config)
    }

    /**
     * Get a file relative to the project directory
     */
    fun file(path: String): File = File(projectDir, path)

    /**
     * Clean up the project directory
     */
    fun cleanup() {
        if (projectDir.exists()) {
            projectDir.deleteRecursively()
        }
        System.setProperty("user.dir", originalUserDir)
    }

    companion object {
        /**
         * Create a temporary test project context
         */
        fun create(name: String = "test-project"): TestProjectContext {
            val projectDir = File("build/test-projects/${System.currentTimeMillis()}/$name")
            projectDir.mkdirs()
            return TestProjectContext(projectDir)
        }
    }
}
