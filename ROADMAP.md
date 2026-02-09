# Dropper CLI - Development Roadmap

## Vision

Transform Dropper into a comprehensive, end-to-end Minecraft modding toolkit that handles the entire mod development lifecycle: from initialization to publication. The goal is to eliminate tedious boilerplate work and let modders focus on creativity.

---

## Current Status (Completed)

### ✅ Phase 0: Foundation (COMPLETE)

**Commands Implemented:**
- `dropper init` - Initialize new multi-loader projects
- `dropper create item` - Create items with assets
- `dropper create block` - Create blocks (12 types) with blockstates/models
- `dropper create entity` - Create entities with renderers (5 types)
- `dropper create recipe` - Create recipes (6 types)
- `dropper create enchantment` - Create enchantments with registration
- `dropper create biome` - Create biomes with worldgen data
- `dropper create tag` - Create tags for blocks/items/entities/fluids
- `dropper create version` - Add new Minecraft version support
- `dropper create asset-pack` - Create shared asset packs
- `dropper build` - Build all versions and loaders
- `dropper docs` - Generate CLI documentation

**Test Coverage:** 117+ tests across all create commands

**Architecture:** Multi-loader (Fabric, Forge, NeoForge) with layered structure

---

## Phase 1: Development Workflow (Q2 2026)

**Priority:** HIGH - Essential for daily development

### 1.1 `dropper dev` - Development Server

**Purpose:** Launch Minecraft in development mode with hot-reloading for rapid iteration.

#### Subcommands:

```bash
dropper dev run [OPTIONS]          # Launch dev environment
dropper dev client [OPTIONS]       # Launch client only
dropper dev server [OPTIONS]       # Launch server only
dropper dev test [OPTIONS]         # Run tests in dev environment
dropper dev reload                 # Hot reload changes
```

#### Options:
- `--version <MC_VERSION>` - Minecraft version to run (default: latest configured)
- `--loader <LOADER>` - Mod loader (fabric, forge, neoforge)
- `--debug` - Enable debug mode with breakpoints
- `--port <PORT>` - Server port (default: 25565)
- `--world <WORLD_NAME>` - World to load
- `--clean` - Start with fresh world

#### Implementation Details:

**File Structure:**
```
src/cli/src/main/kotlin/dev/dropper/commands/
├── DevCommand.kt                    # Main dev command
├── dev/
│   ├── DevRunCommand.kt            # Launch game
│   ├── DevClientCommand.kt         # Client-only mode
│   ├── DevServerCommand.kt         # Server-only mode
│   ├── DevTestCommand.kt           # Test runner
│   └── DevReloadCommand.kt         # Hot reload
```

**Technical Requirements:**
1. **Gradle Integration:**
   - Execute `runClient`, `runServer` Gradle tasks
   - Pass JVM arguments for debugging
   - Configure classpath for hot-reload

2. **Hot Reload Support:**
   - Watch file system for changes
   - Recompile changed classes
   - Use Java agent for class reloading (JVM HotSwap)
   - Fabric: Use Fabric Loader's development features
   - Forge/NeoForge: Use Forge development runtime

3. **Configuration:**
   - Store dev preferences in `.dropper/dev-config.yml`
   - Remember last used version/loader
   - Save world snapshots for quick testing

4. **Process Management:**
   - Track running Minecraft processes
   - Clean shutdown on Ctrl+C
   - Auto-restart on crashes (optional)

**Expected Output:**
```
[Dropper] Starting Minecraft 1.20.1 with Fabric loader...
[Dropper] Watching for file changes...
[Minecraft] [main/INFO]: Loading Minecraft 1.20.1 with Fabric Loader 0.16.0
[Dropper] Dev server ready! Connect to localhost:25565
[Dropper] Hot reload enabled - Edit files to see changes live
```

**Success Criteria:**
- ✅ Launch Minecraft in <30 seconds
- ✅ Hot reload works for 80% of code changes
- ✅ Supports all 3 loaders
- ✅ Clean process management
- ✅ Comprehensive error messages

**Testing:**
- Integration tests with actual Minecraft launch
- Mock Gradle task execution
- File watcher tests
- Process cleanup tests

---

### 1.2 `dropper validate` - Project Validation

**Purpose:** Catch errors before building by validating project structure, assets, and data files.

#### Subcommands:

```bash
dropper validate                   # Validate everything
dropper validate assets            # Check textures, models, blockstates
dropper validate metadata          # Check config.yml consistency
dropper validate structure         # Check directory structure
dropper validate recipes           # Check recipe JSON validity
dropper validate lang              # Find missing translations
dropper validate code              # Check Java/Kotlin syntax
dropper validate dependencies      # Check dependency conflicts
```

#### Options:
- `--fix` - Automatically fix issues when possible
- `--strict` - Enable strict validation (fail on warnings)
- `--format <FORMAT>` - Output format (text, json, junit)
- `--version <VERSION>` - Validate specific version only

#### Implementation Details:

