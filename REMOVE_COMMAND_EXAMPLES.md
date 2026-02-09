# Dropper Remove Command - Example Output

## Example 1: Basic Item Removal

```bash
$ dropper remove item ruby_sword
```

**Output:**
```
ℹ Removing item: ruby_sword
⚠ This will remove item 'ruby_sword' and all associated files
Continue? (y/n): y
ℹ Backup created at: .dropper/backups/1704123456789_ruby_sword
ℹ Removed: shared/common/src/main/java/com/testmod/items/RubySword.java
ℹ Removed: shared/fabric/src/main/java/com/testmod/platform/fabric/RubySwordFabric.java
ℹ Removed: shared/forge/src/main/java/com/testmod/platform/forge/RubySwordForge.java
ℹ Removed: shared/neoforge/src/main/java/com/testmod/platform/neoforge/RubySwordNeoForge.java
ℹ Removed: versions/shared/v1/assets/testmod/models/item/ruby_sword.json
ℹ Removed: versions/shared/v1/assets/testmod/textures/item/ruby_sword.png
ℹ Removed: versions/shared/v1/data/testmod/recipe/ruby_sword.json
ℹ Removed empty directory: shared/common/src/main/java/com/testmod/items
✓ Successfully removed item 'ruby_sword'
ℹ Files removed: 7
ℹ Empty directories cleaned: 1
```

---

## Example 2: Dry-Run Preview

```bash
$ dropper remove block ruby_ore --dry-run
```

**Output:**
```
ℹ Removing block: ruby_ore
DRY RUN: Would remove the following files:
ℹ   - shared/common/src/main/java/com/testmod/blocks/RubyOre.java
ℹ   - shared/fabric/src/main/java/com/testmod/platform/fabric/RubyOreFabric.java
ℹ   - shared/forge/src/main/java/com/testmod/platform/forge/RubyOreForge.java
ℹ   - shared/neoforge/src/main/java/com/testmod/platform/neoforge/RubyOreNeoForge.java
ℹ   - versions/shared/v1/assets/testmod/blockstates/ruby_ore.json
ℹ   - versions/shared/v1/assets/testmod/models/block/ruby_ore.json
ℹ   - versions/shared/v1/assets/testmod/models/item/ruby_ore.json
ℹ   - versions/shared/v1/assets/testmod/textures/block/ruby_ore.png
ℹ   - versions/shared/v1/data/testmod/loot_table/blocks/ruby_ore.json
✓ DRY RUN: Would remove 9 file(s)
```

---

## Example 3: Dependency Detection (Blocked)

```bash
$ dropper remove item ruby_gem
```

**Output:**
```
ℹ Removing item: ruby_gem
⚠ Item 'ruby_gem' is referenced by 3 other component(s):
⚠   - Recipe crafting_ruby_sword uses ruby_gem
⚠   - Recipe smelting_ruby uses ruby_gem
⚠   - Tag valuable_items references ruby_gem
⚠ Use --force to remove anyway
✗ Cannot remove item with dependencies (use --force to override)
```

---

## Example 4: Force Removal (Ignoring Dependencies)

```bash
$ dropper remove item ruby_gem --force
```

**Output:**
```
ℹ Removing item: ruby_gem
⚠ Item 'ruby_gem' is referenced by 3 other component(s):
⚠   - Recipe crafting_ruby_sword uses ruby_gem
⚠   - Recipe smelting_ruby uses ruby_gem
⚠   - Tag valuable_items references ruby_gem
ℹ Backup created at: .dropper/backups/1704123456790_ruby_gem
ℹ Removed: shared/common/src/main/java/com/testmod/items/RubyGem.java
ℹ Removed: shared/fabric/src/main/java/com/testmod/platform/fabric/RubyGemFabric.java
ℹ Removed: shared/forge/src/main/java/com/testmod/platform/forge/RubyGemForge.java
ℹ Removed: shared/neoforge/src/main/java/com/testmod/platform/neoforge/RubyGemNeoForge.java
ℹ Removed: versions/shared/v1/assets/testmod/models/item/ruby_gem.json
ℹ Removed: versions/shared/v1/assets/testmod/textures/item/ruby_gem.png
ℹ Removed: versions/shared/v1/data/testmod/recipe/ruby_gem.json
✓ Successfully removed item 'ruby_gem'
ℹ Files removed: 7
```

