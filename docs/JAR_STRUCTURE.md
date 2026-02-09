# Minecraft Mod JAR Structure

This document explains the required JAR structure for each mod loader and how Dropper handles it automatically.

## Overview

Each mod loader expects specific metadata files in specific locations within the JAR file. Dropper's build system automatically generates these files and places them correctly.

## Fabric JAR Structure

### Requirements
- **Metadata file**: `fabric.mod.json` at JAR **root** (not in META-INF)
- **Java version**: MC 1.20.x → Java 17, MC 1.21.x → Java 21

### JAR Layout
```
mymod-1.0.0-fabric.jar
├── fabric.mod.json          ← ROOT LEVEL (required by Fabric Loader)
├── META-INF/
│   ├── MANIFEST.MF
│   └── services/
│       └── com.mymod.platform.PlatformHelper  ← ServiceLoader config
├── com/
│   └── mymod/
│       ├── MyMod.class
│       └── ...
├── assets/
│   └── mymod/
│       ├── textures/
│       ├── models/
│       └── lang/
└── data/
    └── mymod/
        ├── recipes/
        └── tags/
```

### Generated fabric.mod.json
```json
{
  "schemaVersion": 1,
  "id": "mymod",
  "version": "1.0.0",
  "name": "My Mod",
  "description": "...",
  "authors": ["Author"],
  "license": "MIT",
  "environment": "*",
  "entrypoints": {
    "main": ["com.mymod.fabric.MyModFabric"]
  },
  "depends": {
    "fabricloader": ">=0.16.9",
    "minecraft": "1.20.1",
    "java": ">=17"
  }
}
```

**Key Point**: Fabric Loader looks for `fabric.mod.json` at the JAR root, not in META-INF.

---

## Forge JAR Structure (MC 1.20.x)

### Requirements
- **Metadata file**: `META-INF/mods.toml`
- **Java version**: MC 1.20.x → Java 17

### JAR Layout
```
mymod-1.0.0-forge.jar
├── META-INF/
│   ├── MANIFEST.MF
│   ├── mods.toml            ← Required for Forge
│   └── services/
│       └── com.mymod.platform.PlatformHelper
├── com/
│   └── mymod/
│       └── ...
├── assets/
│   └── mymod/
│       └── ...
└── data/
    └── mymod/
        └── ...
```

### Generated mods.toml
```toml
modLoader = "javafml"
loaderVersion = "[47,)"
license = "MIT"

[[mods]]
modId = "mymod"
version = "1.0.0"
displayName = "My Mod"
description = '''...'''
authors = "Author"

[[dependencies.mymod]]
modId = "forge"
mandatory = true
versionRange = "[47.0.0,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.mymod]]
modId = "minecraft"
mandatory = true
versionRange = "[1.20.1]"
ordering = "NONE"
side = "BOTH"
```

---

## NeoForge JAR Structure (MC 1.20.1+)

### Requirements
- **Metadata file**: `META-INF/neoforge.mods.toml` (renamed from `mods.toml`)
- **Java version**: MC 1.20.x → Java 17, MC 1.21.x → Java 21

### JAR Layout
```
mymod-1.0.0-neoforge.jar
├── META-INF/
│   ├── MANIFEST.MF
│   ├── neoforge.mods.toml   ← NeoForge-specific (not mods.toml)
│   └── services/
│       └── com.mymod.platform.PlatformHelper
├── com/
│   └── mymod/
│       └── ...
├── assets/
│   └── mymod/
│       └── ...
└── data/
    └── mymod/
        └── ...
```

### Generated neoforge.mods.toml
```toml
modLoader = "javafml"
loaderVersion = "[21.1.0,)"
license = "MIT"

[[mods]]
modId = "mymod"
version = "1.0.0"
displayName = "My Mod"
description = '''...'''
authors = "Author"

[[dependencies.mymod]]
modId = "neoforge"
type = "required"
versionRange = "[21.1.0,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.mymod]]
modId = "minecraft"
type = "required"
versionRange = "[1.20.1]"
ordering = "NONE"
side = "BOTH"
```

**Key Difference from Forge**: The file is named `neoforge.mods.toml` (not `mods.toml`) to distinguish NeoForge mods from Forge mods.

---

## Java Version Requirements

| Minecraft Version | Required Java Version | Dropper Auto-Detection |
|-------------------|----------------------|------------------------|
| 1.20.1            | Java 17              | ✅ Automatic            |
| 1.20.4            | Java 17              | ✅ Automatic            |
| 1.21.1            | Java 21              | ✅ Automatic            |
| 1.21.4            | Java 21              | ✅ Automatic            |

Dropper automatically selects the correct Java toolchain based on the Minecraft version in `versions/{version}/config.yml`.

### How It Works

