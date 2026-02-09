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

tasks.register("buildAll") {
    group = "build"
    description = "Builds all version-loader combinations"
    dependsOn(subprojects.map { it.tasks.named("build") })
}
