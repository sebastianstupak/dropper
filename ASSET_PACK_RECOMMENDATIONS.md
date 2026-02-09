# Asset Pack System - Recommendations & Improvements

## Overview

Based on comprehensive testing of the asset pack system, this document outlines specific recommendations for improvements, organized by priority.

---

## Priority 1: Critical Validation (Implement Before 1.0)

### 1.1 Add Parent Asset Pack Validation

**Issue:** Currently, creating an asset pack with a non-existent parent succeeds but fails at build time.

**Current Code (AddAssetPackCommand.kt:58):**
```kotlin
appendLine("  inherits: ${inherits ?: "null"}")
// No validation that parent exists!
```

**Recommended Fix:**
```kotlin
override fun run() {
    val projectDir = File(System.getProperty("user.dir"))
    val configFile = File(projectDir, "config.yml")

    if (!configFile.exists()) {
        Logger.error("No config.yml found. Are you in a Dropper project directory?")
        return
    }

    // ADD THIS: Validate parent exists
    if (inherits != null) {
        val parentDir = File(projectDir, "versions/shared/$inherits")
        if (!parentDir.exists()) {
            Logger.error("Parent asset pack '$inherits' does not exist")

            // List available packs
            val availablePacks = File(projectDir, "versions/shared")
                .listFiles()
                ?.filter { it.isDirectory }
                ?.map { it.name }
                ?.sorted()
                ?: emptyList()

            if (availablePacks.isNotEmpty()) {
                Logger.info("Available asset packs: ${availablePacks.joinToString(", ")}")
            } else {
                Logger.info("No asset packs exist yet. Create a base pack first.")
            }
            return
        }
    }

    // ... rest of command
}
```

**Benefits:**
- Prevents confusing build errors
- Provides immediate feedback
- Lists available packs for user

**Estimated Effort:** 30 minutes
**Impact:** High (better UX, fewer support questions)

---

### 1.2 Add Asset Pack Version Format Validation

**Issue:** Asset pack version accepts any string, leading to inconsistent naming.

**Recommended Implementation:**
```kotlin
private fun validatePackVersion(version: String): Boolean {
    // Warn if not following convention, but don't block
    if (!version.matches(Regex("v\\d+"))) {
        Logger.warn("Asset pack version '$version' doesn't follow convention")
        Logger.info("Recommended format: v1, v2, v3, etc.")
        Logger.info("Proceeding anyway...")
    }

    // Block obviously invalid names
    if (version.contains("..") || version.contains("/") || version.contains("\\")) {
        Logger.error("Invalid asset pack version: $version")
        Logger.info("Version cannot contain path separators or '..'")
        return false
    }

    return true
}

override fun run() {
    // Add at start
    if (!validatePackVersion(packVersion)) {
        return
    }
    // ... rest of command
}
```

**Benefits:**
- Consistent naming convention
- Security (prevents path traversal)
- Better project organization

**Estimated Effort:** 20 minutes
**Impact:** Medium (prevents edge case issues)

---

## Priority 2: Essential Tooling (Implement Soon)

### 2.1 Add "List Asset Packs" Command

**Issue:** No way to see available asset packs or inheritance relationships.

**Proposed Command:**
```bash
dropper list asset-packs
```

