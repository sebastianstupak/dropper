package dev.dropper.importers

import java.io.File

/**
 * Analyzes existing mod projects to extract metadata
 */
class ProjectAnalyzer {

    /**
     * Detect the mod loader from project files
     */
    fun detectLoader(projectDir: File): String? {
        return when {
            File(projectDir, "fabric.mod.json").exists() -> "fabric"
            File(projectDir, "src/main/resources/fabric.mod.json").exists() -> "fabric"
            File(projectDir, "META-INF/mods.toml").exists() -> "forge"
            File(projectDir, "src/main/resources/META-INF/mods.toml").exists() -> "forge"
            File(projectDir, "META-INF/neoforge.mods.toml").exists() -> "neoforge"
            File(projectDir, "src/main/resources/META-INF/neoforge.mods.toml").exists() -> "neoforge"
            else -> null
        }
    }

    /**
     * Find metadata file for the detected loader
     */
    fun findMetadataFile(projectDir: File, loader: String): File? {
        val candidates = when (loader) {
            "fabric" -> listOf(
                "fabric.mod.json",
                "src/main/resources/fabric.mod.json"
            )
            "forge" -> listOf(
                "META-INF/mods.toml",
                "src/main/resources/META-INF/mods.toml"
            )
            "neoforge" -> listOf(
                "META-INF/neoforge.mods.toml",
                "src/main/resources/META-INF/neoforge.mods.toml"
            )
            else -> emptyList()
        }

        return candidates.map { File(projectDir, it) }.firstOrNull { it.exists() }
    }

    /**
     * Find all Java source files
     */
    fun findSourceFiles(projectDir: File): List<File> {
        val srcDir = File(projectDir, "src/main/java")
        if (!srcDir.exists()) return emptyList()

        return srcDir.walkTopDown()
            .filter { it.isFile && it.extension == "java" }
            .toList()
    }

    /**
     * Extract package name from Java file
     */
    fun extractPackageName(javaFile: File): String? {
        val content = javaFile.readText()
        val packageRegex = Regex("^package\\s+([a-zA-Z0-9_.]+);", RegexOption.MULTILINE)
        return packageRegex.find(content)?.groupValues?.get(1)
    }

    /**
     * Extract class name from Java file
     */
    fun extractClassName(javaFile: File): String? {
        val content = javaFile.readText()
        val classRegex = Regex("public\\s+class\\s+([A-Za-z0-9_]+)")
        return classRegex.find(content)?.groupValues?.get(1)
    }

    /**
     * Find asset directories
     */
    fun findAssetDirectories(projectDir: File): List<File> {
        val resourcesDir = File(projectDir, "src/main/resources")
        if (!resourcesDir.exists()) return emptyList()

        val assetsDir = File(resourcesDir, "assets")
        if (!assetsDir.exists()) return emptyList()

        return assetsDir.listFiles()?.filter { it.isDirectory }?.toList() ?: emptyList()
    }

    /**
     * Find data directories
     */
    fun findDataDirectories(projectDir: File): List<File> {
        val resourcesDir = File(projectDir, "src/main/resources")
        if (!resourcesDir.exists()) return emptyList()

        val dataDir = File(resourcesDir, "data")
        if (!dataDir.exists()) return emptyList()

        return dataDir.listFiles()?.filter { it.isDirectory }?.toList() ?: emptyList()
    }

    /**
     * Detect base package from source files
     */
    fun detectBasePackage(projectDir: File): String? {
        val sourceFiles = findSourceFiles(projectDir)
        if (sourceFiles.isEmpty()) return null

        // Find the most common base package
        val packages = sourceFiles.mapNotNull { extractPackageName(it) }
        if (packages.isEmpty()) return null

        // Find shortest package (likely the base)
        return packages.minByOrNull { it.length }
    }

    /**
     * Detect mod ID from metadata or package structure
     */
    fun detectModId(projectDir: File, loader: String?): String? {
        if (loader != null) {
            val metadataFile = findMetadataFile(projectDir, loader)
            if (metadataFile != null) {
                return extractModIdFromMetadata(metadataFile, loader)
            }
        }

        // Fallback: use base package
        val basePackage = detectBasePackage(projectDir)
        return basePackage?.split(".")?.lastOrNull()
    }

    private fun extractModIdFromMetadata(file: File, loader: String): String? {
        val content = file.readText()
        return when (loader) {
            "fabric" -> {
                // Parse JSON
                val idRegex = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
                idRegex.find(content)?.groupValues?.get(1)
            }
            "forge", "neoforge" -> {
                // Parse TOML
                val idRegex = Regex("^modId\\s*=\\s*\"([^\"]+)\"", RegexOption.MULTILINE)
                idRegex.find(content)?.groupValues?.get(1)
            }
            else -> null
        }
    }
}
