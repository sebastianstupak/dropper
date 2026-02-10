package config

/**
 * Root configuration from config.yml
 */
class RootConfig {
    var mod: ModInfo = ModInfo()
}

class ModInfo {
    var id: String = ""
    var name: String = ""
    var version: String = "1.0.0"
    var description: String = ""
    var author: String = ""
    var license: String = "MIT"
    var curse_project_id: Int? = null
    var modrinth_id: String? = null
}

/**
 * Version configuration from versions/{version}/config.yml
 */
class VersionConfig {
    var minecraft_version: String = ""
    var asset_pack: String = ""
    var loaders: List<String> = emptyList()
    var java_version: Int = 17
    var neoforge_version: String? = null
    var forge_version: String? = null
    var fabric_loader_version: String? = null
    var fabric_api_version: String? = null
    var architectury_api_version: String? = null
}

/**
 * Asset pack configuration from versions/shared/{pack}/config.yml
 */
class AssetPackConfig {
    var asset_pack: AssetPackInfo = AssetPackInfo()
}

class AssetPackInfo {
    var version: String = ""
    var minecraft_versions: List<String> = emptyList()
    var description: String = ""
    var inherits: String? = null
}
