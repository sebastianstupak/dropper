package dev.dropper.publishers

import dev.dropper.util.Logger
import java.io.File

/**
 * Generates changelogs from git commits or files
 */
class ChangelogGenerator {

    /**
     * Generate changelog from git commits since last tag
     */
    fun generateFromGit(projectDir: File): String {
        try {
            // Get last tag
            val lastTag = executeGitCommand(projectDir, listOf("git", "describe", "--tags", "--abbrev=0"))
                .trim()
                .takeIf { it.isNotBlank() }

            val commitRange = if (lastTag != null) {
                "$lastTag..HEAD"
            } else {
                "HEAD"
            }

            // Get commits
            val commits = executeGitCommand(
                projectDir,
                listOf("git", "log", commitRange, "--pretty=format:%s")
            ).lines().filter { it.isNotBlank() }

            if (commits.isEmpty()) {
                return "No changes since last release."
            }

            return formatChangelog(commits)
        } catch (e: Exception) {
            Logger.warn("Failed to generate changelog from git: ${e.message}")
            return "Release ${System.currentTimeMillis()}"
        }
    }

    /**
     * Load changelog from file
     */
    fun loadFromFile(file: File): String {
        if (!file.exists()) {
            throw IllegalArgumentException("Changelog file not found: ${file.absolutePath}")
        }
        return file.readText().trim()
    }

    /**
     * Format commits into a structured changelog
     */
    private fun formatChangelog(commits: List<String>): String {
        val categorized = mutableMapOf<String, MutableList<String>>()

        commits.forEach { commit ->
            val category = when {
                commit.startsWith("feat:") || commit.startsWith("feature:") -> "Features"
                commit.startsWith("fix:") -> "Bug Fixes"
                commit.startsWith("docs:") -> "Documentation"
                commit.startsWith("refactor:") -> "Refactoring"
                commit.startsWith("test:") -> "Testing"
                commit.startsWith("chore:") -> "Maintenance"
                commit.startsWith("perf:") -> "Performance"
                else -> "Other Changes"
            }

            val message = commit
                .replaceFirst(Regex("^(feat|feature|fix|docs|refactor|test|chore|perf):\\s*"), "")
                .trim()
                .replaceFirstChar { it.uppercase() }

            categorized.getOrPut(category) { mutableListOf() }.add(message)
        }

        val builder = StringBuilder()
        val order = listOf("Features", "Bug Fixes", "Performance", "Refactoring", "Documentation", "Testing", "Maintenance", "Other Changes")

        order.forEach { category ->
            categorized[category]?.let { messages ->
                if (messages.isNotEmpty()) {
                    builder.append("## $category\n\n")
                    messages.forEach { message ->
                        builder.append("- $message\n")
                    }
                    builder.append("\n")
                }
            }
        }

        return builder.toString().trim()
    }

    /**
     * Execute git command and return output
     */
    private fun executeGitCommand(workingDir: File, command: List<String>): String {
        val process = ProcessBuilder(command)
            .directory(workingDir)
            .redirectErrorStream(true)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        val exitCode = process.waitFor()

        if (exitCode != 0) {
            throw RuntimeException("Git command failed: $output")
        }

        return output
    }

    /**
     * Validate git repository
     */
    fun isGitRepository(projectDir: File): Boolean {
        return try {
            executeGitCommand(projectDir, listOf("git", "rev-parse", "--git-dir"))
            true
        } catch (e: Exception) {
            false
        }
    }
}
