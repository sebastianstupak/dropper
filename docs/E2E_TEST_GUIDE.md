# E2E Test Guide

This guide explains Dropper's End-to-End testing strategy and how to run the tests.

## Test Structure

Dropper has comprehensive E2E tests organized by speed and purpose:

### Fast Tests (Default)

Located in `src/cli/src/test/kotlin/dev/dropper/integration/`

**Tests:**
- `CompleteWorkflowTest.kt` - Full workflow (init, create items/blocks)
- `DevCommandTest.kt` - Dev command utilities (ConfigReader, GradleRunner)
- `CreateCommandTest.kt` - Create command functionality
- `BuildCommandTest.kt` - Build command testing
- `AddVersionCommandTest.kt` - Adding versions to projects
- `AssetPackCommandTest.kt` - Asset pack management

**Runtime:** ~30 seconds total
**Network:** Not required
**Downloads:** None

### Slow Tests (Tagged)

Located in `src/cli/src/test/kotlin/dev/dropper/e2e/`

**Tests:**
- `FullCLIBuildTest.kt` - Real project creation and Gradle builds

**Runtime:** 5-15 minutes (first run per MC version)
**Network:** Required
**Downloads:** ~500MB-1GB per Minecraft version

## Running Tests

### Run All Fast Tests

```bash
# Run all tests (excludes slow tests by default)
./gradlew :src:cli:test

# With verbose output
./gradlew :src:cli:test --info
```

### Run Specific Test Class

```bash
# Run CompleteWorkflowTest
./gradlew :src:cli:test --tests "CompleteWorkflowTest"

# Run DevCommandTest
./gradlew :src:cli:test --tests "DevCommandTest"

# Run FullCLIBuildTest
./gradlew :src:cli:test --tests "FullCLIBuildTest"
```

### Run Specific Test Method

```bash
# Run specific test
./gradlew :src:cli:test --tests "FullCLIBuildTest.create real project*"

# Run multiple patterns
./gradlew :src:cli:test --tests "*DevCommandTest*" --tests "*ConfigReader*"
```

### Run Slow Tests

```bash
# Run all tests including slow ones
./gradlew :src:cli:test -PincludeSlow=true

# Run only slow tests
./gradlew :src:cli:test --tests "*FullCLIBuildTest*" -PincludeSlow=true
```

### Run with Debug Output

```bash
# Show test progress
./gradlew :src:cli:test --info

# Show full stack traces
./gradlew :src:cli:test --stacktrace

# Show debug output
./gradlew :src:cli:test --debug
```

## Test Reports

After running tests, view the HTML report:

```bash
# Open test report (Unix/macOS/Linux)
open src/cli/build/reports/tests/test/index.html

# Open test report (Windows)
start src/cli/build/reports/tests/test/index.html
```

Or navigate to: `src/cli/build/reports/tests/test/index.html`

## FullCLIBuildTest Details

### What It Does

The `FullCLIBuildTest` creates a **real project** in `examples/test-dev-mod/` and verifies:

1. **Project Generation**
   - Creates complete project structure
   - Generates all necessary files
   - Sets up buildSrc with build logic

2. **Project Structure**
   - Verifies config.yml exists and is valid
   - Checks build.gradle.kts and settings.gradle.kts
   - Validates shared/ directory structure
   - Confirms versions/ directory structure
   - Validates buildSrc/ structure

3. **ConfigReader**
   - Can read config.yml
   - Detects all versions correctly
   - Detects all loaders per version
   - Validates version-loader combinations

4. **GradleRunner**
   - Constructs correct Gradle commands
   - Builds proper module IDs (e.g., 1_20_1-fabric)
   - Handles JVM arguments for debug mode
   - Handles Gradle arguments

5. **Version-Loader Combinations**
   - Verifies 1.20.1-fabric exists
   - Verifies 1.20.1-neoforge exists
   - Verifies 1.21.1-fabric exists
   - Verifies 1.21.1-neoforge exists
   - Correctly rejects invalid combinations

### Test Output

The test creates a project at: `examples/test-dev-mod/`

**Project structure:**
```
examples/test-dev-mod/
├── config.yml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .gitignore
├── README.md
├── AGENTS.md
├── buildSrc/
│   ├── build.gradle.kts
│   └── src/main/kotlin/
│       ├── ModLoaderPlugin.kt
│       ├── config/
│       ├── tasks/
│       └── utils/
├── shared/
│   ├── common/src/main/java/com/testdevmod/
│   ├── fabric/src/main/java/com/testdevmod/platform/
│   └── neoforge/src/main/java/com/testdevmod/platform/
└── versions/
    ├── 1_20_1/
    │   ├── config.yml
    │   ├── fabric/
    │   └── neoforge/
    ├── 1_21_1/
    │   ├── config.yml
    │   ├── fabric/
    │   └── neoforge/
    └── shared/v1/
        ├── config.yml
        ├── assets/testdevmod/
        └── data/testdevmod/
```

