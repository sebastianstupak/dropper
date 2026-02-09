# Asset Pack & Cascading Layer System

## Overview

Dropper uses a **cascading layer system** to maximize code and asset reuse across multiple Minecraft versions and mod loaders. This system allows you to:

- Share items, blocks, and assets across multiple Minecraft versions
- Override specific assets for certain versions without duplicating everything
- Maintain clean separation between loader-agnostic and loader-specific code

## Layer Hierarchy

When building a specific version-loader combination (e.g., `1.20.1-fabric`), Dropper merges layers in this order:

### Code Layers (Java/Kotlin)

```
1. shared/common/src/main/java          ← All versions, all loaders
2. shared/{loader}/src/main/java        ← All versions, specific loader
3. versions/shared/v1/common/           ← MC version range (e.g., 1.20.x)
4. versions/shared/v2/common/           ← MC version range (e.g., 1.21.x, inherits v1)
5. versions/1_20_1/common/              ← Specific MC version
6. versions/1_20_1/{loader}/            ← Specific MC version + loader
```

**Rule**: Later layers override earlier layers. Files in layer 6 override files in layer 1.

### Asset Layers (textures, models, blockstates)

```
1. versions/shared/v1/assets/           ← Base asset pack
2. versions/shared/v2/assets/           ← Inherits v1, overrides specific files
3. versions/1_20_1/assets/              ← Version-specific overrides
4. versions/1_20_1/{loader}/assets/     ← Loader-specific overrides (rare)
```

### Data Layers (recipes, loot tables, tags)

```
1. versions/shared/v1/data/             ← Base data pack
2. versions/shared/v2/data/             ← Inherits v1
3. versions/1_20_1/data/                ← Version-specific
4. versions/1_20_1/{loader}/data/       ← Loader-specific (rare)
```

## Asset Pack Inheritance

Asset packs can inherit from each other, creating an inheritance chain:

```yaml
# versions/shared/v1/config.yml
asset_pack:
  version: "v1"
  minecraft_versions: ["1.20.1", "1.20.4", "1.20.6"]
  description: "Base asset pack for 1.20.x"
  inherits: null  # No parent

# versions/shared/v2/config.yml
asset_pack:
  version: "v2"
  minecraft_versions: ["1.21.1", "1.21.4"]
  description: "Asset pack for 1.21.x (inherits 1.20.x)"
  inherits: "v1"  # Inherits from v1
```

### Inheritance Chain Resolution

When building `1.21.1-fabric` with asset pack `v2`:

1. Load `v2` config
2. Check `inherits: "v1"`
3. Load `v1` config
4. Check `inherits: null` → stop
5. **Result**: Chain is `[v1, v2]`
6. Assets are merged: `v1 assets` → `v2 assets` (v2 overrides v1)

## Example Scenarios

### Scenario 1: Item Works Same in All Versions

**Problem**: Ruby sword works identically in 1.20.1 and 1.21.1

**Solution**:
```
shared/common/src/main/java/com/mymod/items/RubySword.java
versions/shared/v1/assets/mymod/models/item/ruby_sword.json
versions/shared/v1/assets/mymod/textures/item/ruby_sword.png
```

**Assign v1 to both versions**:
```yaml
# versions/1_20_1/config.yml
asset_pack: "v1"

# versions/1_21_1/config.yml
asset_pack: "v1"
```

### Scenario 2: Item Changes in 1.21

**Problem**: Ruby sword model changed in 1.21.1 (new JSON format)

**Solution**:
1. Keep base in v1 (for 1.20.x)
2. Create v2 inheriting v1
3. Override just the model in v2

```
# Unchanged from v1
shared/common/src/main/java/com/mymod/items/RubySword.java
versions/shared/v1/assets/mymod/textures/item/ruby_sword.png

# Override model in v2
versions/shared/v2/assets/mymod/models/item/ruby_sword.json  ← New format
```

**Configure inheritance**:
```yaml
# versions/shared/v2/config.yml
asset_pack:
  version: "v2"
  inherits: "v1"  # Reuse everything from v1 except overridden files

# versions/1_21_1/config.yml
asset_pack: "v2"  # Use v2 which inherits v1
```

**Build result for 1.21.1**:
- Texture from `v1` (inherited)
- Model from `v2` (override)
- Code from `shared/common` (version-agnostic)

