# Mod Loader Version Matrix

Quick reference for Minecraft mod loader versions and requirements.

## Fabric Loader Versions

| Minecraft | Fabric Loader | Java | Metadata File | Location |
|-----------|---------------|------|---------------|----------|
| 1.21.1    | 0.15.0+       | 21+  | fabric.mod.json | JAR root |
| 1.20.1    | 0.14.0+       | 17+  | fabric.mod.json | JAR root |
| 1.19.2    | 0.14.0+       | 17+  | fabric.mod.json | JAR root |
| 1.18.2    | 0.13.0+       | 17+  | fabric.mod.json | JAR root |
| 1.16.5    | 0.11.0+       | 8+   | fabric.mod.json | JAR root |

**Notes:**
- Schema version is always `1` for modern Fabric
- Metadata format is version-agnostic (controlled by Fabric Loader, not MC version)
- All versions use same `fabric.mod.json` structure

## Forge Loader Versions

| Minecraft | Loader Version | Forge Version | Java | Metadata File | Location |
|-----------|----------------|---------------|------|---------------|----------|
| 1.20.1    | `[46,)`        | 47.0.0+       | 17+  | mods.toml     | META-INF/ |
| 1.19.2    | `[41,)`        | 43.0.0+       | 17+  | mods.toml     | META-INF/ |
| 1.18.2    | `[40,)`        | 40.0.0+       | 17+  | mods.toml     | META-INF/ |
| 1.16.5    | `[36,)`        | 36.0.0+       | 8+   | mods.toml     | META-INF/ |

**Dependency Declaration:**
```toml
[[dependencies.modid]]
    modId="forge"
    type="required"
    versionRange="[46,)"  # Use appropriate version
```

## NeoForge Loader Versions

| Minecraft | Loader Version | NeoForge Version | Java | Metadata File | Location |
|-----------|----------------|------------------|------|---------------|----------|
| 1.21.1    | `[2,)`         | 21.1.0+          | 21+  | neoforge.mods.toml | META-INF/ |
| 1.20.1    | `[2,)`         | 20.1.0+          | 17+  | mods.toml     | META-INF/ |

**Important Notes:**
- **1.20.1-1.20.4:** Uses `mods.toml` (same as Forge)
- **1.20.5+:** Renamed to `neoforge.mods.toml`
- NeoForge is for MC 1.20.1+ only (fork of Forge)

**Dependency Declaration:**
```toml
[[dependencies.modid]]
    modId="neoforge"  # Different from Forge!
    type="required"
    versionRange="[2,)"
```

## Quick Comparison

### Fabric

**Pros:**
- Version-agnostic metadata format
- Fast updates to new MC versions (hours/days)
- Lightweight and developer-friendly
- Excellent mixin support
- Simple JSON configuration

**Cons:**
- Smaller ecosystem than Forge
- Less tooling for complex features
- Requires Fabric API for most functionality

**Best For:**
- Quick prototyping
- Modern Minecraft versions
- Client-side mods
- Performance-focused mods

### Forge

**Pros:**
- Largest mod ecosystem
- Extensive documentation
- Mature tooling and APIs
- Wide version support (1.6+)
- Strong community

**Cons:**
- Slower updates to new MC versions
- Heavier than Fabric
- More complex setup
- Mixins not officially supported

**Best For:**
- Large mod projects
- Maximum compatibility
- Complex gameplay mods
- Older MC versions (pre-1.20)

### NeoForge

**Pros:**
- Community-driven (no single owner)
- Modern codebase
- Compatible with Forge mods (mostly)
- Enhanced development practices
- Active development

**Cons:**
- Newer (less mature)
- Only for MC 1.20.1+
- Smaller ecosystem than Forge
- Migration overhead from Forge

**Best For:**
- New projects (MC 1.20.1+)
- Community-focused development
- Modern Forge alternative
- Future-proofing

## Multi-Loader Support

For projects supporting multiple loaders:

### Metadata File Locations

```
multi-loader-project/
├── fabric/
│   └── src/main/resources/
│       └── fabric.mod.json                    # JAR root
├── forge/
│   └── src/main/resources/
│       └── META-INF/
│           └── mods.toml                      # META-INF/
└── neoforge/
    └── src/main/resources/
        └── META-INF/
            └── neoforge.mods.toml             # META-INF/ (1.20.5+)
            └── mods.toml                      # META-INF/ (1.20.1-1.20.4)
```

### Dependency Version Ranges

**Fabric (fabric.mod.json):**
```json
{
  "depends": {
    "fabricloader": ">=0.14.0",
    "minecraft": "~1.20.1",
    "java": ">=17"
  }
}
```

**Forge/NeoForge (mods.toml):**
```toml
[[dependencies.modid]]
    modId="minecraft"
    versionRange="[1.20.1,1.21)"  # Maven range format
```

### Version Range Syntax

