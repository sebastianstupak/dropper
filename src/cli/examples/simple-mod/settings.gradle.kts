pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.minecraftforge.net/")
    }

    includeBuild("build-logic")
}

rootProject.name = "simplemod"

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

        val projectDir = file("build-temp/$projectName")
        project(":$projectName").projectDir = projectDir

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
