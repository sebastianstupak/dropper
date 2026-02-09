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

    // Conservative settings to prevent executor crashes
    forkEvery = 1  // Fork for each test class
    maxParallelForks = 1  // No parallel execution

    // Reduce heap size to prevent OOM
    maxHeapSize = "2g"
    minHeapSize = "512m"

    // JVM args for stability
    jvmArgs(
        "-XX:+HeapDumpOnOutOfMemoryError",
        "-XX:MaxMetaspaceSize=256m",
        "-Dfile.encoding=UTF-8",
        "-Djava.io.tmpdir=${layout.buildDirectory.get().asFile}/tmp"
    )

    // Proper test output
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
        showExceptions = true
        showCauses = true
    }
}

// Environment detection
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val isWSL = System.getenv("WSL_DISTRO_NAME") != null ||
            System.getenv("DROPPER_TEST_ENV") == "wsl"
val isContainer = System.getenv("DROPPER_TEST_ENV") == "docker" ||
                  System.getenv("DROPPER_TEST_ENV") == "container"
val shouldExcludeTests = isWindows && !isWSL && !isContainer

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

    enabled = !shouldExcludeTests

    // Add delay after previous batch
    mustRunAfter(integrationTests1)
    doFirst {
        println("Waiting 5 seconds before starting integration tests batch 2...")
        Thread.sleep(5000)
    }
}

// Integration tests batch 3 - Integration tests N-Z
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

    enabled = !shouldExcludeTests

    mustRunAfter(integrationTests2)
    doFirst {
        println("Waiting 5 seconds before starting integration tests batch 3...")
        Thread.sleep(5000)
    }
}

// E2E tests - Small focused tests
val e2eTests by tasks.registering(Test::class) {
    configureTestTask()

    filter {
        includeTestsMatching("dev.dropper.e2e.*")
    }

    enabled = !shouldExcludeTests

    mustRunAfter(integrationTests3)
    doFirst {
        println("Waiting 5 seconds before starting E2E tests...")
        Thread.sleep(5000)
    }
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
