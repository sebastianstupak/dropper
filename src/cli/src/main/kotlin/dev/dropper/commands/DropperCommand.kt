package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Base class for all Dropper commands.
 * Provides configurable project directory for testability.
 */
abstract class DropperCommand(
    name: String,
    help: String = ""
) : CliktCommand(name = name, help = help) {

    /**
     * Project directory - can be set for testing.
     * Defaults to current working directory.
     */
    var projectDir: File = File(System.getProperty("user.dir"))

    /**
     * Get the project config file
     */
    protected fun getConfigFile(): File = File(projectDir, "config.yml")

    /**
     * Extract the mod ID from a config.yml file
     */
    protected fun extractModId(configFile: File): String? {
        val content = configFile.readText()
        return Regex("id:\\s*\"?([a-z0-9_-]+)\"?").find(content)?.groupValues?.get(1)
    }

    /**
     * Extract minecraft_versions list from a config.yml file.
     * Expects a YAML block like:
     *   minecraft_versions:
     *     - "1.20.1"
     *     - "1.21.1"
     */
    protected fun extractMinecraftVersions(configFile: File): List<String> {
        val content = configFile.readText()
        val versions = mutableListOf<String>()
        var inVersionsBlock = false
        for (line in content.lines()) {
            if (line.trimStart().startsWith("minecraft_versions:")) {
                inVersionsBlock = true
                continue
            }
            if (inVersionsBlock) {
                val trimmed = line.trimStart()
                if (trimmed.startsWith("- ")) {
                    val version = trimmed.removePrefix("- ").trim().removeSurrounding("\"")
                    versions.add(version)
                } else {
                    break
                }
            }
        }
        return versions
    }

    /**
     * Update the main mod class init() method to call a registry's init().
     * Finds the main mod Java class in shared/common and adds the registry init call.
     */
    protected fun updateMainModInit(projectDir: File, sanitizedModId: String, registryClass: String) {
        // Find main mod class in shared/common
        val packageDir = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId")
        val mainModFile = packageDir.listFiles()?.firstOrNull {
            it.isFile && it.extension == "java" && !it.name.contains("package-info")
        } ?: return

        val content = mainModFile.readText()
        val initCall = "$registryClass.init();"

        if (content.contains(initCall)) return  // Already registered

        // Add import if not present
        val importLine = "import com.$sanitizedModId.registry.$registryClass;"
        var updatedContent = if (!content.contains(importLine)) {
            content.replaceFirst("public class ", "$importLine\n\npublic class ")
        } else content

        // Add init() call - insert after opening brace of init() method
        val initMethodPattern = "public static void init() {"
        updatedContent = if (updatedContent.contains(initMethodPattern)) {
            updatedContent.replaceFirst(initMethodPattern, "$initMethodPattern\n        $initCall")
        } else updatedContent

        FileUtil.writeText(mainModFile, updatedContent)
        Logger.info("  ✓ Added $initCall to ${mainModFile.name}")
    }
}
