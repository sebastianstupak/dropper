# Minecraft Modding Research & Multi-Loader Architecture

**Last Updated**: February 2026
**Author**: Dropper Development Team
**Purpose**: Comprehensive guide to Minecraft modding platforms and Dropper's multi-loader architecture

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Mod Loader Deep Dive](#mod-loader-deep-dive)
3. [Block Creation Workflows](#block-creation-workflows)
4. [Dropper Architecture Analysis](#dropper-architecture-analysis)
5. [Best Practices & Patterns](#best-practices--patterns)
6. [Migration Guides](#migration-guides)
7. [Troubleshooting](#troubleshooting)
8. [References](#references)

---

## Executive Summary

### Key Findings

1. **NeoForge is the future** - 18 months after 1.21's release, NeoForge has accumulated 16,000+ mods with growth rates surpassing Forge for 1.20.1
2. **Asset reuse is critical** - Blockstates, models, and textures are identical across loaders until Minecraft changes them
3. **Registration patterns differ significantly** - Each loader has distinct registration mechanisms that must be abstracted
4. **Dropper's architecture is sound** - Our layered approach aligns perfectly with modding best practices

### Recommendations for Dropper

- ✅ **Prioritize NeoForge** in documentation and examples
- ✅ **Complete block generator templates** with working implementations
- ✅ **Expand block types** beyond basic/ore/pillar
- ✅ **Add entity and item generation** following the same patterns
- ⚠️ **Consider dropping legacy Forge** for 1.20.4+ versions

---

## Mod Loader Deep Dive

### Fabric

**Philosophy**: Minimal, lightweight, fast-updating mod loader focused on modern Minecraft development.

#### Technical Architecture
- **Core**: Fabric Loader (minimal injection framework)
- **API**: Fabric API (optional but recommended)
- **Build System**: Gradle with Loom plugin
- **Mapping**: Yarn mappings (community-driven)

#### Strengths
1. **Update Speed**:
   - Updates to snapshot versions within hours
   - First to support new Minecraft releases
   - Minimal breaking changes between versions

2. **Performance**:
   - Lightweight injection (< 1MB)
   - Minimal runtime overhead
   - Optimized for client-side mods

3. **Modern Development**:
   - Clean, documented API
   - Mixin-based modification system
   - Active Discord community

#### Weaknesses
1. **Smaller mod ecosystem** (though growing)
2. **Less cross-mod compatibility** infrastructure
3. **Fewer "big tech mods" historically

#### Block Registration Pattern
```java
package com.example.fabric;

import net.fabricmc.api.ModInitializer;
import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.MapColor;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class ExampleMod implements ModInitializer {
    public static final Block EXAMPLE_BLOCK = new Block(
        AbstractBlock.Settings.create()
            .mapColor(MapColor.STONE_GRAY)
            .strength(3.0f, 3.0f)
            .requiresTool()
            .sounds(BlockSoundGroup.STONE)
    );

    @Override
    public void onInitialize() {
        // Register block
        Registry.register(
            Registries.BLOCK,
            Identifier.of("example", "example_block"),
            EXAMPLE_BLOCK
        );

        // Register block item
        Registry.register(
            Registries.ITEM,
            Identifier.of("example", "example_block"),
            new BlockItem(EXAMPLE_BLOCK, new Item.Settings())
        );
    }
}
```

#### When to Use Fabric
- **Performance-focused mods** (optimization, graphics)
- **Client-side only mods** (HUDs, minimap, shaders)
- **Latest Minecraft versions** (snapshots, bleeding edge)
- **Small to medium mod count** (< 50 mods)

---

### Forge (Legacy)

**Philosophy**: Feature-rich, heavyweight mod loader with extensive compatibility infrastructure.

#### Technical Architecture
- **Core**: MinecraftForge (extensive hooks)
- **Build System**: Gradle with ForgeGradle
- **Mapping**: MCP/SRG mappings
- **Registry**: DeferredRegister system

#### Strengths
1. **Ecosystem**:
   - Largest historical mod library
   - Most modpacks built on Forge
   - Extensive cross-mod APIs

2. **Features**:
   - Built-in networking
   - Configuration systems
   - Event bus architecture
   - Capability system

3. **Stability**:
   - Well-tested on older versions
   - Proven for large modpacks
   - Strong backward compatibility

#### Weaknesses
1. **Update Speed**: Slow to update to new versions
2. **Performance**: Heavier than Fabric
3. **Development**: More complex, older patterns
4. **Community**: Original team moved to NeoForge

#### Block Registration Pattern
```java
package com.example.forge;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
        DeferredRegister.create(ForgeRegistries.BLOCKS, "example");

    public static final DeferredRegister<Item> ITEMS =
        DeferredRegister.create(ForgeRegistries.ITEMS, "example");

    public static final RegistryObject<Block> EXAMPLE_BLOCK =
        BLOCKS.register("example_block", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0f, 3.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
        ));

    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM =
        ITEMS.register("example_block", () ->
            new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties())
        );
}

// In main mod class:
@Mod("example")
public class ExampleMod {
    public ExampleMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
    }
}
```

#### When to Use Forge
- **Older Minecraft versions** (1.19.x and below)
- **Legacy modpack maintenance**
- **Specific Forge-only mods** required
- **NOT RECOMMENDED** for new 1.20.4+ projects

---

### NeoForge (Recommended)

**Philosophy**: Modern evolution of Forge by the original team, focusing on performance and modern development practices.

#### Technical Architecture
- **Core**: NeoForge (refactored Forge codebase)
- **Build System**: Gradle with modular design
- **Mapping**: Mojang official mappings
- **Registry**: Enhanced DeferredRegister system

#### History & Context
- **Split Date**: 2023 (during 1.20 development)
- **Team**: Nearly entire original Forge team
- **Cause**: Internal disagreements, desire for modernization
- **Leadership**: LexManos stayed with Forge; rest joined NeoForged

#### Strengths
1. **Modern Architecture**:
   - Cleaner codebase
   - Better performance (reduced memory, faster loads)
   - Modular design principles

2. **Active Development**:
   - Rapid updates for 1.20.4+
   - Better support for newer versions
   - Active improvement cycle

3. **Growing Ecosystem**:
   - 16,000+ mods on 1.21
   - Many mods dropping Forge-only support
   - Major tech mods migrating (AE2, IE, etc.)

4. **Future-Proof**:
   - Original team expertise
   - Strong community momentum
   - Better long-term trajectory

#### Weaknesses
1. **Incompatible with Forge** (mods don't cross-load)
2. **Smaller library than Forge** (for now)
3. **Breaking changes** from Forge migration

#### Block Registration Pattern
```java
package com.example.neoforge;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModBlocks {
    // Modern NeoForge uses type-specific registers
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks("example");

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems("example");

    public static final DeferredBlock<Block> EXAMPLE_BLOCK =
        BLOCKS.register("example_block", () -> new Block(
            BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(3.0f, 3.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
        ));

    // Simplified block item registration
    public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM =
        ITEMS.registerSimpleBlockItem(EXAMPLE_BLOCK);
}

// In main mod class:
@Mod("example")
public class ExampleMod {
    public ExampleMod(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
```

**Key Improvements over Forge**:
- Type-safe `DeferredBlock<T>` and `DeferredItem<T>`
- Simplified `registerSimpleBlockItem()` helper
- Better IDE autocomplete
- Cleaner separation of concerns

#### When to Use NeoForge
- ✅ **New projects on 1.20.4+** (strongly recommended)
- ✅ **Large content mods** (tech, magic, dimensions)
- ✅ **Forge-like features needed** (capabilities, events)
- ✅ **Future-proofing** your mod

---

## Block Creation Workflows

### Complete Block Creation Checklist

Every block requires these components **regardless of mod loader**:

#### 1. Java Code
- [ ] Block class (properties, behavior)
- [ ] Registration code (loader-specific)
- [ ] Optional: Custom block entity
- [ ] Optional: Custom block state properties

#### 2. Assets (Textures & Models)
- [ ] Block texture: `assets/<modid>/textures/block/<name>.png`
- [ ] Blockstate JSON: `assets/<modid>/blockstates/<name>.json`
- [ ] Block model: `assets/<modid>/models/block/<name>.json`
- [ ] Item model: `assets/<modid>/models/item/<name>.json`

#### 3. Data (Recipes & Loot)
- [ ] Loot table: `data/<modid>/loot_tables/blocks/<name>.json`
- [ ] Optional: Recipes
- [ ] Optional: Tags
- [ ] Optional: Advancements

#### 4. Localization
- [ ] Translation: `assets/<modid>/lang/en_us.json`

---

### Asset File Examples

#### Blockstate JSON (Simple)
```json
{
  "variants": {
    "": {
      "model": "example:block/example_block"
    }
  }
}
```

#### Blockstate JSON (Directional)
```json
{
  "variants": {
    "facing=north": { "model": "example:block/example_block" },
    "facing=south": { "model": "example:block/example_block", "y": 180 },
    "facing=west": { "model": "example:block/example_block", "y": 270 },
    "facing=east": { "model": "example:block/example_block", "y": 90 }
  }
}
```

#### Blockstate JSON (Pillar/Log)
```json
{
  "variants": {
    "axis=x": { "model": "example:block/example_log", "x": 90, "y": 90 },
    "axis=y": { "model": "example:block/example_log" },
    "axis=z": { "model": "example:block/example_log", "x": 90 }
  }
}
```

#### Block Model (Cube All)
```json
{
  "parent": "block/cube_all",
  "textures": {
    "all": "example:block/example_block"
  }
}
```

#### Block Model (Pillar)
```json
{
  "parent": "block/cube_column",
  "textures": {
    "end": "example:block/example_log_top",
    "side": "example:block/example_log"
  }
}
```

#### Item Model (Block)
```json
{
  "parent": "example:block/example_block"
}
```

#### Loot Table (Self-Drop)
```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "example:example_block"
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

#### Loot Table (Ore with Fortune)
```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {
          "type": "minecraft:item",
          "name": "example:ruby",
          "functions": [
            {
              "function": "minecraft:apply_bonus",
              "enchantment": "minecraft:fortune",
              "formula": "minecraft:ore_drops"
            },
            {
              "function": "minecraft:explosion_decay"
            }
          ]
        }
      ]
    }
  ]
}
```

---

## Dropper Architecture Analysis

### Overview

Dropper uses a **layered architecture** that maximizes code and asset reuse while supporting multiple mod loaders and Minecraft versions simultaneously.

### Directory Structure

```
dropper-project/
├── config.yml                      # Project metadata (single source of truth)
│
├── shared/                         # Loader-agnostic & shared code
│   ├── common/                     # Platform-neutral code
│   │   └── src/main/java/
│   │       └── com/modid/
│   │           ├── blocks/         # Block definitions
│   │           ├── items/          # Item definitions
│   │           └── ModMain.java    # Common initialization
│   │
│   ├── fabric/                     # Fabric-specific abstractions
│   │   └── src/main/java/
│   │       └── com/modid/platform/fabric/
│   │           └── *Fabric.java    # Fabric registration helpers
│   │
│   ├── forge/                      # Forge-specific abstractions
│   │   └── src/main/java/
│   │       └── com/modid/platform/forge/
│   │           └── *Forge.java     # Forge registration helpers
│   │
│   └── neoforge/                   # NeoForge-specific abstractions
│       └── src/main/java/
│           └── com/modid/platform/neoforge/
│               └── *NeoForge.java  # NeoForge registration helpers
│
├── versions/                       # Version-specific code and assets
│   ├── shared/                     # Shared asset packs
│   │   ├── v1/                     # Asset pack for MC 1.20.x
│   │   │   ├── config.yml
│   │   │   ├── common/             # 1.20.x-specific code
│   │   │   ├── assets/             # Textures, models, blockstates
│   │   │   │   └── modid/
│   │   │   │       ├── blockstates/
│   │   │   │       ├── models/
│   │   │   │       │   ├── block/
│   │   │   │       │   └── item/
│   │   │   │       └── textures/
│   │   │   │           └── block/
│   │   │   └── data/               # Recipes, tags, loot tables
│   │   │       └── modid/
│   │   │           └── loot_tables/blocks/
│   │   │
│   │   └── v2/                     # Asset pack for MC 1.21.x
│   │       ├── config.yml          # Can inherit from v1
│   │       └── ...
│   │
│   └── 1_20_1/                     # MC 1.20.1 specific
│       ├── config.yml              # References asset pack (v1)
│       ├── common/                 # Version-specific code
│       ├── assets/                 # Overrides for v1 assets
│       └── data/                   # Overrides for v1 data
│
├── build-logic/                    # Gradle convention plugins
│   └── src/main/kotlin/
│       ├── mod.common.gradle.kts
│       └── mod.loader.gradle.kts
│
└── build/                          # Generated JARs
    └── 1_20_1/
        ├── fabric.jar
        ├── forge.jar
        └── neoforge.jar
```

### Layer Hierarchy & Merging

**Build-time source merging** (for 1.20.1 NeoForge):
```
1. shared/common/              (all versions, all loaders)
2. shared/neoforge/            (all versions, NeoForge only)
3. versions/shared/v1/common/  (1.20.x, all loaders)
4. versions/1_20_1/common/     (1.20.1 only, all loaders)
5. versions/1_20_1/neoforge/   (1.20.1 NeoForge only)
```

**Build-time asset merging**:
```
1. versions/shared/v1/assets/  (base assets for 1.20.x)
2. versions/1_20_1/assets/     (overrides for 1.20.1)
3. versions/1_20_1/neoforge/assets/  (loader-specific, rare)
```

**Result**: Later layers override earlier layers, allowing:
- Maximum reuse of common code
- Minimal duplication
- Easy version/loader-specific fixes

---

### Block Creation Flow in Dropper

When you run `dropper create block ruby_ore --type ore`:

#### Step 1: Common Block Class
**Location**: `shared/common/src/main/java/com/modid/blocks/RubyOre.java`

```java
package com.modid.blocks;

/**
 * Ruby Ore Block - Common definition
 *
 * This class contains the shared block properties that work
 * across all mod loaders. Loader-specific registration happens
 * in the platform-specific files.
 */
public class RubyOre {
    public static final String ID = "ruby_ore";

    // Common properties that translate across loaders:
    // - Hardness: 3.0
    // - Resistance: 3.0
    // - Requires tool: true
    // - Sound: Stone
    //
    // The actual Block instance is created in loader-specific code
    // using these shared constants.
}
```

#### Step 2: Fabric Registration
**Location**: `shared/fabric/src/main/java/com/modid/platform/fabric/RubyOreFabric.java`

```java
package com.modid.platform.fabric;

import com.modid.blocks.RubyOre;
import net.minecraft.block.Block;
import net.minecraft.block.AbstractBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.Identifier;

public class RubyOreFabric {
    public static void register() {
        Block block = new Block(
            AbstractBlock.Settings.create()
                .strength(3.0f, 3.0f)
                .requiresTool()
                .sounds(BlockSoundGroup.STONE)
        );

        Registry.register(
            Registries.BLOCK,
            Identifier.of("modid", RubyOre.ID),
            block
        );

        Registry.register(
            Registries.ITEM,
            Identifier.of("modid", RubyOre.ID),
            new BlockItem(block, new Item.Settings())
        );
    }
}
```

#### Step 3: NeoForge Registration
**Location**: `shared/neoforge/src/main/java/com/modid/platform/neoforge/RubyOreNeoForge.java`

```java
package com.modid.platform.neoforge;

import com.modid.blocks.RubyOre;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks("modid");

    public static final DeferredRegister.Items ITEMS =
        DeferredRegister.createItems("modid");

    public static final DeferredBlock<Block> RUBY_ORE =
        BLOCKS.register(RubyOre.ID, () -> new Block(
            BlockBehaviour.Properties.of()
                .strength(3.0f, 3.0f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.STONE)
        ));

    public static final DeferredItem<BlockItem> RUBY_ORE_ITEM =
        ITEMS.registerSimpleBlockItem(RUBY_ORE);
}
```

#### Step 4: Shared Assets
**Location**: `versions/shared/v1/assets/modid/`

**Blockstate** (`blockstates/ruby_ore.json`):
```json
{
  "variants": {
    "": { "model": "modid:block/ruby_ore" }
  }
}
```

**Block Model** (`models/block/ruby_ore.json`):
```json
{
  "parent": "block/cube_all",
  "textures": {
    "all": "modid:block/ruby_ore"
  }
}
```

**Item Model** (`models/item/ruby_ore.json`):
```json
{
  "parent": "modid:block/ruby_ore"
}
```

**Loot Table** (`data/modid/loot_tables/blocks/ruby_ore.json`):
```json
{
  "type": "minecraft:block",
  "pools": [{
    "rolls": 1,
    "entries": [{
      "type": "minecraft:item",
      "name": "modid:ruby",
      "functions": [
        {
          "function": "minecraft:apply_bonus",
          "enchantment": "minecraft:fortune",
          "formula": "minecraft:ore_drops"
        },
        { "function": "minecraft:explosion_decay" }
      ]
    }]
  }]
}
```

**Texture**: `textures/block/ruby_ore.png` (16x16 or 32x32 PNG)

---

### Why This Architecture Works

#### 1. **Loader Abstraction**
- Common code contains **shared logic**
- Platform code handles **registration differences**
- Build system merges them correctly per target

#### 2. **Asset Reuse**
- Assets are **loader-independent**
- Textures, models, blockstates work everywhere
- Only one copy needed for all 3 loaders

#### 3. **Version Management**
- Asset packs group **version-similar assets**
- `v1` for 1.20.x, `v2` for 1.21.x, etc.
- Inheritance allows incremental changes

#### 4. **Minimal Duplication**
- Write block logic **once** in common
- Write registration **once per loader**
- Write assets **once per version family**

#### 5. **Easy Maintenance**
- Fix a bug in common → **all loaders benefit**
- Update texture in v1 → **all 1.20.x versions update**
- Add version-specific fix → **doesn't affect others**

---

## Best Practices & Patterns

### 1. Shared vs Platform-Specific Code

**Rule**: If it compiles identically across loaders, it's shared. If it uses loader-specific APIs, it's platform.

#### ✅ Good: Shared Common Code
```java
// shared/common/ - Works everywhere
public class CustomBlock {
    public static final String ID = "custom_block";
    public static final float HARDNESS = 3.0f;
    public static final float RESISTANCE = 3.0f;

    // Logic that doesn't depend on loader APIs
    public static boolean isValidPlacement(BlockPos pos) {
        // Common logic
        return true;
    }
}
```

#### ❌ Bad: Platform Code in Common
```java
// DON'T DO THIS in shared/common/
import net.minecraft.registry.Registry; // Fabric-only API!

public class CustomBlock {
    public static void register() {
        Registry.register(...); // Won't compile for Forge!
    }
}
```

#### ✅ Good: Platform-Specific Registration
```java
// shared/fabric/ - Fabric APIs
public class CustomBlockFabric {
    public static void register() {
        Registry.register(Registries.BLOCK, ...);
    }
}

// shared/neoforge/ - NeoForge APIs
public class CustomBlockNeoForge {
    public static final DeferredBlock<Block> BLOCK = ...;
}
```

---

### 2. Asset Organization

#### Block Type Templates

**Simple Block** (stone, planks):
- Parent: `block/cube_all`
- Textures: 1 file (all sides)
- States: None

**Directional Block** (furnace, dispenser):
- Parent: `block/orientable`
- Textures: front, side, top
- States: `facing=north|south|east|west|up|down`

**Pillar Block** (logs, pillars):
- Parent: `block/cube_column`
- Textures: end, side
- States: `axis=x|y|z`

**Slab Block**:
- Parent: `block/slab`
- Textures: bottom, top, side
- States: `type=bottom|top|double`

**Stair Block**:
- Parent: `block/stairs`
- Textures: bottom, top, side
- States: `facing`, `half=bottom|top`, `shape=straight|inner_left|...`

---

### 3. Loot Table Patterns

#### Self-Drop (Most Blocks)
```json
{
  "type": "minecraft:block",
  "pools": [{
    "rolls": 1,
    "entries": [{
      "type": "minecraft:item",
      "name": "modid:block_name"
    }],
    "conditions": [{
      "condition": "minecraft:survives_explosion"
    }]
  }]
}
```

#### Ore with Fortune
```json
{
  "type": "minecraft:block",
  "pools": [{
    "rolls": 1,
    "entries": [{
      "type": "minecraft:item",
      "name": "modid:gem",
      "functions": [
        {
          "function": "minecraft:apply_bonus",
          "enchantment": "minecraft:fortune",
          "formula": "minecraft:ore_drops"
        },
        {
          "function": "minecraft:explosion_decay"
        }
      ]
    }]
  }]
}
```

#### Silk Touch Required
```json
{
  "type": "minecraft:block",
  "pools": [{
    "rolls": 1,
    "entries": [
      {
        "type": "minecraft:alternatives",
        "children": [
          {
            "type": "minecraft:item",
            "name": "modid:special_block",
            "conditions": [{
              "condition": "minecraft:match_tool",
              "predicate": {
                "enchantments": [{
                  "enchantment": "minecraft:silk_touch",
                  "levels": { "min": 1 }
                }]
              }
            }]
          },
          {
            "type": "minecraft:item",
            "name": "minecraft:dirt"
          }
        ]
      }
    ]
  }]
}
```

---

### 4. Version Migration Strategies

#### When Minecraft Updates Block APIs

**Example**: Minecraft 1.21 changed how block sounds work.

**Solution**: Use version-specific code
```
versions/shared/v1/common/     # 1.20.x compatible code
versions/shared/v2/common/     # 1.21.x compatible code
```

#### When Asset Format Changes

**Example**: Minecraft 1.19.3 changed model format.

**Solution**: Create new asset pack
```
versions/shared/v1/assets/     # Old format (1.19.2 and below)
versions/shared/v2/assets/     # New format (1.19.3+)
```

#### When Loader API Changes

**Example**: NeoForge deprecated old registry system.

**Solution**: Update loader-specific code only
```
shared/neoforge/               # Update to new API
shared/fabric/                 # No changes needed
shared/forge/                  # No changes needed
```

---

## Migration Guides

### From Single-Loader to Multi-Loader

#### Step 1: Identify Shared Code
**Ask**: Does this code use loader-specific APIs?
- ✅ No → Move to `shared/common/`
- ❌ Yes → Keep in `shared/<loader>/`

#### Step 2: Extract Common Logic
```java
// Before (Fabric-only):
public class MyBlock extends Block {
    public MyBlock() {
        super(AbstractBlock.Settings.create()...);
    }
}

// After (Multi-loader):
// shared/common/
public class MyBlock {
    public static final String ID = "my_block";
    public static final float HARDNESS = 3.0f;
}

// shared/fabric/
public class MyBlockFabric {
    public static final Block INSTANCE = new Block(
        AbstractBlock.Settings.create()
            .strength(MyBlock.HARDNESS, MyBlock.HARDNESS)
    );
}
```

#### Step 3: Move Assets
```
Your Mod/                   Dropper Project/
├── assets/         →      ├── versions/shared/v1/assets/
└── data/           →      └── versions/shared/v1/data/
```

#### Step 4: Configure Build
Update `config.yml` to specify supported loaders:
```yaml
loaders: [fabric, forge, neoforge]
```

---

### From Forge to NeoForge

#### Block Registration Changes

**Forge (Old)**:
```java
public static final DeferredRegister<Block> BLOCKS =
    DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);

public static final RegistryObject<Block> MY_BLOCK =
    BLOCKS.register("my_block", () -> new Block(...));
```

**NeoForge (New)**:
```java
public static final DeferredRegister.Blocks BLOCKS =
    DeferredRegister.createBlocks(MODID);

public static final DeferredBlock<Block> MY_BLOCK =
    BLOCKS.register("my_block", () -> new Block(...));
```

#### BlockItem Registration Changes

**Forge**:
```java
public static final RegistryObject<Item> MY_BLOCK_ITEM =
    ITEMS.register("my_block", () ->
        new BlockItem(MY_BLOCK.get(), new Item.Properties()));
```

**NeoForge**:
```java
public static final DeferredItem<BlockItem> MY_BLOCK_ITEM =
    ITEMS.registerSimpleBlockItem(MY_BLOCK);
```

#### Package Changes
- `net.minecraftforge.*` → `net.neoforged.*`
- `ForgeRegistries` → Still works, but prefer new methods
- Maven group: `net.neoforged:neoforge`

---

## Troubleshooting

### Common Issues

#### 1. "Block not appearing in creative inventory"

**Cause**: Block not added to creative tab or ItemGroup
**Solution**: Register BlockItem and add to creative tab

```java
// Fabric
ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS)
    .register(entries -> entries.add(MY_BLOCK));

// NeoForge
CreativeModeTabEvent.BuildContents event = ...;
event.accept(MY_BLOCK);
```

---

#### 2. "Missing texture: minecraft:block/missing"

**Cause**: Blockstate or model JSON pointing to wrong texture
**Solution**: Check file paths and JSON syntax

**Checklist**:
- [ ] Blockstate exists: `assets/modid/blockstates/name.json`
- [ ] Model exists: `assets/modid/models/block/name.json`
- [ ] Texture exists: `assets/modid/textures/block/name.png`
- [ ] Paths in JSON match file locations
- [ ] Mod ID in paths matches actual mod ID

---

#### 3. "Block drops nothing when broken"

**Cause**: Missing loot table
**Solution**: Create loot table at `data/modid/loot_tables/blocks/name.json`

---

#### 4. "ClassNotFoundException" or "NoSuchMethodError"

**Cause**: Wrong Minecraft version or loader version
**Solution**: Check `build.gradle` dependencies match target version

---

#### 5. "Block has no name (shows 'block.modid.name')"

**Cause**: Missing translation
**Solution**: Add to language file

```json
// assets/modid/lang/en_us.json
{
  "block.modid.my_block": "My Block"
}
```

---

## References

### Official Documentation

- [Fabric Wiki](https://fabricmc.net/wiki/)
- [Fabric Documentation](https://docs.fabricmc.net/)
- [NeoForge Documentation](https://docs.neoforged.net/)
- [Forge Documentation](https://docs.minecraftforge.net/)
- [Minecraft Wiki - Data Pack](https://minecraft.wiki/w/Data_pack)

### Research Sources

- [Forge, NeoForge, and Fabric Comparison](https://madelinemiller.dev/blog/forge-vs-fabric/)
- [NeoForge vs. Fabric Analysis](https://www.oreateai.com/blog/neoforge-vs-fabric-the-battle-for-minecraft-modding-supremacy/410cf302fc51b88db0a5aea33763e8c8)
- [What is Minecraft Fabric, Forge, NeoForge](https://godlike.host/minecraft-fabric-forge-and-neoforge-which-should-you-choose-blog/)
- [NeoForge vs Forge Differences](https://www.oreateai.com/blog/neoforge-vs-forge-a-new-era-in-minecraft-modding/0d9b8bc9b7ff251687bb3afc2cea02b9)
- [2025 NeoForge Retrospective](https://neoforged.net/news/2025-retrospection/)

### Community Resources

- [Fabric Discord](https://discord.gg/v6v4pMv)
- [NeoForged Discord](https://discord.neoforged.net/)
- [r/fabricmc](https://reddit.com/r/fabricmc)
- [Minecraft Mod Development](https://discord.gg/minecraft-mod-development)

---

## Appendix: Block Types Reference

### Basic Building Blocks

| Type | Parent Model | Textures | States | Example |
|------|-------------|----------|--------|---------|
| Cube All | `block/cube_all` | 1 (all) | None | Stone, Dirt |
| Cube Bottom Top | `block/cube_bottom_top` | 3 (bottom, top, side) | None | Grass Block |
| Cube Column | `block/cube_column` | 2 (end, side) | `axis` | Logs, Pillars |
| Directional | `block/orientable` | 3 (front, side, top) | `facing` | Furnace, Dropper |
| Slab | `block/slab` | 3 (bottom, top, side) | `type` | Stone Slab |
| Stairs | `block/stairs` | 3 (bottom, top, side) | `facing`, `half`, `shape` | Stone Stairs |
| Fence | `block/fence_post` + sides | 1 (texture) | `north`, `south`, `east`, `west` | Oak Fence |
| Wall | `block/template_wall_post` | 1 (texture) | `north`, `south`, `east`, `west`, `up` | Cobblestone Wall |

### Functional Blocks

| Type | Parent Model | Additional Requirements | Example |
|------|-------------|------------------------|---------|
| Crop | Custom | Age property, loot table per age | Wheat, Carrots |
| Door | `block/door_*` | Two-block tall, hinge/half states | Oak Door |
| Trapdoor | `block/template_trapdoor_*` | Open/half states | Oak Trapdoor |
| Button | `block/button` | Powered state, redstone | Stone Button |
| Pressure Plate | `block/pressure_plate_*` | Powered state, redstone | Stone Pressure Plate |
| Chest | Custom | Block Entity, inventory | Chest, Barrel |

---

**End of Document**

For questions or contributions, see [CONTRIBUTING.md](./CONTRIBUTING.md)
