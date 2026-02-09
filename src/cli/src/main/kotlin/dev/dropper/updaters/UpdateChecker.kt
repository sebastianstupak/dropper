package dev.dropper.updaters

import dev.dropper.util.Logger
import java.io.File

/**
 * Checks for available updates in a Dropper project
 */
class UpdateChecker(
    private val versionResolver: VersionResolver = VersionResolver()
) : Updater {

    override fun checkUpdates(projectDir: File): UpdateCheckResult {
        if (!projectDir.exists()) {
            throw IllegalArgumentException("Project directory does not exist")
        }

        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) {
            throw IllegalArgumentException("Not a Dropper project (config.yml not found)")
        }

        val available = mutableListOf<Update>()
        val current = mutableMapOf<String, String>()

        // Check version directories for Minecraft versions
        val versionsDir = File(projectDir, "versions")
        if (versionsDir.exists()) {
            versionsDir.listFiles()?.forEach { versionDir ->
                if (versionDir.isDirectory && !versionDir.name.startsWith("shared")) {
                    checkVersionDirectory(versionDir, available, current)
                }
            }
        }

        return UpdateCheckResult(available, current)
    }

    override fun applyUpdates(projectDir: File, updates: List<Update>) {
        updates.forEach { update ->
            when (update.type) {
                UpdateType.MINECRAFT -> applyMinecraftUpdate(projectDir, update)
                UpdateType.FABRIC_LOADER -> applyFabricLoaderUpdate(projectDir, update)
                UpdateType.FABRIC_API -> applyFabricApiUpdate(projectDir, update)
                UpdateType.FORGE -> applyForgeUpdate(projectDir, update)
                UpdateType.NEOFORGE -> applyNeoForgeUpdate(projectDir, update)
                UpdateType.DEPENDENCY -> applyDependencyUpdate(projectDir, update)
            }
        }
    }

    private fun checkVersionDirectory(
        versionDir: File,
        available: MutableList<Update>,
        current: MutableMap<String, String>
    ) {
        val configFile = File(versionDir, "config.yml")
        if (!configFile.exists()) return

        val config = parseVersionConfig(configFile)

        // Check Minecraft version
        config["minecraft_version"]?.let { mcVersion ->
            current["minecraft_${versionDir.name}"] = mcVersion
            val latest = versionResolver.getLatestMinecraftVersion()
            if (versionResolver.isOlderVersion(mcVersion, latest)) {
                available.add(Update(
                    type = UpdateType.MINECRAFT,
                    name = "Minecraft (${versionDir.name})",
                    currentVersion = mcVersion,
                    latestVersion = latest,
                    description = "New Minecraft version available",
                    breaking = true
                ))
            }
        }

        // Check Fabric Loader
        config["fabric_loader_version"]?.let { version ->
            current["fabric_loader_${versionDir.name}"] = version
            val latest = versionResolver.getLatestFabricLoaderVersion()
            if (versionResolver.isOlderVersion(version, latest)) {
                available.add(Update(
                    type = UpdateType.FABRIC_LOADER,
                    name = "Fabric Loader (${versionDir.name})",
                    currentVersion = version,
                    latestVersion = latest
                ))
            }
        }

        // Check Fabric API
        config["fabric_api_version"]?.let { version ->
            current["fabric_api_${versionDir.name}"] = version
            config["minecraft_version"]?.let { mcVersion ->
                val latest = versionResolver.getLatestFabricApiVersion(mcVersion)
                if (latest != null && versionResolver.isOlderVersion(version, latest)) {
                    available.add(Update(
                        type = UpdateType.FABRIC_API,
                        name = "Fabric API (${versionDir.name})",
                        currentVersion = version,
                        latestVersion = latest
                    ))
                }
            }
        }

        // Check Forge
        config["forge_version"]?.let { version ->
            current["forge_${versionDir.name}"] = version
            config["minecraft_version"]?.let { mcVersion ->
                val latest = versionResolver.getLatestForgeVersion(mcVersion)
                if (latest != null && versionResolver.isOlderVersion(version, latest)) {
                    available.add(Update(
                        type = UpdateType.FORGE,
                        name = "Forge (${versionDir.name})",
                        currentVersion = version,
                        latestVersion = latest
                    ))
                }
            }
        }

        // Check NeoForge
        config["neoforge_version"]?.let { version ->
            current["neoforge_${versionDir.name}"] = version
            config["minecraft_version"]?.let { mcVersion ->
                val latest = versionResolver.getLatestNeoForgeVersion(mcVersion)
                if (latest != null && versionResolver.isOlderVersion(version, latest)) {
                    available.add(Update(
                        type = UpdateType.NEOFORGE,
                        name = "NeoForge (${versionDir.name})",
                        currentVersion = version,
                        latestVersion = latest
                    ))
                }
            }
        }
    }

    private fun parseVersionConfig(file: File): Map<String, String> {
        val result = mutableMapOf<String, String>()
        file.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.contains(":") && !trimmed.startsWith("#")) {
                val parts = trimmed.split(":", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim().removeSurrounding("\"")
                    result[key] = value
                }
            }
        }
        return result
    }

    private fun applyMinecraftUpdate(projectDir: File, update: Update) {
        Logger.info("Applying Minecraft update: ${update.currentVersion} -> ${update.latestVersion}")
        // Update version config files
        updateVersionConfigs(projectDir, "minecraft_version", update.latestVersion)
    }

    private fun applyFabricLoaderUpdate(projectDir: File, update: Update) {
        Logger.info("Applying Fabric Loader update: ${update.currentVersion} -> ${update.latestVersion}")
        updateVersionConfigs(projectDir, "fabric_loader_version", update.latestVersion)
    }

    private fun applyFabricApiUpdate(projectDir: File, update: Update) {
        Logger.info("Applying Fabric API update: ${update.currentVersion} -> ${update.latestVersion}")
        updateVersionConfigs(projectDir, "fabric_api_version", update.latestVersion)
    }

    private fun applyForgeUpdate(projectDir: File, update: Update) {
        Logger.info("Applying Forge update: ${update.currentVersion} -> ${update.latestVersion}")
        updateVersionConfigs(projectDir, "forge_version", update.latestVersion)
    }

    private fun applyNeoForgeUpdate(projectDir: File, update: Update) {
        Logger.info("Applying NeoForge update: ${update.currentVersion} -> ${update.latestVersion}")
        updateVersionConfigs(projectDir, "neoforge_version", update.latestVersion)
    }

    private fun applyDependencyUpdate(projectDir: File, update: Update) {
        Logger.info("Applying dependency update: ${update.name} ${update.currentVersion} -> ${update.latestVersion}")
    }

    private fun updateVersionConfigs(projectDir: File, key: String, newValue: String) {
        val versionsDir = File(projectDir, "versions")
        versionsDir.listFiles()?.forEach { versionDir ->
            if (versionDir.isDirectory && !versionDir.name.startsWith("shared")) {
                val configFile = File(versionDir, "config.yml")
                if (configFile.exists()) {
                    val content = configFile.readText()
                    val updated = content.replace(
                        Regex("^$key:\\s*\"[^\"]+\"", RegexOption.MULTILINE),
                        "$key: \"$newValue\""
                    )
                    configFile.writeText(updated)
                }
            }
        }
    }
}
