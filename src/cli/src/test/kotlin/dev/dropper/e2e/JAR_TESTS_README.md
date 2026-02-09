# JAR Output E2E Tests

Comprehensive end-to-end tests that generate actual JAR files for each mod loader and Minecraft version, then verify the JAR structure and correctness.

## Overview

These tests validate that Dropper-generated projects can:
1. Successfully build with Gradle
2. Produce valid JAR files
3. Include correct metadata for each mod loader
4. Compile with the correct Java version
5. Include all assets and data files
6. Have proper package structure and class files

## Test Coverage

### Fabric Tests (7 tests)
- ✅ 1.20.1 + Items
- ✅ 1.20.1 + Blocks
- ✅ 1.20.1 + Full components
- ✅ 1.20.4 + Full components
- ✅ 1.21 + Items
- ✅ 1.21.1 + Full components

### NeoForge Tests (2 tests)
- ✅ 1.20.4 + Items
- ✅ 1.21.1 + Full components

### Forge Tests (2 tests)
- ✅ 1.20.1 + Items
- ✅ 1.20.4 + Full components

### Additional Tests
- ✅ Multi-version project (builds for multiple MC versions)
- ✅ Performance test (build time validation)
- ✅ Validation utilities (unit tests for helpers)

**Total: 14 comprehensive JAR output tests**

## Running the Tests

### Prerequisites

1. **Java Development Kit**
   - Java 17 or 21 installed
   - `JAVA_HOME` environment variable set

2. **Gradle**
   - Gradle 8.6+ (included via wrapper)

3. **Disk Space**
   - At least 10GB free (for Gradle caches and dependencies)

4. **Time**
   - First run: 30-60 minutes (downloads all dependencies)
   - Subsequent runs: 15-30 minutes

### Quick Start

Run all JAR tests:

```bash
# Unix/Linux/macOS
export RUN_JAR_TESTS=true
./gradlew :src:cli:test --tests "JarOutputE2ETest"

# Windows (PowerShell)
$env:RUN_JAR_TESTS="true"
.\gradlew.bat :src:cli:test --tests "JarOutputE2ETest"

# Windows (CMD)
set RUN_JAR_TESTS=true
.\gradlew.bat :src:cli:test --tests "JarOutputE2ETest"
```

### Run Specific Tests

Run only Fabric tests:

```bash
export RUN_JAR_TESTS=true
./gradlew :src:cli:test --tests "JarOutputE2ETest.Fabric*"
```

Run only a specific version:

```bash
export RUN_JAR_TESTS=true
./gradlew :src:cli:test --tests "JarOutputE2ETest.*1_20_1*"
```

Run validation utils (fast, no JAR builds):

```bash
./gradlew :src:cli:test --tests "JarValidationUtilsTest"
```

### CI/CD Integration

The tests are disabled by default (require `RUN_JAR_TESTS=true`) because they're slow and resource-intensive.

For CI/CD, add to your workflow:

```yaml
# .github/workflows/jar-tests.yml
name: JAR Output Tests

on:
  schedule:
    - cron: '0 0 * * 0'  # Weekly on Sunday
  workflow_dispatch:  # Manual trigger

jobs:
  jar-tests:
    runs-on: ubuntu-latest
    timeout-minutes: 90

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'

      - name: Run JAR tests
        run: |
          export RUN_JAR_TESTS=true
          ./gradlew :src:cli:test --tests "JarOutputE2ETest"

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: jar-test-results
          path: src/cli/build/reports/tests/
```

## Test Structure

### Test Organization

```
src/cli/src/test/kotlin/dev/dropper/e2e/
├── JarOutputE2ETest.kt           # Main JAR build tests
├── JarValidationUtilsTest.kt     # Validation utility tests
└── JAR_TESTS_README.md           # This file
```

### Test Lifecycle

Each test follows this pattern:

1. **Generate Project**
   - Create ModConfig
   - Call ProjectGenerator.generate()

2. **Add Components**
   - Create items, blocks, etc.
   - Uses actual CLI commands

3. **Build JAR**
   - Copy gradle wrapper
   - Execute `./gradlew build`
   - Capture output

4. **Verify JAR**
   - Structure validation
   - Metadata validation
   - Java version check
   - Asset/class verification

### What Gets Verified

