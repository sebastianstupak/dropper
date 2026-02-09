package dev.dropper.updaters

import dev.dropper.util.Logger
import java.io.File

/**
 * Updates mod loader versions in a Dropper project
 */
class LoaderUpdater(
    private val versionResolver: VersionResolver = VersionResolver(),
    private val updateChecker: UpdateChecker = UpdateChecker(versionResolver)
) {

    fun updateAllLoaders(projectDir: File): Boolean {
        Logger.info("Checking for loader updates...")

        val checkResult = updateChecker.checkUpdates(projectDir)
        val loaderUpdates = checkResult.available.filter {
            it.type in listOf(UpdateType.FABRIC_LOADER, UpdateType.FORGE, UpdateType.NEOFORGE)
        }

        if (loaderUpdates.isEmpty()) {
            Logger.success("All loaders are up to date!")
            return false
        }

        Logger.info("Found ${loaderUpdates.size} loader update(s)")
        loaderUpdates.forEach { update ->
            Logger.info("  ${update.name}: ${update.currentVersion} -> ${update.latestVersion}")
        }

        updateChecker.applyUpdates(projectDir, loaderUpdates)
        Logger.success("Loaders updated successfully!")
        return true
    }

    fun updateFabricLoader(projectDir: File): Boolean {
        Logger.info("Checking for Fabric Loader updates...")

        val checkResult = updateChecker.checkUpdates(projectDir)
        val fabricUpdates = checkResult.available.filter { it.type == UpdateType.FABRIC_LOADER }

        if (fabricUpdates.isEmpty()) {
            Logger.success("Fabric Loader is up to date!")
            return false
        }

        updateChecker.applyUpdates(projectDir, fabricUpdates)
        Logger.success("Fabric Loader updated!")
        return true
    }

    fun updateForge(projectDir: File): Boolean {
        Logger.info("Checking for Forge updates...")

        val checkResult = updateChecker.checkUpdates(projectDir)
        val forgeUpdates = checkResult.available.filter { it.type == UpdateType.FORGE }

        if (forgeUpdates.isEmpty()) {
            Logger.success("Forge is up to date!")
            return false
        }

        updateChecker.applyUpdates(projectDir, forgeUpdates)
        Logger.success("Forge updated!")
        return true
    }

    fun updateNeoForge(projectDir: File): Boolean {
        Logger.info("Checking for NeoForge updates...")

        val checkResult = updateChecker.checkUpdates(projectDir)
        val neoforgeUpdates = checkResult.available.filter { it.type == UpdateType.NEOFORGE }

        if (neoforgeUpdates.isEmpty()) {
            Logger.success("NeoForge is up to date!")
            return false
        }

        updateChecker.applyUpdates(projectDir, neoforgeUpdates)
        Logger.success("NeoForge updated!")
        return true
    }
}