**File Structure:**
```
src/cli/src/main/kotlin/dev/dropper/commands/
├── ValidateCommand.kt
├── validate/
│   ├── ValidateAssetsCommand.kt
│   ├── ValidateMetadataCommand.kt
│   ├── ValidateStructureCommand.kt
│   ├── ValidateRecipesCommand.kt
│   ├── ValidateLangCommand.kt
│   ├── ValidateCodeCommand.kt
│   └── ValidateDependenciesCommand.kt
└── validators/
    ├── AssetValidator.kt
    ├── MetadataValidator.kt
    ├── StructureValidator.kt
    ├── RecipeValidator.kt
    ├── LangValidator.kt
    ├── CodeValidator.kt
    └── DependencyValidator.kt
```

**Validation Rules:**

**1. Assets Validation:**
- ✅ Every item has a texture (or parent model)
- ✅ Every block has a blockstate JSON
- ✅ Every blockstate references existing models
- ✅ Every model references existing textures or parents
- ✅ Texture files are valid PNG format
- ✅ No unused textures (warn only)
- ✅ Consistent texture dimensions (16x16, 32x32, etc.)

**2. Metadata Validation:**
- ✅ `config.yml` has all required fields
- ✅ Mod ID follows naming conventions (lowercase, no spaces)
- ✅ Version numbers follow semantic versioning
- ✅ Minecraft versions exist and are compatible
- ✅ Loader versions are compatible with MC versions
- ✅ Asset pack references are valid

**3. Structure Validation:**
- ✅ Required directories exist (`shared/`, `versions/`, etc.)
- ✅ No files in wrong locations
- ✅ Java files have correct package declarations
- ✅ Asset pack inheritance is valid (no circular deps)

**4. Recipe Validation:**
- ✅ JSON syntax is valid
- ✅ Recipe types exist (crafting_shaped, smelting, etc.)
- ✅ Ingredient items exist
- ✅ Result items exist
- ✅ Recipe IDs are unique

**5. Lang Validation:**
- ✅ All items have translations
- ✅ All blocks have translations
- ✅ All entities have translations
- ✅ All enchantments have translations
- ✅ No missing or duplicate keys
- ✅ Consistent formatting across languages

**6. Code Validation:**
- ✅ Java/Kotlin syntax is valid (compile check)
- ✅ No unused imports
- ✅ Consistent code style
- ✅ No TODO/FIXME in production code (warn only)

**7. Dependency Validation:**
- ✅ No conflicting dependencies
- ✅ All dependencies available in repositories
- ✅ Version ranges are valid
- ✅ No circular dependencies

**Expected Output:**
```
[Dropper] Validating project...

✅ Metadata: Valid
✅ Structure: Valid
⚠️  Assets: 3 warnings
   - Missing texture: assets/testmod/textures/item/ruby.png
   - Unused texture: assets/testmod/textures/block/old_ore.png
   - Model references missing parent: models/block/custom.json
✅ Recipes: Valid
⚠️  Lang: 5 missing translations
   - item.testmod.ruby
   - block.testmod.custom_ore
   - entity.testmod.zombie_knight
   - enchantment.testmod.fire_aspect
   - biome.testmod.crystal_plains
✅ Code: Valid
✅ Dependencies: Valid

Validation complete: 2 errors, 8 warnings
Run with --fix to auto-fix fixable issues
```

**Auto-Fix Capabilities:**
- Generate missing texture placeholders
- Add missing lang entries with placeholder text
- Fix package declarations
- Remove unused imports
- Format JSON files

**Success Criteria:**
- ✅ Detect 95%+ of common errors
- ✅ Auto-fix 70%+ of fixable issues
- ✅ Run in <5 seconds for typical projects
- ✅ Integrate with CI/CD pipelines
- ✅ Helpful error messages with file:line references

**Testing:**
- Unit tests for each validator
- Integration tests with real project structures
- Test cases for common mistakes
- Performance tests for large projects

---

### 1.3 `dropper list` - Component Inventory

**Purpose:** Quick overview of all mod content with search and filtering.

#### Subcommands:

```bash
dropper list items                 # List all items
dropper list blocks                # List all blocks
dropper list entities              # List all entities
dropper list recipes               # List all recipes
dropper list enchantments          # List all enchantments
dropper list biomes                # List all biomes
dropper list tags                  # List all tags
dropper list all                   # Complete inventory
```

#### Options:
- `--format <FORMAT>` - Output format (table, json, csv, tree)
- `--version <VERSION>` - Filter by version
- `--loader <LOADER>` - Filter by loader
- `--search <QUERY>` - Search by name
- `--export <FILE>` - Export to file

#### Implementation Details:

**File Structure:**
```
src/cli/src/main/kotlin/dev/dropper/commands/
├── ListCommand.kt
├── list/
│   ├── ListItemsCommand.kt
│   ├── ListBlocksCommand.kt
│   ├── ListEntitiesCommand.kt
│   ├── ListRecipesCommand.kt
│   ├── ListEnchantmentsCommand.kt
│   ├── ListBiomesCommand.kt
│   ├── ListTagsCommand.kt
│   └── ListAllCommand.kt
└── indexers/
    ├── ItemIndexer.kt
    ├── BlockIndexer.kt
    ├── EntityIndexer.kt
    ├── RecipeIndexer.kt
    └── ComponentIndexer.kt (interface)
```

