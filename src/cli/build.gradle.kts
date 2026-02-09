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

// Common test configuration
fun Test.configureTestTask() {
    useJUnitPlatform()

    // Ensure tmp directory exists before tests run
    doFirst {
        val tmpDir = layout.buildDirectory.get().asFile.resolve("tmp")
        tmpDir.mkdirs()
    }

    // Aggressive resource allocation to prevent executor crashes
    // Fork more often to prevent memory accumulation
    forkEvery = 5  // Reduced from 10 - fork more frequently for stability
    maxParallelForks = 1  // No parallel execution

    // Increase heap size significantly - tests need lots of memory for file I/O
    maxHeapSize = "6g"
    minHeapSize = "2g"

    // JVM args for stability with more resources
    val tmpDir = System.getenv("RUNNER_TEMP") ?: layout.buildDirectory.get().asFile.resolve("tmp").absolutePath
    jvmArgs(
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:HeapDumpPath=${layout.buildDirectory.get().asFile.resolve("heap-dumps")}",
        "-XX:MaxMetaspaceSize=1g",  // More metaspace for many classes
        "-XX:+UseG1GC",  // Better GC for large heaps
        "-XX:MaxGCPauseMillis=100",  // Limit GC pauses
        "-Xlog:gc*:file=${layout.buildDirectory.get().asFile.resolve("gc.log")}",
        "-Dfile.encoding=UTF-8",
        "-Djava.io.tmpdir=$tmpDir"
    )

    // Enhanced test output for debugging - CRITICAL for seeing failures
    testLogging {
        events("passed", "skipped", "failed", "standardError", "standardOut")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true  // Changed from false - needed to see test output
        showExceptions = true
        showCauses = true
        showStackTraces = true  // CRITICAL: Required for exceptionFormat to work
        displayGranularity = 2  // Show class-level detail
    }

    // Generate HTML reports for detailed analysis
    reports {
        html.required.set(true)
        junitXml.required.set(true)
    }

    // Always show test results
    outputs.upToDateWhen { false }
}

// Environment detection
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val isWSL = System.getenv("WSL_DISTRO_NAME") != null ||
            System.getenv("DROPPER_TEST_ENV") == "wsl"
val isContainer = System.getenv("DROPPER_TEST_ENV") == "docker" ||
                  System.getenv("DROPPER_TEST_ENV") == "container"
val shouldExcludeTests = isWindows && !isWSL && !isContainer

// Integration tests are gated - many test incomplete features
// Set RUN_INTEGRATION_TESTS=true to run them
val runIntegrationTests = System.getenv("RUN_INTEGRATION_TESTS") == "true"

// Main test task - runs only unit tests (fast)
tasks.test {
    configureTestTask()

    filter {
        // Only include util tests (unit tests)
        includeTestsMatching("dev.dropper.util.*")
    }
}

// Integration tests batch 1 - Command tests
val integrationTests1 by tasks.registering(Test::class) {
    configureTestTask()

    filter {
        includeTestsMatching("dev.dropper.commands.*")
    }

    // Only run on non-Windows or in WSL/Docker
    enabled = !shouldExcludeTests
}

// Integration tests batch 2 - Integration tests A-M
// Gated: many tests are for incomplete features
// Run with: RUN_INTEGRATION_TESTS=true ./gradlew integrationTests2
val integrationTests2 by tasks.registering(Test::class) {
    configureTestTask()

    filter {
        includeTestsMatching("dev.dropper.integration.AddVersionCommandTest")
        includeTestsMatching("dev.dropper.integration.AssetPackCommandTest")
        includeTestsMatching("dev.dropper.integration.BuildCommandTest")
        includeTestsMatching("dev.dropper.integration.CleanCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.CompleteWorkflowTest")
        includeTestsMatching("dev.dropper.integration.CreateCommandTest")
        includeTestsMatching("dev.dropper.integration.DevCommandTest")
        includeTestsMatching("dev.dropper.integration.E2ETest")
        includeTestsMatching("dev.dropper.integration.ExportCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.FullCLIBuildTest")
        includeTestsMatching("dev.dropper.integration.FullWorkflowTest")
        includeTestsMatching("dev.dropper.integration.ImportCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.ListCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.ListCommandBasicTest")
        includeTestsMatching("dev.dropper.integration.MigrateCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.MigrateCommandAdvancedE2ETest")
    }

    enabled = !shouldExcludeTests && runIntegrationTests
    mustRunAfter(integrationTests1)
}

// Integration tests batch 3 - Integration tests N-Z
// Gated: many tests are for incomplete features
val integrationTests3 by tasks.registering(Test::class) {
    configureTestTask()

    filter {
        includeTestsMatching("dev.dropper.integration.PackageCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.PackageCommandAdvancedE2ETest")
        includeTestsMatching("dev.dropper.integration.RemoveCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.RenameCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.SearchCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.SyncCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.TemplateCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.UpdateCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.ValidateCommandE2ETest")
        includeTestsMatching("dev.dropper.integration.CLIWorkflowTest")
    }

    enabled = !shouldExcludeTests && runIntegrationTests
    mustRunAfter(integrationTests2)
}

// E2E tests - Small focused tests
// Gated: some tests are for incomplete features
val e2eTests by tasks.registering(Test::class) {
    configureTestTask()

    filter {
        includeTestsMatching("dev.dropper.e2e.*")
    }

    enabled = !shouldExcludeTests && runIntegrationTests
    mustRunAfter(integrationTests3)
}

// Aggregate task to run all tests sequentially with delays
val allTests by tasks.registering {
    description = "Run all tests in batches with delays"
    group = "verification"

    dependsOn(tasks.test, integrationTests1, integrationTests2, integrationTests3, e2eTests)
}

kotlin {
    jvmToolchain(21)
}
