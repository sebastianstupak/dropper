# Dropper Sync Command - Quick Reference

## Commands

### Sync All Assets
```bash
dropper sync assets --from v1 --to v2
```
Syncs models, textures, blockstates, lang files, etc.

### Sync Language Files
```bash
dropper sync lang --from v1 --to v2
```
Intelligently merges translation files (preserves custom translations).

### Sync Data Files
```bash
dropper sync recipes --from v1 --to v2
```
Syncs recipes, loot tables, tags, etc.

### Sync Textures Only
```bash
dropper sync textures --from v1 --to v2
```
Syncs PNG/JPG/GIF texture files only.

### Sync Models Only
```bash
dropper sync models --from v1 --to v2
```
Syncs item and block model JSON files only.

### Sync Blockstates Only
```bash
dropper sync blockstates --from v1 --to v2
```
Syncs blockstate JSON files only.

## Options

### Preview Changes (Dry-Run)
```bash
dropper sync assets --from v1 --to v2 --dry-run
```
Shows what would be synced without making changes.

### Force Overwrite
```bash
dropper sync assets --from v1 --to v2 --force
```
Overwrites conflicts (default: keeps target).

### Exclude Files
```bash
dropper sync assets --from v1 --to v2 --exclude "test_*"
```
Skip files matching pattern (glob supported).

### Multiple Exclusions
```bash
dropper sync assets --from v1 --to v2 \
  --exclude "*.old" \
  --exclude "test_*" \
  --exclude "debug/*"
```

### Bidirectional Sync
```bash
dropper sync assets --from v1 --to v2 --bidirectional
```
Syncs both v1→v2 and v2→v1.

## Common Workflows

### Create item, sync to other versions
```bash
# Create item
dropper create item ruby_sword --type tool

# Sync to v2
dropper sync assets --from v1 --to v2

# Sync to 1.21.1
dropper sync assets --from v1 --to 1.21.1
```

### Update textures across versions
```bash
# Update texture in v1
# Sync to all versions
dropper sync textures --from v1 --to v2 --force
dropper sync textures --from v1 --to 1.20.1 --force
dropper sync textures --from v1 --to 1.21.1 --force
```

### Merge translations
```bash
# Add keys in v1
dropper sync lang --from v1 --to v2

# Custom translations in v2 are preserved!
```

### Safe exploration
```bash
# Always preview first
dropper sync assets --from v1 --to v2 --dry-run

# Then apply
dropper sync assets --from v1 --to v2
```

## Sync Behavior

### What Gets Synced

| Command | Syncs |
|---------|-------|
| `sync assets` | Models, textures, blockstates, lang, sounds |
| `sync lang` | Translation files (*.json in lang/) |
| `sync recipes` | Recipes, loot tables, tags, advancements |
| `sync textures` | PNG, JPG, GIF, BMP, TGA files |
| `sync models` | Item and block model JSON files |
| `sync blockstates` | Blockstate JSON files |

### Conflict Resolution

| Scenario | Without --force | With --force |
|----------|----------------|--------------|
| File in source only | Copy to target | Copy to target |
| File in target only | Keep in target | Keep in target |
| Files identical | Skip (no action) | Skip (no action) |
| Source newer | Copy to target | Copy to target |
| Target newer, different | **Keep target** | **Overwrite with source** |
| Lang files | **Merge** (always) | **Merge** (always) |

### Lang File Merging

Lang files are **always merged**, never replaced:
- Missing keys from source → Added to target
- Existing keys in target → **Preserved** (not overwritten)
- Result: Combined set of all keys

Example:
```
v1/en_us.json:        v2/en_us.json:           After sync:
{                     {                        {
  "key1": "value1",     "key1": "custom",        "key1": "custom",
  "key2": "value2"      "key3": "value3"         "key2": "value2",
}                     }                          "key3": "value3"
                                               }
```

## Tips

1. **Always use --dry-run first** to preview changes
2. **Lang files are safe** - custom translations never overwritten
3. **Default is safe** - target preserved on conflicts
4. **Use --force carefully** - overwrites your work
5. **Exclude patterns** - use for temp/test files
6. **Bidirectional sync** - merge changes from both sides