#### JAR Structure (Fabric)
- ✅ `fabric.mod.json` exists
- ✅ `assets/{modId}/` directory present
- ✅ `data/{modId}/` directory present
- ✅ Compiled classes in `com/` packages

#### JAR Structure (Forge/NeoForge)
- ✅ `META-INF/mods.toml` or `META-INF/neoforge.mods.toml` exists
- ✅ `META-INF/MANIFEST.MF` exists
- ✅ `assets/{modId}/` directory present
- ✅ `data/{modId}/` directory present

#### Metadata Validation
- ✅ JSON/TOML is valid and parseable
- ✅ Contains required fields (id, version, name)
- ✅ Contains entrypoints/mod configuration
- ✅ Mod ID matches project configuration

#### Java Version Validation
- ✅ MC 1.20.x → Java 17 (bytecode major version 61)
- ✅ MC 1.21.x → Java 21 (bytecode major version 65)
- ✅ Reads actual class file bytecode

#### Asset Validation
- ✅ Item models exist in `assets/{modId}/models/item/`
- ✅ Block models exist in `assets/{modId}/models/block/`
- ✅ Blockstates exist in `assets/{modId}/blockstates/`
- ✅ Textures exist in `assets/{modId}/textures/`

#### Size Validation
- ✅ JAR is at least 1KB (not empty)
- ✅ JAR is reasonable size (1-50MB for basic mods)

## Test Results

### Output Format

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

[... test assertions ...]

================================================================================
  JAR OUTPUT TEST SUMMARY
================================================================================
  Total JARs built: 7
    ✓ fabric20items-1.0.0-1_20_1-fabric.jar (2.05 MB)
    ✓ fabric20blocks-1.0.0-1_20_1-fabric.jar (2.12 MB)
    ✓ fabric20full-1.0.0-1_20_1-fabric.jar (2.34 MB)
    ✓ fabric204full-1.0.0-1_20_4-fabric.jar (2.28 MB)
    ✓ fabric21items-1.0.0-1_21-fabric.jar (2.45 MB)
    ✓ fabric211full-1.0.0-1_21_1-fabric.jar (2.52 MB)
    ✓ multiver-1.0.0-1_20_1-fabric.jar (2.15 MB)