In `build-logic/mod.loader.gradle.kts`:
```kotlin
val requiredJavaVersion = when {
    versionConfig.minecraft_version.startsWith("1.20") -> 17
    versionConfig.minecraft_version.startsWith("1.21") -> 21
    else -> versionConfig.java_version
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(requiredJavaVersion))
    }
}
```

### Automatic Java Download

Dropper includes the Foojay Toolchain Resolver plugin in `settings.gradle.kts`, which automatically downloads the required Java version if not installed locally.

---

## ServiceLoader Configuration

All three loaders use Java's ServiceLoader pattern for platform abstraction. Dropper automatically generates the required service configuration files.

### File Location
```
META-INF/
└── services/
    └── com.mymod.platform.PlatformHelper  ← File name = interface FQN
```

### File Content
```
com.mymod.platform.fabric.FabricPlatformHelper
```

The file name is the **fully qualified name of the interface**, and the content is the **fully qualified name of the implementation class**.

### Generated Services

Dropper automatically creates service files for:
- `com.{modId}.platform.PlatformHelper` → `com.{modId}.platform.{loader}.{Loader}PlatformHelper`

---

## How Dropper Handles This

### 1. Metadata Generation (`GenerateMetadataTask`)

**For Fabric**:
- Generates `fabric.mod.json` at JAR root
- Includes correct entrypoint: `com.{modId}.fabric.{ModName}Fabric`
- Sets Java version dependency

**For Forge**:
- Generates `META-INF/mods.toml`
- Uses `javafml` mod loader
- Sets correct Forge version dependency

**For NeoForge**:
- Generates `META-INF/neoforge.mods.toml` (not `mods.toml`)
- Uses `javafml` mod loader
- Sets correct NeoForge version dependency

### 2. Java Toolchain Selection

Dropper automatically detects the required Java version:
```kotlin
when {
    MC 1.20.x → Java 17
    MC 1.21.x → Java 21
}
```

### 3. Mod Loader Dependencies

The `ModLoaderPlugin` automatically adds the correct dependencies:

**For Fabric**:
```kotlin
implementation("net.fabricmc:fabric-loader:${fabric_loader_version}")
implementation("net.fabricmc.fabric-api:fabric-api:${fabric_api_version}")
```

**For Forge**:
```kotlin
implementation("net.minecraftforge:forge:${minecraft_version}-${forge_version}")
```

**For NeoForge**:
```kotlin
implementation("net.neoforged:neoforge:${neoforge_version}")
```

All versions are pulled from `versions/{version}/config.yml`.

### 4. Resource Processing

All resources are processed through Gradle's standard resource handling:
```
src/main/resources/ → JAR root
META-INF/ → JAR META-INF/
```

---

## Verifying JAR Structure

You can verify the generated JAR structure using:

### Linux/Mac
```bash
unzip -l build/1_20_1/fabric.jar
```

### Windows
```bash
jar -tf build/1_20_1/fabric.jar
```

### Expected Output (Fabric)
```
fabric.mod.json
META-INF/MANIFEST.MF
META-INF/services/com.mymod.platform.PlatformHelper
com/mymod/MyMod.class
assets/mymod/...
data/mymod/...
```

### Expected Output (NeoForge)
```
META-INF/MANIFEST.MF
META-INF/neoforge.mods.toml
META-INF/services/com.mymod.platform.PlatformHelper
com/mymod/MyMod.class
assets/mymod/...
data/mymod/...
```

---

## Common Issues

### Issue: "fabric.mod.json not found"
**Cause**: fabric.mod.json is in META-INF instead of JAR root
**Fix**: Dropper places it at JAR root automatically - ensure you're using `dropper build`

### Issue: "NeoForge mod not loading"
**Cause**: Using `mods.toml` instead of `neoforge.mods.toml`
**Fix**: Dropper generates `neoforge.mods.toml` for NeoForge (v20.5+)

### Issue: "Unsupported Java version"
**Cause**: Wrong Java version for Minecraft version
**Fix**: Dropper auto-selects correct Java version - enable Foojay resolver for auto-download

### Issue: "ServiceLoader implementation not found"
**Cause**: Missing META-INF/services configuration
**Fix**: Dropper auto-generates service files - ensure GenerateMetadataTask runs

---

## References

### Official Documentation
- [Fabric Wiki - fabric.mod.json](https://wiki.fabricmc.net/documentation:fabric_mod_json)
- [Forge Docs - mods.toml](https://docs.minecraftforge.net/en/latest/gettingstarted/modfiles/)
- [NeoForge Docs - Mod Files](https://docs.neoforged.net/docs/gettingstarted/modfiles/)
- [Gradle Toolchains](https://docs.gradle.org/current/userguide/toolchains.html)

### Dropper Implementation
- `build-logic/tasks/GenerateMetadataTask.kt` - Metadata generation
- `build-logic/mod.loader.gradle.kts` - Java toolchain selection
- `settings.gradle.kts` - Foojay resolver configuration