### Scenario 3: Version-Specific Code

**Problem**: 1.20.1 uses `Item.Settings()`, 1.21.1 uses `Item.Properties()`

**Solution**:
1. Create common interface in `shared/common`
2. Add version-specific implementations

```java
// shared/common/src/main/java/com/mymod/items/RubySword.java
public class RubySword {
    public static final String ID = "ruby_sword";
    // Common logic here
}

// versions/shared/v1/common/src/main/java/com/mymod/items/RubySwordFactory.java
// For MC 1.20.x
public class RubySwordFactory {
    public static Item create() {
        return new SwordItem(..., new Item.Settings());
    }
}

// versions/shared/v2/common/src/main/java/com/mymod/items/RubySwordFactory.java
// For MC 1.21.x (overrides v1 implementation)
public class RubySwordFactory {
    public static Item create() {
        return new SwordItem(..., new Item.Properties());
    }
}
```

### Scenario 4: Loader-Specific Registration

**Problem**: Fabric, Forge, and NeoForge have different registration APIs

**Solution**: Use loader-specific directories

```
shared/fabric/src/main/java/com/mymod/platform/fabric/RubySwordFabric.java
shared/forge/src/main/java/com/mymod/platform/forge/RubySwordForge.java
shared/neoforge/src/main/java/com/mymod/platform/neoforge/RubySwordNeoForge.java
```

Each loader gets its own registration code, but they all reference the common item class.

## How Build Works

### Building 1.20.1-fabric

1. **Source Assembly** (`AssemblePackagesTask`):
   ```
   shared/common/                  → build/assembled-src/
   shared/fabric/                  → build/assembled-src/ (merges)
   versions/shared/v1/common/      → build/assembled-src/ (merges)
   versions/1_20_1/common/         → build/assembled-src/ (merges)
   versions/1_20_1/fabric/         → build/assembled-src/ (merges)
   ```

2. **Asset Assembly** (Gradle resources):
   ```
   versions/shared/v1/assets/      → build/resources/main/assets/
   versions/1_20_1/assets/         → build/resources/main/assets/ (overrides)
   versions/1_20_1/fabric/assets/  → build/resources/main/assets/ (overrides)
   ```

3. **Data Assembly** (Gradle resources):
   ```
   versions/shared/v1/data/        → build/resources/main/data/
   versions/1_20_1/data/           → build/resources/main/data/ (overrides)
   versions/1_20_1/fabric/data/    → build/resources/main/data/ (overrides)
   ```

4. **Compilation**:
   - Compile assembled sources with Fabric API dependencies
   - Generate `fabric.mod.json` from `config.yml`

5. **JAR Creation**:
   - Package compiled classes + resources
   - Output: `build/1_20_1/fabric.jar`

## Commands

### Create Asset Pack

```bash
dropper create asset-pack v2 --inherits v1 --mc-versions "1.21.1,1.21.4"
```

Creates:
```
versions/shared/v2/
├── config.yml
├── assets/
├── data/
└── common/
    └── src/main/java/
```

### Add Version Using Asset Pack

```bash
dropper create version 1.21.1 --asset-pack v2
```

Links version `1.21.1` to use asset pack `v2`.

### Create Item (Goes to Shared)

```bash
dropper create item ruby_sword --type tool
```

Creates:
```
shared/common/src/main/java/com/mymod/items/RubySword.java
shared/fabric/src/main/java/com/mymod/platform/fabric/RubySwordFabric.java
shared/forge/src/main/java/com/mymod/platform/forge/RubySwordForge.java
shared/neoforge/src/main/java/com/mymod/platform/neoforge/RubySwordNeoForge.java
versions/shared/v1/assets/mymod/models/item/ruby_sword.json
versions/shared/v1/assets/mymod/textures/item/ruby_sword.png
```

Item is automatically available in **all versions** using asset pack `v1`.

## Best Practices

### 1. Default to Shared

- Put items/blocks in `shared/common` unless version-specific
- Put assets in `versions/shared/v1` unless version-specific
- Only add to version directories when APIs actually differ

### 2. Asset Pack Naming

- `v1`: First asset pack (base)
- `v2`: Second asset pack (typically for new MC major version)
- Use semantic versioning if needed: `v1_20`, `v1_21`

### 3. Inheritance Chains

