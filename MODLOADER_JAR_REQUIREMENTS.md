# Mod Loader JAR Requirements

Comprehensive documentation of JAR structure and requirements for Fabric, Forge, and NeoForge mod loaders across different Minecraft versions.

**Last Updated:** 2026-02-09

---

## Table of Contents

1. [Java Version Requirements](#java-version-requirements)
2. [Fabric Mod JARs](#fabric-mod-jars)
3. [Forge Mod JARs](#forge-mod-jars)
4. [NeoForge Mod JARs](#neoforge-mod-jars)
5. [Common Asset Structure](#common-asset-structure)
6. [Common Data Structure](#common-data-structure)
7. [Version-Specific Changes](#version-specific-changes)
8. [Validation Checklist](#validation-checklist)
9. [Common Mistakes](#common-mistakes)

---

## Java Version Requirements

| Minecraft Version | Java Version | Notes |
|------------------|--------------|-------|
| 1.20.1 | Java 17+ | Minimum Java 17 required |
| 1.20.4 | Java 17+ | Minimum Java 17 required |
| 1.21 | Java 21+ | **Breaking change:** Java 21 required starting with 1.20.5 |
| 1.21.1 | Java 21+ | Java 21 required |

**Important:** The transition to Java 21 occurs at Minecraft 1.20.5. All versions from 1.20.5 onwards require Java 21.

---

## Fabric Mod JARs

### JAR Structure

```
fabric-mod-1.0.0.jar
├── fabric.mod.json                    # Required: Fabric metadata
├── com/example/modid/                 # Compiled Java classes
│   ├── ExampleMod.class
│   ├── items/
│   │   └── CustomItem.class
│   └── blocks/
│       └── CustomBlock.class
├── assets/modid/                      # Asset files (textures, models, lang)
│   ├── blockstates/
│   ├── models/
│   ├── textures/
│   └── lang/
└── data/modid/                        # Data files (recipes, loot tables, tags)
    ├── recipes/
    ├── loot_tables/
    └── tags/
```

### fabric.mod.json Specification

**Location:** Root of JAR (not in META-INF)

**Schema Version:** Always use `"schemaVersion": 1` for modern Fabric Loader (0.4.0+)

#### Required Fields

```json
{
  "schemaVersion": 1,
  "id": "examplemod",
  "version": "1.0.0"
}
```

- **schemaVersion**: Must be `1` (integer)
- **id**: Mod identifier (2-64 characters, lowercase letters, digits, underscores)
- **version**: Version string (Semantic Versioning 2.0.0 recommended)

#### Common Optional Fields

```json
{
  "schemaVersion": 1,
  "id": "examplemod",
  "version": "1.0.0",
  "name": "Example Mod",
  "description": "An example mod for Minecraft",
  "authors": ["Author Name"],
  "contributors": ["Contributor Name"],
  "contact": {
    "homepage": "https://example.com",
    "sources": "https://github.com/example/mod",
    "issues": "https://github.com/example/mod/issues"
  },
  "license": "MIT",
  "icon": "assets/examplemod/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": ["com.example.ExampleMod"],
    "client": ["com.example.ExampleModClient"],
    "server": ["com.example.ExampleModServer"]
  },
  "mixins": [
    "examplemod.mixins.json"
  ],
  "depends": {
    "fabricloader": ">=0.15.0",
    "minecraft": "1.20.1",
    "java": ">=17"
  },
  "recommends": {
    "fabric-api": "*"
  },
  "suggests": {
    "optionalmod": "*"
  },
  "breaks": {
    "incompatiblemod": "*"
  },
  "conflicts": {
    "conflictingmod": "*"
  }
}
```

#### Environment Values

- `"*"`: Both client and server (default)
- `"client"`: Client-only mod
- `"server"`: Server-only mod

#### Entrypoints

Standard entrypoint types:

- **main**: Implements `ModInitializer` - runs on both sides
- **client**: Implements `ClientModInitializer` - runs on client only
- **server**: Implements `DedicatedServerModInitializer` - runs on dedicated server only

#### Version-Specific Dependencies

**Minecraft 1.20.1 / 1.20.4:**
```json
"depends": {
  "fabricloader": ">=0.15.0",
  "minecraft": "1.20.x",
  "java": ">=17"
}
```

**Minecraft 1.21 / 1.21.1:**
```json
"depends": {
  "fabricloader": ">=0.15.0",
  "minecraft": "1.21.x",
  "java": ">=21"
}
```

**Note:** Fabric Loader 0.15.11 is the latest stable version as of February 2026 and supports all versions from 1.20.1 through 1.21.11.

### Nested JARs

Fabric supports nested JARs for bundling dependencies:

```json
{
  "jars": [
    {
      "file": "libs/dependency-1.0.0.jar"
    }
  ]
}
```

---

## Forge Mod JARs

### JAR Structure

```
forge-mod-1.0.0.jar
├── META-INF/
│   ├── mods.toml                      # Required: Forge metadata
│   └── MANIFEST.MF                    # Required: JAR manifest
├── com/example/modid/                 # Compiled Java classes
│   ├── ExampleMod.class
│   └── ...
├── assets/modid/                      # Asset files
│   └── ...
└── data/modid/                        # Data files
    └── ...
```

### mods.toml Specification

**Location:** `META-INF/mods.toml`

**Format:** TOML (Tom's Obvious Minimal Language)

#### Complete Structure

```toml
# Non-mod-specific properties (JAR-level)
modLoader = "javafml"
loaderVersion = "[46,)"
license = "MIT"
issueTrackerURL = "https://github.com/example/mod/issues"

# Mod properties
[[mods]]
modId = "examplemod"
version = "${file.jarVersion}"
displayName = "Example Mod"
description = '''
A multi-line description
of the example mod.
'''
authors = "Author Name"
credits = "Contributors"
logoFile = "assets/examplemod/logo.png"
displayURL = "https://example.com"
updateJSONURL = "https://example.com/update.json"

# Dependencies
[[dependencies.examplemod]]
modId = "forge"
mandatory = true
versionRange = "[46,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.examplemod]]
modId = "minecraft"
mandatory = true
versionRange = "[1.20.1]"
ordering = "NONE"
side = "BOTH"
```

#### Required Global Properties

- **modLoader**: Language loader (`"javafml"` for Java-based Forge mods)
- **loaderVersion**: Maven version range for Forge Loader
- **license**: SPDX identifier or license URL

#### Required Mod Properties

Under each `[[mods]]` section:

- **modId**: Unique identifier (lowercase, 2-64 characters, alphanumeric + underscores)
- **version**: Mod version (can use `${file.jarVersion}` to pull from MANIFEST.MF)
- **displayName**: Human-readable mod name
- **description**: Mod description (supports triple-quoted multi-line strings)

#### Optional Mod Properties

- **authors**: Mod author(s)
- **credits**: Attribution
- **logoFile**: Path to logo image (valid characters: `[a-z0-9_-.]`)
- **displayURL**: Mod homepage
- **updateJSONURL**: Update checker JSON location

#### Dependency Format

```toml
[[dependencies.<modid>]]
modId = "dependencymod"
mandatory = true              # Boolean: true = required, false = optional
versionRange = "[1.0,)"       # Maven version range (empty = any version)
ordering = "NONE"             # Load order: "BEFORE", "AFTER", or "NONE"
side = "BOTH"                 # Physical side: "CLIENT", "SERVER", or "BOTH"
```

#### Version-Specific Loader Versions

**Minecraft 1.20.1:**
- Forge version: `47.x.x` (e.g., 47.4.10 recommended, 47.4.16 latest)
- `loaderVersion = "[46,)"`

**Minecraft 1.20.4:**
- Forge version: `49.x.x` (e.g., 49.1.0, 49.2.0)
- `loaderVersion = "[47,)"`

### META-INF/MANIFEST.MF

While Forge/NeoForge generate this automatically during build, key attributes include:

```
Manifest-Version: 1.0
Specification-Title: examplemod
Specification-Vendor: Author Name
Specification-Version: 1
Implementation-Title: Example Mod
Implementation-Version: 1.0.0
Implementation-Vendor: Author Name
```

For library-type mods:
```
FMLModType: LIBRARY
Automatic-Module-Name: com.example.modid
```

---

## NeoForge Mod JARs

### JAR Structure

```
neoforge-mod-1.0.0.jar
├── META-INF/
│   ├── neoforge.mods.toml             # Required: NeoForge metadata (1.20.5+)
│   │   OR
│   ├── mods.toml                      # Required: NeoForge metadata (1.20.4 only)
│   └── MANIFEST.MF                    # Required: JAR manifest
├── com/example/modid/                 # Compiled Java classes
│   └── ...
├── assets/modid/                      # Asset files
│   └── ...
└── data/modid/                        # Data files
    └── ...
```

### Critical Version Difference

**Minecraft 1.20.4 and earlier:**
- File name: `mods.toml`
- Location: `META-INF/mods.toml`

**Minecraft 1.20.5 and later (1.21, 1.21.1):**
- File name: `neoforge.mods.toml`
- Location: `META-INF/neoforge.mods.toml`

**Reason for rename:** Distinguishes NeoForge from Lex's Forge, preventing confusion since mods are not cross-compatible.

### neoforge.mods.toml Specification

**Format:** TOML (similar to Forge but with NeoForge-specific values)

#### Complete Structure

```toml
# Non-mod-specific properties (JAR-level)
modLoader = "javafml"
loaderVersion = "[1,)"
license = "MIT"
showAsResourcePack = false
showAsDataPack = false
issueTrackerURL = "https://github.com/example/mod/issues"

# Mod properties
[[mods]]
modId = "examplemod"
namespace = "examplemod"
version = "${file.jarVersion}"
displayName = "Example Mod"
description = '''
A multi-line description
of the example mod.
'''
authors = "Author Name"
credits = "Contributors"
logoFile = "assets/examplemod/logo.png"
logoBlur = true
displayURL = "https://example.com"
updateJSONURL = "https://example.com/update.json"

# Feature flags
features = { javaVersion = "[17,)" }

# Dependencies
[[dependencies.examplemod]]
modId = "neoforge"
type = "required"
versionRange = "[1.0,)"
ordering = "NONE"
side = "BOTH"

[[dependencies.examplemod]]
modId = "minecraft"
type = "required"
versionRange = "[1.21]"
ordering = "NONE"
side = "BOTH"
```

#### Required Fields

- **license**: SPDX identifier or license string (mandatory)
- **modId**: Unique mod identifier (lowercase, 2-64 characters)

#### Global Properties

| Property | Type | Default | Purpose |
|----------|------|---------|---------|
| `modLoader` | string | `"javafml"` | Language loader identifier |
| `loaderVersion` | string | `""` | Maven version range for loader |
| `license` | string | **mandatory** | Licensing information |
| `showAsResourcePack` | boolean | `false` | Display as separate resource pack |
| `showAsDataPack` | boolean | `false` | Display as separate data pack |
| `issueTrackerURL` | string | — | Bug report location |

#### Mod Properties

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `modId` | string | **mandatory** | Mod identifier |
| `namespace` | string | value of `modId` | Resource namespace |
| `version` | string | `"1"` | Mod version |
| `displayName` | string | value of `modId` | UI-facing name |
| `description` | string | `'''MISSING DESCRIPTION'''` | Mod description |
| `authors` | string | — | Creator attribution |
| `credits` | string | — | Contributor attribution |
| `logoFile` | string | — | Logo image path |
| `logoBlur` | boolean | `true` | Logo rendering mode |
| `displayURL` | string | — | Mod homepage |
| `updateJSONURL` | string | — | Update checker endpoint |

#### Feature Flags

```toml
features = { javaVersion = "[17,)" }
```

Supported features:
- **javaVersion**: Maven version range for Java requirements

#### Dependency Format

```toml
[[dependencies.<modid>]]
modId = "dependencymod"
type = "required"             # "required", "optional", "incompatible", "discouraged"
reason = "User-facing explanation"
versionRange = "[1.0,)"       # Maven version range
ordering = "NONE"             # "BEFORE", "AFTER", or "NONE"
side = "BOTH"                 # "CLIENT", "SERVER", or "BOTH"
referralUrl = "https://example.com/download"
```

**Dependency Types:**
- `"required"`: Mandatory dependency
- `"optional"`: Suggested but not required
- `"incompatible"`: Cannot load with this mod
- `"discouraged"`: Can load but not recommended

#### Version-Specific Configurations

**Minecraft 1.20.4:**
```toml
# Use mods.toml (not neoforge.mods.toml)
modLoader = "javafml"
loaderVersion = "[20.4,)"
features = { javaVersion = "[17,)" }

[[dependencies.examplemod]]
modId = "neoforge"
type = "required"
versionRange = "[20.4,)"
```

**Minecraft 1.21 / 1.21.1:**
```toml
# Use neoforge.mods.toml
modLoader = "javafml"
loaderVersion = "[21,)"
features = { javaVersion = "[21,)" }

[[dependencies.examplemod]]
modId = "neoforge"
type = "required"
versionRange = "[21,)"
```

### Key Differences from Forge

1. **File name:** `neoforge.mods.toml` vs `mods.toml` (1.20.5+)
2. **Dependency format:** Uses `type` instead of `mandatory`
3. **Branding:** Depends on `neoforge` instead of `forge`
4. **Feature flags:** Explicit `features` table for Java version
5. **Additional display options:** `showAsResourcePack`, `showAsDataPack`, `logoBlur`, `namespace`

---

## Common Asset Structure

All mod loaders use the same asset directory structure:

```
assets/modid/
├── blockstates/
│   ├── example_block.json
│   └── another_block.json
├── models/
│   ├── block/
│   │   ├── example_block.json
│   │   └── another_block.json
│   └── item/
│       ├── example_item.json
│       └── block_item.json
├── textures/
│   ├── block/
│   │   ├── example_block.png
│   │   └── another_block.png
│   └── item/
│       ├── example_item.png
│       └── tool_item.png
├── lang/
│   ├── en_us.json
│   ├── es_es.json
│   └── fr_fr.json
└── sounds/
    └── example_sound.ogg
```

### Asset File Examples

#### Blockstate (blockstates/example_block.json)

```json
{
  "variants": {
    "": {
      "model": "modid:block/example_block"
    }
  }
}
```

#### Block Model (models/block/example_block.json)

```json
{
  "parent": "minecraft:block/cube_all",
  "textures": {
    "all": "modid:block/example_block"
  }
}
```

#### Item Model (models/item/example_item.json)

```json
{
  "parent": "minecraft:item/generated",
  "textures": {
    "layer0": "modid:item/example_item"
  }
}
```

#### Language File (lang/en_us.json)

```json
{
  "item.modid.example_item": "Example Item",
  "block.modid.example_block": "Example Block",
  "itemGroup.modid.tab": "Example Mod"
}
```

---

## Common Data Structure

All mod loaders use the same data directory structure:

```
data/modid/
├── recipes/
│   ├── example_item.json
│   ├── example_block.json
│   └── smelting/
│       └── example_ingot.json
├── loot_tables/
│   ├── blocks/
│   │   ├── example_block.json
│   │   └── another_block.json
│   └── entities/
│       └── example_entity.json
├── tags/
│   ├── blocks/
│   │   ├── mineable/
│   │   │   ├── pickaxe.json
│   │   │   └── axe.json
│   │   └── custom_tag.json
│   └── items/
│       ├── custom_items.json
│       └── tools.json
├── advancements/
│   └── example_advancement.json
└── worldgen/
    └── ...
```

### Data File Examples

#### Crafting Recipe (recipes/example_item.json)

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "###",
    "# #",
    "###"
  ],
  "key": {
    "#": {
      "item": "minecraft:stick"
    }
  },
  "result": {
    "item": "modid:example_item"
  }
}
```

#### Block Loot Table (loot_tables/blocks/example_block.json)

```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "modid:example_block"
        }
      ],
      "conditions": [
        {
          "condition": "minecraft:survives_explosion"
        }
      ]
    }
  ]
}
```

#### Tag (tags/items/custom_items.json)

**Minecraft 1.20.1 - 1.20.4:**
```json
{
  "replace": false,
  "values": [
    "modid:example_item",
    "modid:another_item"
  ]
}
```

**Minecraft 1.21+:** (singular directory names)
```
data/modid/tags/item/custom_items.json
```

---

## Version-Specific Changes

### Minecraft 1.20.1 → 1.20.4

**Major Changes:**
- Networking system refactored (affects Forge/NeoForge)
- NeoForge switched to Fabric's Mixin implementation
- Capabilities and networking APIs changed significantly in NeoForge

**Compatibility:**
- Forge 1.20.4 mods are NOT compatible with NeoForge 1.20.4
- Fabric mods generally require minor updates

### Minecraft 1.20.4 → 1.21

**Breaking Changes:**

1. **Java Version:** Java 21 required (was Java 17)

2. **Enchantment System Overhaul:**
   - Data-driven enchantments
   - Use `EnchantmentHelper` methods instead of direct checks
   - Example: `EnchantmentHelper#onTargetDamaged` for Thorns
   - Example: `EnchantmentHelper#modifyKnockback` for Knockback

3. **Data Pack Structure:**
   - **Singular directory names:** `tags/blocks` → `tags/block`
   - Changes apply to:
     - `tags/blocks` → `tags/block`
     - `tags/entity_types` → `tags/entity_type`
     - `tags/functions` → `tags/function`
   - Similar changes for advancements, functions, item modifiers, loot tables, predicates, recipes, structures

4. **Fabric API Changes:**
   - `FabricDimensions` removed (use `Entity#teleportTo`)
   - `HudRenderCallback` now passes `RenderTickCounter` instead of `tickDelta`
   - Deprecated module removed: `fabric-models-v0`
   - Registry sync freezes earlier (register during `ModInitializer`)

5. **NeoForge File Rename:**
   - `mods.toml` → `neoforge.mods.toml` (starting 1.20.5)

### Minecraft 1.21 → 1.21.1

**Minor Changes:**
- Mostly maintenance updates
- API additions, few breaking changes
- Same Java 21 requirement

---

## Validation Checklist

Use this checklist to verify generated JARs are valid:

### All Loaders

- [ ] Correct Java version used for compilation (17 for 1.20.1-1.20.4, 21 for 1.21+)
- [ ] Java classes in proper package structure (`com/modid/package/Class.class`)
- [ ] Assets in `assets/modid/` directory
- [ ] Data files in `data/modid/` directory
- [ ] No duplicate classes or resources
- [ ] File naming follows conventions (lowercase, underscores for spaces)

### Fabric-Specific

- [ ] `fabric.mod.json` exists at JAR root (not in META-INF)
- [ ] `schemaVersion` is `1` (integer)
- [ ] `id` field matches mod identifier (lowercase, 2-64 chars)
- [ ] `version` field present
- [ ] `entrypoints` correctly reference existing classes
- [ ] `depends` specifies minimum Fabric Loader version
- [ ] `depends` includes Java version (`>=17` or `>=21`)
- [ ] Environment set correctly (`*`, `client`, or `server`)
- [ ] Entrypoint classes implement correct interfaces
  - `main`: `ModInitializer`
  - `client`: `ClientModInitializer`
  - `server`: `DedicatedServerModInitializer`

### Forge-Specific

- [ ] `META-INF/mods.toml` exists
- [ ] `modLoader` is `"javafml"`
- [ ] `loaderVersion` matches Forge version range
- [ ] `license` field present
- [ ] Each `[[mods]]` section has `modId`, `version`, `displayName`, `description`
- [ ] `modId` matches `@Mod` annotation value in code
- [ ] Dependencies include `forge` and `minecraft`
- [ ] `mandatory` field used (not `type`)
- [ ] `versionRange` uses Maven format
- [ ] Main mod class has `@Mod("modid")` annotation

### NeoForge-Specific

- [ ] **1.20.4:** `META-INF/mods.toml` exists
- [ ] **1.21+:** `META-INF/neoforge.mods.toml` exists (note the prefix)
- [ ] `modLoader` is `"javafml"`
- [ ] `loaderVersion` matches NeoForge version range
- [ ] `license` field present (mandatory)
- [ ] Each `[[mods]]` section has `modId`
- [ ] Dependencies use `type` field (`required`, `optional`, etc.)
- [ ] Dependencies reference `neoforge` (not `forge`)
- [ ] `features` table includes `javaVersion` if needed
- [ ] Main mod class has `@Mod("modid")` annotation

### Asset/Data Structure

- [ ] Blockstates reference models that exist
- [ ] Models reference textures that exist
- [ ] All texture files are PNG format
- [ ] Language files use correct format (`item.modid.name`)
- [ ] Recipe JSON is valid
- [ ] Loot tables reference items/blocks that exist
- [ ] Tags use correct directory structure:
  - **1.20.1-1.20.4:** `tags/blocks/`, `tags/items/`
  - **1.21+:** `tags/block/`, `tags/item/` (singular)

---

## Common Mistakes

### All Loaders

1. **Wrong Java Version**
   - Using Java 17 to compile for 1.21+ (needs Java 21)
   - Using Java 8 for any modern version

2. **Incorrect Package Structure**
   - Flat directory instead of nested packages
   - Missing package declarations in Java files
   - Package name doesn't match directory structure

3. **Case Sensitivity**
   - Using uppercase in mod IDs
   - Mismatched case in file references (Windows dev → Linux server)

4. **Missing Assets/Data**
   - Blockstate references non-existent model
   - Model references non-existent texture
   - Recipe references non-existent item

5. **Invalid JSON**
   - Missing commas
   - Trailing commas (invalid in strict JSON)
   - Single quotes instead of double quotes

### Fabric-Specific

1. **Wrong fabric.mod.json Location**
   - Placing in `META-INF/` instead of JAR root
   - Common mistake from Forge/NeoForge convention

2. **Wrong schemaVersion Type**
   - Using `"1"` (string) instead of `1` (integer)

3. **Entrypoint Class Not Found**
   - Class path doesn't match actual package structure
   - Typo in class name
   - Class not implementing required interface

4. **Missing Java Dependency**
   - Not specifying `"java": ">=17"` or `">=21"`
   - Mod loads but crashes with "Unsupported class file major version"

### Forge-Specific

1. **Wrong mods.toml Location**
   - Placing outside `META-INF/`

2. **Missing @Mod Annotation**
   - Mod won't be discovered at runtime
   - `modId` in annotation must match `mods.toml`

3. **Using 'type' Instead of 'mandatory'**
   - NeoForge uses `type`, Forge uses `mandatory`

4. **Incorrect Version Range**
   - Not using Maven format: `[min,max)`, `[min,)`, `[exact]`
   - Examples:
     - `[46,)` = 46 or higher
     - `[46,50)` = 46 to 49 (not including 50)
     - `[1.20.1]` = exactly 1.20.1

5. **Wrong Loader Version**
   - Using NeoForge version range for Forge
   - Using version numbers instead of Maven ranges

### NeoForge-Specific

1. **Wrong File Name**
   - Using `mods.toml` for 1.21+ (should be `neoforge.mods.toml`)
   - Using `neoforge.mods.toml` for 1.20.4 (should be `mods.toml`)

2. **Using 'mandatory' Instead of 'type'**
   - Forge uses `mandatory`, NeoForge uses `type`

3. **Depending on 'forge' Instead of 'neoforge'**
   - Must reference `neoforge` in dependencies
   - Mods are not cross-compatible

4. **Missing License Field**
   - License is mandatory in NeoForge (optional in Forge)

5. **Wrong Java Version in features**
   - Specifying `[17,)` for 1.21+ (should be `[21,)`)

### Asset/Data Structure

1. **Wrong Tag Directory Names (1.21+)**
   - Using `tags/blocks` instead of `tags/block`
   - Using `tags/items` instead of `tags/item`
   - Using `tags/entity_types` instead of `tags/entity_type`

2. **Incorrect Namespace**
   - Using `minecraft:` for mod resources (should be `modid:`)
   - Forgetting namespace entirely in JSON references

3. **Missing Loot Tables**
   - Block breaks but drops nothing
   - Always create loot table for blocks

4. **Invalid Model Parents**
   - Referencing non-existent parent models
   - Using wrong parent type (block parent for item)

---

## References

### Official Documentation

- [Fabric Wiki - fabric.mod.json Specification](https://wiki.fabricmc.net/documentation:fabric_mod_json)
- [Fabric Documentation - Project Structure](https://docs.fabricmc.net/develop/getting-started/project-structure)
- [Forge Documentation - Mod Files (1.20.1)](https://docs.minecraftforge.net/en/1.20.1/gettingstarted/modfiles/)
- [NeoForged Documentation - Mod Files](https://docs.neoforged.net/docs/gettingstarted/modfiles/)
- [NeoForged Documentation - Versioning](https://docs.neoforged.net/docs/gettingstarted/versioning/)

### Version Information

- [Fabric for Minecraft 1.20.3 & 1.20.4](https://fabricmc.net/2023/11/30/1203.html)
- [Fabric for Minecraft 1.21 & 1.21.1](https://fabricmc.net/2024/05/31/121.html)
- [NeoForge 21.0 for Minecraft 1.21](https://neoforged.net/news/21.0release/)
- [Minecraft 1.20.5/6 → 1.21 Mod Migration Primer](https://gist.github.com/ChampionAsh5357/d895a7b1a34341e19c80870720f9880f)

### Java Requirements

- [Minecraft Wiki - Java Version Requirements](https://minecraft.wiki/w/Tutorial:Update_Java)
- [Which Java version for Minecraft server? Guide 2025](https://nexus-games.com/us/blog/which-java-version-minecraft-server/)

### Community Resources

- [Forge Community Wiki - Mods.toml](https://forge.gemwire.uk/wiki/Mods.toml)
- [NeoForge vs Forge: Modding Comparison](https://madelinemiller.dev/blog/forge-vs-fabric/)

---

## Appendix: Example Complete JARs

### Example Fabric JAR (1.21.1)

```
example-fabric-1.0.0.jar
├── fabric.mod.json
├── com/
│   └── example/
│       ├── ExampleMod.class
│       ├── items/
│       │   └── CustomItem.class
│       └── blocks/
│           └── CustomBlock.class
├── assets/
│   └── examplemod/
│       ├── icon.png
│       ├── blockstates/
│       │   └── custom_block.json
│       ├── models/
│       │   ├── block/
│       │   │   └── custom_block.json
│       │   └── item/
│       │       ├── custom_item.json
│       │       └── custom_block.json
│       ├── textures/
│       │   ├── block/
│       │   │   └── custom_block.png
│       │   └── item/
│       │       └── custom_item.png
│       └── lang/
│           └── en_us.json
└── data/
    └── examplemod/
        ├── recipe/
        │   ├── custom_item.json
        │   └── custom_block.json
        ├── loot_table/
        │   └── blocks/
        │       └── custom_block.json
        └── tags/
            ├── block/
            │   └── mineable/
            │       └── pickaxe.json
            └── item/
                └── custom_items.json
```

### Example NeoForge JAR (1.21.1)

```
example-neoforge-1.0.0.jar
├── META-INF/
│   ├── neoforge.mods.toml
│   └── MANIFEST.MF
├── com/
│   └── example/
│       ├── ExampleMod.class
│       ├── items/
│       │   └── CustomItem.class
│       └── blocks/
│           └── CustomBlock.class
├── assets/
│   └── examplemod/
│       └── (same structure as Fabric)
└── data/
    └── examplemod/
        └── (same structure as Fabric)
```

---

**End of Document**