**Implementation:**
```kotlin
package dev.dropper.commands

import com.github.ajalt.clikt.core.CliktCommand
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import org.yaml.snakeyaml.Yaml
import java.io.File

class ListAssetPacksCommand : CliktCommand(
    name = "asset-packs",
    help = "List all asset packs and their inheritance relationships"
) {
    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val sharedDir = File(projectDir, "versions/shared")

        if (!sharedDir.exists()) {
            Logger.warn("No asset packs found in versions/shared/")
            return
        }

        val packs = sharedDir.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name }
            ?: emptyList()

        if (packs.isEmpty()) {
            Logger.warn("No asset packs found")
            return
        }

        Logger.info("Asset Packs:\n")

        // Build inheritance map
        val packData = packs.associate { packDir ->
            val config = loadPackConfig(packDir)
            packDir.name to config
        }

        // Find root packs (no parent)
        val rootPacks = packData.filter { (_, config) -> config.parent == null }

        // Print tree
        rootPacks.forEach { (name, config) ->
            printPackTree(name, packData, "", true)
        }

        // Print orphaned packs (parent doesn't exist)
        val orphaned = packData.filter { (_, config) ->
            config.parent != null && !packData.containsKey(config.parent)
        }

        if (orphaned.isNotEmpty()) {
            Logger.warn("\nOrphaned packs (parent doesn't exist):")
            orphaned.forEach { (name, config) ->
                Logger.warn("  $name → ${config.parent} (missing)")
            }
        }
    }

    private fun printPackTree(
        packName: String,
        allPacks: Map<String, PackInfo>,
        prefix: String,
        isLast: Boolean
    ) {
        val pack = allPacks[packName] ?: return
        val connector = if (isLast) "└─" else "├─"

        println("$prefix$connector $packName")
        println("$prefix${if (isLast) "  " else "│ "}   MC: ${pack.mcVersions.joinToString(", ")}")
        println("$prefix${if (isLast) "  " else "│ "}   Used by: ${pack.usedBy.joinToString(", ")}")

        // Find children
        val children = allPacks.filter { (_, config) -> config.parent == packName }
        val newPrefix = prefix + if (isLast) "   " else "│  "

        children.entries.forEachIndexed { index, (childName, _) ->
            printPackTree(childName, allPacks, newPrefix, index == children.size - 1)
        }
    }

    private fun loadPackConfig(packDir: File): PackInfo {
        val configFile = File(packDir, "config.yml")
        if (!configFile.exists()) {
            return PackInfo(null, emptyList(), emptyList())
        }

        val yaml = Yaml()
        val config = configFile.inputStream().use {
            yaml.load<Map<String, Any>>(it)
        }

        val assetPack = config["asset_pack"] as? Map<String, Any> ?: emptyMap()
        val parent = assetPack["inherits"] as? String
        val mcVersions = assetPack["minecraft_versions"] as? List<String> ?: emptyList()

        return PackInfo(parent, mcVersions, emptyList())
    }

    data class PackInfo(
        val parent: String?,
        val mcVersions: List<String>,
        val usedBy: List<String>
    )
}
```

**Register in ListCommand.kt:**
```kotlin
.subcommands(
    ListItemsCommand(),
    ListBlocksCommand(),
    ListAssetPacksCommand()  // Add this
)
```

**Example Output:**
```
Asset Packs:

└─ v1
   MC: 1.20.1, 1.20.4
   Used by: 1_20_1, 1_20_4
   ├─ v2
   │  MC: 1.21.1
   │  Used by: 1_21_1
   │  └─ v3
   │     MC: 1.21.4
   │     Used by: none
   └─ v2-experimental
      MC: 1.21.1
      Used by: none
```

**Benefits:**
- Visualize inheritance tree
- See which versions use which packs
- Identify orphaned or unused packs
- Great for documentation

**Estimated Effort:** 2-3 hours
**Impact:** High (major UX improvement)

---

### 2.2 Add "Inspect Asset Pack" Command

**Proposed Command:**
```bash
dropper inspect asset-pack v2
```