### Manual Testing

After the test runs, you can manually test the generated project:

```bash
cd examples/test-dev-mod

# Setup Gradle wrapper (required for first run)
gradle wrapper

# List tasks
./gradlew tasks

# Build project
./gradlew build

# Run Minecraft
./gradlew :1_20_1-fabric:runClient
```

### Slow Tests

The `@Tag("slow")` tests actually compile the project:

```kotlin
@Test
@Tag("slow")
fun `verify project builds successfully`() {
    // Runs: ./gradlew build
    // Downloads Minecraft, applies mappings, compiles
    // Takes 5-15 minutes first time
}

@Test
@Tag("slow")
fun `verify Gradle tasks exist`() {
    // Runs: ./gradlew tasks --all
    // Verifies runClient, runServer, build, test tasks exist
}
```

**Run with:**
```bash
./gradlew :src:cli:test -PincludeSlow=true
```

## CI Integration

See [CI_INTEGRATION.md](CI_INTEGRATION.md) for GitHub Actions workflows.

**Summary:**
- Fast tests run on every push/PR (~30 seconds)
- Slow tests run nightly or manually (~15 minutes)
- Multi-OS testing (Ubuntu, macOS, Windows)

## Test Best Practices

### 1. Fast Feedback Loop

Default tests should be fast (<1 minute) for quick iteration.

### 2. Use Tags

Tag slow tests with `@Tag("slow")` so they can be excluded:

```kotlin
@Test
@Tag("slow")
fun `expensive test`() {
    // This won't run by default
}
```

### 3. Clear Test Names

Use descriptive test names with backticks:

```kotlin
@Test
fun `create real project and verify dev command setup`() {
    // Clear what this test does
}
```

### 4. Proper Assertions

Always include descriptive messages:

```kotlin
assertTrue(file.exists(), "Config file should exist at: ${file.absolutePath}")
```

### 5. Test Isolation

Each test should be independent:
- Clean up after itself
- Use unique temporary directories
- Don't rely on test execution order

### 6. Assumptions

Use `assumeTrue()` for conditional tests:

```kotlin
assumeTrue(
    gradlewFile.exists(),
    "Gradle wrapper must exist. Run 'gradle wrapper' first."
)
```

## Troubleshooting Tests

### Test Fails Locally

```bash
# Clean and retry
./gradlew clean :src:cli:test

# Run with verbose output
./gradlew :src:cli:test --info --stacktrace

# Run specific failing test
./gradlew :src:cli:test --tests "FailingTest"
```

### Test Passes Locally but Fails in CI

- Check for absolute paths (use relative paths)
- Check for OS-specific code (Windows vs Unix)
- Check for timing issues
- Verify all dependencies are available in CI

### Slow Test Times Out

Increase timeout in CI:

```yaml
jobs:
  test:
    timeout-minutes: 30  # Increase as needed
```

### Out of Memory

Increase Gradle memory:

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4G
```

## Adding New Tests

### 1. Fast Test (Integration)

Create in `src/cli/src/test/kotlin/dev/dropper/integration/`:

```kotlin
package dev.dropper.integration

import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class MyNewTest {
    @Test
    fun `test my feature`() {
        // Fast test logic
        assertTrue(true)
    }
}
```

### 2. Slow Test (E2E)

Create in `src/cli/src/test/kotlin/dev/dropper/e2e/`:

```kotlin
package dev.dropper.e2e

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class MySlowTest {
    @Test
    @Tag("slow")
    fun `test expensive operation`() {
        // Slow test logic (actual builds, etc.)
        assertTrue(true)
    }
}
```

## Test Coverage

Current test coverage:

✅ **Project Generation** - Complete
✅ **Create Commands** - Complete (items, blocks)
✅ **Dev Command Utils** - Complete (ConfigReader, GradleRunner)
✅ **Build Command** - Complete
✅ **Add Version** - Complete
✅ **Asset Packs** - Complete
✅ **Full E2E Build** - Complete

🔄 **Upcoming:**
- Create enchantment command
- Create entity command
- Create recipe command
- Create biome command

## Summary

**Quick Start:**
```bash
# Run fast tests (default)
./gradlew :src:cli:test

# Run all tests (including slow)
./gradlew :src:cli:test -PincludeSlow=true

# Run specific test
./gradlew :src:cli:test --tests "FullCLIBuildTest"

# View report
open src/cli/build/reports/tests/test/index.html
```

**Test Strategy:**
- ✅ Fast tests: Every commit (30 seconds)
- ✅ Slow tests: Nightly/manual (15 minutes)
- ✅ Real project: Created in examples/test-dev-mod
- ✅ CI integration: GitHub Actions

See [CI_INTEGRATION.md](CI_INTEGRATION.md) for CI setup.
