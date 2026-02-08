pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.minecraftforge.net/")
    }

    // Include build-logic for convention plugins
    includeBuild("build-logic")
}

rootProject.name = "example-mod"

// Dynamically discover version-loader combinations
val versionsDir = file("versions")
val mcVersions = versionsDir.listFiles()
    ?.filter { it.isDirectory && it.name != "shared" }
    ?.map { it.name }
    ?: emptyList()

val loaders = listOf("fabric", "forge", "neoforge")

// Create subproject for each combination
mcVersions.forEach { version ->
    loaders.forEach { loader ->
        val projectName = "$version-$loader"
        include(projectName)

        // Point to build-temp directory
        val projectDir = file("build-temp/$projectName")
        project(":$projectName").projectDir = projectDir

        // Create build.gradle.kts if it doesn't exist
        projectDir.mkdirs()
        val buildFile = File(projectDir, "build.gradle.kts")
        if (!buildFile.exists()) {
            buildFile.writeText("""
                plugins {
                    id("mod.loader")
                }
            """.trimIndent())
        }
    }
}

// Log discovered projects (helpful for debugging)
gradle.settingsEvaluated {
    println("┌─────────────────────────────────────────┐")
    println("│ Multi-Loader Mod Build Configuration   │")
    println("├─────────────────────────────────────────┤")
    println("│ MC Versions: ${mcVersions.size.toString().padEnd(24)}│")
    mcVersions.forEach { v ->
        println("│   - ${v.padEnd(34)}│")
    }
    println("│ Loaders: ${loaders.size.toString().padEnd(28)}│")
    loaders.forEach { l ->
        println("│   - ${l.padEnd(34)}│")
    }
    println("│ Total Projects: ${(mcVersions.size * loaders.size).toString().padEnd(23)}│")
    println("└─────────────────────────────────────────┘")
}
