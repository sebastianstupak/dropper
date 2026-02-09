package config

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.LoaderOptions
import java.io.File

/**
 * Utility for loading YAML configuration files
 */
object ConfigLoader {
    private fun createYaml(clazz: Class<*>): Yaml {
        val options = LoaderOptions()
        val constructor = Constructor(clazz, options)
        return Yaml(constructor)
    }

    fun loadRootConfig(rootDir: File): RootConfig {
        val configFile = File(rootDir, "config.yml")
        require(configFile.exists()) { "Root config.yml not found at ${configFile.absolutePath}" }

        return configFile.inputStream().use {
            createYaml(RootConfig::class.java).loadAs(it, RootConfig::class.java)
        }
    }

    fun loadVersionConfig(rootDir: File, version: String): VersionConfig {
        val configFile = File(rootDir, "versions/$version/config.yml")
        require(configFile.exists()) {
            "Version config not found at ${configFile.absolutePath}"
        }

        return configFile.inputStream().use {
            createYaml(VersionConfig::class.java).loadAs(it, VersionConfig::class.java)
        }
    }

    fun loadAssetPackConfig(rootDir: File, packVersion: String): AssetPackConfig {
        val configFile = File(rootDir, "versions/shared/$packVersion/config.yml")
        require(configFile.exists()) {
            "Asset pack config not found at ${configFile.absolutePath}"
        }

        return configFile.inputStream().use {
            createYaml(AssetPackConfig::class.java).loadAs(it, AssetPackConfig::class.java)
        }
    }
}
