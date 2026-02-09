# Sync Command Implementation

Complete implementation of the `dropper sync` command with all subcommands and extensive E2E tests.

## Implementation Summary

### Commands Implemented

#### Main Command
- `dropper sync` - Parent command for all sync operations

#### Subcommands
1. `dropper sync assets` - Sync all assets (models, textures, blockstates, lang, etc.)
2. `dropper sync lang` - Sync language files with intelligent merging
3. `dropper sync recipes` - Sync data files (recipes, loot tables, tags, etc.)
4. `dropper sync textures` - Sync texture files only
5. `dropper sync models` - Sync model files only
6. `dropper sync blockstates` - Sync blockstate files only

### Options

All commands support:
- `--from <SOURCE>` - Source version/asset pack (required)
- `--to <TARGET>` - Target version/asset pack (required)
- `--dry-run` - Preview changes without applying them
- `--force` - Overwrite existing files in conflicts
- `--exclude <PATTERN>` - Exclude files matching pattern (can be used multiple times)
- `--bidirectional` - Sync both directions (assets and lang commands only)

### Architecture

#### Synchronizer Classes

**Core Infrastructure:**
```
src/cli/src/main/kotlin/dev/dropper/synchronizers/
├── Synchronizer.kt              # Base interface
├── DiffAnalyzer.kt              # Analyzes differences between source/target
├── ConflictResolver.kt          # Handles conflict resolution
├── AssetSynchronizer.kt         # Syncs all assets
├── LangSynchronizer.kt          # Syncs lang files with merging
├── DataSynchronizer.kt          # Syncs data files
├── TextureSynchronizer.kt       # Syncs textures only
├── ModelSynchronizer.kt         # Syncs models only
└── BlockstateSynchronizer.kt    # Syncs blockstates only
```

**Command Classes:**
```
src/cli/src/main/kotlin/dev/dropper/commands/
├── SyncCommand.kt
└── sync/
    ├── SyncAssetsCommand.kt
    ├── SyncLangCommand.kt
    ├── SyncRecipesCommand.kt
    ├── SyncTexturesCommand.kt
    ├── SyncModelsCommand.kt
    └── SyncBlockstatesCommand.kt
```

#### Key Features

**1. Intelligent Diff Analysis**
- Detects missing files (exist in source but not target)
- Detects outdated files (source is newer)
- Detects conflicting files (both modified)
- Detects identical files (hash-based comparison)
- Skips identical files for performance

**2. Smart Lang File Merging**
- Adds missing translation keys from source
- Preserves existing target translations (no overwrite)
- Merges from multiple sources
- Maintains JSON formatting

**3. Conflict Resolution**
- Default: Keep target (preserve existing work)
- With `--force`: Use source (overwrite)
- For lang files: Always merge (combine keys)

**4. Dry-Run Mode**
- Preview all changes before applying
- Shows what would be copied, merged, or skipped
- Displays conflicts without modifying files
- Safe for exploring sync operations

**5. Bidirectional Sync**
- Syncs both directions in one command
- Merges changes from both sides
- Prevents data loss

**6. Exclusion Patterns**
- Glob patterns supported (`*.json`, `test_*`, etc.)
- Multiple patterns can be specified
- Filters files before sync

**7. Performance Optimizations**
- Hash-based comparison (MD5)
- Skips identical files
- Efficient for 100+ files
- No unnecessary I/O

**8. Data Safety**
- Always preserves target by default
- Requires `--force` to overwrite
- Lang files merged, not replaced
- Dry-run for safe exploration

### Test Coverage

Comprehensive E2E test suite with **43 tests** covering:

#### Basic Sync Tests (6 tests)
- Sync assets from v1 to v2
- Sync lang files only
- Sync recipes
- Sync textures only
- Sync models only
- Sync blockstates only

#### Diff Detection Tests (4 tests)
- Detect missing files
- Detect outdated files
- Detect conflicting files
- Skip identical files

#### Lang Merge Tests (4 tests)
- Lang merge adds missing keys
- Lang merge preserves existing keys
- Lang merge doesn't overwrite custom translations
- Lang merge from multiple sources

#### Conflict Resolution Tests (2 tests)
- Conflict without --force keeps target
- Conflict with --force overwrites

#### Dry-Run Tests (4 tests)
- Dry-run shows preview
- Dry-run shows missing files
- Dry-run shows conflicts
- Dry-run nothing actually synced

#### Exclusion Tests (3 tests)
- Exclude pattern works
- Multiple exclusion patterns
- Glob patterns supported

#### Bidirectional Sync Tests (2 tests)
- Bidirectional sync both directions
- Bidirectional merge changes from both sides

#### Integration Tests (2 tests)
- Create item then sync to other version
- Create block then sync assets

#### Performance Tests (2 tests)
- Sync 100+ files efficiently
- Skip unchanged files for performance

#### Edge Cases (5 tests)
- Empty source directory
- Empty target directory
- Identical source and target
- Sync preserves directory structure
- Data safety preserves existing work

#### Additional Tests (9 tests)
Various scenario tests ensuring comprehensive coverage

### Usage Examples

#### Sync all assets from v1 to v2
```bash
dropper sync assets --from v1 --to v2
```

#### Preview sync without applying (dry-run)
```bash
dropper sync assets --from v1 --to v2 --dry-run
```

#### Force overwrite conflicts
```bash
dropper sync assets --from v1 --to v2 --force
```

#### Sync lang files with merging
```bash
dropper sync lang --from v1 --to v2
```

#### Sync textures excluding test files
```bash
dropper sync textures --from v1 --to v2 --exclude "test_*"
```