---

## Example 5: Complex Block (Slab) Removal

```bash
$ dropper remove block ruby_slab
```

**Output:**
```
ℹ Removing block: ruby_slab
⚠ This will remove block 'ruby_slab' and all associated files
Continue? (y/n): y
ℹ Backup created at: .dropper/backups/1704123456791_ruby_slab
ℹ Removed: shared/common/src/main/java/com/testmod/blocks/RubySlab.java
ℹ Removed: shared/fabric/src/main/java/com/testmod/platform/fabric/RubySlabFabric.java
ℹ Removed: shared/forge/src/main/java/com/testmod/platform/forge/RubySlabForge.java
ℹ Removed: shared/neoforge/src/main/java/com/testmod/platform/neoforge/RubySlabNeoForge.java
ℹ Removed: versions/shared/v1/assets/testmod/blockstates/ruby_slab.json
ℹ Removed: versions/shared/v1/assets/testmod/models/block/ruby_slab.json
ℹ Removed: versions/shared/v1/assets/testmod/models/block/ruby_slab_top.json
ℹ Removed: versions/shared/v1/assets/testmod/models/block/ruby_slab_double.json
ℹ Removed: versions/shared/v1/assets/testmod/models/item/ruby_slab.json
ℹ Removed: versions/shared/v1/assets/testmod/textures/block/ruby_slab.png
ℹ Removed: versions/shared/v1/data/testmod/loot_table/blocks/ruby_slab.json
✓ Successfully removed block 'ruby_slab'
ℹ Files removed: 11
```

---

## Example 6: Crop Block (8 Stages) Removal

```bash
$ dropper remove block wheat_crop --force
```

**Output:**
```
ℹ Removing block: wheat_crop
ℹ Backup created at: .dropper/backups/1704123456792_wheat_crop
ℹ Removed: shared/common/src/main/java/com/testmod/blocks/WheatCrop.java
ℹ Removed: shared/fabric/src/main/java/com/testmod/platform/fabric/WheatCropFabric.java
ℹ Removed: shared/forge/src/main/java/com/testmod/platform/forge/WheatCropForge.java
ℹ Removed: shared/neoforge/src/main/java/com/testmod/platform/neoforge/WheatCropNeoForge.java
ℹ Removed: versions/shared/v1/assets/testmod/blockstates/wheat_crop.json
ℹ Removed: versions/shared/v1/assets/testmod/models/block/wheat_crop_stage0.json
ℹ Removed: versions/shared/v1/assets/testmod/models/block/wheat_crop_stage1.json
ℹ Removed: versions/shared/v1/assets/testmod/models/block/wheat_crop_stage2.json
ℹ Removed: versions/shared/v1/assets/testmod/models/block/wheat_crop_stage3.json
ℹ Removed: versions/shared/v1/assets/testmod/models/block/wheat_crop_stage4.json
ℹ Removed: versions/shared/v1/assets/testmod/models/block/wheat_crop_stage5.json
ℹ Removed: versions/shared/v1/assets/testmod/models/block/wheat_crop_stage6.json
ℹ Removed: versions/shared/v1/assets/testmod/models/block/wheat_crop_stage7.json
ℹ Removed: versions/shared/v1/assets/testmod/textures/block/wheat_crop_stage0.png
ℹ Removed: versions/shared/v1/assets/testmod/textures/block/wheat_crop_stage1.png
ℹ Removed: versions/shared/v1/assets/testmod/textures/block/wheat_crop_stage2.png
ℹ Removed: versions/shared/v1/assets/testmod/textures/block/wheat_crop_stage3.png
ℹ Removed: versions/shared/v1/assets/testmod/textures/block/wheat_crop_stage4.png
ℹ Removed: versions/shared/v1/assets/testmod/textures/block/wheat_crop_stage5.png
ℹ Removed: versions/shared/v1/assets/testmod/textures/block/wheat_crop_stage6.png
ℹ Removed: versions/shared/v1/assets/testmod/textures/block/wheat_crop_stage7.png
ℹ Removed: versions/shared/v1/assets/testmod/models/item/wheat_crop.json
✓ Successfully removed block 'wheat_crop'
ℹ Files removed: 21
```

---

## Example 7: Keep Assets Option