**Indexing Strategy:**
1. Scan `shared/common/` for registration code
2. Scan `versions/shared/*/` for assets and data
3. Build in-memory index with component metadata
4. Cache index in `.dropper/cache/index.json`
5. Invalidate cache on file changes

**Expected Output:**

```
$ dropper list items

ITEMS (15 total)
┌─────────────────────┬────────────┬────────────────┬────────────┐
│ Name                │ Type       │ Texture        │ Recipe     │
├─────────────────────┼────────────┼────────────────┼────────────┤
│ ruby                │ basic      │ ✅             │ ✅         │
│ ruby_sword          │ tool       │ ✅             │ ✅         │
│ ruby_pickaxe        │ tool       │ ✅             │ ✅         │
│ ruby_axe            │ tool       │ ✅             │ ✅         │
│ magic_wand          │ basic      │ ✅             │ ❌         │
│ custom_food         │ food       │ ⚠️  (missing)  │ ✅         │
└─────────────────────┴────────────┴────────────────┴────────────┘

$ dropper list blocks --format tree

BLOCKS (8 total)
└── testmod
    ├── Ores
    │   ├── ruby_ore
    │   └── deepslate_ruby_ore
    ├── Building
    │   ├── stone_bricks
    │   ├── stone_stairs
    │   └── stone_slab
    └── Decorative
        ├── custom_pillar
        └── magic_door

$ dropper list all --format json --export inventory.json
[Dropper] Exported component inventory to inventory.json (45 components)
```

**Success Criteria:**
- ✅ Accurate component detection
- ✅ Fast indexing (<1 second for typical projects)
- ✅ Multiple output formats
- ✅ Search and filtering
- ✅ Export capabilities

**Testing:**
- Unit tests for each indexer
- Test with various project structures
- Performance tests with large projects
- JSON/CSV export validation

---

## Phase 2: Quality of Life (Q3 2026)

**Priority:** MEDIUM - Improves developer experience

### 2.1 `dropper remove` - Safe Component Deletion

**Purpose:** Remove components and all associated files safely.

#### Usage:

```bash
dropper remove item <NAME>         # Remove item + assets
dropper remove block <NAME>        # Remove block + assets
dropper remove entity <NAME>       # Remove entity + all files
dropper remove recipe <NAME>       # Remove recipe
dropper remove enchantment <NAME>  # Remove enchantment
dropper remove biome <NAME>        # Remove biome
dropper remove tag <NAME>          # Remove tag
```

#### Options:
- `--dry-run` - Preview what would be deleted
- `--force` - Skip confirmation prompts
- `--keep-assets` - Remove code but keep textures/models
- `--version <VERSION>` - Remove from specific version only

#### Implementation Details:

**File Structure:**
```
src/cli/src/main/kotlin/dev/dropper/commands/
├── RemoveCommand.kt
├── remove/
│   ├── RemoveItemCommand.kt
│   ├── RemoveBlockCommand.kt
│   ├── RemoveEntityCommand.kt
│   ├── RemoveRecipeCommand.kt
│   ├── RemoveEnchantmentCommand.kt
│   ├── RemoveBiomeCommand.kt
│   └── RemoveTagCommand.kt
└── removers/
    ├── ComponentRemover.kt (interface)
    ├── ItemRemover.kt
    ├── BlockRemover.kt
    └── ...
```

**Removal Strategy:**

1. **Discovery Phase:**
   - Find all files related to component
   - Check for dependencies (other components using this)
   - Identify references in recipes, tags, etc.

2. **Analysis Phase:**
   - Determine safe deletion candidates
   - Warn about dependencies
   - Check for shared assets (used by multiple components)

3. **Deletion Phase:**
   - Remove registration code
   - Remove assets (textures, models, blockstates)
   - Remove data files (recipes, tags, loot tables)
   - Remove lang entries
   - Update caches

4. **Cleanup Phase:**
   - Remove empty directories
   - Update index
   - Report deleted files

**Expected Output:**

```
$ dropper remove item ruby_sword --dry-run

[Dropper] Analyzing component: ruby_sword

Files to be deleted:
  ✓ shared/common/src/main/java/com/testmod/items/RubySword.java
  ✓ versions/shared/v1/assets/testmod/models/item/ruby_sword.json
  ✓ versions/shared/v1/assets/testmod/textures/item/ruby_sword.png
  ✓ versions/shared/v1/data/testmod/recipes/ruby_sword.json

Lang entries to be removed:
  ✓ item.testmod.ruby_sword

⚠️  Warnings:
  - Tag 'minecraft:swords' references this item
  - Used in advancement 'testmod:craft_ruby_tools'

Run without --dry-run to delete (use --force to skip confirmation)

$ dropper remove item ruby_sword

[Dropper] Are you sure you want to delete 'ruby_sword'? (y/N): y
[Dropper] Removing ruby_sword...
  ✓ Deleted 4 files
  ✓ Removed 1 lang entry
  ⚠️  2 references remain (manual cleanup required)
[Dropper] Component removed successfully
```

**Safety Features:**
- Confirmation prompts (unless --force)
- Dry-run mode
- Dependency detection
- Backup creation before deletion
- Rollback capability

