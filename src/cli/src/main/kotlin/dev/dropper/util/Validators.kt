package dev.dropper.util

import java.io.File
import java.nio.file.Path
import kotlin.io.path.pathString

/**
 * Comprehensive validation utilities for all Dropper CLI inputs
 */
object Validators {

    /**
     * Validation result with error message and suggestion
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val suggestion: String? = null
    ) {
        companion object {
            fun success() = ValidationResult(true)
            fun error(message: String, suggestion: String? = null) =
                ValidationResult(false, message, suggestion)
        }
    }

    // ========== Name Validation ==========

    /**
     * Validate mod ID (allows hyphens)
     */
    fun validateModId(modId: String): ValidationResult {
        if (modId.isEmpty()) {
            return ValidationResult.error(
                "Mod ID cannot be empty",
                "Example: mymod or my-awesome-mod"
            )
        }

        if (modId.length > 64) {
            return ValidationResult.error(
                "Mod ID too long (${modId.length} chars, max 64)"
            )
        }

        val pattern = Regex("^[a-z][a-z0-9_-]*[a-z0-9]$")
        val singleChar = Regex("^[a-z]$")

        if (!pattern.matches(modId) && !singleChar.matches(modId)) {
            val suggestion = sanitizeModId(modId)
            return ValidationResult.error(
                "Invalid mod ID: '$modId'",
                "Use lowercase letters, numbers, hyphens, underscores. Example: '$suggestion'"
            )
        }

        // Check for reserved mod IDs
        val reserved = setOf("minecraft", "forge", "fabric", "neoforge", "java", "mcp")
        if (modId.lowercase() in reserved) {
            return ValidationResult.error(
                "'$modId' is a reserved name",
                "Try '${modId}_mod' or 'custom_$modId'"
            )
        }

        return ValidationResult.success()
    }

    /**
     * Validate component name (items, blocks, entities, etc.)
     */
    fun validateComponentName(name: String, type: String = "name"): ValidationResult {
        if (name.isEmpty()) {
            return ValidationResult.error(
                "$type cannot be empty",
                "Example: ruby_sword, diamond_ore"
            )
        }

        if (name.length > 64) {
            return ValidationResult.error(
                "$type too long (${name.length} chars, max 64)",
                "Use a shorter name"
            )
        }

        // Check for spaces
        if (name.contains(' ')) {
            val suggestion = sanitizeComponentName(name)
            return ValidationResult.error(
                "$type cannot contain spaces: '$name'",
                "Use snake_case: '$suggestion'"
            )
        }

        // Must be lowercase
        if (name != name.lowercase()) {
            val suggestion = sanitizeComponentName(name)
            return ValidationResult.error(
                "$type must be lowercase: '$name'",
                "Use: '$suggestion'"
            )
        }

        // Valid pattern
        val pattern = Regex("^[a-z][a-z0-9_]*[a-z0-9]$")
        val singleChar = Regex("^[a-z]$")

        if (!pattern.matches(name) && !singleChar.matches(name)) {
            val suggestion = sanitizeComponentName(name)
            return ValidationResult.error(
                "Invalid $type: '$name'",
                "Use lowercase letters, numbers, underscores. Example: '$suggestion'"
            )
        }

        // Check reserved keywords
        val reserved = setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while", "item", "block", "entity", "minecraft"
        )

        if (name.lowercase() in reserved) {
            return ValidationResult.error(
                "'$name' is a reserved keyword",
                "Try '${name}_item' or 'custom_$name'"
            )
        }

