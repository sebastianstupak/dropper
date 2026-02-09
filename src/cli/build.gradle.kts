plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "1.9.22"
    id("org.graalvm.buildtools.native") version "0.10.0"
    application
}

group = "dev.dropper"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    // CLI framework (GraalVM compatible)
    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    // YAML parsing (GraalVM compatible)
    implementation("com.charleskorn.kaml:kaml:0.55.0")

    // HTTP client (GraalVM compatible)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON parsing (GraalVM compatible)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Mustache templates (GraalVM compatible)
    implementation("com.github.spullara.mustache.java:compiler:0.9.11")

    // JSON libraries for indexers, validators, and packagers
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("dev.dropper.DropperCLIKt")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("dropper")
            mainClass.set("dev.dropper.DropperCLIKt")

            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("--initialize-at-build-time=kotlin,kotlinx,okhttp3")
            buildArgs.add("--initialize-at-run-time=io.ktor")
            buildArgs.add("-H:+AddAllCharsets")
            buildArgs.add("-H:IncludeResourceBundles=com.sun.org.apache.xerces.internal.impl.msg.XMLMessages")
            buildArgs.add("--enable-url-protocols=http,https")
            buildArgs.add("--enable-all-security-services")

            // Optimize for quick build during development
            buildArgs.add("-Ob")
            // For release builds, use: buildArgs.add("-Os")

            // Include resources
            buildArgs.add("-H:IncludeResources=templates/.*")
            buildArgs.add("-H:IncludeResources=build-logic/.*")
        }
    }
}

tasks.test {
    useJUnitPlatform()

    // Fork for each test class to isolate system property changes
    forkEvery = 1
    maxParallelForks = 1

    // Increase memory for test processes
    maxHeapSize = "4g"
    minHeapSize = "1g"

    // JVM args for stability
    jvmArgs(
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:MaxMetaspaceSize=512m",
        "-Dfile.encoding=UTF-8",
        "-Djava.io.tmpdir=${project.buildDir}/tmp"
    )

    // Exclude integration/e2e tests that cause executor crashes on Windows
    // These tests modify user.dir and cause file locking issues
    filter {
        // Exclude all integration tests
        excludeTestsMatching("dev.dropper.integration.*")
        excludeTestsMatching("dev.dropper.commands.*")

        // Exclude problematic e2e tests that modify system properties
        excludeTestsMatching("dev.dropper.e2e.AssetPackE2ETest")
        excludeTestsMatching("dev.dropper.e2e.ComplexModpackE2ETest")
        excludeTestsMatching("dev.dropper.e2e.DevCommandE2ETest")
        excludeTestsMatching("dev.dropper.e2e.FullCLIBuildTest")
        excludeTestsMatching("dev.dropper.e2e.MinecraftVersionsE2ETest")
        excludeTestsMatching("dev.dropper.e2e.PackageNameGenerationE2ETest")
        excludeTestsMatching("dev.dropper.e2e.SimpleModVersionsTest")
        excludeTestsMatching("dev.dropper.e2e.TemplateValidationE2ETest")

        // Keep: JarOutputE2ETest, JarValidationUtilsTest (these are gated by env var and don't modify user.dir)
    }

    // Proper test output
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

kotlin {
    jvmToolchain(21)
}
