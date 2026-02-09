package dev.dropper.updaters

import dev.dropper.util.Logger
import java.io.File

/**
 * Updates Minecraft version in a Dropper project
 */
class MinecraftUpdater(
    private val versionResolver: VersionResolver = VersionResolver(),
    private val updateChecker: UpdateChecker = UpdateChecker(versionResolver)
) {

    fun updateToLatest(projectDir: File): Boolean {
        Logger.info("Checking for Minecraft updates...")

        val checkResult = updateChecker.checkUpdates(projectDir)
        val mcUpdates = checkResult.available.filter { it.type == UpdateType.MINECRAFT }

        if (mcUpdates.isEmpty()) {
            Logger.success("Already on latest Minecraft version!")
            return false
        }

        Logger.info("Found ${mcUpdates.size} Minecraft version update(s)")
        mcUpdates.forEach { update ->
            Logger.info("  ${update.name}: ${update.currentVersion} -> ${update.latestVersion}")
        }

        updateChecker.applyUpdates(projectDir, mcUpdates)
        Logger.success("Minecraft version updated successfully!")
        return true
    }

    fun updateToVersion(projectDir: File, targetVersion: String) {
        Logger.info("Updating Minecraft to version $targetVersion...")

        val versionsDir = File(projectDir, "versions")
        versionsDir.listFiles()?.forEach { versionDir ->
            if (versionDir.isDirectory && !versionDir.name.startsWith("shared")) {
                val configFile = File(versionDir, "config.yml")
                if (configFile.exists()) {
                    val content = configFile.readText()
                    val updated = content.replace(
                        Regex("^minecraft_version:\\s*\"[^\"]+\"", RegexOption.MULTILINE),
                        "minecraft_version: \"$targetVersion\""
                    )
                    configFile.writeText(updated)
                }
            }
        }

        Logger.success("Minecraft version updated to $targetVersion!")
    }
}