## Source/Target Formats

### Asset Packs
```bash
--from v1 --to v2
--from v1 --to v3
```
Resolves to: `versions/shared/v1/` and `versions/shared/v2/`

### Version Directories
```bash
--from 1.20.1 --to 1.21.1
```
Resolves to: `versions/1.20.1/` and `versions/1.21.1/`

### Mixed
```bash
--from v1 --to 1.21.1
--from 1.20.1 --to v2
```

## Output

### Successful Sync
```
ℹ Syncing assets: v1 -> v2

SYNC RESULTS:

Copied 15 file(s):
  - ruby_sword.json (Missing in target)
  - ruby_ore.json (Missing in target)
  ...

Merged 3 file(s):
  - en_us.json (Lang file merged)
  - es_es.json (Lang file merged)

Skipped 42 file(s) (already up-to-date)

✓ Sync completed: 15 copied, 3 merged, 42 skipped
```

### Dry-Run
```
ℹ Syncing assets: v1 -> v2
⚠ DRY RUN MODE: No changes will be made

DRY RUN RESULTS:

Would copy 15 file(s):
  - ruby_sword.json (Missing in target)
  ...

Would merge 3 file(s):
  - en_us.json (Lang file merged)

Would skip 42 file(s) (already up-to-date)

ℹ Dry run: 15 files would be synced, 0 conflicts
ℹ Run without --dry-run to apply changes
```

### Conflicts Detected
```
⚠ Conflicts detected (3 file(s)):
  - custom_model.json
  - modified_texture.png

ℹ Use --force to overwrite conflicting files
```

## Performance

- **Hash-based comparison** - Fast, accurate
- **Skips identical files** - No unnecessary I/O
- **Efficient for 100+ files** - Tested with 120 files
- **Parallel-safe** - Can run multiple syncs

## Data Safety

The sync command is designed to be **safe by default**:

1. **Preserves target work** - Conflicts kept in target
2. **Requires --force** - Explicit opt-in to overwrite
3. **Dry-run available** - Preview before apply
4. **Lang merge** - Never loses translations
5. **No destructive defaults** - Conservative behavior

## Error Handling

### Source not found
```
✗ Source directory not found: versions/shared/v1/assets
```
**Solution:** Check that source exists

### Target not found
```
# Not an error - target created automatically
```

### No config.yml
```
✗ No config.yml found. Are you in a Dropper project directory?
```
**Solution:** Run from project root

## Examples

### Example 1: New mod version
```bash
# Created v2 asset pack
dropper create asset-pack v2

# Sync all assets from v1
dropper sync assets --from v1 --to v2

# Customize v2-specific assets
# ...

# Build for all versions
dropper build
```

### Example 2: Translation updates
```bash
# Add new keys to v1/lang/en_us.json
# Sync to all versions
dropper sync lang --from v1 --to v2
dropper sync lang --from v1 --to 1.20.1
dropper sync lang --from v1 --to 1.21.1

# Custom translations in other versions preserved!
```

### Example 3: Texture update
```bash
# Update ruby_sword.png in v1
# Force sync to all versions
dropper sync textures --from v1 --to v2 --force
dropper sync textures --from v1 --to 1.20.1 --force
dropper sync textures --from v1 --to 1.21.1 --force
```

### Example 4: Safe exploration
```bash
# Preview what would change
dropper sync assets --from v1 --to v2 --dry-run

# Looks good? Apply
dropper sync assets --from v1 --to v2

# Made a mistake? Target preserved by default
# Your custom work is safe
```

## Test Coverage

43 E2E tests covering:
- Basic sync operations (6 tests)
- Diff detection (4 tests)
- Lang file merging (4 tests)
- Conflict resolution (2 tests)
- Dry-run mode (4 tests)
- Exclusion patterns (3 tests)
- Bidirectional sync (2 tests)
- Integration workflows (2 tests)
- Performance (2 tests)
- Edge cases (5 tests)
- Additional scenarios (9 tests)

Run tests:
```bash
./gradlew :src:cli:test --tests "SyncCommandE2ETest"
```