        return ValidationResult.success()
    }

    /**
     * Validate Java package name
     */
    fun validatePackageName(packageName: String): ValidationResult {
        if (packageName.isEmpty()) {
            return ValidationResult.error(
                "Package name cannot be empty",
                "Example: com.example.mymod"
            )
        }

        val parts = packageName.split(".")
        if (parts.size < 2) {
            return ValidationResult.error(
                "Package name must have at least 2 parts",
                "Example: com.example or dev.mymod"
            )
        }

        for (part in parts) {
            if (part.isEmpty()) {
                return ValidationResult.error(
                    "Package name cannot have empty parts",
                    "Example: com.example (not com..example)"
                )
            }

            val pattern = Regex("^[a-z][a-z0-9_]*$")
            if (!pattern.matches(part)) {
                return ValidationResult.error(
                    "Invalid package part: '$part'",
                    "Each part must start with lowercase letter and contain only lowercase letters, numbers, underscores"
                )
            }
        }

        return ValidationResult.success()
    }

    /**
     * Validate semantic version string
     */
    fun validateVersion(version: String): ValidationResult {
        val pattern = Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.]+)?$")
        if (!pattern.matches(version)) {
            return ValidationResult.error(
                "Invalid version format: '$version'",
                "Use semantic versioning: 1.0.0 or 1.0.0-alpha"
            )
        }
        return ValidationResult.success()
    }

    /**
     * Validate Minecraft version
     */
    fun validateMinecraftVersion(version: String): ValidationResult {
        val pattern = Regex("^1\\.[0-9]{1,2}(\\.[0-9]{1,2})?$")
        if (!pattern.matches(version)) {
            return ValidationResult.error(
                "Invalid Minecraft version: '$version'",
                "Example: 1.20.1, 1.21, 1.19.4"
            )
        }
        return ValidationResult.success()
    }

    /**
     * Validate modloader name
     */
    fun validateLoader(loader: String): ValidationResult {
        val validLoaders = setOf("fabric", "forge", "neoforge")
        if (loader.lowercase() !in validLoaders) {
            return ValidationResult.error(
                "Invalid loader: '$loader'",
                "Valid loaders: fabric, forge, neoforge"
            )
        }
        return ValidationResult.success()
    }

    // ========== File Path Validation ==========

    /**
     * Validate file path exists
     */
    fun validatePathExists(path: File, type: String = "Path"): ValidationResult {
        if (!path.exists()) {
            return ValidationResult.error(
                "$type does not exist: ${path.absolutePath}",
                "Verify the path is correct"
            )
        }
        return ValidationResult.success()
    }

    /**
     * Validate path is a directory
     */
    fun validateIsDirectory(path: File): ValidationResult {
        if (!path.exists()) {
            return ValidationResult.error(
                "Directory does not exist: ${path.absolutePath}"
            )
        }
        if (!path.isDirectory) {
            return ValidationResult.error(
                "Not a directory: ${path.absolutePath}"
            )
        }
        return ValidationResult.success()
    }

    /**
     * Validate path is writable
     */
    fun validateWritable(path: File): ValidationResult {
        val testDir = if (path.isDirectory) path else path.parentFile
        if (testDir?.canWrite() != true) {
            return ValidationResult.error(
                "No write permission: ${path.absolutePath}",
                "Check file permissions"
            )
        }
        return ValidationResult.success()
    }

    /**
     * Validate path is safe (no directory traversal)
     */
    fun validateSafePath(basePath: File, targetPath: File): ValidationResult {
        val base = basePath.canonicalPath
        val target = targetPath.canonicalPath
        if (!target.startsWith(base)) {
            return ValidationResult.error(
                "Path traversal detected: ${targetPath.path}",
                "Path must be within project directory"
            )
        }
        return ValidationResult.success()
    }

    /**
     * Validate path length (Windows MAX_PATH = 260)
     */
    fun validatePathLength(path: Path): ValidationResult {
        val isWindows = System.getProperty("os.name").lowercase().contains("windows")
        if (isWindows && path.pathString.length > 260) {
            return ValidationResult.error(
                "Path too long (${path.pathString.length} chars, max 260 on Windows)",
                "Use a shorter path or enable long path support"
            )
        }
        return ValidationResult.success()
    }

    /**
     * Validate filename has no invalid characters
     */
    fun validateFileName(name: String): ValidationResult {
        val invalidChars = if (System.getProperty("os.name").lowercase().contains("windows")) {
            setOf('<', '>', ':', '"', '/', '\\', '|', '?', '*')
        } else {
            setOf('/', '\u0000')
        }

        val hasInvalid = name.any { it in invalidChars }
        if (hasInvalid) {
            return ValidationResult.error(
                "Invalid characters in filename: '$name'",
                "Remove special characters: ${invalidChars.joinToString("")}"
            )
        }

        return ValidationResult.success()
    }

    /**
     * Check if we're in a Dropper project
     */
    fun validateDropperProject(directory: File = File(System.getProperty("user.dir"))): ValidationResult {
        val configFile = File(directory, "config.yml")
        if (!configFile.exists()) {
            return ValidationResult.error(
                "Not in a Dropper project directory (config.yml not found)",
                "Run 'dropper init' or cd into a project directory"
            )
        }
        return ValidationResult.success()
    }

    // ========== Disk Space Validation ==========

    /**
     * Check available disk space
     */
    fun validateDiskSpace(path: File, requiredBytes: Long): ValidationResult {
        val available = path.usableSpace
        if (available < requiredBytes) {
            val requiredMB = requiredBytes / (1024 * 1024)
            val availableMB = available / (1024 * 1024)
            return ValidationResult.error(
                "Insufficient disk space: ${availableMB}MB available, ${requiredMB}MB required",
                "Free up disk space or use a different location"
            )
        }
        return ValidationResult.success()
    }

    // ========== Sanitization Helpers ==========

    private fun sanitizeModId(modId: String): String {
        return modId.lowercase()
            .replace(Regex("^[^a-z]+"), "")
            .replace(Regex("[^a-z0-9_-]"), "_")
            .replace(Regex("[_-]+"), "_")
            .replace(Regex("[_-]$"), "")
            .take(64)
            .ifEmpty { "mymod" }
    }

    private fun sanitizeComponentName(name: String): String {
        return name.lowercase()
            .trim()
            .replace(Regex("\\s+"), "_")
            .replace(Regex("^[^a-z]+"), "")
            .replace(Regex("[^a-z0-9_]"), "_")
            .replace(Regex("_+"), "_")
            .replace(Regex("_$"), "")
            .take(64)
            .ifEmpty { "component" }
    }

    /**
     * Helper to exit with validation error
     */
    fun exitWithError(result: ValidationResult): Nothing {
        if (result.errorMessage != null) {
            Logger.error(result.errorMessage)
            if (result.suggestion != null) {
                Logger.info("Suggestion: ${result.suggestion}")
            }
        }
        kotlin.system.exitProcess(1)
    }
}