================================================================================
```

### Artifacts

After tests run, you can inspect:

```
build/test-jar-output/{timestamp}/
├── fabric-1_20_1-items/
│   └── build/
│       └── 1_20_1/
│           └── fabric.jar
├── fabric-1_20_1-blocks/
│   └── build/
│       └── 1_20_1/
│           └── fabric.jar
└── ...
```

Each project directory contains:
- Full generated project structure
- Build outputs (JARs, logs)
- Gradle cache

**Note:** Test directories are deleted after successful runs. On failure, they're preserved for debugging.

## Troubleshooting

### Test Fails: "Gradle build failed"

**Symptom:**
```
Gradle build failed with exit code 1
```

**Common Causes:**
1. Missing Java version
2. Gradle dependency download failure
3. Build system configuration error

**Solutions:**
- Ensure Java 17+ is installed: `java -version`
- Check internet connectivity (for dependency downloads)
- Run with `--stacktrace` for details
- Check preserved test directory for full logs

### Test Fails: "JAR not found at expected location"

**Symptom:**
```
JAR not found at expected location: build/1_20_1/fabric.jar
```

**Causes:**
- Build completed but JAR wasn't copied to final location
- Gradle plugin issue

**Solutions:**
- Check `build-temp/` directory for built JARs
- Verify buildSrc plugin is working
- Check Gradle output logs

### Test Times Out

**Symptom:**
```
Test exceeded timeout of 90 minutes
```

**Causes:**
- Slow internet (downloading GB of dependencies)
- Underpowered machine

**Solutions:**
- Increase timeout in CI configuration
- Pre-cache Gradle dependencies
- Run tests on faster hardware

### NeoForge/Forge Tests Skip

**Symptom:**
```
⚠ NeoForge build skipped - requires Gradle 9.1+ and manual configuration
```

**This is expected behavior:**
- Forge/NeoForge require additional manual configuration
- Tests gracefully skip if configuration is missing
- Focus on Fabric tests for initial validation

## Performance Expectations

### First Run (Cold Cache)
- **Time:** 30-60 minutes
- **Downloads:** 2-4 GB
- **Disk Usage:** 8-10 GB

### Subsequent Runs (Warm Cache)
- **Time:** 15-30 minutes
- **Downloads:** Minimal
- **Disk Usage:** 4-6 GB (incremental builds)

### Per-Test Timing
- Simple mod (items only): 2-4 minutes
- Complex mod (items + blocks): 3-5 minutes
- Multi-version: 5-8 minutes

## Best Practices

### Local Development

1. **Run validation tests first:**
   ```bash
   ./gradlew :src:cli:test --tests "JarValidationUtilsTest"
   ```

2. **Test one loader at a time:**
   ```bash
   export RUN_JAR_TESTS=true
   ./gradlew :src:cli:test --tests "JarOutputE2ETest.Fabric*"
   ```

3. **Keep Gradle daemon running:**
   - Speeds up subsequent builds
   - Remove `--no-daemon` from test code for local dev

### CI/CD

1. **Run weekly, not on every commit:**
   - Tests are resource-intensive
   - Use scheduled workflows

2. **Cache Gradle dependencies:**
   ```yaml
   - uses: actions/cache@v4
     with:
       path: |
         ~/.gradle/caches
         ~/.gradle/wrapper
       key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}
   ```

3. **Set reasonable timeouts:**
   - 90 minutes minimum
   - 120 minutes recommended

4. **Upload artifacts on failure:**
   - Preserve test directories
   - Upload Gradle logs

## Adding New Tests

### Test Template

```kotlin
@Test
@EnabledIfEnvironmentVariable(named = "RUN_JAR_TESTS", matches = "true")
fun `Fabric X_Y_Z - generates valid JAR with components`() {
    val projectDir = File(testRootDir, "fabric-X_Y_Z-test")
    val config = ModConfig(
        id = "testmod",
        name = "Test Mod",
        version = "1.0.0",
        description = "Test",
        author = "Test",
        license = "MIT",
        minecraftVersions = listOf("X.Y.Z"),
        loaders = listOf("fabric")
    )

    // Generate project
    ProjectGenerator().generate(projectDir, config)
    System.setProperty("user.dir", projectDir.absolutePath)

    // Add components
    CreateItemCommand().parse(arrayOf("test_item"))

    // Build JAR
    val jarFile = buildJar(projectDir, "X_Y_Z", "fabric")
    builtJars.add(jarFile)

    // Verify
    verifyFabricJarStructure(jarFile, config.id)
    verifyFabricMetadata(jarFile, "X.Y.Z", config.id, config.name)
    verifyJavaVersion(jarFile, 21) // or 17
}
```

### Adding Verification Methods

```kotlin
private fun verifyCustomFeature(jarFile: File) {
    ZipFile(jarFile).use { zip ->
        // Your verification logic
        val entry = zip.getEntry("path/to/file")
        assertNotNull(entry, "Custom feature should exist")
    }
}
```

## Success Criteria

A successful test run should show:

- ✅ All tests pass
- ✅ All JARs build successfully
- ✅ All JARs have correct structure
- ✅ All metadata is valid
- ✅ Correct Java bytecode version
- ✅ All assets and classes present
- ✅ Reasonable JAR sizes
- ✅ Build times within limits

## Future Enhancements

Potential improvements to these tests:

1. **Runtime Validation**
   - Launch Minecraft with mod loaded
   - Verify mod initializes correctly

2. **Cross-Platform Testing**
   - Run on Windows, macOS, Linux
   - Verify consistent results

3. **Dependency Testing**
   - Test with Fabric API
   - Test with other mod dependencies

4. **Advanced Features**
   - Test with multiple asset packs
   - Test with complex mod interactions

5. **Performance Benchmarking**
   - Track build time trends
   - Optimize slow builds

## Support

For issues with these tests:

1. Check this README
2. Review test output logs
3. Inspect preserved test directories
4. Open an issue with:
   - Test name
   - Full error output
   - Test directory contents
   - System info (OS, Java version, Gradle version)

## References

- [Fabric Mod Development](https://fabricmc.net/develop/)
- [NeoForge Documentation](https://docs.neoforged.net/)
- [MinecraftForge Documentation](https://docs.minecraftforge.net/)
- [Gradle Build Tool](https://gradle.org/)