**Implementation:**
```kotlin
class InspectAssetPackCommand : CliktCommand(
    name = "inspect",
    help = "Show detailed information about an asset pack"
) {
    private val packVersion by argument(
        name = "PACK_VERSION",
        help = "Asset pack to inspect (e.g., v2)"
    )

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val packDir = File(projectDir, "versions/shared/$packVersion")

        if (!packDir.exists()) {
            Logger.error("Asset pack '$packVersion' not found")
            return
        }

        Logger.info("Asset Pack: $packVersion\n")

        // Load config
        val config = loadConfig(packDir)
        Logger.info("Configuration:")
        Logger.info("  Inherits: ${config.parent ?: "none (base pack)"}")
        Logger.info("  MC Versions: ${config.mcVersions.joinToString(", ")}")
        Logger.info("  Description: ${config.description}")

        // Show inheritance chain
        Logger.info("\nInheritance Chain:")
        val chain = resolveChain(projectDir, packVersion)
        chain.forEachIndexed { index, pack ->
            Logger.info("  ${index + 1}. $pack")
        }

        // Count assets
        val assetCounts = countAssets(packDir)
        Logger.info("\nAsset Counts:")
        assetCounts.forEach { (type, count) ->
            Logger.info("  $type: $count files")
        }

        // Show which versions use this pack
        val usedBy = findUsedBy(projectDir, packVersion)
        Logger.info("\nUsed By:")
        if (usedBy.isNotEmpty()) {
            usedBy.forEach { version ->
                Logger.info("  - $version")
            }
        } else {
            Logger.warn("  No versions currently use this pack")
        }
    }

    private fun countAssets(packDir: File): Map<String, Int> {
        return mapOf(
            "Models" to countFiles(packDir, "assets/**/models"),
            "Textures" to countFiles(packDir, "assets/**/textures"),
            "Blockstates" to countFiles(packDir, "assets/**/blockstates"),
            "Recipes" to countFiles(packDir, "data/**/recipes"),
            "Loot Tables" to countFiles(packDir, "data/**/loot_tables"),
            "Tags" to countFiles(packDir, "data/**/tags")
        )
    }

    // ... helper methods
}
```

**Example Output:**
```
Asset Pack: v2

Configuration:
  Inherits: v1
  MC Versions: 1.21.1, 1.21.2
  Description: Asset pack for 1.21.x versions

Inheritance Chain:
  1. v1 (base)
  2. v2

Asset Counts:
  Models: 15 files
  Textures: 12 files
  Blockstates: 8 files
  Recipes: 10 files
  Loot Tables: 3 files
  Tags: 5 files

Used By:
  - 1_21_1
  - 1_21_2
```

**Benefits:**
- Quick overview of pack contents
- Verify inheritance chain
- Identify unused packs
- Helpful for debugging

**Estimated Effort:** 2-3 hours
**Impact:** Medium (nice to have, aids debugging)

---

## Priority 3: Validation Enhancements

### 3.1 Add Asset Pack Validation to "dropper validate"

**Current State:** `dropper validate` checks items, blocks, recipes, etc., but not asset packs.

**Proposed Checks:**
1. ✅ All asset pack parents exist
2. ✅ No circular inheritance
3. ✅ Asset pack configs are valid YAML
4. ⚠️ Warn about unused asset packs
5. ⚠️ Warn about missing assets in inheritance chain
6. ⚠️ Validate MC version references

**Implementation:**
```kotlin
class AssetPackValidator {
    fun validate(projectDir: File): List<ValidationError> {
        val errors = mutableListOf<ValidationError>()

        val sharedDir = File(projectDir, "versions/shared")
        if (!sharedDir.exists()) return errors

        val packs = sharedDir.listFiles()?.filter { it.isDirectory } ?: return errors

        packs.forEach { packDir ->
            val packName = packDir.name

            // Check 1: Valid config
            val configFile = File(packDir, "config.yml")
            if (!configFile.exists()) {
                errors.add(ValidationError(
                    type = "error",
                    message = "Asset pack '$packName' missing config.yml"
                ))
            } else {
                // Check 2: Parent exists
                val config = loadConfig(configFile)
                if (config.parent != null) {
                    val parentDir = File(sharedDir, config.parent)
                    if (!parentDir.exists()) {
                        errors.add(ValidationError(
                            type = "error",
                            message = "Asset pack '$packName' inherits '$config.parent' which doesn't exist"
                        ))
                    }
                }
            }

            // Check 3: Is pack used?
            val usedBy = findVersionsUsingPack(projectDir, packName)
            if (usedBy.isEmpty()) {
                errors.add(ValidationError(
                    type = "warning",
                    message = "Asset pack '$packName' is not used by any version"
                ))
            }
        }

        // Check 4: Circular inheritance
        packs.forEach { packDir ->
            try {
                resolveInheritanceChain(projectDir, packDir.name)
            } catch (e: IllegalStateException) {
                errors.add(ValidationError(
                    type = "error",
                    message = "Circular inheritance detected: ${e.message}"
                ))
            }
        }

        return errors
    }
}
```