**Success Criteria:**
- ✅ Safe deletion with confirmations
- ✅ Detect all related files
- ✅ Warn about dependencies
- ✅ No accidental deletions
- ✅ Rollback capability

**Testing:**
- Test removal of each component type
- Test dependency detection
- Test dry-run mode
- Test rollback functionality
- Test with shared assets

---

### 2.2 `dropper rename` - Component Refactoring

**Purpose:** Rename components and update all references automatically.

#### Usage:

```bash
dropper rename item <OLD_NAME> <NEW_NAME>
dropper rename block <OLD_NAME> <NEW_NAME>
dropper rename entity <OLD_NAME> <NEW_NAME>
dropper rename enchantment <OLD_NAME> <NEW_NAME>
dropper rename mod <OLD_ID> <NEW_ID>              # Rename entire mod
dropper rename package <OLD_PKG> <NEW_PKG>        # Refactor package name
```

#### Options:
- `--dry-run` - Preview changes
- `--force` - Skip confirmation
- `--version <VERSION>` - Rename in specific version only

#### Implementation Details:

**File Structure:**
```
src/cli/src/main/kotlin/dev/dropper/commands/
├── RenameCommand.kt
├── rename/
│   ├── RenameItemCommand.kt
│   ├── RenameBlockCommand.kt
│   ├── RenameEntityCommand.kt
│   ├── RenameEnchantmentCommand.kt
│   ├── RenameModCommand.kt
│   └── RenamePackageCommand.kt
└── renamers/
    ├── ComponentRenamer.kt (interface)
    ├── FileRenamer.kt
    ├── ContentRenamer.kt
    └── ReferenceUpdater.kt
```

**Rename Strategy:**

1. **Discovery Phase:**
   - Find all files for component
   - Find all references (code, assets, data)
   - Build rename plan

2. **Validation Phase:**
   - Check new name doesn't conflict
   - Validate naming conventions
   - Check for external references

3. **Rename Phase:**
   - Rename Java/Kotlin classes
   - Update package declarations
   - Rename asset files
   - Update asset references
   - Rename data files
   - Update data references
   - Update lang keys
   - Update registration code

4. **Verification Phase:**
   - Validate project still compiles
   - Check no broken references
   - Update caches

**Expected Output:**

```
$ dropper rename item ruby_sword emerald_sword

[Dropper] Analyzing rename: ruby_sword → emerald_sword

Changes to be made:
  Code (3 files):
    ✓ RubySword.java → EmeraldSword.java
    ✓ Update registration in ItemRegistry.java
    ✓ Update reference in ToolItems.java

  Assets (2 files):
    ✓ ruby_sword.json → emerald_sword.json
    ✓ ruby_sword.png → emerald_sword.png

  Data (3 files):
    ✓ recipes/ruby_sword.json → recipes/emerald_sword.json
    ✓ Update tag minecraft:swords
    ✓ Update advancement craft_ruby_tools

  Lang (1 entry):
    ✓ item.testmod.ruby_sword → item.testmod.emerald_sword

Continue? (y/N): y
[Dropper] Renaming component...
  ✓ Renamed 8 files
  ✓ Updated 12 references
  ✓ Validation successful
[Dropper] Rename complete!
```

**Success Criteria:**
- ✅ Update all references automatically
- ✅ Validate project after rename
- ✅ No broken references
- ✅ Support undo/rollback
- ✅ Handle edge cases (shared assets, etc.)

**Testing:**
- Test each component type rename
- Test cross-references
- Test mod-wide rename
- Test package refactoring
- Test rollback

---

### 2.3 `dropper sync` - Asset Synchronization

**Purpose:** Keep assets synchronized across versions and asset packs.

#### Usage:

```bash
dropper sync assets                           # Sync all assets
dropper sync lang                            # Sync translations
dropper sync recipes --from v1 --to v2       # Sync data files
dropper sync textures --version 1.20.1       # Sync textures only
dropper sync models --version 1.21.1         # Sync models only
```

#### Options:
- `--from <SOURCE>` - Source version/asset pack
- `--to <TARGET>` - Target version/asset pack
- `--dry-run` - Preview changes
- `--force` - Overwrite existing files
- `--exclude <PATTERN>` - Exclude files matching pattern

#### Implementation Details:

**File Structure:**
```
src/cli/src/main/kotlin/dev/dropper/commands/
├── SyncCommand.kt
├── sync/
│   ├── SyncAssetsCommand.kt
│   ├── SyncLangCommand.kt
│   ├── SyncRecipesCommand.kt
│   ├── SyncTexturesCommand.kt
│   └── SyncModelsCommand.kt
└── synchronizers/
    ├── AssetSynchronizer.kt
    ├── LangSynchronizer.kt
    └── DataSynchronizer.kt
```

**Sync Strategy:**

1. **Diff Phase:**
   - Compare source and target
   - Identify missing files
   - Identify outdated files
   - Identify conflicts

2. **Resolution Phase:**
   - Determine sync actions (copy, update, skip)
   - Handle conflicts (use --force or prompt)
   - Validate compatibility

3. **Sync Phase:**
   - Copy missing files
   - Update outdated files
   - Merge lang files (add missing keys)
   - Update references if needed

