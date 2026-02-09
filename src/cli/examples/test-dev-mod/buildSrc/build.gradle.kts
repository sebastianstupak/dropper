plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
    maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
}

dependencies {
    // YAML and JSON parsing
    implementation("org.yaml:snakeyaml:2.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")

    // Mod loader Gradle plugins
    // Fabric Loom - support for MC 1.20.x and 1.21.x
    implementation("net.fabricmc:fabric-loom:1.6-SNAPSHOT")

    // ForgeGradle 6.x - compatible with Gradle 8.6+
    implementation("net.minecraftforge.gradle:ForgeGradle:6.0.+")

    // Note: NeoGradle requires Gradle 9.1+ for newer versions
    // Users can add manually if needed
}