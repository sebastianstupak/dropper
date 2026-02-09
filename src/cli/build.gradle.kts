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
        "-Djava.io.tmpdir=${layout.buildDirectory.get().asFile}/tmp"
    )

    // Exclude tests based on system property
    // On Windows, exclude problematic tests that haven't been migrated yet.
    // On Linux/Mac, run all tests.
    // Tests using TestProjectContext are safe on all platforms.
    val isWindows = System.getProperty("os.name").lowercase().contains("windows")

    if (isWindows) {
        // On Windows, exclude integration/e2e tests that still use user.dir modification
        // NOTE: Tests migrated to TestProjectContext may still crash on Windows due to test executor issues
        filter {
            // Exclude integration tests that haven't been migrated yet
            excludeTestsMatching("dev.dropper.integration.AddVersionCommandTest")
            excludeTestsMatching("dev.dropper.integration.AssetPackCommandTest")
            excludeTestsMatching("dev.dropper.integration.BuildCommandTest")
            excludeTestsMatching("dev.dropper.integration.CleanCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.CompleteWorkflowTest")
            excludeTestsMatching("dev.dropper.integration.CreateCommandTest")
            excludeTestsMatching("dev.dropper.integration.DevCommandTest")
            excludeTestsMatching("dev.dropper.integration.E2ETest")
            excludeTestsMatching("dev.dropper.integration.ExportCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.FullCLIBuildTest")
            excludeTestsMatching("dev.dropper.integration.FullWorkflowTest")
            excludeTestsMatching("dev.dropper.integration.ImportCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.ListCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.ListCommandBasicTest")
            excludeTestsMatching("dev.dropper.integration.MigrateCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.MigrateCommandAdvancedE2ETest")
            excludeTestsMatching("dev.dropper.integration.PackageCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.PackageCommandAdvancedE2ETest")
            excludeTestsMatching("dev.dropper.integration.RemoveCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.RenameCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.SearchCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.SyncCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.TemplateCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.UpdateCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.ValidateCommandE2ETest")
            excludeTestsMatching("dev.dropper.integration.CLIWorkflowTest")

            // Exclude command tests
            excludeTestsMatching("dev.dropper.commands.*")

            // Exclude e2e tests
            excludeTestsMatching("dev.dropper.e2e.AssetPackE2ETest")
            excludeTestsMatching("dev.dropper.e2e.ComplexModpackE2ETest")
            excludeTestsMatching("dev.dropper.e2e.DevCommandE2ETest")
            excludeTestsMatching("dev.dropper.e2e.FullCLIBuildTest")
            excludeTestsMatching("dev.dropper.e2e.MinecraftVersionsE2ETest")
            excludeTestsMatching("dev.dropper.e2e.PackageNameGenerationE2ETest")
            excludeTestsMatching("dev.dropper.e2e.SimpleModVersionsTest")
            excludeTestsMatching("dev.dropper.e2e.TemplateValidationE2ETest")
        }
    }

    // Proper test output
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
        showExceptions = true
        showCauses = true
    }
}

kotlin {
    jvmToolchain(21)
}
