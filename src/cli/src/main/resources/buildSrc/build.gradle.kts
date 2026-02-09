plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.snakeyaml)
    implementation(libs.jackson.databind)
}

kotlin {
    jvmToolchain(17)
}