```bash
$ dropper remove item custom_sword --keep-assets --force
```

**Output:**
```
ℹ Removing item: custom_sword
ℹ Backup created at: .dropper/backups/1704123456793_custom_sword
ℹ Removed: shared/common/src/main/java/com/testmod/items/CustomSword.java
ℹ Removed: shared/fabric/src/main/java/com/testmod/platform/fabric/CustomSwordFabric.java
ℹ Removed: shared/forge/src/main/java/com/testmod/platform/forge/CustomSwordForge.java
ℹ Removed: shared/neoforge/src/main/java/com/testmod/platform/neoforge/CustomSwordNeoForge.java
✓ Successfully removed item 'custom_sword'
ℹ Files removed: 4
ℹ Note: Assets preserved (textures and models not removed)
```

---

## Example 8: Recipe Removal

```bash
$ dropper remove recipe diamond_sword_advanced --force
```

**Output:**
```
ℹ Removing recipe: diamond_sword_advanced
ℹ Backup created at: .dropper/backups/1704123456794_recipe_diamond_sword_advanced
ℹ Removed: versions/shared/v1/data/testmod/recipe/diamond_sword_advanced.json
✓ Successfully removed recipe 'diamond_sword_advanced'
ℹ Files removed: 1
```

---

## Example 9: Tag Removal

```bash
$ dropper remove tag valuable_ores --force
```

**Output:**
```
ℹ Removing tag: valuable_ores
ℹ Backup created at: .dropper/backups/1704123456795_tag_valuable_ores
ℹ Removed: versions/shared/v1/data/testmod/tags/blocks/valuable_ores.json
✓ Successfully removed tag 'valuable_ores'
ℹ Files removed: 1
```

---

## Example 10: Non-Existent Component

```bash
$ dropper remove item nonexistent_item --force
```

**Output:**
```
ℹ Removing item: nonexistent_item
✗ Item 'nonexistent_item' not found
```

---

## Example 11: Already Removed

```bash
$ dropper remove item ruby_sword --force
# (run twice)
```

**First run:**
```
ℹ Removing item: ruby_sword
ℹ Backup created at: .dropper/backups/1704123456796_ruby_sword
ℹ Removed: shared/common/src/main/java/com/testmod/items/RubySword.java
ℹ Removed: shared/fabric/src/main/java/com/testmod/platform/fabric/RubySwordFabric.java
ℹ Removed: shared/forge/src/main/java/com/testmod/platform/forge/RubySwordForge.java
ℹ Removed: shared/neoforge/src/main/java/com/testmod/platform/neoforge/RubySwordNeoForge.java
ℹ Removed: versions/shared/v1/assets/testmod/models/item/ruby_sword.json
ℹ Removed: versions/shared/v1/assets/testmod/textures/item/ruby_sword.png
✓ Successfully removed item 'ruby_sword'
ℹ Files removed: 6
```

**Second run:**
```
ℹ Removing item: ruby_sword
✗ Item 'ruby_sword' not found
```

---

## Example 12: Entity Removal

```bash
$ dropper remove entity zombie_villager --force
```

**Output:**
```
ℹ Removing entity: zombie_villager
ℹ Backup created at: .dropper/backups/1704123456797_zombie_villager
ℹ Removed: shared/common/src/main/java/com/testmod/entities/ZombieVillager.java
ℹ Removed: shared/fabric/src/main/java/com/testmod/platform/fabric/ZombieVillagerFabric.java
ℹ Removed: shared/forge/src/main/java/com/testmod/platform/forge/ZombieVillagerForge.java
ℹ Removed: shared/neoforge/src/main/java/com/testmod/platform/neoforge/ZombieVillagerNeoForge.java
ℹ Removed: shared/common/src/main/java/com/testmod/entities/renderer/ZombieVillagerRenderer.java
ℹ Removed: shared/common/src/main/java/com/testmod/entities/model/ZombieVillagerModel.java
ℹ Removed: versions/shared/v1/assets/testmod/textures/entity/zombie_villager.png
✓ Successfully removed entity 'zombie_villager'
ℹ Files removed: 7
```

---

## Example 13: Enchantment Removal

```bash
$ dropper remove enchantment fire_aspect --force
```

