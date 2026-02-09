# JAR Tests Quick Start Guide

## TL;DR

```bash
# Fast validation tests (5 seconds)
./gradlew :src:cli:test --tests "JarValidationUtilsTest"

# Fabric JAR tests (15-25 minutes, automated)
export RUN_JAR_TESTS=true
./gradlew :src:cli:test --tests "JarOutputE2ETest.Fabric*"

# All tests (30-60 minutes, some need manual setup)
export RUN_JAR_TESTS=true
./gradlew :src:cli:test --tests "JarOutputE2ETest"
```

## What These Tests Do

1. **Generate complete Minecraft mod projects** using ProjectGenerator
2. **Add components** (items, blocks) using CLI commands
3. **Build actual JARs** with Gradle
4. **Verify JAR structure** (metadata, assets, classes)
5. **Validate Java bytecode version** (17 for 1.20.x, 21 for 1.21.x)
6. **Check asset and class files** are present and correct

## Prerequisites

- Java 17 or 21
- 10GB+ free disk space
- Internet connection (first run)

## Test Structure

```
src/cli/src/test/kotlin/dev/dropper/e2e/
├── JarOutputE2ETest.kt          # Main tests (14 tests)
├── JarValidationUtilsTest.kt    # Validation utils (10 tests)
├── JAR_TESTS_README.md          # Full documentation
├── JAR_TEST_MATRIX.md           # Coverage matrix
└── QUICK_START.md               # This file
```

## Available Tests

| Test                                    | Time     | Automated |
|-----------------------------------------|----------|-----------|
| Fabric 1.20.1 + Items                   | 2-4 min  | ✅        |
| Fabric 1.20.1 + Blocks                  | 2-4 min  | ✅        |
| Fabric 1.20.1 + Full                    | 3-5 min  | ✅        |
| Fabric 1.20.4 + Full                    | 3-5 min  | ✅        |
| Fabric 1.21 + Items                     | 2-4 min  | ✅        |
| Fabric 1.21.1 + Full                    | 3-5 min  | ✅        |
| Multi-version (1.20.1 + 1.21.1)         | 5-8 min  | ✅        |
| NeoForge 1.20.4 + Items                 | 3-5 min  | ⚠️        |
| NeoForge 1.21.1 + Full                  | 3-5 min  | ⚠️        |
| Forge 1.20.1 + Items                    | 3-5 min  | ⚠️        |
| Forge 1.20.4 + Full                     | 3-5 min  | ⚠️        |
| Performance test                        | 3-5 min  | ✅        |
| Validation utils                        | <5 sec   | ✅        |

✅ = Fully automated
⚠️ = Gracefully skips if not configured

## What Gets Verified

### ✅ JAR Structure
- Loader metadata files (fabric.mod.json, mods.toml, etc.)
- Asset directories (models, textures, blockstates)
- Data directories (recipes, tags, loot tables)
- Compiled class files

### ✅ Metadata
- Valid JSON/TOML format
- Required fields present (id, version, name)
- Entrypoints configured
- Mod ID matches

### ✅ Java Bytecode
- MC 1.20.x → Java 17 (major version 61)
- MC 1.21.x → Java 21 (major version 65)
- Reads actual class file headers

### ✅ Assets
- Item models: `assets/{modId}/models/item/*.json`
- Block models: `assets/{modId}/models/block/*.json`
- Blockstates: `assets/{modId}/blockstates/*.json`
- Textures: `assets/{modId}/textures/**/*.png`

### ✅ Classes
- Main mod class compiled
- Component classes (items, blocks) compiled
- Platform helpers compiled
- Proper package structure

### ✅ Size
- At least 1KB (not empty)
- Between 1-50MB (reasonable for basic mods)

## Common Commands

### Run One Test
```bash
export RUN_JAR_TESTS=true
./gradlew :src:cli:test --tests "JarOutputE2ETest.*1_20_1*items*"
```

### Run Fabric Only
```bash
export RUN_JAR_TESTS=true
./gradlew :src:cli:test --tests "JarOutputE2ETest.Fabric*"
```

### Run by Version
```bash
export RUN_JAR_TESTS=true
./gradlew :src:cli:test --tests "JarOutputE2ETest.*1_21*"
```

### Windows PowerShell
```powershell
$env:RUN_JAR_TESTS="true"
.\gradlew.bat :src:cli:test --tests "JarOutputE2ETest"
```

## Expected Output

```
================================================================================
  JAR OUTPUT E2E TESTS - Building actual JARs for verification
  Test root: /path/to/build/test-jar-output/1234567890
================================================================================

--------------------------------------------------------------------------------
  Building JAR: fabric-1_20_1-items - 1_20_1-fabric
--------------------------------------------------------------------------------
  Running: /path/to/gradlew build --no-daemon --console=plain
  ✓ Built: fabric20items-1.0.0-1_20_1-fabric.jar (2048KB)

✅ Test passed

================================================================================
  JAR OUTPUT TEST SUMMARY
================================================================================
  Total JARs built: 7
    ✓ fabric20items-1.0.0-1_20_1-fabric.jar (2.05 MB)
    ✓ fabric20blocks-1.0.0-1_20_1-fabric.jar (2.12 MB)
    ...
================================================================================
```

