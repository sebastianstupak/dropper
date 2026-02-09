# Block Creation Guide for Dropper

**Quick reference for creating blocks in multi-loader Minecraft mods using Dropper**

---

## Quick Start

```bash
# Create a basic block
dropper create block ruby_ore

# Create an ore block (with Fortune enchantment support)
dropper create block ruby_ore --type ore

# Create a pillar block (like logs)
dropper create block marble_pillar --type pillar

# Create a slab block (half blocks)
dropper create block stone_slab --type slab

# Create stairs
dropper create block stone_stairs --type stairs

# Create a fence
dropper create block oak_fence --type fence

# Create a wall
dropper create block cobblestone_wall --type wall

# Create a door
dropper create block oak_door --type door

# Create a trapdoor
dropper create block oak_trapdoor --type trapdoor

# Create a button
dropper create block stone_button --type button

# Create a pressure plate
dropper create block stone_pressure_plate --type pressure_plate

# Create a crop (with custom max age)
dropper create block wheat --type crop --max-age 7

# Create a block that doesn't drop itself
dropper create block grass_block --drops-self false
```

---

## What Gets Generated

When you run `dropper create block <name>`, Dropper generates:

### 1. Java Code (Multi-Loader)

```
shared/common/src/main/java/com/<modid>/blocks/
  └── <Name>.java                    # Common block definition

shared/fabric/src/main/java/com/<modid>/platform/fabric/
  └── <Name>Fabric.java             # Fabric registration

shared/forge/src/main/java/com/<modid>/platform/forge/
  └── <Name>Forge.java              # Forge registration

shared/neoforge/src/main/java/com/<modid>/platform/neoforge/
  └── <Name>NeoForge.java           # NeoForge registration
```

### 2. Assets (Shared Across Loaders)

```
versions/shared/v1/assets/<modid>/
  ├── blockstates/<name>.json       # Block variants/rotations
  ├── models/
  │   ├── block/<name>.json         # 3D block model
  │   └── item/<name>.json          # Inventory item model
  └── textures/block/<name>.png     # Block texture (placeholder)
```

### 3. Data (Shared Across Loaders)

```
versions/shared/v1/data/<modid>/
  └── loot_tables/blocks/<name>.json  # What drops when broken
```

---

## Block Types

### Basic Block (`--type basic`)

**Use for**: Simple cubes with same texture on all sides (stone, planks)

**Generated blockstate**:
```json
{
  "variants": {
    "": { "model": "modid:block/my_block" }
  }
}
```

**Generated model**:
```json
{
  "parent": "block/cube_all",
  "textures": {
    "all": "modid:block/my_block"
  }
}
```

**What you need to add**:
- Texture: `16x16` or `32x32` PNG at `versions/shared/v1/assets/<modid>/textures/block/my_block.png`

---

### Ore Block (`--type ore`)

**Use for**: Minable ores that drop gems/items with Fortune enchantment

**Same as basic, but loot table includes**:
```json
{
  "functions": [
    {
      "function": "minecraft:apply_bonus",
      "enchantment": "minecraft:fortune",
      "formula": "minecraft:ore_drops"
    }
  ]
}
```

**What you need to add**:
- Ore texture (darker edges, lighter center pattern)
- The item it drops (create separately with `dropper create item ruby`)

---

### Pillar Block (`--type pillar`)

**Use for**: Rotatable pillars/logs with different top and side textures

**Generated blockstate**:
```json
{
  "variants": {
    "axis=x": { "model": "modid:block/pillar", "x": 90, "y": 90 },
    "axis=y": { "model": "modid:block/pillar" },
    "axis=z": { "model": "modid:block/pillar", "x": 90 }
  }
}
```

**Generated model**:
```json
{
  "parent": "block/cube_column",
  "textures": {
    "end": "modid:block/pillar_top",
    "side": "modid:block/pillar"
  }
}
```

**What you need to add**:
- Side texture: `pillar.png`
- Top texture: `pillar_top.png`

---

### Slab Block (`--type slab`)

**Use for**: Half-height blocks that can stack

**Generated blockstate**:
```json
{
  "variants": {
    "type=bottom": { "model": "modid:block/stone_slab" },
    "type=top": { "model": "modid:block/stone_slab_top" },
    "type=double": { "model": "modid:block/stone_slab_double" }
  }
}
```

**Generated models**: Three models (bottom, top, double)

**What you need to add**:
- Texture: Single `stone_slab.png` texture used for all variants

