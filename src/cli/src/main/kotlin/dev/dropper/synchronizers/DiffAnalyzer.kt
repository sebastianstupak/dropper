package dev.dropper.synchronizers

import java.io.File
import java.security.MessageDigest

/**
 * Analyzes differences between source and target directories
 */
object DiffAnalyzer {

    /**
     * Analyze differences between source and target
     *
     * @return Lists of missing, outdated, conflicting, and identical files
     */
    fun analyze(
        sourceDir: File,
        targetDir: File,
        excludePatterns: List<String> = emptyList()
    ): DiffResult {
        val missing = mutableListOf<File>()
        val outdated = mutableListOf<File>()
        val conflicts = mutableListOf<File>()
        val identical = mutableListOf<File>()

        if (!sourceDir.exists()) {
            return DiffResult(missing, outdated, conflicts, identical)
        }

        sourceDir.walkTopDown()
            .filter { it.isFile }
            .filterNot { shouldExclude(it, sourceDir, excludePatterns) }
            .forEach { sourceFile ->
                val relativePath = sourceFile.relativeTo(sourceDir).path
                val targetFile = File(targetDir, relativePath)

                when {
                    !targetFile.exists() -> missing.add(sourceFile)
                    filesIdentical(sourceFile, targetFile) -> identical.add(sourceFile)
                    sourceFile.lastModified() > targetFile.lastModified() -> outdated.add(sourceFile)
                    else -> {
                        // Source is older but content differs - potential conflict
                        if (!contentIdentical(sourceFile, targetFile)) {
                            conflicts.add(sourceFile)
                        } else {
                            identical.add(sourceFile)
                        }
                    }
                }
            }

        return DiffResult(missing, outdated, conflicts, identical)
    }

    /**
     * Check if file should be excluded based on patterns
     */
    private fun shouldExclude(file: File, baseDir: File, patterns: List<String>): Boolean {
        val relativePath = file.relativeTo(baseDir).path.replace('\\', '/')
        return patterns.any { pattern ->
            val regex = globToRegex(pattern)
            regex.matches(relativePath)
        }
    }

    /**
     * Convert glob pattern to regex
     */
    private fun globToRegex(pattern: String): Regex {
        val regexPattern = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".")
        return Regex(regexPattern)
    }

    /**
     * Check if two files are identical (by hash)
     */
    private fun filesIdentical(file1: File, file2: File): Boolean {
        if (file1.length() != file2.length()) return false
        return contentIdentical(file1, file2)
    }

    /**
     * Check if file contents are identical using MD5 hash
     */
    private fun contentIdentical(file1: File, file2: File): Boolean {
        return hash(file1) == hash(file2)
    }

    /**
     * Calculate MD5 hash of file
     */
    private fun hash(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

/**
 * Result of diff analysis
 */
data class DiffResult(
    val missing: List<File>,
    val outdated: List<File>,
    val conflicts: List<File>,
    val identical: List<File>
) {
    fun needsSync(): Boolean = missing.isNotEmpty() || outdated.isNotEmpty()
    fun hasConflicts(): Boolean = conflicts.isNotEmpty()
}
