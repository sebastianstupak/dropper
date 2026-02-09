# Dropper Remove Command - Quick Reference

## Command Structure

```bash
dropper remove <component-type> <name> [options]
```

## Component Types

| Type | Description | Example |
|------|-------------|---------|
| `item` | Remove items | `dropper remove item ruby_sword` |
| `block` | Remove blocks | `dropper remove block ruby_ore` |
| `entity` | Remove entities | `dropper remove entity zombie_villager` |
| `recipe` | Remove recipes | `dropper remove recipe diamond_sword` |
| `enchantment` | Remove enchantments | `dropper remove enchantment fire_aspect` |
| `biome` | Remove biomes | `dropper remove biome custom_desert` |
| `tag` | Remove tags | `dropper remove tag stone_blocks` |

## Options

| Option | Short | Description |
|--------|-------|-------------|
| `--dry-run` | - | Preview what would be deleted (no actual deletion) |
| `--force` | `-f` | Skip confirmation prompts and ignore dependencies |
| `--keep-assets` | - | Remove code files but preserve textures/models |
| `--version <VERSION>` | - | Remove from specific Minecraft version only |

## What Gets Removed

### Items
- ✓ Common item class (`shared/common/.../items/ItemName.java`)
- ✓ Fabric registration (`shared/fabric/.../ItemNameFabric.java`)
- ✓ Forge registration (`shared/forge/.../ItemNameForge.java`)
- ✓ NeoForge registration (`shared/neoforge/.../ItemNameNeoForge.java`)
- ✓ Item model (`assets/.../models/item/item_name.json`)
- ✓ Texture (`assets/.../textures/item/item_name.png`)
- ✓ Recipe (if exists)

### Blocks
- ✓ Common block class
- ✓ All loader registrations (Fabric, Forge, NeoForge)
- ✓ Blockstate JSON
- ✓ All block models (including variants)
- ✓ Item model
- ✓ All textures
- ✓ Loot table

### Complex Block Variants
- **Slabs**: Removes bottom, top, double models
- **Stairs**: Removes stairs models with rotations
- **Crops**: Removes all stage models (stage0-stage7)
- **Fences/Walls**: Removes post and side models
- **Doors/Trapdoors**: Removes all state variants

### Entities
- ✓ Entity class
- ✓ Loader registrations
- ✓ Renderer (if exists)
- ✓ Model (if exists)
- ✓ Textures

### Recipes
- ✓ Recipe JSON file

### Tags
- ✓ Tag JSON (searches items, blocks, entity_types, biomes directories)

### Enchantments
- ✓ Enchantment class
- ✓ Loader registrations

### Biomes
- ✓ Biome JSON from `worldgen/biome/`

## Safety Features

### 1. Confirmation Prompts
```bash
dropper remove item ruby_sword
# Output:
# ⚠ This will remove item 'ruby_sword' and all associated files
# Continue? (y/n):
```

Skip with `--force`:
```bash
dropper remove item ruby_sword --force
```

### 2. Dependency Detection
Automatically checks for:
- Recipes using the item/block
- Tags referencing the component
- Loot tables dropping the item
- Advancements requiring the item

If dependencies found:
```bash
dropper remove item ruby_gem
# Output:
# ⚠ Item 'ruby_gem' is referenced by 2 other component(s):
#   - Recipe consumer_recipe uses ruby_gem
#   - Tag valuable_items references ruby_gem
# Use --force to remove anyway
# ✗ Cannot remove item with dependencies (use --force to override)
```

Override with `--force`:
```bash
dropper remove item ruby_gem --force
```

### 3. Automatic Backups
Before deletion, creates backup at:
```
.dropper/backups/[timestamp]_[component_name]/
```

Example:
```
.dropper/backups/1704123456789_ruby_sword/
  └── shared/
      └── common/
          └── src/
              └── main/
                  └── java/
                      └── com/
                          └── testmod/
                              └── items/
                                  └── RubySword.java
```

### 4. Empty Directory Cleanup
Automatically removes empty directories after file deletion:
```bash
dropper remove item only_item_in_folder
# Output:
# ℹ Removed: shared/common/.../items/OnlyItemInFolder.java
# ℹ Removed empty directory: shared/common/.../items
```

## Common Workflows

