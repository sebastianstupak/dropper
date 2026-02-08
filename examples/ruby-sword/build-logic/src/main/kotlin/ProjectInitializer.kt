import config.ConfigLoader
import org.gradle.api.Project
import java.io.File

/**
 * Initializes build-temp subproject directories with build.gradle.kts
 * that applies the mod.loader convention plugin.
 */
object ProjectInitializer {

    fun initializeSubproject(project: Project) {
        val projectDir = project.projectDir
        projectDir.mkdirs()

        // Create build.gradle.kts that applies convention plugin
        val buildFile = File(projectDir, "build.gradle.kts")
        if (!buildFile.exists()) {
            buildFile.writeText("""
                plugins {
                    id("mod.loader")
                }
            """.trimIndent())
        }

        project.logger.debug("Initialized subproject: ${project.name} at ${projectDir.absolutePath}")
    }
}
