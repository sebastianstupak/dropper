# Migration Command Quick Reference

## Commands

### Version Migration
```bash
dropper migrate version <VERSION> [OPTIONS]
```
**Options:**
- `--from, -f <VERSION>` - Source version to migrate from
- `--asset-pack, -p <PACK>` - Asset pack to use (default: v1)
- `--dry-run` - Preview changes without executing
- `--auto-fix` - Automatically fix common API changes
- `--force` - Override safety checks

**Examples:**
```bash
dropper migrate version 1.21.1
dropper migrate version 1.21.1 --from 1.20.4 --auto-fix
dropper migrate version 1.21.1 --dry-run
```

---

### Loader Migration
```bash
dropper migrate loader <LOADER> [OPTIONS]
```
**Loaders:** `fabric`, `forge`, `neoforge`

**Options:**
- `--version, -v <VERSION>` - Specific MC version (default: all)
- `--dry-run` - Preview changes
- `--force` - Override safety checks

**Examples:**
```bash
dropper migrate loader neoforge
dropper migrate loader fabric --version 1.21.1
dropper migrate loader forge --dry-run
```

---

### Mappings Migration
```bash
dropper migrate mappings <VERSION> [OPTIONS]
```
**Options:**
- `--dry-run` - Preview changes
- `--force` - Override safety checks

**Examples:**
```bash
dropper migrate mappings 1.21.1+build.5
dropper migrate mappings 1.21.1+build.5 --dry-run
```

---

### Package Refactoring
```bash
dropper migrate refactor <OLD_PACKAGE> <NEW_PACKAGE> [OPTIONS]
```
**Options:**
- `--dry-run` - Preview changes
- `--force` - Override safety checks

**Examples:**
```bash
dropper migrate refactor com.old.mod com.new.mod
dropper migrate refactor com.example.test com.example.prod --dry-run
```

---

## Common Workflows

### Upgrade Minecraft Version
```bash
# 1. Preview
dropper migrate version 1.21.1 --dry-run

# 2. Execute with auto-fix
dropper migrate version 1.21.1 --auto-fix

# 3. Build and test
dropper build --version 1.21.1
```

### Add Multi-Loader Support
```bash
# Add NeoForge to all versions
dropper migrate loader neoforge

# Add Fabric to specific version
dropper migrate loader fabric --version 1.21.1
```

### Update Mappings
```bash
# Update and rebuild
dropper migrate mappings 1.21.1+build.5
./gradlew --refresh-dependencies
dropper build
```

### Refactor Package
```bash
# Preview refactor
dropper migrate refactor com.old com.new --dry-run

# Execute refactor
dropper migrate refactor com.old com.new

# Verify
dropper build
```

---

## API Auto-Fixes (1.20.4 → 1.21.1)

### Block Properties
```java
// Before
.strength(1.5f)

// After (auto-fixed)
.destroyTime(1.5f)
```

### Creative Tabs
```java
// Before
CreativeModeTab.

// After (auto-fixed)
CreativeModeTabs.
```

---

## Flags

| Flag | Description |
|------|-------------|
| `--dry-run` | Preview changes without executing |
| `--auto-fix` | Automatically fix common patterns (version only) |
| `--force` | Override warnings and safety checks |
| `--from <VERSION>` | Specify source version (version only) |
| `--asset-pack <PACK>` | Specify asset pack (version only) |
| `--version <VERSION>` | Specific MC version (loader only) |

---

## Migration Report Format

```
╔═══════════════════════════════════════════════════════════════╗
║               Migration Report                                ║
╚═══════════════════════════════════════════════════════════════╝

Status: ✓ SUCCESS / ✗ FAILED
Operations executed: <count>

Changes made:
  ✓ Change 1
  ✓ Change 2
  ...

Warnings:
  ⚠ Warning 1
  ⚠ Warning 2
  ...

Errors:
  ✗ Error 1
  ✗ Error 2
  ...

Manual steps required:
  → Step 1
  → Step 2
  ...
```

---

## Best Practices

1. **Always preview first:**
   ```bash
   dropper migrate <command> --dry-run
   ```

2. **Commit before migrating:**
   ```bash
   git add . && git commit -m "Before migration"
   ```

3. **Use auto-fix for common changes:**
   ```bash
   dropper migrate version 1.21.1 --auto-fix
   ```

4. **Review changes after migration:**
   ```bash
   git diff
   git status
   ```

5. **Test build after migration:**
   ```bash
   dropper build
   ```

---

## Troubleshooting

### Migration Fails with Warnings
```bash
# Use --force to override
dropper migrate version 1.21.1 --force
```

### Version Already Exists
```bash
# Option 1: Force overwrite
dropper migrate version 1.21.1 --force

# Option 2: Delete manually first
rm -rf versions/1_21_1
dropper migrate version 1.21.1
```

### Auto-Fix Breaks Code
```bash
# Revert changes
git checkout .

# Run without auto-fix
dropper migrate version 1.21.1
```

### Build Fails After Migration
1. Check migration report for manual steps
2. Review API changes
3. Update dependencies
4. Consult MC version changelog

---

## File Locations

**Migrators:** `src/cli/src/main/kotlin/dev/dropper/migrators/`
**Commands:** `src/cli/src/main/kotlin/dev/dropper/commands/migrate/`
**Tests:** `src/cli/src/test/kotlin/dev/dropper/integration/MigrateCommandE2ETest.kt`
**Docs:** `docs/MIGRATION.md`

---

## Test Coverage

**37 E2E Tests:**
- 8 version migration tests
- 6 loader migration tests
- 4 mappings migration tests
- 5 refactor migration tests
- 8 auto-fix tests
- 6 integration tests

Run tests:
```bash
./gradlew :src:cli:test --tests "MigrateCommandE2ETest"
```

---

## Related Commands

- `dropper create` - Create new components
- `dropper build` - Build project
- `dropper validate` - Validate structure
- `dropper dev` - Development mode
- `dropper list` - List components

---

For full documentation, see: `docs/MIGRATION.md`