**Output:**
```
ℹ Removing enchantment: fire_aspect
ℹ Backup created at: .dropper/backups/1704123456798_fire_aspect
ℹ Removed: shared/common/src/main/java/com/testmod/enchantments/FireAspect.java
ℹ Removed: shared/fabric/src/main/java/com/testmod/platform/fabric/FireAspectFabric.java
ℹ Removed: shared/forge/src/main/java/com/testmod/platform/forge/FireAspectForge.java
ℹ Removed: shared/neoforge/src/main/java/com/testmod/platform/neoforge/FireAspectNeoForge.java
✓ Successfully removed enchantment 'fire_aspect'
ℹ Files removed: 4
```

---

## Example 14: Biome Removal

```bash
$ dropper remove biome custom_desert --force
```

**Output:**
```
ℹ Removing biome: custom_desert
ℹ Backup created at: .dropper/backups/1704123456799_biome_custom_desert
ℹ Removed: versions/shared/v1/data/testmod/worldgen/biome/custom_desert.json
✓ Successfully removed biome 'custom_desert'
ℹ Files removed: 1
```

---

## Example 15: Bulk Removal (Script)

```bash
$ for item in old_item_1 old_item_2 old_item_3; do
    dropper remove item $item --force
  done
```

**Output:**
```
ℹ Removing item: old_item_1
ℹ Backup created at: .dropper/backups/1704123456800_old_item_1
ℹ Removed: shared/common/src/main/java/com/testmod/items/OldItem1.java
... (more files)
✓ Successfully removed item 'old_item_1'
ℹ Files removed: 6

ℹ Removing item: old_item_2
ℹ Backup created at: .dropper/backups/1704123456801_old_item_2
ℹ Removed: shared/common/src/main/java/com/testmod/items/OldItem2.java
... (more files)
✓ Successfully removed item 'old_item_2'
ℹ Files removed: 6

ℹ Removing item: old_item_3
ℹ Backup created at: .dropper/backups/1704123456802_old_item_3
ℹ Removed: shared/common/src/main/java/com/testmod/items/OldItem3.java
... (more files)
✓ Successfully removed item 'old_item_3'
ℹ Files removed: 6
```

---

## Example 16: User Cancels Removal

```bash
$ dropper remove item valuable_item
```

**Output:**
```
ℹ Removing item: valuable_item
⚠ This will remove item 'valuable_item' and all associated files
Continue? (y/n): n
ℹ Cancelled
```

---

## Example 17: Directory Cleanup

```bash
$ dropper remove item only_item_in_folder --force
```

**Output:**
```
ℹ Removing item: only_item_in_folder
ℹ Backup created at: .dropper/backups/1704123456803_only_item_in_folder
ℹ Removed: shared/common/src/main/java/com/testmod/items/OnlyItemInFolder.java
ℹ Removed: shared/fabric/src/main/java/com/testmod/platform/fabric/OnlyItemInFolderFabric.java
ℹ Removed: shared/forge/src/main/java/com/testmod/platform/forge/OnlyItemInFolderForge.java
ℹ Removed: shared/neoforge/src/main/java/com/testmod/platform/neoforge/OnlyItemInFolderNeoForge.java
ℹ Removed: versions/shared/v1/assets/testmod/models/item/only_item_in_folder.json
ℹ Removed: versions/shared/v1/assets/testmod/textures/item/only_item_in_folder.png
ℹ Removed empty directory: shared/common/src/main/java/com/testmod/items
✓ Successfully removed item 'only_item_in_folder'
ℹ Files removed: 6
ℹ Empty directories cleaned: 1
```

---

## Output Legend

| Symbol | Meaning |
|--------|---------|
| `ℹ` | Info message |
| `✓` | Success message |
| `✗` | Error message |
| `⚠` | Warning message |

## Color Coding (in terminal)

- **Info (ℹ)**: Blue/Cyan
- **Success (✓)**: Green
- **Error (✗)**: Red
- **Warning (⚠)**: Yellow/Orange

## Return Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | Failure (component not found, dependencies exist, etc.) |
| 2 | User cancelled |

## Summary Statistics

All successful removals show:
- Number of files removed
- Number of directories cleaned (if any)
- Backup location

Example:
```
✓ Successfully removed item 'ruby_sword'
ℹ Files removed: 7
ℹ Empty directories cleaned: 1
```
