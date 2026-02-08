package utils

import config.ConfigLoader
import java.io.File

/**
 * Resolves asset pack inheritance chains and directory lists
 */
class AssetPackResolver(private val rootDir: File) {

    /**
     * Resolves the full inheritance chain for an asset pack.
     * Returns list in order from base to most specific.
     *
     * Example: v2 inherits v1 -> returns [v1, v2]
     */
    fun resolveInheritanceChain(packVersion: String): List<String> {
        val chain = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        var current: String? = packVersion

        while (current != null) {
            if (current in visited) {
                throw IllegalStateException("Circular inheritance detected in asset pack: $current")
            }

            visited.add(current)
            chain.add(0, current) // Prepend to maintain base-to-specific order

            val config = ConfigLoader.loadAssetPackConfig(rootDir, current)
            current = config.asset_pack.inherits
        }

        return chain
    }

    /**
     * Resolves asset directories in priority order (base to specific).
     * Later directories override earlier ones.
     */
    fun resolveAssetDirs(version: String, loader: String): List<File> {
        val versionConfig = ConfigLoader.loadVersionConfig(rootDir, version)
        val chain = resolveInheritanceChain(versionConfig.asset_pack)

        val dirs = mutableListOf<File>()

        // Add inherited asset packs in order
        chain.forEach { pack ->
            val packAssets = File(rootDir, "versions/shared/$pack/assets")
            if (packAssets.exists()) {
                dirs.add(packAssets)
            }
        }

        // Add version-specific assets
        val versionAssets = File(rootDir, "versions/$version/assets")
        if (versionAssets.exists()) {
            dirs.add(versionAssets)
        }

        // Add loader-specific assets (rare)
        val loaderAssets = File(rootDir, "versions/$version/$loader/assets")
        if (loaderAssets.exists()) {
            dirs.add(loaderAssets)
        }

        return dirs
    }

    /**
     * Resolves data directories in priority order (base to specific).
     */
    fun resolveDataDirs(version: String, loader: String): List<File> {
        val versionConfig = ConfigLoader.loadVersionConfig(rootDir, version)
        val chain = resolveInheritanceChain(versionConfig.asset_pack)

        val dirs = mutableListOf<File>()

        // Add inherited data packs in order
        chain.forEach { pack ->
            val packData = File(rootDir, "versions/shared/$pack/data")
            if (packData.exists()) {
                dirs.add(packData)
            }
        }

        // Add version-specific data
        val versionData = File(rootDir, "versions/$version/data")
        if (versionData.exists()) {
            dirs.add(versionData)
        }

        // Add loader-specific data (rare)
        val loaderData = File(rootDir, "versions/$version/$loader/data")
        if (loaderData.exists()) {
            dirs.add(loaderData)
        }

        return dirs
    }

    /**
     * Resolves source directories in layer order (base to specific).
     */
    fun resolveSourceDirs(version: String, loader: String): List<File> {
        val versionConfig = ConfigLoader.loadVersionConfig(rootDir, version)
        val chain = resolveInheritanceChain(versionConfig.asset_pack)

        val dirs = mutableListOf<File>()

        // Layer 1: shared/common
        val sharedCommon = File(rootDir, "shared/common")
        if (sharedCommon.exists()) {
            dirs.add(sharedCommon)
        }

        // Layer 2: shared/{loader}
        val sharedLoader = File(rootDir, "shared/$loader")
        if (sharedLoader.exists()) {
            dirs.add(sharedLoader)
        }

        // Layer 3: asset pack common code (in inheritance order)
        chain.forEach { pack ->
            val packCommon = File(rootDir, "versions/shared/$pack/common")
            if (packCommon.exists()) {
                dirs.add(packCommon)
            }
        }

        // Layer 4: version-specific common
        val versionCommon = File(rootDir, "versions/$version/common")
        if (versionCommon.exists()) {
            dirs.add(versionCommon)
        }

        // Layer 5: version+loader specific
        val versionLoader = File(rootDir, "versions/$version/$loader")
        if (versionLoader.exists()) {
            dirs.add(versionLoader)
        }

        return dirs
    }
}