---

### Stairs Block (`--type stairs`)

**Use for**: Stair blocks with proper rotation

**Generated blockstate**: Complex rotation variants for all directions and half positions

**What you need to add**:
- Texture: Single texture file used for all sides

---

### Fence Block (`--type fence`)

**Use for**: Connecting fence blocks

**Generated blockstate**: Multipart model with post and connecting sides

**Generated models**:
- `fence_post.json` - Central post
- `fence_side.json` - Connecting side

**What you need to add**:
- Texture: Single fence texture

---

### Wall Block (`--type wall`)

**Use for**: Connecting wall blocks (like cobblestone walls)

**Generated blockstate**: Multipart model with post and connecting sides

**Generated models**:
- `wall_post.json` - Central post
- `wall_side.json` - Connecting side (low height)

**What you need to add**:
- Texture: Single wall texture

---

### Door Block (`--type door`)

**Use for**: Two-block tall openable doors

**What you need to add**:
- Bottom texture: `door_bottom.png` (16x16)
- Top texture: `door_top.png` (16x16)

**Note**: Door models are complex and use Minecraft's built-in door templates

---

### Trapdoor Block (`--type trapdoor`)

**Use for**: Openable trapdoors

**Generated models**:
- `trapdoor_bottom.json` - Closed on bottom
- `trapdoor_top.json` - Closed on top
- `trapdoor_open.json` - Open state

**What you need to add**:
- Texture: Single trapdoor texture

---

### Button Block (`--type button`)

**Use for**: Redstone buttons

**Generated models**:
- `button.json` - Unpressed state
- `button_pressed.json` - Pressed state

**What you need to add**:
- Texture: Button texture (matches parent block material)

---

### Pressure Plate Block (`--type pressure_plate`)

**Use for**: Redstone pressure plates

**Generated models**:
- `pressure_plate.json` - Unpressed
- `pressure_plate_down.json` - Pressed

**What you need to add**:
- Texture: Plate texture (matches parent block material)

---

### Crop Block (`--type crop`)

**Use for**: Growable farming crops

**Usage**:
```bash
dropper create block wheat --type crop --max-age 7
```

**Generated blockstate**: States for ages 0-7 (or custom max-age)

**Generated models**: One model per growth stage
- `wheat_stage0.json` through `wheat_stage7.json`

**What you need to add**:
- Textures: One per growth stage
  - `wheat_stage0.png` - Just planted
  - `wheat_stage1.png` - Small sprout
  - ...
  - `wheat_stage7.png` - Fully grown

**Special Notes**:
- Crops don't drop themselves (they drop seeds/items)
- Requires custom loot table per age
- Block has no collision

---

## Customization

### Adding Block Properties

Edit the generated common block class:

```java
// shared/common/src/main/java/com/modid/blocks/RubyOre.java
public class RubyOre {
    public static final String ID = "ruby_ore";

    // Add custom properties here
    public static final int LIGHT_LEVEL = 7;  // Emits light
    public static final boolean REQUIRES_TOOL = true;
}
```

Then update the loader-specific files to use these properties:

```java
// shared/fabric/
Block block = new Block(
    AbstractBlock.Settings.create()
        .strength(3.0f, 3.0f)
        .requiresTool()
        .luminance(state -> RubyOre.LIGHT_LEVEL)  // Use custom property
        .sounds(BlockSoundGroup.STONE)
);
```

### Common Block Properties

| Property | Fabric | Forge/NeoForge | Description |
|----------|--------|----------------|-------------|
| Hardness/Resistance | `.strength(3.0f, 3.0f)` | `.strength(3.0f, 3.0f)` | Mining time & blast resistance |
| Requires Tool | `.requiresTool()` | `.requiresCorrectToolForDrops()` | Needs pickaxe/axe/etc |
| Light Level | `.luminance(state -> 15)` | `.lightLevel(state -> 15)` | 0-15 brightness |
| Sound | `.sounds(BlockSoundGroup.STONE)` | `.sound(SoundType.STONE)` | Break/place sounds |
| Slipperiness | `.slipperiness(0.6f)` | `.friction(0.6f)` | Ice-like sliding |
| Random Ticks | `.ticksRandomly()` | `.randomTicks()` | For crops, grass spread |
| No Collision | `.noCollision()` | `.noCollision()` | Walk through like air |
| Not Solid | `.nonOpaque()` | `.noOcclusion()` | Doesn't block light |

