# Dropper CLI - Testing Guide

This document explains how to run tests for the Dropper CLI across different platforms and environments.

---

## Quick Start

### Run Unit Tests (All Platforms)

```bash
./gradlew :src:cli:test
```

**Works on**: Windows (native), Linux, macOS, WSL, Docker
**Tests**: 53 unit tests (ValidationUtil, PackageNameSanitization, JarValidation)

### Run Full E2E Test Suite

#### On Linux/macOS (Native)
```bash
./gradlew :src:cli:test
```
**Tests**: All 450+ tests (unit + integration + e2e)

#### On Windows (via WSL)
```bash
bash scripts/test-in-wsl.sh
```
**Tests**: All 450+ tests (bypasses Windows Gradle issues)

#### On Windows (via Docker)
```bash
bash scripts/test-in-docker.sh
```
**Tests**: All 450+ tests (containerized Linux environment)

---

## Test Architecture

### Test Categories

1. **Unit Tests** (53 tests)
   - `src/cli/src/test/kotlin/dev/dropper/util/ValidationUtilTest.kt`
   - `src/cli/src/test/kotlin/dev/dropper/util/PackageNameSanitizationTest.kt`
   - `src/cli/src/test/kotlin/dev/dropper/e2e/JarValidationUtilsTest.kt`
   - **Platform**: All platforms (Windows native, Linux, macOS, WSL, Docker)

2. **Integration Tests** (41 test files, ~400 tests)
   - `src/cli/src/test/kotlin/dev/dropper/integration/`
   - Tests commands: Create, Build, Package, List, Remove, Rename, etc.
   - **Platform**: Linux, macOS, WSL, Docker only (excluded on Windows native)

3. **E2E Tests** (8 test files, ~20 tests)
   - `src/cli/src/test/kotlin/dev/dropper/e2e/`
   - Tests complete workflows
   - **Platform**: Linux, macOS, WSL, Docker only (excluded on Windows native)

4. **Command Tests** (command-specific tests)
   - `src/cli/src/test/kotlin/dev/dropper/commands/`
   - **Platform**: Linux, macOS, WSL, Docker only (excluded on Windows native)

### Why Different Platforms?

- **Windows Native**: Gradle test executor crashes with heavy file I/O and directory manipulation
- **Linux/macOS**: No issues, all tests run
- **WSL**: Linux environment on Windows, all tests run
- **Docker**: Containerized Linux, all tests run

---

## Running Tests by Platform

### Windows (Native) - Unit Tests Only

```bash
# Windows PowerShell or CMD
.\gradlew.bat :src:cli:test

# Git Bash (Windows)
./gradlew.bat :src:cli:test
```

**Output**: 53 unit tests pass, ~400 integration/e2e tests excluded

**Why limited?** Windows Gradle test executor crashes when tests:
- Modify `System.setProperty("user.dir")`
- Create/delete many temporary directories
- Perform heavy file I/O

**Solution**: Use WSL or Docker for full test suite (see below)

---

### Windows (WSL) - Full Test Suite ✅

#### Option 1: Automated Script

```bash
# From Windows (Git Bash, PowerShell, CMD)
bash scripts/test-in-wsl.sh
```

This script:
- Detects if running in WSL or launches WSL
- Installs Java if needed
- Runs full test suite in Linux environment
- All 450+ tests run

#### Option 2: Manual WSL

```bash
# Enter WSL
wsl

# Navigate to project
cd /mnt/d/dev/minecraft-mod-versioning-example

# Run tests
./gradlew :src:cli:test --no-daemon
```

#### Prerequisites for WSL

**Install WSL** (if not already installed):
```powershell
wsl --install -d Ubuntu
```

**Install Java in WSL**:
```bash
wsl sudo apt-get update
wsl sudo apt-get install -y openjdk-21-jdk
```

**Verify**:
```bash
wsl java -version
```

---

### Docker - Full Test Suite ✅

```bash
# From any platform (Windows, Linux, macOS)
bash scripts/test-in-docker.sh
```

This script:
- Builds Docker test image (one-time, cached)
- Runs full test suite in container
- All 450+ tests run
- No host environment modification

#### Manual Docker Build

```bash
# Build test image
docker build -t dropper-test -f Dockerfile.test .

# Run tests
docker run --rm \
  -v "$(pwd):/project" \
  -e DROPPER_TEST_ENV=container \
  dropper-test
```

---

### Linux/macOS - Full Test Suite ✅

```bash
# Native Linux or macOS
./gradlew :src:cli:test
```

All 450+ tests run natively without issues.

---

## CI/CD Testing

