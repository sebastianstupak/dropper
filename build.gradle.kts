plugins {
    kotlin("jvm") version "1.9.22" apply false
}

group = "dev.dropper"
version = "1.0.0"

tasks.register("clean") {
    delete(rootProject.layout.buildDirectory)
}