**Maven (Forge/NeoForge):**
- `[1.0,)` - 1.0 or higher (inclusive)
- `[1.0,2.0)` - 1.0 to 2.0 (2.0 exclusive)
- `(1.0,2.0)` - Between 1.0 and 2.0 (both exclusive)
- `[1.0]` - Exactly 1.0

**Fabric:**
- `>=0.14.0` - 0.14.0 or higher
- `~1.20.1` - Minecraft 1.20.1 (tilde notation)
- `*` - Any version

## File Size Comparison

Typical empty mod JAR sizes (no code, just metadata):

| Loader   | JAR Size | Main Files |
|----------|----------|------------|
| Fabric   | ~2-5 KB  | fabric.mod.json |
| Forge    | ~3-7 KB  | META-INF/mods.toml, MANIFEST.MF |
| NeoForge | ~3-7 KB  | META-INF/neoforge.mods.toml, MANIFEST.MF |

## Build Time Comparison

Average build times for small mod (10 classes):

| Loader   | Clean Build | Incremental | Notes |
|----------|-------------|-------------|-------|
| Fabric   | 5-10s       | 1-3s        | Fast compilation |
| Forge    | 15-30s      | 3-7s        | Decompilation overhead |
| NeoForge | 15-30s      | 3-7s        | Similar to Forge |

## Development Environment

### IDE Support

**IntelliJ IDEA:**
- ✅ Fabric - Excellent
- ✅ Forge - Excellent
- ✅ NeoForge - Excellent

**Eclipse:**
- ⚠️ Fabric - Good (some setup required)
- ✅ Forge - Excellent
- ✅ NeoForge - Excellent

**VS Code:**
- ⚠️ Fabric - Limited
- ⚠️ Forge - Limited
- ⚠️ NeoForge - Limited

### Gradle Plugin

**Fabric:** Fabric Loom
```kotlin
plugins {
    id("fabric-loom") version "1.5+"
}
```

**Forge:** ForgeGradle
```kotlin
plugins {
    id("net.minecraftforge.gradle") version "6.0+"
}
```

**NeoForge:** NeoGradle
```kotlin
plugins {
    id("net.neoforged.gradle") version "7.0+"
}
```

## Migration Paths

### Forge → NeoForge (1.20.1+)

**Changes Required:**
1. Update Gradle plugin: ForgeGradle → NeoGradle
2. Update metadata file: `mods.toml` → `neoforge.mods.toml` (1.20.5+)
3. Change dependency: `modId="forge"` → `modId="neoforge"`
4. Update loader version: Version-specific → `[2,)`
5. Update imports: `net.minecraftforge` → `net.neoforged`

**Estimated Effort:** 1-4 hours (small mod), 1-3 days (large mod)

### Forge/NeoForge → Fabric

**Major Differences:**
1. No event bus - Use Fabric API callbacks
2. No @Mod annotation - Use fabric.mod.json entrypoints
3. Different registry system
4. Mixins instead of Access Transformers
5. Different resource loading

**Estimated Effort:** 1-2 weeks (complete rewrite recommended)

### Fabric → Forge/NeoForge

**Major Differences:**
1. Add event bus handlers
2. Remove mixins (use ATs if possible)
3. Different registry system
4. Add @Mod annotation
5. Update resource loading

**Estimated Effort:** 1-2 weeks (significant refactoring)

## Recommended Loader by Use Case

| Use Case | Recommended Loader | Reason |
|----------|-------------------|--------|
| New mod (1.20+) | NeoForge or Fabric | Modern, active development |
| New mod (1.16-1.19) | Forge | Largest ecosystem |
| Client-side mod | Fabric | Lightweight, fast updates |
| Performance mod | Fabric | Better performance tooling |
| Large gameplay mod | Forge/NeoForge | More comprehensive APIs |
| Multi-version (1.16-1.21) | Forge | Widest version support |
| Multi-loader | Fabric + NeoForge | Best of both worlds |
| Quick prototype | Fabric | Fastest setup |
| Maximum compatibility | Forge | Largest user base |

## Common Gotchas

### Fabric
- ❌ Forgetting to add Fabric API dependency
- ❌ Using uppercase in mod ID
- ❌ Missing schema version
- ❌ Client code not in client entrypoint
- ❌ Mixin config not in fabric.mod.json

### Forge/NeoForge
- ❌ Wrong loader version for MC version
- ❌ Using `forge` instead of `neoforge` dependency
- ❌ Wrong metadata filename (mods.toml vs neoforge.mods.toml)
- ❌ Missing FMLModType in MANIFEST.MF for libraries
- ❌ Invalid Maven version range syntax

### All Loaders
- ❌ Uppercase in resource paths
- ❌ Wrong mod ID format
- ❌ Resources in wrong directory structure
- ❌ Missing required metadata files
- ❌ Version ranges too restrictive

---

**Last Updated:** 2026-02-09
**Minecraft Versions:** 1.16.5 - 1.21.1

For detailed information, see [JAR_STRUCTURE_REFERENCE.md](./JAR_STRUCTURE_REFERENCE.md)
