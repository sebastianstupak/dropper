# Project Structure Guide

This file explains the structure of your multi-loader Minecraft mod project.

## Overview

This project uses a **layered architecture** where code and assets are organized by:
1. **Shared across all versions/loaders** (`shared/`)
2. **Versioned asset packs** (`versions/shared/v1/`, `v2/`, etc.)
3. **Version-specific** (`versions/1_20_1/`, etc.)
4. **Loader-specific** within versions (`versions/1_20_1/neoforge/`, etc.)

**Key principle**: Files in later layers override earlier layers during build.

## Directory Structure

```
simplemod/
├── config.yml                    # Mod metadata (SINGLE SOURCE OF TRUTH)
├── shared/                       # Version-agnostic code
│   ├── common/                   # Platform-agnostic (*.java + *Test.java)
│   ├── neoforge/                 # NeoForge abstractions
│   ├── forge/                    # Forge abstractions
│   └── fabric/                   # Fabric abstractions
├── versions/
│   ├── shared/
│   │   └── v1/                   # Asset pack for MC versions
│   │       ├── config.yml        # Which MC versions use this
│   │       ├── common/           # Code for versions (all loaders)
│   │       ├── assets/           # Textures, models, etc.
│   │       └── data/             # Recipes, tags, loot tables
│   │   └── 1_20_1/                    # MC version-specific
│       ├── config.yml            # Version configuration
│       ├── common/               # Common code for this version
│       ├── assets/               # Additive to asset pack
│       ├── data/                 # Additive to asset pack
│       ├── neoforge/             # NeoForge-specific
│       ├── fabric/               # Fabric-specific
│       └── forge/                # Forge-specific
│   
├── buildSrc/                     # Build logic (custom Gradle plugin)
│   ├── build.gradle.kts          # Build configuration for buildSrc
│   └── src/main/kotlin/          # ModLoaderPlugin and build tasks
└── build/                        # Generated JARs

```

## Important Concepts

### 1. Asset Pack Inheritance
- Asset packs (like `v1`) can be reused across multiple Minecraft versions
- Build system merges assets: `v1 → version → loader`
- Later layers override earlier ones

### 2. Java Package Structure
- Base package: `com.simplemod`
- All Java files follow proper package structure
- Build system handles source sets automatically

### 3. Tests Co-located with Code
- `MyClass.java` and `MyClassTest.java` live in same source set
- Build system separates them using naming convention (`*Test.java`)

### 4. Source Layering for Compilation
For a specific version-loader (e.g., `1_20_1-neoforge`), sources are layered:
```
1. shared/common/                  (all versions, all loaders)
2. shared/neoforge/                (all versions, neoforge only)
3. versions/shared/v1/common/      (asset pack versions, all loaders)
4. versions/1_20_1/common/         (1.20.1 only, all loaders)
5. versions/1_20_1/neoforge/       (1.20.1 neoforge only)
```

## Working with This Project

### Adding a New Feature

1. **Determine scope**:
   - All versions/loaders? → `shared/common/`
   - All versions in asset pack? → `versions/shared/v1/common/`
   - Only specific version? → `versions/1_20_1/common/`
   - Only specific loader? → `versions/1_20_1/neoforge/`

2. **Use Dropper CLI to generate code**:
   ```bash
   # Create items
   dropper create item ruby_sword --type tool
   dropper create item ruby --type basic
   dropper create item ruby_apple --type food

   # Create blocks
   dropper create block ruby_ore --type ore
   dropper create block ruby_block --type basic
   ```

### Platform Abstraction Pattern

Use **Java ServiceLoader** for platform-specific code:

```java
// shared/common - Interface
public interface PlatformHelper {
    String getPlatformName();
    boolean isModLoaded(String modId);
}

// shared/neoforge - NeoForge implementation
public class NeoForgePlatformHelper implements PlatformHelper {
    @Override
    public String getPlatformName() { return "NeoForge"; }
}

// Access via Services
Services.PLATFORM.getPlatformName();
```

## Build Commands

```bash
# Build everything using Dropper CLI
dropper build --all

# Build specific version
dropper build --version 1.20.1

# Build specific loader
dropper build --loader fabric

# Build specific version-loader combination
dropper build --version 1.20.1 --loader fabric

# Clean build
dropper build --all --clean
```

Output JARs: `build/<version>/<loader>.jar`

## Version Management

```bash
# Add new Minecraft version
dropper create version 1.21.1 --loaders fabric,neoforge

# Create new asset pack
dropper create asset-pack v2 --inherits v1 --mc-versions "1.21.1"
```

## Mod Loader Documentation

### Fabric
- **Wiki**: https://fabricmc.net/wiki/
- **API Docs**: https://maven.fabricmc.net/docs/
- **Discord**: https://discord.gg/v6v4pMv

### NeoForge
- **Docs**: https://docs.neoforged.net/
- **Discord**: https://discord.neoforged.net/

### Forge
- **Docs**: https://docs.minecraftforge.net/
- **Discord**: https://discord.minecraftforge.net/

## Common Patterns

### Registering Items
```java
// shared/common/items/MyItem.java
public class MyItem extends Item {
    public MyItem() {
        super(new Properties());
    }
}

// shared/<loader>/ModItems.java
// Loader-specific registration
```

### Adding Assets
```bash
# Textures
versions/shared/v1/assets/simplemod/textures/item/my_item.png

# Models
versions/shared/v1/assets/simplemod/models/item/my_item.json

# Recipes
versions/shared/v1/data/simplemod/recipes/my_item.json
```

## Metadata Management

**ALWAYS edit `config.yml`** - do NOT manually edit:
- `fabric.mod.json`
- `mods.toml`
- `META-INF/` files

These are auto-generated during build from `config.yml`.

## Philosophy

This structure optimizes for:
- **Maximum reuse** - Define once, use everywhere
- **Clear separation** - Each layer has a specific purpose
- **Maintainability** - Add versions/loaders without touching existing code
- **Build performance** - Incremental builds, proper caching

**The goal**: Support multiple Minecraft versions across multiple loaders with minimal code duplication.

---

Generated with [Dropper](https://github.com/sebastianstupak/dropper)