## Troubleshooting

### Tests Don't Run
**Problem:** Tests are skipped

**Solution:** Set environment variable:
```bash
export RUN_JAR_TESTS=true
```

### Build Fails
**Problem:** Gradle build exits with error

**Solutions:**
1. Check Java version: `java -version`
2. Check internet connection
3. Look at Gradle output in test artifacts
4. Check preserved test directory

### Slow First Run
**Problem:** First run takes 30-60 minutes

**Explanation:** This is normal - Gradle downloads:
- Minecraft dependencies (~1-2GB)
- Fabric Loom (~500MB)
- Other dependencies (~500MB)

**Subsequent runs:** 15-30 minutes (warm cache)

### NeoForge/Forge Skip
**Problem:** Tests skip with warning

**Explanation:** This is expected - these loaders require manual configuration

**If you want to test them:**
1. Configure buildSrc for Forge/NeoForge
2. Re-run tests

## Test Artifacts

### On Success
Test directories are deleted automatically

### On Failure
Test directories preserved at:
```
build/test-jar-output/{timestamp}/
├── project-name/
│   ├── config.yml
│   ├── build.gradle.kts
│   ├── build/              # Built JARs here
│   └── ...
```

Inspect these to debug issues.

## CI/CD Example

```yaml
name: JAR Tests

on:
  schedule:
    - cron: '0 0 * * 0'  # Weekly

jobs:
  jar-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - run: |
          export RUN_JAR_TESTS=true
          ./gradlew :src:cli:test --tests "JarOutputE2ETest.Fabric*"
```

See `.github/workflows/jar-tests.yml.example` for complete workflow.

## Performance Tips

### Local Development
1. Run validation tests first (fast)
2. Run one Fabric test to verify
3. Run full suite before committing

### CI/CD
1. Daily: Validation tests only
2. Weekly: Fabric tests
3. Monthly: Full test suite

### Speed Up Builds
1. Use Gradle daemon locally (remove `--no-daemon`)
2. Cache Gradle dependencies in CI
3. Use parallel execution for independent tests

## What to Do Next

### First Time Running
1. Run validation tests to verify setup:
   ```bash
   ./gradlew :src:cli:test --tests "JarValidationUtilsTest"
   ```

2. Run one Fabric test:
   ```bash
   export RUN_JAR_TESTS=true
   ./gradlew :src:cli:test --tests "JarOutputE2ETest.*1_20_1*items*"
   ```

3. If successful, run full Fabric suite:
   ```bash
   export RUN_JAR_TESTS=true
   ./gradlew :src:cli:test --tests "JarOutputE2ETest.Fabric*"
   ```

### Development Workflow
1. Make changes to Dropper CLI
2. Run validation tests (fast feedback)
3. Run relevant JAR test (e.g., Fabric 1.20.1)
4. If passes, commit changes

### Before Release
1. Run full Fabric test suite
2. Verify all tests pass
3. Check built JARs are valid
4. Review test summary

## Files Reference

- **JAR_TESTS_README.md** - Complete documentation (400+ lines)
- **JAR_TEST_MATRIX.md** - Test coverage and matrix (300+ lines)
- **QUICK_START.md** - This file
- **.github/workflows/jar-tests.yml.example** - CI workflow

## Need Help?

1. Check **JAR_TESTS_README.md** for detailed troubleshooting
2. Check **JAR_TEST_MATRIX.md** for coverage details
3. Review test output and artifacts
4. Open an issue with:
   - Test name that failed
   - Full error output
   - System info (OS, Java version)

## Quick Checklist

Before running JAR tests:
- [ ] Java 17 or 21 installed
- [ ] 10GB+ free disk space
- [ ] Internet connection available
- [ ] `RUN_JAR_TESTS=true` environment variable set
- [ ] 30-60 minutes available (first run)

Running tests:
- [ ] Validation tests pass
- [ ] At least one Fabric test passes
- [ ] Built JARs are valid
- [ ] Test summary looks correct

CI/CD setup:
- [ ] Workflow configured
- [ ] Java setup correct
- [ ] Caching enabled
- [ ] Artifacts uploaded

## Success Criteria

Tests pass if:
- ✅ All assertions pass
- ✅ JARs build successfully
- ✅ JARs have correct structure
- ✅ Metadata is valid
- ✅ Java version is correct
- ✅ Assets and classes present
- ✅ Reasonable JAR sizes

## Summary

The JAR output E2E tests provide comprehensive validation that Dropper-generated projects can build production-ready JAR files. They're designed to:

1. **Catch real issues** - Build actual JARs with Gradle
2. **Verify all aspects** - Structure, metadata, bytecode, assets
3. **Be maintainable** - Clear code, good documentation
4. **Integrate with CI** - GitHub Actions example provided
5. **Run efficiently** - Fast validation tests, slower full tests

Start with validation tests, then Fabric tests, then full suite as needed.