**Add to ValidateCommand:**
```kotlin
// In ValidateCommand.run()
val assetPackValidator = AssetPackValidator()
val assetPackErrors = assetPackValidator.validate(projectDir)

if (assetPackErrors.isNotEmpty()) {
    Logger.warn("\nAsset Pack Issues:")
    assetPackErrors.forEach { error ->
        when (error.type) {
            "error" -> Logger.error("  ✗ ${error.message}")
            "warning" -> Logger.warn("  ⚠ ${error.message}")
        }
    }
}
```

**Benefits:**
- Catch configuration errors early
- Prevent circular inheritance at validation time
- Identify unused assets
- Better overall project health

**Estimated Effort:** 2 hours
**Impact:** Medium (improves robustness)

---

## Priority 4: Documentation Improvements

### 4.1 Enhanced AGENTS.md Section

**Current:** Basic explanation of asset packs
**Proposed:** Add detailed examples, diagrams, and troubleshooting

**Enhanced Section:**
```markdown
## Asset Pack System

### Overview

Dropper uses a cascading layer system for managing multi-version assets. This allows you to:
- Share common assets across Minecraft versions
- Override specific assets for newer versions
- Maintain a clean, DRY codebase

### Asset Pack Structure

```
versions/shared/
├── v1/                      # Base asset pack
│   ├── config.yml           # Pack metadata
│   ├── assets/              # Textures, models, sounds
│   ├── data/                # Recipes, loot tables, tags
│   └── common/              # Optional version-specific code
│       └── src/main/java/
└── v2/                      # Child asset pack
    ├── config.yml           # Inherits from v1
    ├── assets/              # Overrides or additions
    └── data/
```

### Creating Asset Packs

```bash
# Create base pack
dropper create asset-pack v1

# Create pack inheriting from v1
dropper create asset-pack v2 --inherits v1

# Create with MC version metadata
dropper create asset-pack v3 \
  --inherits v2 \
  --mc-versions 1.21.1,1.21.2
```

### Inheritance Example

```yaml
# v2/config.yml
asset_pack:
  version: "v2"
  inherits: v1              # Inherits all assets from v1
  minecraft_versions: [1.21.1]
  description: "1.21+ updates"
```

### How Asset Resolution Works

When building MC 1.21.1 (using v2) for Fabric:

**Resolution Order (lowest to highest priority):**
1. `versions/shared/v1/assets/` ← Base assets
2. `versions/shared/v2/assets/` ← v2 overrides
3. `versions/1_21_1/assets/` ← Version-specific
4. `versions/1_21_1/fabric/assets/` ← Loader-specific (wins)

**Later directories override earlier ones.**

### Visual Inheritance Tree

```
v1 (1.20.x)
├─ ruby_sword.json     # Original model
├─ ruby_sword.png      # Texture (shared)
└─ ruby_sword.json     # Recipe

v2 (1.21.x) inherits v1
├─ ruby_sword.json     # Updated model (overrides v1)
└─ (inherits texture and recipe from v1)
```

### Common Patterns

#### Pattern 1: Version-Specific Assets
```
v1: Shared assets for 1.20.x
v2: Updated assets for 1.21.x (inherits v1)
v3: Updated assets for 1.22.x (inherits v2)
```

#### Pattern 2: Experimental Features
```
v1: Stable assets
v2-experimental: Test new features (inherits v1)
```

#### Pattern 3: Multiple Branches
```
v1: Base
├─ v2-fabric: Fabric-specific features
└─ v2-forge: Forge-specific features
```

### Troubleshooting

**Q: My assets aren't showing up in-game**
A: Check the inheritance chain:
```bash
dropper list asset-packs
```

**Q: I changed an asset but it's still using the old one**
A: Clean and rebuild:
```bash
./gradlew clean build
```

**Q: How do I see which versions use which packs?**
A: List versions and check their configs:
```bash
dropper list versions
cat versions/1_21_1/config.yml
```

### Best Practices

✅ **DO:**
- Use semantic versioning (v1, v2, v3)
- Create base pack (v1) first
- Document what each pack is for
- Use inheritance to reduce duplication

❌ **DON'T:**
- Create circular inheritance (v2→v3→v2)
- Mix assets from different MC versions in same pack
- Create deep inheritance chains (>5 levels)
- Forget to assign packs to versions
```