### GitHub Actions Workflows

The project uses GitHub Actions for continuous integration with multiple test strategies:

#### 1. Unit Tests (ci.yml)
- **Platforms**: Ubuntu, Windows, macOS
- **Tests**: 53 unit tests
- **Runs on**: Every push, every PR

#### 2. E2E Tests - Linux (ci.yml)
- **Platform**: Ubuntu
- **Tests**: All 450+ tests
- **Runs on**: Every push, every PR

#### 3. E2E Tests - Windows WSL (ci.yml)
- **Platform**: Windows with WSL Ubuntu
- **Tests**: All 450+ tests
- **Runs on**: Every push, every PR
- **How**: Uses `Vampire/setup-wsl` action

#### 4. E2E Tests - Docker (ci.yml)
- **Platform**: Ubuntu with Docker
- **Tests**: All 450+ tests
- **Runs on**: Every push, every PR
- **How**: Builds and runs test container

#### 5. Integration Tests (ci.yml)
- **Platform**: Ubuntu
- **Tests**: Generated project validation
- **Runs on**: Every push, every PR

### View CI Results

```bash
# Local - see what CI will run
act -l  # requires 'act' tool

# GitHub
# Visit: https://github.com/your-repo/actions
```

---

## Test Configuration

### build.gradle.kts Test Configuration

The test configuration automatically detects the environment:

```kotlin
val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val isWSL = System.getenv("WSL_DISTRO_NAME") != null ||
            System.getenv("DROPPER_TEST_ENV") == "wsl"
val isContainer = System.getenv("DROPPER_TEST_ENV") == "docker" ||
                  System.getenv("DROPPER_TEST_ENV") == "container"

// Only exclude on native Windows (not WSL or containers)
val shouldExcludeTests = isWindows && !isWSL && !isContainer
```

### Environment Variables

Set these to control test behavior:

- `DROPPER_TEST_ENV=wsl` - Runs all tests (WSL mode)
- `DROPPER_TEST_ENV=docker` - Runs all tests (Docker mode)
- `DROPPER_TEST_ENV=container` - Runs all tests (container mode)
- `DROPPER_TEST_ENV=linux` - Runs all tests (native Linux)

### Manual Override

To force running all tests on Windows (may crash):

```bash
# PowerShell
$env:DROPPER_TEST_ENV="wsl"
.\gradlew.bat :src:cli:test

# Git Bash
DROPPER_TEST_ENV=wsl ./gradlew.bat :src:cli:test
```

**Warning**: This may still crash on Windows native due to Gradle test executor issues.

---

## Test Development

### Writing New Tests

1. **Unit Tests** - Always work on all platforms
   ```kotlin
   class MyUtilTest {
       @Test
       fun `test my util function`() {
           val result = MyUtil.doSomething("input")
           assertEquals("expected", result)
       }
   }
   ```

2. **Integration Tests** - Use TestProjectContext
   ```kotlin
   class MyCommandTest {
       private lateinit var context: TestProjectContext

       @BeforeEach
       fun setup() {
           context = TestProjectContext(File("build/test-${UUID.randomUUID()}"))
       }

       @AfterEach
       fun cleanup() {
           context.cleanup()
       }

       @Test
       fun `test my command`() {
           val command = MyCommand()
           command.projectDir = context.projectDir
           command.parse(arrayOf("arg1", "arg2"))

           assertTrue(context.file("output.txt").exists())
       }
   }
   ```

3. **E2E Tests** - Full workflow testing
   ```kotlin
   @Test
   fun `test complete workflow`() {
       // Create project
       val createCmd = CreateCommand()
       createCmd.projectDir = context.projectDir
       createCmd.parse(arrayOf("my-mod"))

       // Generate item
       val itemCmd = CreateItemCommand()
       itemCmd.projectDir = context.projectDir
       itemCmd.parse(arrayOf("ruby"))

       // Build
       val buildCmd = BuildCommand()
       buildCmd.projectDir = context.projectDir
       buildCmd.parse(arrayOf())

       // Verify outputs
       assertTrue(context.file("build/libs/my-mod-fabric-1.0.0.jar").exists())
   }
   ```

### Test Best Practices

✅ **DO**:
- Use `TestProjectContext` for integration tests
- Set `command.projectDir` instead of modifying `user.dir`
- Clean up temporary directories in `@AfterEach`
- Test on Linux (or WSL/Docker) before committing

❌ **DON'T**:
- Modify `System.setProperty("user.dir")` (crashes on Windows)
- Create files in project root (use test directories)
- Leave temporary files behind
- Assume Windows native will run all tests

