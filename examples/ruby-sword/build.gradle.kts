plugins {
    id("base")
}

// Define repositories for all subprojects
subprojects {
    repositories {
        mavenCentral()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases/")
        maven("https://maven.minecraftforge.net/")
        maven("https://libraries.minecraft.net/")
    }
}

// Global clean task
tasks.clean {
    delete(file("build"))
    delete(file("build-temp"))
}

// Helpful tasks
tasks.register("listProjects") {
    group = "help"
    description = "Lists all configured version-loader subprojects"

    doLast {
        println("\n=== Configured Projects ===")
        subprojects.forEach { proj ->
            println("  - ${proj.name}")
        }
        println("\nTotal: ${subprojects.size} projects")
    }
}

tasks.register("buildAll") {
    group = "build"
    description = "Builds all version-loader combinations"
    dependsOn(subprojects.map { it.tasks.named("build") })
}