4. **Verification Phase:**
   - Validate synced files
   - Report changes
   - Update caches

**Expected Output:**

```
$ dropper sync lang --from v1 --to v2

[Dropper] Synchronizing translations: v1 → v2

Analysis:
  Missing in v2: 15 entries
  Outdated in v2: 3 entries
  Conflicts: 0

Missing entries to add:
  ✓ item.testmod.ruby
  ✓ block.testmod.custom_ore
  ✓ entity.testmod.zombie_knight
  ... (12 more)

Continue? (y/N): y
[Dropper] Syncing translations...
  ✓ Added 15 missing entries
  ✓ Updated 3 outdated entries
[Dropper] Sync complete!
```

**Success Criteria:**
- ✅ Accurate diff detection
- ✅ Smart conflict resolution
- ✅ Preserve manual overrides
- ✅ Bidirectional sync support
- ✅ Dry-run mode

**Testing:**
- Test sync between versions
- Test sync between asset packs
- Test conflict resolution
- Test bidirectional sync
- Test partial sync

---

## Phase 3: Distribution (Q4 2026)

**Priority:** HIGH - Essential for releasing mods

### 3.1 `dropper publish` - Platform Publishing

**Purpose:** Automated publishing to Modrinth, CurseForge, and GitHub.

#### Usage:

```bash
dropper publish modrinth [OPTIONS]
dropper publish curseforge [OPTIONS]
dropper publish github [OPTIONS]
dropper publish all [OPTIONS]
```

#### Options:
- `--version <VERSION>` - Release version (default: from config.yml)
- `--changelog <FILE>` - Changelog file (markdown)
- `--auto-changelog` - Generate from git commits
- `--game-versions <VERSIONS>` - Minecraft versions (comma-separated)
- `--loaders <LOADERS>` - Mod loaders (comma-separated)
- `--release-type <TYPE>` - alpha, beta, release
- `--dependencies <DEPS>` - Dependencies (JSON file)
- `--dry-run` - Preview without publishing

#### Implementation Details:

**File Structure:**
```
src/cli/src/main/kotlin/dev/dropper/commands/
├── PublishCommand.kt
├── publish/
│   ├── PublishModrinthCommand.kt
│   ├── PublishCurseForgeCommand.kt
│   ├── PublishGitHubCommand.kt
│   └── PublishAllCommand.kt
└── publishers/
    ├── ModrinthPublisher.kt
    ├── CurseForgePublisher.kt
    ├── GitHubPublisher.kt
    └── ChangelogGenerator.kt
```

**Publishing Workflow:**

1. **Preparation Phase:**
   - Build all configured loaders
   - Generate changelog (from git or file)
   - Validate metadata
   - Check API credentials

2. **Upload Phase:**
   - Upload JAR files
   - Set version metadata
   - Set game version compatibility
   - Set loader compatibility
   - Add dependencies

3. **Release Phase:**
   - Publish release
   - Create GitHub tag/release
   - Update version in config.yml
   - Commit and push changes

**API Integration:**

**Modrinth:**
- Use Modrinth API v2
- Upload to `/version` endpoint
- Support for dependencies, game versions, loaders
- Automatic project creation if needed

**CurseForge:**
- Use CurseForge Upload API
- Support for relations (dependencies, incompatibilities)
- Game version mapping

**GitHub:**
- Use GitHub Releases API
- Create git tag
- Upload JAR as release asset
- Use changelog as release notes

**Configuration:**

`.dropper/publish-config.yml`:
```yaml
modrinth:
  project_id: "abc123"
  api_token: "${MODRINTH_TOKEN}"  # From environment

curseforge:
  project_id: 123456
  api_token: "${CURSEFORGE_TOKEN}"

github:
  repository: "username/repo"
  api_token: "${GITHUB_TOKEN}"

defaults:
  release_type: release
  auto_changelog: true
  git_tag: true
```

**Expected Output:**

```
$ dropper publish all --version 1.2.0 --auto-changelog

[Dropper] Publishing version 1.2.0 to all platforms...

Building:
  ✓ Built 1.20.1-fabric.jar
  ✓ Built 1.20.1-forge.jar
  ✓ Built 1.20.1-neoforge.jar
  ✓ Built 1.21.1-fabric.jar
  ✓ Built 1.21.1-forge.jar
  ✓ Built 1.21.1-neoforge.jar

Generating changelog from git commits:
  ✓ Found 12 commits since v1.1.0
  ✓ Generated CHANGELOG.md

Publishing to Modrinth:
  ✓ Uploaded 6 files
  ✓ Created version 1.2.0
  ✓ Published: https://modrinth.com/mod/testmod/version/1.2.0

Publishing to CurseForge:
  ✓ Uploaded 6 files
  ✓ Created version 1.2.0
  ✓ Published: https://www.curseforge.com/minecraft/mc-mods/testmod/files/123456

Publishing to GitHub:
  ✓ Created tag v1.2.0
  ✓ Created release
  ✓ Uploaded 6 files
  ✓ Published: https://github.com/username/testmod/releases/tag/v1.2.0

[Dropper] Successfully published to all platforms!
```

