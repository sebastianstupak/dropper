package dev.dropper.updaters

import dev.dropper.util.Logger
import java.io.File

/**
 * Updates dependencies in a Dropper project
 */
class DependencyUpdater(
    private val versionResolver: VersionResolver = VersionResolver(),
    private val updateChecker: UpdateChecker = UpdateChecker(versionResolver)
) {

    fun updateAllDependencies(projectDir: File): Boolean {
        Logger.info("Checking for dependency updates...")

        val checkResult = updateChecker.checkUpdates(projectDir)
        val depUpdates = checkResult.available.filter {
            it.type in listOf(UpdateType.DEPENDENCY, UpdateType.FABRIC_API)
        }

        if (depUpdates.isEmpty()) {
            Logger.success("All dependencies are up to date!")
            return false
        }

        Logger.info("Found ${depUpdates.size} dependency update(s)")
        depUpdates.forEach { update ->
            Logger.info("  ${update.name}: ${update.currentVersion} -> ${update.latestVersion}")
        }

        updateChecker.applyUpdates(projectDir, depUpdates)
        Logger.success("Dependencies updated successfully!")
        return true
    }

    fun updateFabricApi(projectDir: File): Boolean {
        Logger.info("Checking for Fabric API updates...")

        val checkResult = updateChecker.checkUpdates(projectDir)
        val apiUpdates = checkResult.available.filter { it.type == UpdateType.FABRIC_API }

        if (apiUpdates.isEmpty()) {
            Logger.success("Fabric API is up to date!")
            return false
        }

        updateChecker.applyUpdates(projectDir, apiUpdates)
        Logger.success("Fabric API updated!")
        return true
    }
}
