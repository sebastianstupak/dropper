# Dropper CLI - Test Scripts

This directory contains scripts for running tests across different environments.

---

## Quick Start

### Run All Tests (Recommended)

**On Windows:**
```bash
bash scripts/test-in-wsl.sh
```

**On Linux/macOS:**
```bash
./gradlew :src:cli:test
```

---

## Available Scripts

### Test Execution

#### `test-in-wsl.sh`
Run full test suite in Windows Subsystem for Linux

**Usage:**
```bash
bash scripts/test-in-wsl.sh
```

**Requirements:**
- WSL installed (`wsl --install`)
- Java 21 in WSL (script auto-installs if missing)

**What it does:**
- Detects/launches WSL environment
- Installs Java if needed
- Runs all 450+ tests in Linux environment
- Bypasses Windows Gradle test executor issues

**Output:**
```
╔═══════════════════════════════════════════════════════════════╗
║           Dropper CLI - WSL Test Runner                      ║
╚═══════════════════════════════════════════════════════════════╝

✅ Running in WSL
📁 Project Directory: /mnt/d/dev/minecraft-mod-versioning-example
...
✅ ALL TESTS PASSED IN WSL! ✅
```

---

#### `test-in-docker.sh`
Run full test suite in Docker container

**Usage:**
```bash
bash scripts/test-in-docker.sh
```

**Requirements:**
- Docker installed and running
- Docker Desktop (Windows/macOS) or Docker Engine (Linux)

**What it does:**
- Builds Docker test image (one-time)
- Runs all 450+ tests in containerized Linux
- No host environment modification

**Output:**
```
╔═══════════════════════════════════════════════════════════════╗
║           Dropper CLI - Docker Test Runner                   ║
╚═══════════════════════════════════════════════════════════════╝

🐳 Building Docker test image...
✅ Docker image built
...
✅ ALL TESTS PASSED IN DOCKER! ✅
```

---

### E2E Validation

#### `e2e-validation.sh` / `e2e-validation.ps1`
Manual E2E validation scripts (legacy)

**Usage:**
```bash
# Unix/Linux/macOS/WSL
bash scripts/e2e-validation.sh

# Windows PowerShell
powershell -ExecutionPolicy Bypass -File scripts\e2e-validation.ps1
```

**What it does:**
- Creates test project structure
- Generates sample item/block files
- Validates file existence and content
- Does NOT use Gradle test executor

**Note:** These are fallback scripts. Use `test-in-wsl.sh` or `test-in-docker.sh` for complete test coverage.

---

### Package Validation

#### `verify-package-sanitization.sh` / `verify-package-sanitization.ps1`
Verify package name sanitization

**Usage:**
```bash
# Unix/Linux/macOS/WSL
bash scripts/verify-package-sanitization.sh

# Windows PowerShell
powershell -File scripts\verify-package-sanitization.ps1
```

**What it does:**
- Tests mod ID sanitization (hyphens, underscores)
- Verifies Java package name generation
- Validates generated package directories

---

### Installation

#### `install.sh` / `install.ps1`
Install Dropper CLI globally

**Usage:**
```bash
# Unix/Linux/macOS
bash scripts/install.sh

# Windows PowerShell
powershell -ExecutionPolicy Bypass -File scripts\install.ps1
```

**What it does:**
- Builds Dropper CLI
- Installs to system PATH
- Creates `dropper` command

---

### Pre-push Validation

#### `pre-push-validation.sh`
Comprehensive pre-push checks

**Usage:**
```bash
bash scripts/pre-push-validation.sh
```

**What it does:**
- Runs all unit tests
- Validates code compiles
- Checks for common issues
- Verifies project structure

---

## Environment Detection

Scripts automatically detect the environment:

| Environment | Detected By | Tests Run |
|------------|-------------|-----------|
| Windows Native | `os.name` | 53 unit tests (limited) |
| WSL | `WSL_DISTRO_NAME` env var | All 450+ tests |
| Docker | `DROPPER_TEST_ENV=docker` | All 450+ tests |
| Linux/macOS | `os.name` | All 450+ tests |

---

## Environment Variables

Set these to control test behavior:

```bash
# Force WSL mode
export DROPPER_TEST_ENV=wsl

# Force Docker mode
export DROPPER_TEST_ENV=docker

# Force container mode
export DROPPER_TEST_ENV=container

# Linux native
export DROPPER_TEST_ENV=linux
```

---

## Troubleshooting

### WSL: "java: command not found"

**Problem:** Java not installed in WSL

**Solution:**
```bash
wsl sudo apt-get update
wsl sudo apt-get install -y openjdk-21-jdk
wsl java -version  # Verify
```

### Docker: "Cannot connect to Docker daemon"

**Problem:** Docker not running

**Solution:**
```bash
# Windows/macOS: Start Docker Desktop
# Linux: sudo systemctl start docker
docker ps  # Verify
```

### Windows: "Permission denied" on scripts

**Problem:** Scripts not executable

**Solution:**
```bash
chmod +x scripts/*.sh
```

### WSL: "Distribution not found"

**Problem:** WSL not installed

**Solution:**
```powershell
wsl --install -d Ubuntu
wsl --list --verbose  # Verify
```

---

## Development Workflow

### Quick Feedback Loop (Windows)

```bash
# 1. Run unit tests (fast, 10s)
./gradlew.bat :src:cli:test

# 2. If unit tests pass, run full suite (3min)
bash scripts/test-in-wsl.sh
```

### Quick Feedback Loop (Linux/macOS)

```bash
# Run all tests (2min)
./gradlew :src:cli:test
```

### Before Commit

```bash
# Run pre-push validation
bash scripts/pre-push-validation.sh
```

### Before Push

```bash
# Run full test suite
bash scripts/test-in-wsl.sh      # Windows
./gradlew :src:cli:test          # Linux/macOS
```

---

## CI/CD Integration

These scripts are used in GitHub Actions workflows:

### `.github/workflows/ci.yml`

```yaml
# Unit tests (all platforms)
- name: Run unit tests
  run: ./gradlew :src:cli:test --no-daemon

# E2E tests (Linux)
- name: Run full test suite
  run: ./gradlew :src:cli:test --no-daemon
  env:
    DROPPER_TEST_ENV: linux

# E2E tests (Windows WSL)
- name: Run full test suite in WSL
  shell: wsl-bash {0}
  run: ./gradlew :src:cli:test --no-daemon
  env:
    DROPPER_TEST_ENV: wsl

# E2E tests (Docker)
- name: Run full test suite in Docker
  run: docker run --rm dropper-test
```

---

## Performance

| Script | Environment | Duration | Test Count |
|--------|-------------|----------|------------|
| `./gradlew test` (Windows) | Native | ~10s | 53 tests |
| `test-in-wsl.sh` | WSL | ~180s | ~470 tests |
| `test-in-docker.sh` | Docker | ~240s | ~470 tests |
| `./gradlew test` (Linux) | Native | ~120s | ~470 tests |

---

## See Also

- **TESTING.md** - Comprehensive testing guide
- **E2E_TEST_COVERAGE_REPORT.md** - What's NOT tested
- **E2E_TEST_RESULTS.md** - What IS tested
- **E2E_TESTING_SUMMARY.md** - Complete test status

---

**Last Updated**: 2026-02-09