**Changelog Generation:**

Auto-generated from git commits:
```markdown
# Changelog - v1.2.0

## Added
- New entity: Zombie Knight
- New enchantment: Fire Aspect II
- New biome: Crystal Plains

## Changed
- Improved Ruby Sword damage
- Updated textures for custom blocks

## Fixed
- Fixed crash when opening custom GUI
- Fixed recipe conflict with vanilla items
```

**Success Criteria:**
- ✅ Upload to all 3 platforms
- ✅ Automatic changelog generation
- ✅ Secure credential management
- ✅ Retry logic for network failures
- ✅ Rollback on partial failure

**Testing:**
- Mock API tests for each platform
- Integration tests with sandbox accounts
- Test credential handling
- Test changelog generation
- Test error recovery

---

### 3.2 `dropper package` - Advanced Packaging

**Purpose:** Create platform-specific packages with proper metadata.

#### Usage:

```bash
dropper package modrinth                      # Package for Modrinth
dropper package curseforge                    # Package for CurseForge
dropper package bundle --versions 1.20,1.21   # Bundle multiple versions
dropper package universal                     # Universal JAR
```

#### Options:
- `--output <DIR>` - Output directory (default: build/packages/)
- `--include-sources` - Include source JAR
- `--include-javadoc` - Include javadoc JAR
- `--obfuscate` - Obfuscate code (optional)

#### Implementation Details:

**Platform-Specific Packaging:**

**Modrinth:**
- Include `modrinth.json` metadata
- Set featured versions
- Include project icon
- Add gallery images

**CurseForge:**
- Include `manifest.json`
- Set category tags
- Add screenshots

**Bundle:**
- Create ZIP with all versions
- Include README
- Include changelogs
- Include license

**Expected Output:**
```
$ dropper package bundle

[Dropper] Creating bundle package...
  ✓ Built 6 JARs
  ✓ Generated README.md
  ✓ Generated CHANGELOG.md
  ✓ Added license file
  ✓ Created bundle: build/packages/testmod-1.2.0-bundle.zip

Contents:
  - testmod-1.20.1-fabric-1.2.0.jar
  - testmod-1.20.1-forge-1.2.0.jar
  - testmod-1.20.1-neoforge-1.2.0.jar
  - testmod-1.21.1-fabric-1.2.0.jar
  - testmod-1.21.1-forge-1.2.0.jar
  - testmod-1.21.1-neoforge-1.2.0.jar
  - README.md
  - CHANGELOG.md
  - LICENSE
```

**Success Criteria:**
- ✅ Platform-specific metadata
- ✅ Proper file structure
- ✅ Include documentation
- ✅ Compression optimization

---

## Phase 4: Advanced Features (2027)

**Priority:** LOW - Nice to have

### 4.1 `dropper migrate` - Version/Loader Migration

**Purpose:** Automate migration to new Minecraft versions or add new loaders.

#### Usage:

```bash
dropper migrate version <NEW_VERSION>         # Migrate to new MC version
dropper migrate loader <LOADER>               # Add new loader support
dropper migrate mappings                      # Update to latest mappings
dropper migrate refactor <OLD> <NEW>          # Refactor package names
```

#### Implementation Details:

**Version Migration:**
1. Analyze API changes between versions
2. Update Minecraft version in config
3. Update loader versions
4. Migrate breaking changes (mapping updates)
5. Generate migration report

**Loader Addition:**
1. Create loader-specific directories
2. Generate loader registration code
3. Add build configuration
4. Update metadata files

**Mappings Update:**
1. Download latest mappings (Yarn/Mojmap/Parchment)
2. Remap class/method/field names
3. Update imports
4. Validate compilation

**Expected Output:**
```
$ dropper migrate version 1.21.4

[Dropper] Analyzing migration: 1.20.1 → 1.21.4

Breaking changes detected:
  ⚠️  Registry API changed (auto-fixable)
  ⚠️  Block settings renamed (auto-fixable)
  ⚠️  Item properties updated (manual review needed)

Performing migration:
  ✓ Updated config.yml
  ✓ Updated loader versions
  ✓ Applied 15 automatic fixes
  ⚠️  3 files need manual review

Migration complete! Review these files:
  - shared/common/ItemRegistry.java (line 42)
  - shared/common/BlockRegistry.java (line 67)
  - shared/neoforge/NeoForgePlatform.java (line 103)

Run 'dropper validate' to check for issues.
```

**Success Criteria:**
- ✅ Detect API changes
- ✅ Auto-fix common patterns
- ✅ Generate migration report
- ✅ Preserve custom code

---

### 4.2 `dropper import` - Project Conversion

**Purpose:** Import existing mods into Dropper structure.

#### Usage:

```bash
dropper import fabric <PATH>                  # Import Fabric mod
dropper import forge <PATH>                   # Import Forge mod
dropper import neoforge <PATH>                # Import NeoForge mod
dropper import convert --from fabric          # Convert to multi-loader
```

#### Implementation Details:

**Import Strategy:**
1. Analyze existing project structure
2. Map files to Dropper structure
3. Convert build configuration
4. Extract metadata
5. Reorganize files
6. Generate missing files

