# CI Integration Guide

This guide explains how to run Dropper's E2E tests in CI environments.

## Test Categories

Dropper has two categories of E2E tests:

### 1. Fast Tests (Default)

**What they test:**
- Project generation
- File structure validation
- ConfigReader functionality
- GradleRunner command construction
- Dev command infrastructure

**Runtime:** ~10-30 seconds
**Requirements:** Java 21+
**No network required:** Uses local project generation

**Run with:**
```bash
./gradlew :src:cli:test
```

### 2. Slow Tests (Tagged with `@Tag("slow")`)

**What they test:**
- Actual Gradle builds (`./gradlew build`)
- Gradle task existence verification
- Full project compilation

**Runtime:** 5-15 minutes (first run per MC version)
**Requirements:** Java 21+, Internet connection
**Downloads:** ~500MB-1GB per Minecraft version

**Run with:**
```bash
./gradlew :src:cli:test --tests "*FullCLIBuildTest" -PincludeSlow=true
```

---

## GitHub Actions Workflows

### Workflow 1: Fast CI (Every Push)

Run fast tests on every push/PR:

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  test:
    name: Fast E2E Tests
    runs-on: ubuntu-latest

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Run fast E2E tests
        run: ./gradlew :src:cli:test

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: |
            **/build/test-results/**/*.xml
            **/build/reports/tests/**/*

  test-matrix:
    name: Multi-OS Fast Tests
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
    runs-on: ${{ matrix.os }}

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Run tests
        run: ./gradlew :src:cli:test
```

**Run time:** ~2-5 minutes
**Cost:** Low (standard GitHub Actions)

### Workflow 2: Slow Tests (Nightly / Manual)

Run slow tests nightly or manually:

```yaml
# .github/workflows/e2e-full.yml
name: Full E2E Tests

on:
  # Manual trigger
  workflow_dispatch:

  # Nightly at 2 AM UTC
  schedule:
    - cron: '0 2 * * *'

  # On release tags
  push:
    tags:
      - 'v*'

jobs:
  full-build-test:
    name: Full E2E Build Test
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Run full E2E tests (with actual builds)
        run: ./gradlew :src:cli:test --tests "*FullCLIBuildTest*"
        timeout-minutes: 20

      - name: Upload generated project
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: generated-test-project
          path: examples/test-dev-mod/

      - name: Upload test reports
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: full-test-results
          path: |
            **/build/test-results/**/*.xml
            **/build/reports/tests/**/*

  multi-version-test:
    name: Test Multiple Minecraft Versions
    strategy:
      matrix:
        mc-version: ['1.20.1', '1.21.1']
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Cache Gradle caches
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches/fabric-loom/
            ~/.gradle/caches/forge_gradle/
            ~/.gradle/caches/modules-2/
          key: mc-${{ matrix.mc-version }}-${{ hashFiles('**/gradle.properties') }}
          restore-keys: |
            mc-${{ matrix.mc-version }}-

      - name: Run tests for MC ${{ matrix.mc-version }}
        run: |
          echo "Testing Minecraft ${{ matrix.mc-version }}"
          ./gradlew :src:cli:test --tests "*FullCLIBuildTest*"
```

**Run time:** ~15-30 minutes
**Cost:** Higher (longer runtime)
**When to use:** Nightly builds, before releases, manual testing

### Workflow 3: Smoke Test (Before Release)

Quick smoke test before creating a release:

```yaml
# .github/workflows/smoke-test.yml
name: Release Smoke Test

on:
  push:
    tags:
      - 'v*'

jobs:
  smoke-test:
    name: Pre-Release Smoke Test
    runs-on: ubuntu-latest

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Build CLI
        run: ./gradlew :src:cli:build

      - name: Run fast E2E tests
        run: ./gradlew :src:cli:test

      - name: Create test project
        run: |
          # This verifies the CLI can actually generate a project
          ./gradlew :src:cli:test --tests "*FullCLIBuildTest.create real project*"

      - name: Upload test project
        uses: actions/upload-artifact@v4
        with:
          name: release-smoke-test-project
          path: examples/test-dev-mod/
```

---

## Test Environment Configuration

### Gradle Properties for CI

Create `gradle.properties` optimized for CI:

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx3G -XX:MaxMetaspaceSize=512m
org.gradle.daemon=false
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=false
```

### Cache Strategy

**What to cache:**
- ✅ Gradle wrapper (`~/.gradle/wrapper/`)
- ✅ Gradle dependencies (`~/.gradle/caches/modules-2/`)
- ✅ Minecraft cache (`~/.gradle/caches/fabric-loom/`)

**What NOT to cache:**
- ❌ Build outputs (`build/` directories)
- ❌ Temporary files

**Example cache configuration:**

```yaml
- name: Cache Gradle caches
  uses: actions/cache@v4
  with:
    path: |
      ~/.gradle/caches
      ~/.gradle/wrapper
    key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
    restore-keys: |
      ${{ runner.os }}-gradle-
```

---

## Running Tests Locally

### Run fast tests only

```bash
./gradlew :src:cli:test
```

### Run all tests (including slow)

```bash
./gradlew :src:cli:test -PincludeSlow=true
```

### Run specific test class

```bash
./gradlew :src:cli:test --tests "FullCLIBuildTest"
```

### Run specific test method

```bash
./gradlew :src:cli:test --tests "FullCLIBuildTest.create real project*"
```

### Run with verbose output

```bash
./gradlew :src:cli:test --info
```

### Run with stack traces

```bash
./gradlew :src:cli:test --stacktrace
```

---

## Test Tagging Strategy

### Fast Tests (Default)

