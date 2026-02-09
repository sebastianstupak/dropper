# Migration Guide

The `dropper migrate` command automates migrations to new versions, loaders, mappings, and package refactoring.

## Commands

### Version Migration

Migrate your project to a new Minecraft version:

```bash
dropper migrate version 1.21.1
dropper migrate version 1.21.1 --from 1.20.4
dropper migrate version 1.21.1 --asset-pack v2
```

**Options:**
- `--from, -f` - Source version to migrate from (auto-detected if omitted)
- `--asset-pack, -p` - Asset pack to use (default: v1)
- `--dry-run` - Preview changes without executing
- `--auto-fix` - Automatically fix common API changes
- `--force` - Override safety checks

**What it does:**
1. Creates new version directory structure
2. Copies existing code from source version
3. Updates version config.yml
4. Detects API changes between versions
5. Applies auto-fixes for common patterns (if --auto-fix)
6. Updates root config.yml
7. Generates migration report

**Example:**

```bash
# Preview migration
dropper migrate version 1.21.1 --dry-run

# Migrate with auto-fix
dropper migrate version 1.21.1 --auto-fix

# Force overwrite existing version
dropper migrate version 1.21.1 --force
```

### Loader Migration

Add support for a new mod loader:

```bash
dropper migrate loader fabric
dropper migrate loader forge --version 1.21.1
dropper migrate loader neoforge
```

**Options:**
- `--version, -v` - Specific Minecraft version to add loader to (default: all)
- `--dry-run` - Preview changes without executing
- `--force` - Override safety checks

**What it does:**
1. Creates loader directory structure for each version
2. Generates loader-specific registration code
3. Updates version and root config files
4. Provides manual steps for build configuration

**Example:**

```bash
# Add NeoForge support to all versions
dropper migrate loader neoforge

# Add Fabric only to 1.21.1
dropper migrate loader fabric --version 1.21.1

# Preview changes
dropper migrate loader forge --dry-run
```

### Mappings Migration

Update mappings versions in build files:

```bash
dropper migrate mappings 1.21.1+build.1
```

**Options:**
- `--dry-run` - Preview changes without executing
- `--force` - Override safety checks

**What it does:**
1. Finds all build.gradle.kts files
2. Updates Yarn mappings (Fabric)
3. Updates Parchment mappings (Forge/NeoForge)
4. Generates report of updated files
5. Provides manual steps for Gradle refresh

**Example:**

```bash
# Update to new mappings
dropper migrate mappings 1.21.1+build.2

# Preview changes
dropper migrate mappings 1.21.1+build.2 --dry-run
```

### Package Refactoring

Refactor package names across the entire project:

```bash
dropper migrate refactor com.old.mod com.new.mod
```

**Options:**
- `--dry-run` - Preview changes without executing
- `--force` - Override safety checks

**What it does:**
1. Updates all package declarations
2. Moves files to new directory structure
3. Updates all imports
4. Updates config.yml
5. Deletes old directory structure

**Example:**

```bash
# Refactor package
dropper migrate refactor com.example.oldmod com.example.newmod

# Preview changes
dropper migrate refactor com.old com.new --dry-run
```

## API Change Detection

The migration system detects known API changes between Minecraft versions:

### Supported Detections

**1.20.4 → 1.21.1:**
- Block properties builder changes
- Creative tab reorganization
- Item properties changes
- Registry API updates

### Auto-Fix Patterns

With `--auto-fix`, the migrator can automatically fix:

1. **Block Properties:**
   ```java
   // Before
   .strength(1.5f)

   // After (auto-fixed)
   .destroyTime(1.5f)
   ```

2. **Creative Tabs:**
   ```java
   // Before
   CreativeModeTab.

   // After (auto-fixed)
   CreativeModeTabs.
   ```

3. **Import Updates:**
   - Automatically updates moved packages
   - Updates deprecated imports

### Manual Review Required

Some changes require manual review:
- Complex method signature changes
- Removed APIs without direct replacements
- Structural changes to registration systems

## Migration Reports

All migrations generate detailed reports showing:
- Operations executed
- Changes made
- Warnings
- Errors
- Manual steps required

### Report Sections