### Sound Types

| Sound | Use For |
|-------|---------|
| `STONE` | Stone, ores, bricks |
| `WOOD` | Planks, logs, crafting tables |
| `GRAVEL` | Gravel, dirt |
| `GRASS` | Grass, leaves |
| `METAL` | Iron blocks, anvils |
| `GLASS` | Glass, ice |
| `WOOL` | Wool, carpet |
| `SAND` | Sand, sandstone |
| `SNOW` | Snow blocks |
| `POWDER_SNOW` | Powder snow |

---

## Advanced: Custom Blockstates

### Directional Block (Facing)

**Use for**: Furnaces, dispensers, observers

```json
{
  "variants": {
    "facing=north": { "model": "modid:block/furnace" },
    "facing=south": { "model": "modid:block/furnace", "y": 180 },
    "facing=west": { "model": "modid:block/furnace", "y": 270 },
    "facing=east": { "model": "modid:block/furnace", "y": 90 },
    "facing=up": { "model": "modid:block/furnace", "x": -90 },
    "facing=down": { "model": "modid:block/furnace", "x": 90 }
  }
}
```

**Required textures**: `front`, `side`, `top`

---

### Powered Block (Redstone)

**Use for**: Buttons, levers, pressure plates

```json
{
  "variants": {
    "powered=false": { "model": "modid:block/button" },
    "powered=true": { "model": "modid:block/button_pressed" }
  }
}
```

---

### Connected Textures (Fences, Walls)

**Use for**: Blocks that visually connect to neighbors

```json
{
  "multipart": [
    { "apply": { "model": "modid:block/fence_post" }},
    {
      "when": { "north": "true" },
      "apply": { "model": "modid:block/fence_side", "uvlock": true }
    },
    {
      "when": { "south": "true" },
      "apply": { "model": "modid:block/fence_side", "y": 180, "uvlock": true }
    }
    // ... east, west
  ]
}
```

---

## Advanced: Custom Loot Tables

### Drop Different Item

```json
{
  "type": "minecraft:block",
  "pools": [{
    "rolls": 1,
    "entries": [{
      "type": "minecraft:item",
      "name": "modid:ruby",  // Drop ruby instead of ore
      "functions": [
        {
          "function": "minecraft:set_count",
          "count": { "min": 2, "max": 5 }  // Random amount
        },
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

### Conditional Drops (Silk Touch)

```json
{
  "type": "minecraft:block",
  "pools": [{
    "rolls": 1,
    "entries": [{
      "type": "minecraft:alternatives",
      "children": [
        {
          "type": "minecraft:item",
          "name": "modid:ore_block",  // With Silk Touch: drop block
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
          "name": "modid:raw_ore"  // Without Silk Touch: drop raw ore
        }
      ]
    }]
  }]
}
```

### Multiple Drops

```json
{
  "type": "minecraft:block",
  "pools": [
    {
      "rolls": 1,
      "entries": [{
        "type": "minecraft:item",
        "name": "modid:seeds"
      }]
    },
    {
      "rolls": 1,
      "entries": [{
        "type": "minecraft:item",
        "name": "modid:crop_item",
        "functions": [{
          "function": "minecraft:set_count",
          "count": { "min": 1, "max": 4 }
        }]
      }]
    }
  ]
}
```

---

## Texturing Guide

### Texture Requirements

- **Format**: PNG
- **Size**: 16x16 (vanilla), 32x32 (HD), or higher powers of 2
- **Transparency**: Alpha channel supported
- **Animation**: Supported via `.mcmeta` files

### Basic Block Texture

For a simple cube block, you need **one texture**:
```
versions/shared/v1/assets/modid/textures/block/
  └── my_block.png (16x16)
```

### Pillar Block Textures

For logs/pillars, you need **two textures**:
```
versions/shared/v1/assets/modid/textures/block/
  ├── pillar.png       (16x16, side texture)
  └── pillar_top.png   (16x16, end texture)
```

### Directional Block Textures

For furnaces/dispensers, you need **three textures**:
```
versions/shared/v1/assets/modid/textures/block/
  ├── furnace_front.png  (16x16, unique front)
  ├── furnace_side.png   (16x16, sides)
  └── furnace_top.png    (16x16, top/bottom)