- Keep chains short (1-2 levels)
- `v2 → v1` is good
- `v4 → v3 → v2 → v1` is getting complex

### 4. Version-Specific Overrides

Only override what changed:
```
# If only model changed in 1.21
versions/shared/v2/assets/mymod/models/item/ruby_sword.json  ✓
versions/shared/v2/assets/mymod/textures/item/ruby_sword.png  ✗ (unnecessary duplicate)
```

### 5. Testing

Test builds for each version-loader combination:
```bash
dropper build --version 1.20.1 --loader fabric
dropper build --version 1.21.1 --loader fabric
```

Verify assets are correct in each JAR.

## Directory Structure Reference

```
my-mod/
├── config.yml                          # Root mod config
├── shared/
│   ├── common/                         # Code for ALL versions & loaders
│   │   └── src/main/java/
│   ├── fabric/                         # Fabric code for ALL versions
│   │   └── src/main/java/
│   ├── forge/                          # Forge code for ALL versions
│   │   └── src/main/java/
│   └── neoforge/                       # NeoForge code for ALL versions
│       └── src/main/java/
├── versions/
│   ├── shared/
│   │   ├── v1/                         # Asset pack for MC 1.20.x
│   │   │   ├── config.yml              # Pack config (inherits: null)
│   │   │   ├── common/                 # Code for 1.20.x all loaders
│   │   │   │   └── src/main/java/
│   │   │   ├── assets/                 # Textures, models for 1.20.x
│   │   │   └── data/                   # Recipes, loot tables for 1.20.x
│   │   └── v2/                         # Asset pack for MC 1.21.x
│   │       ├── config.yml              # Pack config (inherits: v1)
│   │       ├── common/                 # Code for 1.21.x (overrides v1)
│   │       ├── assets/                 # Assets for 1.21.x (overrides v1)
│   │       └── data/                   # Data for 1.21.x (overrides v1)
│   ├── 1_20_1/
│   │   ├── config.yml                  # Version config (asset_pack: v1)
│   │   ├── common/                     # 1.20.1-specific code
│   │   ├── fabric/                     # 1.20.1-fabric-specific
│   │   ├── forge/                      # 1.20.1-forge-specific
│   │   └── neoforge/                   # 1.20.1-neoforge-specific
│   └── 1_21_1/
│       ├── config.yml                  # Version config (asset_pack: v2)
│       ├── common/                     # 1.21.1-specific code
│       └── fabric/                     # 1.21.1-fabric-specific
└── build/
    ├── 1_20_1/
    │   ├── fabric.jar                  # Merged: shared + v1 + 1_20_1 + fabric
    │   ├── forge.jar
    │   └── neoforge.jar
    └── 1_21_1/
        ├── fabric.jar                  # Merged: shared + v1 + v2 + 1_21_1 + fabric
        └── neoforge.jar
```

## Troubleshooting

### Assets Not Overriding

**Problem**: Changed asset in v2 but still seeing v1 asset in build

**Solution**:
1. Check path matches exactly: `versions/shared/v2/assets/mymod/textures/item/ruby_sword.png`
2. Verify inheritance chain: `v2` → `v1`
3. Clean build: `dropper build --clean`

### Circular Inheritance

**Problem**: `Circular inheritance detected in asset pack: v2`

**Solution**:
```yaml
# ✗ WRONG
# v2/config.yml: inherits: v3
# v3/config.yml: inherits: v2  ← circular!

# ✓ CORRECT
# v2/config.yml: inherits: v1
# v3/config.yml: inherits: v2
```

### Class Not Found

**Problem**: `ClassNotFoundException` for item class

**Solution**:
1. Verify class is in `shared/common` or appropriate layer
2. Check package declaration matches directory structure
3. Ensure `src/main/java` structure is used

## Advanced: Custom Layer Order

Edit `AssetPackResolver.kt` in `build-logic/` to customize layer resolution order. By default:

```kotlin
fun resolveSourceDirs(version: String, loader: String): List<File> {
    return listOf(
        shared/common,           // Layer 1
        shared/{loader},         // Layer 2
        ...assetPackChain,       // Layer 3-N
        versions/{version}/common,   // Layer N+1
        versions/{version}/{loader}  // Layer N+2 (highest priority)
    )
}
```

Later layers override earlier ones during build.