---

## Troubleshooting

### "Tests are excluded on Windows"

**Problem**: Running `./gradlew.bat :src:cli:test` only runs 53 tests

**Solution**: Use WSL or Docker
```bash
bash scripts/test-in-wsl.sh
# or
bash scripts/test-in-docker.sh
```

### "Connection reset by peer" on Windows

**Problem**: Test crashes with "Connection reset by peer" error

**Cause**: Gradle test executor crash on Windows with heavy file I/O

**Solution**: Use WSL or Docker (not native Windows)

### WSL "Java not found"

**Problem**: `bash scripts/test-in-wsl.sh` fails with "java: command not found"

**Solution**: Install Java in WSL
```bash
wsl sudo apt-get update
wsl sudo apt-get install -y openjdk-21-jdk
```

### Docker "Cannot connect to Docker daemon"

**Problem**: Docker tests fail with connection error

**Solution**: Start Docker Desktop
```bash
# Windows/macOS: Start Docker Desktop application
# Linux: sudo systemctl start docker
```

### "Permission denied" on gradlew

**Problem**: `./gradlew` fails with permission error

**Solution**: Grant execute permission
```bash
chmod +x gradlew
```

---

## Test Coverage

### Current Coverage (2026-02-09)

| Test Category | Total Tests | Windows Native | Linux/macOS/WSL/Docker |
|--------------|-------------|----------------|------------------------|
| Unit Tests | 53 | ✅ 53 | ✅ 53 |
| Integration Tests | ~400 | ❌ 0 | ✅ ~400 |
| E2E Tests | ~20 | ❌ 0 | ✅ ~20 |
| JAR Tests | 18 | ⏸️ @Ignore | ⏸️ @Ignore |
| **TOTAL** | **~470** | **53 (11%)** | **~470 (100%)** |

### Coverage by Feature

| Feature | Unit Tests | Integration Tests | E2E Tests |
|---------|-----------|-------------------|-----------|
| Mod ID Validation | ✅ 22 | ✅ Yes | ✅ Yes |
| Package Sanitization | ✅ 21 | ✅ Yes | ✅ Yes |
| JAR Validation | ✅ 10 | ⏸️ Pending | ⏸️ Pending |
| Item Generation | ❌ No | ✅ Yes | ✅ Yes |
| Block Generation | ❌ No | ✅ Yes | ✅ Yes |
| Entity Generation | ❌ No | ✅ Yes | ✅ Yes |
| Build/Package | ❌ No | ✅ Yes | ✅ Yes |
| Import/Export | ❌ No | ✅ Yes | ✅ Yes |
| CRUD Operations | ❌ No | ✅ Yes | ✅ Yes |

---

## Performance

### Test Execution Times

| Environment | Test Count | Duration | Notes |
|------------|-----------|----------|-------|
| Windows Native | 53 | ~10s | Unit tests only |
| Linux (native) | ~470 | ~120s | All tests |
| WSL | ~470 | ~180s | All tests + WSL overhead |
| Docker | ~470 | ~240s | All tests + container overhead |
| CI (GitHub Actions) | ~470 | ~300s | All tests + cold start |

### Optimization Tips

1. **Use test filters** for specific tests:
   ```bash
   ./gradlew :src:cli:test --tests "ValidationUtilTest"
   ```

2. **Run in parallel** (Linux/macOS only):
   ```bash
   ./gradlew :src:cli:test --parallel
   ```

3. **Use Gradle daemon** (local dev):
   ```bash
   ./gradlew :src:cli:test  # daemon enabled by default
   ```

4. **Disable daemon** (CI/containers):
   ```bash
   ./gradlew :src:cli:test --no-daemon
   ```

---

## Summary

### For Local Development

**Windows Developers**:
1. Quick feedback: `./gradlew.bat :src:cli:test` (unit tests, 10s)
2. Full validation: `bash scripts/test-in-wsl.sh` (all tests, 3min)

**Linux/macOS Developers**:
1. All tests: `./gradlew :src:cli:test` (all tests, 2min)

### For CI/CD

GitHub Actions automatically runs:
- Unit tests on Windows, Linux, macOS
- Full E2E tests on Linux
- Full E2E tests on Windows (via WSL)
- Full E2E tests in Docker
- Integration tests with generated projects

### For Contributors

Before submitting PR:
1. Run unit tests locally
2. Verify tests pass in CI
3. (Optional) Run full test suite via WSL/Docker

---

**Last Updated**: 2026-02-09
**For Questions**: See E2E_TEST_COVERAGE_REPORT.md and E2E_TESTING_SUMMARY.md