**Benefits:**
- Clearer understanding for new users
- Visual examples aid learning
- Troubleshooting section reduces support burden
- Best practices guide prevents common mistakes

**Estimated Effort:** 1 hour
**Impact:** High (reduces learning curve)

---

### 4.2 Add "Cascading Layers" Diagram

**Proposed:** Visual diagram showing layer priority

```
┌─────────────────────────────────────────┐
│  HIGHEST PRIORITY (wins conflicts)      │
├─────────────────────────────────────────┤
│  versions/{mc}/{loader}/assets/         │  ← Loader-specific
├─────────────────────────────────────────┤
│  versions/{mc}/assets/                  │  ← Version-specific
├─────────────────────────────────────────┤
│  versions/shared/v3/assets/             │  ← Asset pack v3
├─────────────────────────────────────────┤
│  versions/shared/v2/assets/             │  ← Asset pack v2
├─────────────────────────────────────────┤
│  versions/shared/v1/assets/             │  ← Base asset pack
├─────────────────────────────────────────┤
│  LOWEST PRIORITY (overridden)           │
└─────────────────────────────────────────┘

Example: ruby_sword.json resolution
• If exists in fabric/: USE THIS ✓
• Else if in 1_21_1/: USE THIS
• Else if in v2/: USE THIS
• Else if in v1/: USE THIS
• Else: FILE NOT FOUND
```

**Location:** Add to AGENTS.md and README.md

**Benefits:**
- Instantly understand priority system
- Visual learners benefit
- Reduces confusion about overrides

**Estimated Effort:** 30 minutes
**Impact:** Medium (clarity improvement)

---

## Priority 5: Developer Experience

### 5.1 Interactive Asset Pack Creation Wizard

**Proposed Command:**
```bash
dropper create asset-pack --interactive
```

**Flow:**
```
? Asset pack version (e.g., v2): v2
? Inherit from existing pack? (Y/n): Y
? Select parent pack:
  > v1
    v2-experimental

? Add MC version metadata? (y/N): Y
? Enter MC versions (comma-separated): 1.21.1, 1.21.2
? Description: "Updated assets for 1.21.x"

Creating asset pack 'v2'...
✓ Created directory structure
✓ Generated config.yml
✓ Asset pack ready!

Next steps:
  1. Add assets to: versions/shared/v2/assets/
  2. Assign to versions: dropper add version 1.21.1 --asset-pack v2
```

**Benefits:**
- Beginner-friendly
- Prevents mistakes
- Guides user through process
- Validates input interactively

**Estimated Effort:** 3-4 hours
**Impact:** Medium (better onboarding)

---

### 5.2 Asset Pack Migration Helper

**Issue:** Moving assets between packs is manual and error-prone.

**Proposed Command:**
```bash
dropper migrate asset-pack --from v1 --to v2 --pattern "ruby_*"
```

**Implementation:**
```kotlin
class MigrateAssetPackCommand : CliktCommand(
    name = "migrate",
    help = "Move assets between asset packs"
) {
    private val from by option("--from", help = "Source asset pack").required()
    private val to by option("--to", help = "Destination asset pack").required()
    private val pattern by option("--pattern", help = "File pattern (glob)").required()
    private val dryRun by option("--dry-run", help = "Preview changes without moving").flag()

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))

        // Validate packs exist
        // Find matching files
        // Move (or preview) files
        // Update any references

        Logger.success("Migrated X files from $from to $to")
    }
}
```