```

### Animated Textures

For lava, water, fire-like blocks:
```
my_block.png        (16x256 - 16 frames stacked vertically)
my_block.png.mcmeta (animation metadata)
```

**.mcmeta example**:
```json
{
  "animation": {
    "frametime": 2,
    "interpolate": true
  }
}
```

---

## Testing Your Block

### 1. Build the Mod

```bash
dropper build --version 1.20.1 --loader fabric
```

### 2. Copy to Minecraft

```bash
cp build/1_20_1/fabric.jar ~/.minecraft/mods/
```

### 3. Test In-Game

1. Launch Minecraft with Fabric
2. Open creative inventory
3. Search for your block
4. Place it in the world
5. Check:
   - [ ] Texture renders correctly
   - [ ] Breaking plays correct sound
   - [ ] Drops correct item(s)
   - [ ] Name displays (not `block.modid.name`)

### 4. Debug Missing Texture

If you see purple/black checkerboard:
1. Check blockstate path: `assets/modid/blockstates/my_block.json`
2. Check model path: `assets/modid/models/block/my_block.json`
3. Check texture path: `assets/modid/textures/block/my_block.png`
4. Check model references correct texture
5. Check mod ID in paths matches your config.yml

---

## Localization

Add block name to language file:

```json
// versions/shared/v1/assets/modid/lang/en_us.json
{
  "block.modid.ruby_ore": "Ruby Ore",
  "block.modid.marble_pillar": "Marble Pillar"
}
```

**Supported languages**: `en_us`, `en_gb`, `de_de`, `es_es`, `fr_fr`, `it_it`, `ja_jp`, `ko_kr`, `pt_br`, `ru_ru`, `zh_cn`, etc.

---

## Recipes

Add crafting recipe (optional):

```json
// versions/shared/v1/data/modid/recipes/marble_pillar.json
{
  "type": "minecraft:crafting_shaped",
  "pattern": [
    "M",
    "M"
  ],
  "key": {
    "M": {
      "item": "modid:marble"
    }
  },
  "result": {
    "item": "modid:marble_pillar",
    "count": 3
  }
}
```

---

## Tags

Add block to tags for tools/categories:

```json
// versions/shared/v1/data/minecraft/tags/blocks/mineable/pickaxe.json
{
  "replace": false,
  "values": [
    "modid:ruby_ore",
    "modid:marble_pillar"
  ]
}
```

**Common tags**:
- `minecraft:mineable/pickaxe` - Requires pickaxe
- `minecraft:mineable/axe` - Requires axe
- `minecraft:mineable/shovel` - Requires shovel
- `minecraft:mineable/hoe` - Requires hoe
- `minecraft:needs_stone_tool` - Needs stone tier+
- `minecraft:needs_iron_tool` - Needs iron tier+
- `minecraft:needs_diamond_tool` - Needs diamond tier+

---

## Troubleshooting

### Block doesn't appear in creative tab

**Solution**: Make sure BlockItem is registered and added to creative tab.

Check loader-specific registration files have BlockItem registration code uncommented.

---

### Block has no name (shows "block.modid.name")

**Solution**: Add translation to `assets/modid/lang/en_us.json`

---

### Block drops nothing

**Solution**: Create loot table at `data/modid/loot_tables/blocks/name.json`

---

### Texture is missing (purple/black)

**Solution**:
1. Verify texture file exists
2. Check paths in blockstate and model JSONs
3. Ensure mod ID matches everywhere
4. Rebuild mod

---

### Block is transparent/not solid

**Solution**: Remove `.nonOpaque()` / `.noOcclusion()` from settings

---

## Next Steps

- **Create Items**: `dropper create item ruby`
- **Create Asset Pack**: `dropper create asset-pack v2`
- **Add Version Support**: `dropper create version 1.21.1`
- **Build for All Loaders**: `dropper build --all`

---

## Quick Reference

| Command | Description |
|---------|-------------|
| `dropper create block <name>` | Create basic block |
| `dropper create block <name> --type ore` | Create ore block |
| `dropper create block <name> --type pillar` | Create pillar/log block |
| `dropper build` | Build all loaders for all versions |
| `dropper build --loader fabric` | Build Fabric only |
| `dropper build --version 1.20.1` | Build one version only |

---

For more information, see:
- [MODDING_RESEARCH.md](./MODDING_RESEARCH.md) - Deep dive into mod loaders
- [STRUCTURE.md](./STRUCTURE.md) - Project structure guide
- [Official Fabric Wiki](https://fabricmc.net/wiki/)
- [Official NeoForge Docs](https://docs.neoforged.net/)