**Expected Output:**
```
$ dropper import fabric ./existing-mod

[Dropper] Analyzing Fabric mod...
  ✓ Detected: ExampleMod v1.0.0
  ✓ Minecraft version: 1.20.1
  ✓ Found 25 items, 12 blocks, 3 entities

Converting to Dropper structure:
  ✓ Moved 47 Java files
  ✓ Reorganized assets
  ✓ Converted build.gradle
  ✓ Generated config.yml
  ✓ Created asset pack v1

[Dropper] Import complete!
Project created at: ./existing-mod-dropper/

Next steps:
  cd existing-mod-dropper
  dropper validate
  dropper build
```

**Success Criteria:**
- ✅ Import from all 3 loaders
- ✅ Preserve all functionality
- ✅ Convert build config
- ✅ Reorganize properly

---

### 4.3 `dropper update` - Dependency Management

**Purpose:** Update Minecraft, loaders, and dependencies.

#### Usage:

```bash
dropper update check                          # Check for updates
dropper update minecraft                      # Update to latest MC
dropper update loaders                        # Update all loaders
dropper update dependencies                   # Update all deps
dropper update apply --all                    # Apply all updates
```

#### Implementation Details:

**Update Checks:**
1. Query Fabric/Forge/NeoForge APIs
2. Query Maven repositories
3. Check version compatibility
4. Generate update report

**Expected Output:**
```
$ dropper update check

[Dropper] Checking for updates...

Minecraft:
  Current: 1.20.1
  Latest:  1.21.4 ⬆️

Loaders:
  Fabric:   0.15.11 → 0.16.0 ⬆️
  Forge:    47.3.0 → 47.3.5 ⬆️
  NeoForge: 20.1.0 → 21.1.15 ⬆️

Dependencies:
  fabric-api: 0.92.0 → 0.95.0 ⬆️

Run 'dropper update apply --all' to update.
```

**Success Criteria:**
- ✅ Check all update sources
- ✅ Validate compatibility
- ✅ Safe update application
- ✅ Rollback on failure

---

### 4.4 `dropper export` - Export Utilities

**Purpose:** Export as datapacks, resource packs, or shared assets.

#### Usage:

```bash
dropper export datapack --version 1.20.1      # Export as datapack
dropper export resourcepack                   # Export as resource pack
dropper export assets --pack v1               # Export asset pack
```

#### Implementation Details:

**Datapack Export:**
1. Extract data files (recipes, tags, loot tables)
2. Add pack.mcmeta
3. Package as ZIP

**Resource Pack Export:**
1. Extract assets (textures, models, sounds)
2. Add pack.mcmeta
3. Package as ZIP

**Success Criteria:**
- ✅ Valid datapack format
- ✅ Valid resource pack format
- ✅ Proper pack.mcmeta

---

### 4.5 `dropper search` - Search Utilities

**Purpose:** Search for components, assets, and code.

#### Usage:

```bash
dropper search texture ruby                   # Find textures
dropper search model sword                    # Find models
dropper search code "ItemRegistry"            # Search in code
dropper search recipe smelting                # Find recipes
```

#### Expected Output:
```
$ dropper search texture ruby

Found 3 textures:
  ✓ versions/shared/v1/assets/testmod/textures/item/ruby.png
  ✓ versions/shared/v1/assets/testmod/textures/block/ruby_ore.png
  ✓ versions/1.20.1/assets/testmod/textures/block/deepslate_ruby_ore.png
```

---

### 4.6 `dropper template` - Template Management

**Purpose:** Create components from reusable templates.

#### Usage:

```bash
dropper template list                         # List templates
dropper template create armor-set             # Create from template
dropper template add ./my-template            # Add custom template
```

#### Built-in Templates:
- `armor-set` - Full armor set (helmet, chestplate, leggings, boots)
- `tool-set` - Full tool set (sword, pickaxe, axe, shovel, hoe)
- `ore-set` - Ore block + raw item + ingot + recipes
- `wood-set` - Log, planks, stairs, slab, fence, door, trapdoor
- `dimension` - Complete custom dimension with biome

**Success Criteria:**
- ✅ Flexible template system
- ✅ Custom template support
- ✅ Variable substitution

---

### 4.7 `dropper clean` - Cleanup Utilities

**Purpose:** Clean build artifacts and caches.

#### Usage:

```bash
dropper clean build                           # Clean build artifacts
dropper clean cache                           # Clean Gradle cache
dropper clean generated                       # Clean generated files
dropper clean all                             # Clean everything
```

**Success Criteria:**
- ✅ Remove build directories
- ✅ Clear caches
- ✅ Preserve source files

---

## Integration & Testing Strategy

### CI/CD Integration

**GitHub Actions Workflow:**
```yaml
name: Dropper CI

on: [push, pull_request]

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - name: Validate project
        run: ./gradlew :src:cli:run --args="validate --strict"

  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Run tests
        run: ./gradlew :src:cli:test

  build:
    runs-on: ubuntu-latest
    needs: [validate, test]
    steps:
      - uses: actions/checkout@v4
      - name: Build all versions
        run: ./gradlew build

  publish:
    runs-on: ubuntu-latest
    needs: build
    if: startsWith(github.ref, 'refs/tags/v')
    steps:
      - uses: actions/checkout@v4
      - name: Publish to platforms
        env:
          MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}
          CURSEFORGE_TOKEN: ${{ secrets.CURSEFORGE_TOKEN }}
        run: ./gradlew :src:cli:run --args="publish all --auto-changelog"
```