### Preview Before Removing
```bash
# 1. Preview what will be deleted
dropper remove block ruby_ore --dry-run

# 2. Review the output
# DRY RUN: Would remove the following files:
#   - shared/common/src/main/java/com/modid/blocks/RubyOre.java
#   - shared/fabric/src/main/java/com/modid/platform/fabric/RubyOreFabric.java
#   - versions/shared/v1/assets/modid/blockstates/ruby_ore.json
#   - versions/shared/v1/assets/modid/models/block/ruby_ore.json
#   - versions/shared/v1/assets/modid/models/item/ruby_ore.json
#   - versions/shared/v1/assets/modid/textures/block/ruby_ore.png
#   - versions/shared/v1/data/modid/loot_table/blocks/ruby_ore.json

# 3. Remove if satisfied
dropper remove block ruby_ore
```

### Remove Code, Keep Assets
Useful when refactoring but want to preserve artwork:
```bash
dropper remove item custom_sword --keep-assets
# Removes Java code and registrations
# Preserves textures and models
```

### Bulk Removal
```bash
# Remove multiple items
dropper remove item item_a --force
dropper remove item item_b --force
dropper remove item item_c --force

# Or create a script
for item in item_a item_b item_c; do
  dropper remove item $item --force
done
```

### Clean Up Failed Experiments
```bash
# Created block but it didn't work out
dropper create block test_block
# ... testing ...
# Didn't work, remove everything
dropper remove block test_block --force
```

### Remove Deprecated Recipe
```bash
# Item still needed, but old recipe should go
dropper remove recipe old_ruby_sword_recipe --force
```

## Error Handling

### Component Not Found
```bash
dropper remove item nonexistent_item --force
# Output:
# ✗ Item 'nonexistent_item' not found
```

### Already Removed
```bash
dropper remove item ruby_sword --force
# ✓ Successfully removed item 'ruby_sword'

dropper remove item ruby_sword --force
# ✗ Item 'ruby_sword' not found
```

### Dependencies Exist
```bash
dropper remove item ruby_gem
# ✗ Cannot remove item with dependencies (use --force to override)
```

## Tips

1. **Always dry-run first** for unfamiliar components:
   ```bash
   dropper remove block complex_block --dry-run
   ```

2. **Check backups** before confirming destructive changes:
   ```bash
   ls .dropper/backups/
   ```

3. **Use keep-assets** when uncertain about textures:
   ```bash
   dropper remove item experimental_item --keep-assets
   ```

4. **Force only when confident** to avoid accidental deletions:
   ```bash
   dropper remove item confirmed_item --force
   ```

5. **Check dependencies manually** for complex mods:
   ```bash
   grep -r "ruby_sword" versions/shared/v1/data/
   ```

## Restoration from Backup

If you need to restore from backup:

```bash
# 1. Find your backup
ls -lt .dropper/backups/

# 2. Copy files back
cp -r .dropper/backups/1704123456789_ruby_sword/* .

# 3. Verify files restored
ls shared/common/src/main/java/com/modid/items/
```

## Integration with Other Commands

### Create → Remove Workflow
```bash
# Experiment with a new item
dropper create item test_item

# Test it
dropper dev client

# Didn't work out, remove it
dropper remove item test_item --force
```

### List → Remove Workflow
```bash
# See all items
dropper list items

# Remove unwanted ones
dropper remove item deprecated_item_1 --force
dropper remove item deprecated_item_2 --force
```

### Validate → Remove Workflow
```bash
# Find broken items
dropper validate assets

# Remove broken ones
dropper remove item broken_item --force
```

## Comparison with Manual Deletion

### Manual (Error-Prone)
```bash
# Manual deletion - easy to miss files
rm shared/common/src/main/java/com/modid/items/RubyGem.java
rm shared/fabric/src/main/java/com/modid/platform/fabric/RubyGemFabric.java
# ... forgot Forge registration
# ... forgot NeoForge registration
# ... forgot item model
# ... forgot texture
# ... forgot recipe
# ... empty directories left behind
```

### Dropper Remove (Safe & Complete)
```bash
# Automated, complete, safe
dropper remove item ruby_gem

# ✓ Removes all 7+ related files
# ✓ Checks dependencies
# ✓ Creates backup
# ✓ Cleans empty directories
# ✓ Confirms before deletion
```

## Performance

- **Fast**: Removes 10 items in < 1 second
- **Efficient**: Only scans for dependencies when needed
- **Smart**: Caches dependency analysis results

## Limitations

- Cannot undo without backup (always creates backup by default)
- `--version` flag currently removes from all versions (planned feature)
- No "remove all of type" bulk command (use bash loop)
- No interactive file selection (use `--dry-run` first)

## Future Enhancements (Planned)

- [ ] Selective file removal (remove only Java code, only assets, etc.)
- [ ] Undo last removal command
- [ ] Remove multiple components at once
- [ ] Interactive mode with file selection
- [ ] Git integration for atomic commits
- [ ] Version-specific removal (--version flag implementation)