**Benefits:**
- Simplifies refactoring
- Reduces manual work
- Prevents copy-paste errors
- Dry-run mode for safety

**Estimated Effort:** 3 hours
**Impact:** Low (nice to have for large projects)

---

## Priority 6: Testing Improvements

### 6.1 Add Missing Edge Case Tests

**Tests to Add:**

```kotlin
@Test
fun `circular inheritance throws exception with helpful message`() {
    // Create v2 → v3 → v2 cycle manually
    // Verify AssetPackResolver throws exception
    // Verify error message includes full cycle path
}

@Test
fun `missing parent fails with helpful error`() {
    // Try to create v2 inheriting non-existent v99
    // Verify command fails immediately (not at build time)
    // Verify error lists available packs
}

@Test
fun `very deep inheritance chain (10 levels) works`() {
    // Create v1 → v2 → ... → v10
    // Verify resolution is correct
    // Verify performance is acceptable (<100ms)
}

@Test
fun `asset pack with invalid YAML fails gracefully`() {
    // Create pack with malformed config.yml
    // Verify clear error message
}

@Test
fun `rename asset pack updates references`() {
    // Rename v2 to v2-updated
    // Verify child packs update their parent references
    // Verify version configs update
}
```

**Estimated Effort:** 2-3 hours
**Impact:** Medium (better test coverage)

---

## Summary of Recommendations

| Priority | Task | Effort | Impact | Status |
|----------|------|--------|--------|--------|
| 1 | Add parent validation | 30 min | High | ⚠️ TODO |
| 1 | Add version format validation | 20 min | Medium | ⚠️ TODO |
| 2 | Add list asset-packs command | 2-3 hrs | High | ⚠️ TODO |
| 2 | Add inspect command | 2-3 hrs | Medium | ⚠️ TODO |
| 3 | Add pack validation to validate command | 2 hrs | Medium | ⚠️ TODO |
| 4 | Enhance AGENTS.md documentation | 1 hr | High | ⚠️ TODO |
| 4 | Add layer priority diagram | 30 min | Medium | ⚠️ TODO |
| 5 | Interactive creation wizard | 3-4 hrs | Medium | ⚠️ Future |
| 5 | Migration helper command | 3 hrs | Low | ⚠️ Future |
| 6 | Add edge case tests | 2-3 hrs | Medium | ⚠️ Future |

**Total Estimated Effort for Priority 1-3:** ~8 hours
**Total Estimated Effort for All:** ~18 hours

---

## Implementation Order

### Phase 1: Pre-Release (Priority 1-2)
1. Add parent validation (30 min)
2. Add version format validation (20 min)
3. Add list asset-packs command (2-3 hrs)
4. Enhance documentation (1.5 hrs)

**Total: ~5 hours** → Ready for 1.0 release

### Phase 2: Post-Release (Priority 3-4)
1. Add inspect command (2-3 hrs)
2. Add validation enhancements (2 hrs)
3. Add layer diagram (30 min)

**Total: ~5 hours** → 1.1 release

### Phase 3: Future (Priority 5-6)
1. Interactive wizard (3-4 hrs)
2. Migration helper (3 hrs)
3. Additional tests (2-3 hrs)

**Total: ~8 hours** → 1.2+ releases

---

## Conclusion

The asset pack system is **already functional and ready for use**, but these recommendations will:

✅ **Prevent common mistakes** (validation)
✅ **Improve discoverability** (list/inspect commands)
✅ **Enhance documentation** (better examples, diagrams)
✅ **Streamline workflows** (migration helpers, wizards)

**Recommended Minimum for 1.0 Release:**
- Parent validation
- List asset-packs command
- Enhanced documentation

These three items address the most critical UX gaps and can be implemented in ~5 hours.

---

**Document Version:** 1.0
**Last Updated:** 2026-02-09
**Next Review:** After implementing Phase 1