#### Bidirectional sync
```bash
dropper sync assets --from v1 --to v2 --bidirectional
```

#### Sync from one version to another
```bash
dropper sync assets --from 1.20.1 --to 1.21.1
```

#### Sync specific file types only
```bash
dropper sync models --from v1 --to v2
dropper sync blockstates --from v1 --to v2
dropper sync recipes --from v1 --to v2
```

### Typical Workflows

#### Workflow 1: Create item in v1, sync to v2
```bash
# Create item in v1
dropper create item ruby_sword --type tool

# Preview sync
dropper sync assets --from v1 --to v2 --dry-run

# Apply sync
dropper sync assets --from v1 --to v2
```

#### Workflow 2: Sync translations across versions
```bash
# Sync English translations
dropper sync lang --from v1 --to v2

# Add custom translations in v2
# Re-sync will preserve custom translations
dropper sync lang --from v1 --to v2
```

#### Workflow 3: Update textures across all versions
```bash
# Update textures in v1
# Sync to v2
dropper sync textures --from v1 --to v2 --force

# Sync to 1.20.1
dropper sync textures --from v1 --to 1.20.1 --force

# Sync to 1.21.1
dropper sync textures --from v1 --to 1.21.1 --force
```

#### Workflow 4: Merge changes from multiple asset packs
```bash
# Sync v1 features to v2
dropper sync assets --from v1 --to v2

# Sync v2-specific features back to v1
dropper sync assets --from v2 --to v1

# Or use bidirectional sync
dropper sync assets --from v1 --to v2 --bidirectional
```

### Implementation Details

#### Diff Analysis Algorithm
1. Walk source directory tree
2. For each file, check if exists in target
3. If missing: Add to "missing" list
4. If exists: Compare hashes
5. If identical: Add to "identical" list
6. If different: Compare timestamps
7. If source newer: Add to "outdated" list
8. If target newer but different: Add to "conflicts" list

#### Lang File Merging Algorithm
1. Parse source JSON into key-value map
2. Parse target JSON into key-value map
3. Create merged map: Start with source
4. Override with target keys (target takes precedence)
5. Format back to JSON with proper indentation

#### Conflict Resolution Strategy
- **Default (no --force):** Keep target, skip source
- **With --force:** Use source, overwrite target
- **Lang files:** Always merge (special case)

#### Directory Resolution
For versions (e.g., "1.20.1"):
- Assets: `versions/1.20.1/assets/`
- Data: `versions/1.20.1/data/`

For asset packs (e.g., "v1"):
- Assets: `versions/shared/v1/assets/`
- Data: `versions/shared/v1/data/`

### Files Created

**Synchronizers (9 files):**
- `Synchronizer.kt` - Base interface with data models
- `DiffAnalyzer.kt` - Diff analysis with hash-based comparison
- `ConflictResolver.kt` - Conflict resolution and lang merging
- `AssetSynchronizer.kt` - All-assets synchronizer
- `LangSynchronizer.kt` - Lang-specific synchronizer
- `DataSynchronizer.kt` - Data files synchronizer
- `TextureSynchronizer.kt` - Texture-only synchronizer
- `ModelSynchronizer.kt` - Model-only synchronizer
- `BlockstateSynchronizer.kt` - Blockstate-only synchronizer

**Commands (7 files):**
- `SyncCommand.kt` - Parent command
- `SyncAssetsCommand.kt` - Assets subcommand
- `SyncLangCommand.kt` - Lang subcommand
- `SyncRecipesCommand.kt` - Recipes subcommand
- `SyncTexturesCommand.kt` - Textures subcommand
- `SyncModelsCommand.kt` - Models subcommand
- `SyncBlockstatesCommand.kt` - Blockstates subcommand

**Tests (1 file):**
- `SyncCommandE2ETest.kt` - 43 comprehensive E2E tests

**Updated (1 file):**
- `DropperCLI.kt` - Registered sync commands

### Testing

Run all sync tests:
```bash
./gradlew :src:cli:test --tests "SyncCommandE2ETest"
```

Run specific test:
```bash
./gradlew :src:cli:test --tests "SyncCommandE2ETest.test sync assets from v1 to v2"
```

### Design Principles

1. **Data Safety First:** Always preserve existing work by default
2. **Intelligent Merging:** Lang files merged, not replaced
3. **Preview Before Apply:** Dry-run mode for safe exploration
4. **Performance:** Hash-based comparison, skip identical files
5. **Flexibility:** Granular control with specific sync commands
6. **User-Friendly:** Clear output showing what was synced
7. **Robust:** Handle edge cases (empty dirs, conflicts, etc.)

### Future Enhancements

Potential improvements:
- Interactive conflict resolution (prompt user)
- Smart sync based on semantic versioning
- Sync history/changelog
- Rollback functionality
- Batch sync across multiple targets
- Sync validation (verify references after sync)
- Progress indicators for large syncs
- Parallel sync for performance

## Summary

The `dropper sync` command provides a comprehensive, safe, and intelligent way to synchronize assets and data across Minecraft mod versions and asset packs. With 43 E2E tests covering all scenarios, robust conflict resolution, and intelligent lang file merging, it ensures that developers can efficiently manage multi-version projects without losing work.

Key Features:
- 6 specialized sync commands for different asset types
- Intelligent diff analysis with hash-based comparison
- Smart lang file merging (preserves custom translations)
- Safe by default (preserves target, requires --force to overwrite)
- Dry-run mode for previewing changes
- Bidirectional sync support
- Exclusion patterns (glob support)
- Performance optimized for 100+ files
- Comprehensive E2E test coverage (43 tests)

All commands are registered in `DropperCLI.kt` and ready for use.