**Status:** Success or failure
**Operations executed:** Count of operations
**Changes made:** List of files created/modified
**Warnings:** Potential issues detected
**Manual steps required:** Actions you need to take

### Example Report

```
╔═══════════════════════════════════════════════════════════════╗
║               Migration Report                                ║
╚═══════════════════════════════════════════════════════════════╝

Status: ✓ SUCCESS
Operations executed: 42

Changes made:
  ✓ Created directory: versions/1_21_1
  ✓ Copied: Test.java
  ✓ Updated config: versions/1_21_1/config.yml
  ...

Warnings:
  ⚠ Detected 3 potential API changes

Manual steps required:
  → Review Block properties builder changes
  → Update build configuration
  → Rebuild project to verify compilation
```

## Best Practices

1. **Always use --dry-run first**
   ```bash
   dropper migrate version 1.21.1 --dry-run
   ```

2. **Commit before migrating**
   ```bash
   git commit -am "Before migration"
   dropper migrate version 1.21.1
   ```

3. **Enable auto-fix for common patterns**
   ```bash
   dropper migrate version 1.21.1 --auto-fix
   ```

4. **Review changes after migration**
   ```bash
   git diff
   git status
   ```

5. **Test compilation**
   ```bash
   dropper build
   ```

6. **Read the migration report**
   - Pay attention to warnings
   - Complete all manual steps
   - Review API changes

## Common Workflows

### Upgrading Minecraft Version

```bash
# 1. Preview migration
dropper migrate version 1.21.1 --dry-run

# 2. Commit current state
git add . && git commit -m "Before MC 1.21.1 migration"

# 3. Execute migration with auto-fix
dropper migrate version 1.21.1 --auto-fix

# 4. Review changes
git diff

# 5. Test build
dropper build --version 1.21.1

# 6. Commit migration
git add . && git commit -m "Migrate to MC 1.21.1"
```

### Adding Multi-Loader Support

```bash
# 1. Add NeoForge to all versions
dropper migrate loader neoforge

# 2. Add Fabric only to specific version
dropper migrate loader fabric --version 1.21.1

# 3. Update build configuration (manual step)
# Edit build.gradle.kts to include new loader

# 4. Test build
dropper build
```

### Updating Mappings

```bash
# 1. Check current mappings version
cat build.gradle.kts | grep yarn

# 2. Preview update
dropper migrate mappings 1.21.1+build.5 --dry-run

# 3. Execute update
dropper migrate mappings 1.21.1+build.5

# 4. Refresh Gradle
./gradlew --refresh-dependencies

# 5. Rebuild
dropper build
```

### Refactoring Packages

```bash
# 1. Preview refactor
dropper migrate refactor com.old.mod com.new.mod --dry-run

# 2. Commit before refactor
git add . && git commit -m "Before package refactor"

# 3. Execute refactor
dropper migrate refactor com.old.mod com.new.mod

# 4. Verify compilation
dropper build

# 5. Commit refactor
git add . && git commit -m "Refactor package to com.new.mod"
```

## Troubleshooting

### Migration Fails with Warnings

Use `--force` to override warnings:
```bash
dropper migrate version 1.21.1 --force
```

### Version Already Exists

Either:
- Use `--force` to overwrite
- Or manually delete the existing version first

### Auto-Fix Breaks Code

1. Review the changes: `git diff`
2. Revert if needed: `git checkout .`
3. Run without `--auto-fix`
4. Apply fixes manually

### Build Fails After Migration

1. Review migration report for manual steps
2. Check for API changes requiring manual fixes
3. Update dependencies if needed
4. Consult Minecraft version changelog

## Advanced Usage

### Chaining Migrations

```bash
# Migrate version then add loader
dropper migrate version 1.21.1 && \
dropper migrate loader neoforge --version 1.21.1
```

### Custom Asset Packs

```bash
dropper migrate version 1.21.1 --asset-pack v2
```

### Selective Loader Addition

```bash
# Add Fabric only to latest versions
dropper migrate loader fabric --version 1.21.1
dropper migrate loader fabric --version 1.20.4
```

## See Also

- [Project Structure](PROJECT_STRUCTURE.md)
- [Build System](BUILD.md)
- [Version Management](VERSIONING.md)
- [API Documentation](API.md)