No tag required. Runs on every commit.

```kotlin
@Test
fun `test project generation`() {
    // Fast test - no @Tag annotation
}
```

### Slow Tests

Tag with `@Tag("slow")`:

```kotlin
@Test
@Tag("slow")
fun `verify project builds successfully`() {
    // Slow test - actual Gradle build
}
```

### Conditional Execution

Configure Gradle to exclude slow tests by default:

```kotlin
// build.gradle.kts in src/cli/
tasks.test {
    useJUnitPlatform {
        if (!project.hasProperty("includeSlow")) {
            excludeTags("slow")
        }
    }
}
```

Run slow tests with:
```bash
./gradlew :src:cli:test -PincludeSlow=true
```

---

## Test Output and Artifacts

### Test Reports

Gradle generates HTML test reports:
```
src/cli/build/reports/tests/test/index.html
```

Upload in CI:
```yaml
- name: Upload test report
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: test-report
    path: src/cli/build/reports/tests/
```

### Generated Projects

The `FullCLIBuildTest` creates a real project in `examples/test-dev-mod/`.

Keep it for manual verification:
```yaml
- name: Upload generated project
  if: always()
  uses: actions/upload-artifact@v4
  with:
    name: generated-project
    path: examples/test-dev-mod/
```

### Test Logs

Capture test logs:
```yaml
- name: Upload test logs
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: test-logs
    path: |
      **/build/test-results/**/*.xml
      **/hs_err_*.log
```

---

## Performance Optimization

### 1. Parallel Test Execution

Enable parallel tests:
```kotlin
// build.gradle.kts
tasks.test {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}
```

### 2. Gradle Daemon

For local development:
```bash
# Enable daemon
./gradlew --daemon

# Disable for CI (more reliable)
./gradlew --no-daemon
```

### 3. Build Cache

Enable Gradle build cache:
```properties
# gradle.properties
org.gradle.caching=true
```

CI configuration:
```yaml
- name: Setup Gradle build cache
  uses: gradle/gradle-build-action@v2
  with:
    cache-read-only: ${{ github.ref != 'refs/heads/main' }}
```

---

## Troubleshooting CI

### Out of Memory

**Symptom:** `OutOfMemoryError` in CI

**Solution:**
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4G
```

Or in CI:
```yaml
env:
  GRADLE_OPTS: "-Xmx4G"
```

### Timeout

**Symptom:** Test times out

**Solution:**
```yaml
jobs:
  test:
    timeout-minutes: 30  # Increase timeout
```

### Cache Miss

**Symptom:** Slow builds every time

**Solution:**
- Check cache key includes all relevant files
- Verify cache is being saved correctly
- Check cache size limits (GitHub Actions: 10GB per repo)

### Flaky Tests

**Symptom:** Tests pass locally but fail in CI

**Solution:**
- Add logging: `println()` statements
- Use `--info` or `--debug` flags
- Check for timing-dependent code
- Verify file system operations complete

### Network Issues

**Symptom:** Download failures

**Solution:**
```yaml
- name: Run tests with retry
  uses: nick-invision/retry@v2
  with:
    timeout_minutes: 20
    max_attempts: 3
    command: ./gradlew :src:cli:test
```

---

## Best Practices

### 1. Fast Feedback Loop

- ✅ Run fast tests on every push
- ✅ Run slow tests nightly or pre-release
- ✅ Use matrix builds for multi-OS testing

### 2. Clear Test Names

```kotlin
@Test
fun `create real project and verify dev command setup`() {
    // Descriptive test name
}
```

### 3. Test Isolation

- Each test should clean up after itself
- Use temporary directories with unique names
- Don't rely on test execution order

### 4. Meaningful Assertions

```kotlin
assertTrue(file.exists(), "Config file should exist at: ${file.absolutePath}")
```

### 5. Progressive Enhancement

- Start with fast tests (structure validation)
- Add slow tests for actual builds
- Use tags to control execution

---

## Example: Complete CI Setup

Here's a complete example for a production setup:

```yaml
# .github/workflows/ci.yml
name: CI

on:
  push:
    branches: [ main, develop ]
  pull_request:
  workflow_dispatch:

jobs:
  fast-tests:
    name: Fast E2E Tests
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]
    runs-on: ${{ matrix.os }}

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      - name: Run fast tests
        run: ./gradlew :src:cli:test

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results-${{ matrix.os }}
          path: src/cli/build/reports/tests/

  slow-tests:
    name: Slow E2E Tests (Manual/Nightly)
    if: github.event_name == 'workflow_dispatch' || github.event_name == 'schedule'
    runs-on: ubuntu-latest
    timeout-minutes: 30

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 21
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '21'

      - name: Cache Gradle
        uses: actions/cache@v4
        with:
          path: |
            ~/.gradle/caches
            ~/.gradle/wrapper
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*') }}

      - name: Run slow tests
        run: ./gradlew :src:cli:test -PincludeSlow=true
        timeout-minutes: 20

      - name: Upload generated project
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: generated-project
          path: examples/test-dev-mod/
```

---

## Summary

**For every PR/push:**
```bash
./gradlew :src:cli:test  # Fast tests only (~30 seconds)
```

**Before releases:**
```bash
./gradlew :src:cli:test -PincludeSlow=true  # All tests (~15 minutes)
```

**CI Strategy:**
- ✅ Fast tests: Every commit (fast feedback)
- ✅ Slow tests: Nightly + manual (thorough validation)
- ✅ Multi-OS: Verify cross-platform support
- ✅ Caching: Speed up repeated runs

This ensures fast iteration while maintaining comprehensive test coverage.
