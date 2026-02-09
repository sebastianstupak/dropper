package dev.dropper.util

import java.io.File

object PublishHelper {
    fun validateProject(projectDir: File): Boolean {
        return File(projectDir, "config.yml").exists()
    }

    fun collectJars(buildDir: File): List<File> {
        return emptyList()
    }
}