### Testing Strategy

**Unit Tests:**
- Every command has comprehensive test suite
- Mock file system operations
- Mock API calls
- Test edge cases

**Integration Tests:**
- Test with real project structures
- Test full workflows (create → validate → build → publish)
- Test cross-command interactions

**E2E Tests:**
- Test CLI commands end-to-end
- Test with actual Minecraft projects
- Verify generated mods work in-game

**Performance Tests:**
- Benchmark indexing/validation for large projects
- Test with 100+ items/blocks
- Measure build times

### Test Coverage Goals

- **Phase 1:** 90%+ coverage
- **Phase 2:** 85%+ coverage
- **Phase 3:** 80%+ coverage
- **Phase 4:** 75%+ coverage

---

## Documentation Strategy

### User Documentation

**docs/commands/**
- `DEV.md` - Development workflow guide
- `VALIDATE.md` - Validation guide
- `PUBLISH.md` - Publishing guide
- `MIGRATE.md` - Migration guide
- `IMPORT.md` - Import guide

### Developer Documentation

**docs/architecture/**
- `COMMAND_STRUCTURE.md` - How to add new commands
- `VALIDATORS.md` - Writing validators
- `PUBLISHERS.md` - Adding new platforms
- `INDEXERS.md` - Component indexing

### Video Tutorials

- Getting Started with Dropper
- Creating Your First Mod
- Publishing to Modrinth
- Multi-Version Development

---

## Success Metrics

### Phase 1 (Dev Workflow)
- ✅ 80% of devs use `dropper dev` daily
- ✅ `dropper validate` catches 95%+ of issues
- ✅ Dev server starts in <30s

### Phase 2 (Quality of Life)
- ✅ `dropper remove` prevents accidental deletions
- ✅ `dropper rename` saves 30+ minutes per refactor
- ✅ `dropper sync` keeps 3+ versions in sync

### Phase 3 (Distribution)
- ✅ `dropper publish` reduces publish time to <5 minutes
- ✅ 50%+ of Dropper projects use automated publishing
- ✅ Zero failed publishes due to metadata errors

### Phase 4 (Advanced)
- ✅ `dropper migrate` successfully migrates 90%+ of code
- ✅ `dropper import` converts existing mods in <10 minutes
- ✅ `dropper update` keeps dependencies current

---

## Timeline

```
Q2 2026  ████████░░░░░░░░░░░░  Phase 1: Dev Workflow
Q3 2026  ░░░░░░░░████████░░░░  Phase 2: Quality of Life
Q4 2026  ░░░░░░░░░░░░░░██████  Phase 3: Distribution
2027     ░░░░░░░░░░░░░░░░░░██  Phase 4: Advanced
```

**Milestone Releases:**
- **v0.2.0** (Q2 2026) - Phase 1 complete
- **v0.3.0** (Q3 2026) - Phase 2 complete
- **v1.0.0** (Q4 2026) - Phase 3 complete (production ready)
- **v1.5.0** (2027) - Phase 4 complete (feature complete)

---

## Risk Mitigation

### Technical Risks

1. **Minecraft API Changes**
   - Mitigation: Version-specific code generation
   - Abstraction layers for common patterns

2. **Platform API Changes**
   - Mitigation: API versioning
   - Graceful degradation

3. **Performance Issues**
   - Mitigation: Aggressive caching
   - Parallel processing
   - Incremental operations

### User Experience Risks

1. **Complex CLI**
   - Mitigation: Excellent help text
   - Interactive prompts
   - Guided workflows

2. **Breaking Changes**
   - Mitigation: Semantic versioning
   - Migration guides
   - Backward compatibility where possible

3. **Learning Curve**
   - Mitigation: Comprehensive docs
   - Video tutorials
   - Example projects

---

## Community & Feedback

### Feedback Channels

- GitHub Issues for bugs
- GitHub Discussions for features
- Discord server for support
- User surveys after each phase

### Beta Testing

- Alpha releases for Phase 1-3 features
- Beta testing program with 50+ modders
- Feedback integration before stable releases

---

## Conclusion

This roadmap transforms Dropper from a project initialization tool into a comprehensive modding toolkit covering the entire development lifecycle. By Phase 3 completion, Dropper will be production-ready for serious mod development. Phase 4 adds advanced features for power users.

**Core Philosophy:**
- **Convention over Configuration** - Smart defaults
- **DRY** - Don't Repeat Yourself across versions/loaders
- **Developer Experience** - Fast, reliable, helpful
- **Safety** - Prevent mistakes, enable recovery
- **Automation** - Eliminate tedious work

**Target Audience:**
- Beginners: Easy project setup and guided workflows
- Intermediate: Powerful commands for common tasks
- Advanced: Full control with automation and customization

The end goal: **Make multi-loader, multi-version Minecraft modding as easy as single-loader development.**
