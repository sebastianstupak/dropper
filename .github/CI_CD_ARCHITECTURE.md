# CI/CD Architecture Documentation

## Overview

The Dropper CI/CD pipeline is designed for **robustness, parallelization, and comprehensive testing** across multiple platforms and Java versions.

## Pipeline Structure

### 8 Parallel Jobs

```
┌─────────────────────────────────────────────────────────────────┐
│                          CI Pipeline                            │
└─────────────────────────────────────────────────────────────────┘
                                 │
        ┌────────────────────────┼────────────────────────┐
        │                        │                        │
   Job 1: Code Quality      Job 2: Unit Tests     Job 3-5: E2E Tests
   ├─ Detekt               Matrix (4 configs)     Matrix (3 categories)
   ├─ Security Scan        ├─ Ubuntu + Java 21    ├─ Package Sanitization
   └─ Compilation          ├─ Ubuntu + Java 17    ├─ Template Validation
                           ├─ Windows + Java 21   └─ Complex Modpack
                           └─ macOS + Java 21
                                 │
                                 ├─────────────┐
                                 │             │
                            Job 6: Build    Job 7: Integration
                            Distribution    Generated Project
                            Matrix (3 OS)
                                 │
                            Job 8: Test Summary
```

## Job Descriptions

### Job 1: Code Quality & Security
**Purpose:** Static analysis and security checks
**Runs on:** ubuntu-latest
**Duration:** ~5-10 minutes

**Checks:**
- ✅ Kotlin code style (Detekt)
- ✅ Dependency vulnerabilities
- ✅ Compilation without warnings

**Why important:** Catches code quality issues early before running expensive tests

---

### Job 2: Unit Tests
**Purpose:** Fast, isolated unit tests
**Runs on:** Matrix (4 configurations)
**Duration:** ~10-15 minutes per config

**Matrix:**
| OS | Java Version | Purpose |
|---|---|---|
| Ubuntu | 21 | Primary platform |
| Ubuntu | 17 | Backwards compatibility |
| Windows | 21 | Windows support |
| macOS | 21 | macOS support |

**Tests:**
- ✅ ValidationUtilTest (13 tests)
- ✅ PackageNameSanitizationTest (14 tests)

**Why important:** Ensures core utilities work across all platforms and Java versions

**Reliability Features:**
- `--stop` kills Gradle daemons before tests (prevents connection issues)
- `--rerun-tasks` forces fresh test execution
- `continue-on-error: true` allows other configs to complete
- Individual test upload per matrix configuration

---

### Job 3: E2E - Package Sanitization
**Purpose:** End-to-end package name generation testing
**Runs on:** Matrix (Ubuntu, Windows, macOS)
**Duration:** ~15-20 minutes per OS

**Tests:**
1. **Verification Scripts:** Run platform-specific validation
   - Unix: `scripts/verify-package-sanitization.sh`
   - Windows: `scripts/verify-package-sanitization.ps1`

2. **E2E Test Suite:** `PackageNameGenerationE2ETest`
   - 11 scenarios covering all mod ID variations
   - Tests actual project generation and file structure

**What it validates:**
- ✅ `my_mod` → `com.mymod` package generation
- ✅ `cool-mod` → `com.coolmod` package generation
- ✅ Multi-component consistency
- ✅ Asset paths preserve original mod IDs
- ✅ All loader registrations use correct packages

**Why important:** This is the PRIMARY fix validation - ensures the package sanitization bug is fixed on all platforms

---

### Job 4: E2E - Template Validation
**Purpose:** Validate Mustache templates render correctly
**Runs on:** Ubuntu, Windows
**Duration:** ~15-20 minutes per OS

**Tests:** `TemplateValidationE2ETest`
- Template variable substitution
- Java syntax validity
- JSON validity
- Package name variations
- Mod ID edge cases

**Why important:** Ensures generated code is syntactically valid across all templates

---

### Job 5: E2E - Complex Modpack
**Purpose:** Test large, complex mod generation
**Runs on:** Ubuntu only (intensive test)
**Duration:** ~30-40 minutes

**Tests:** `ComplexModpackE2ETest` (7 scenarios)
- 5 Minecraft versions with 3 loaders
- Progressive asset pack inheritance (v1→v2→v3→v4→v5)
- Version-specific features
- Mixed loader support
- Large-scale mods (20+ items)
- Complex inheritance chains
- Namespace organization

**Why important:** Validates Dropper handles real-world complex modpack scenarios

---

### Job 6: Build Distribution
**Purpose:** Create distributable CLI artifacts
**Runs on:** Matrix (Ubuntu, Windows, macOS)
**Duration:** ~10 minutes per OS
**Depends on:** code-quality, unit-tests

**Produces:**
- ZIP distributions for each platform
- Retained for 30 days

**Why important:** Ensures the CLI can be packaged for distribution on all platforms

---

### Job 7: Integration - Generated Project Build
**Purpose:** Full integration test with real project generation
**Runs on:** Ubuntu
**Duration:** ~15-20 minutes
**Depends on:** build-distribution

**Workflow:**
1. Generate project with special characters: `my_test-mod`
2. Generate multiple items: `test_sword`, `magic_wand`
3. Generate block: `test_ore`
4. Verify package structure:
   - Check sanitized package exists: `com/mytestmod`
   - Verify Services.java package: `com.mytestmod`
   - Verify item packages: `com.mytestmod.items`

**Why important:** End-to-end validation that the entire generation workflow produces valid, correctly-structured projects

---

### Job 8: Test Summary
**Purpose:** Aggregate and display all test results
**Runs on:** Ubuntu
**Duration:** ~2-5 minutes
**Depends on:** All test jobs
**Runs:** Always (even if tests fail)

**Actions:**
- Downloads all test result artifacts
- Publishes unified test summary using `test-summary/action`
- Shows passed/failed/skipped tests across all jobs

**Why important:** Provides single view of all test results without checking each job individually

---

## Robustness Features

### 1. Gradle Daemon Management
**Problem:** Gradle daemons can cause connection reset errors on CI
**Solution:**
```yaml
- name: Stop existing Gradle daemons
  run: ./gradlew --stop
```
**Applied to:** All jobs that run tests

### 2. Fresh Test Execution
**Problem:** Cached test results can hide failures
**Solution:**
```yaml
run: ./gradlew :src:cli:test --tests "TestClass" --rerun-tasks
```
**Applied to:** All test executions

### 3. Continue on Error
**Problem:** One failing test config stops entire pipeline
**Solution:**
```yaml
continue-on-error: true
```
**Applied to:** Test execution steps (but not build steps)

### 4. Matrix Strategy with fail-fast: false
**Problem:** One platform failure stops testing on other platforms
**Solution:**
```yaml
strategy:
  fail-fast: false
  matrix: ...
```
**Applied to:** All matrix jobs

### 5. Test Result Upload (always)
**Problem:** Test results lost when tests fail
**Solution:**
```yaml
- name: Upload test results
  if: always()
  uses: actions/upload-artifact@...
```
**Applied to:** All test jobs

### 6. Timeouts
**Problem:** Hung jobs waste runner time
**Solution:**
```yaml
timeout-minutes: 30  # Per job
```
**Applied to:** All jobs

### 7. Gradle Optimization
**Global settings:**
```yaml
env:
  GRADLE_OPTS: '-Dorg.gradle.jvmargs="-Xmx2048m -XX:MaxMetaspaceSize=512m" -Dorg.gradle.daemon=false -Dorg.gradle.parallel=true'
```
- Daemon disabled for CI reliability
- Parallel execution for speed
- Memory limits prevent OOM

### 8. Artifact Retention
- Test results: 7 days
- Distributions: 30 days
- Generated projects: 7 days

---

## Test Coverage Matrix

| Test Category | Unit | E2E | Integration | Total |
|---|---|---|---|---|
| Package Sanitization | 14 | 11 | 1 | 26 |
| Validation | 13 | - | - | 13 |
| Template Rendering | - | 7 | - | 7 |
| Complex Modpack | - | 7 | - | 7 |
| **Total** | **27** | **25** | **1** | **53** |

---

## Platforms Tested

| Platform | Java 17 | Java 21 | Total Configs |
|---|---|---|---|
| Ubuntu | ✅ | ✅ | 2 |
| Windows | - | ✅ | 1 |
| macOS | - | ✅ | 1 |
| **Total** | **1** | **3** | **4** |

---

## Validation Before Push

### Pre-Push Checklist

Run this before pushing CI changes:

```bash
# 1. Verify build succeeds
./gradlew :src:cli:assemble

# 2. Verify test files exist
ls src/cli/src/test/kotlin/dev/dropper/util/ValidationUtilTest.kt
ls src/cli/src/test/kotlin/dev/dropper/util/PackageNameSanitizationTest.kt
ls src/cli/src/test/kotlin/dev/dropper/e2e/PackageNameGenerationE2ETest.kt
ls src/cli/src/test/kotlin/dev/dropper/e2e/TemplateValidationE2ETest.kt
ls src/cli/src/test/kotlin/dev/dropper/e2e/ComplexModpackE2ETest.kt

# 3. Verify scripts exist
ls scripts/verify-package-sanitization.sh
ls scripts/verify-package-sanitization.ps1

# 4. Test script execution locally
./scripts/verify-package-sanitization.sh  # Unix
# OR
./scripts/verify-package-sanitization.ps1  # Windows

# 5. Verify CI workflow syntax
cat .github/workflows/ci.yml | grep -E "(name:|runs-on:|uses:)" | head -20

# 6. Check for typos in test class names
grep -r "PackageNameSanitizationTest" .github/workflows/
grep -r "PackageNameGenerationE2ETest" .github/workflows/
grep -r "TemplateValidationE2ETest" .github/workflows/
grep -r "ComplexModpackE2ETest" .github/workflows/
```

---

## Expected CI Duration

| Job | Avg Duration | Max Duration |
|---|---|---|
| Code Quality | 5 min | 15 min |
| Unit Tests (per config) | 10 min | 20 min |
| E2E Package Sanitization | 15 min | 30 min |
| E2E Template Validation | 15 min | 30 min |
| E2E Complex Modpack | 30 min | 45 min |
| Build Distribution | 10 min | 20 min |
| Integration Test | 15 min | 30 min |
| Test Summary | 3 min | 10 min |

**Total Pipeline Duration:** ~35-45 minutes (with parallelization)
**Without Parallelization:** ~2-3 hours

---

## Monitoring CI Results

### After Push:

1. **Go to Actions tab:** https://github.com/YOUR_USERNAME/minecraft-mod-versioning-example/actions

2. **Click on latest run** to see overview

3. **Check each job:**
   - ✅ Green = Passed
   - ❌ Red = Failed
   - 🟡 Yellow = Skipped/Cancelled

4. **For failures:**
   - Click failed job
   - Expand failed step
   - Check logs for error messages
   - Download test result artifacts for detailed analysis

5. **Test Summary:**
   - Scroll to "Test Summary" job
   - View aggregated test results
   - See pass/fail counts across all platforms

---

## Troubleshooting Common Issues

### Issue: "Connection reset by peer" in tests
**Cause:** Gradle daemon connection issues
**Solution:** Already implemented with `./gradlew --stop` and `--rerun-tasks`

### Issue: Tests pass locally but fail in CI
**Cause:** Platform differences, cached results, or timing issues
**Solution:**
1. Check specific OS job that failed
2. Download test artifacts
3. Run tests with same flags: `./gradlew :src:cli:test --tests "TestClass" --rerun-tasks`

### Issue: CI timeout
**Cause:** Hung test or slow runner
**Solution:**
1. Check job logs for last completed step
2. Increase timeout-minutes if needed
3. Optimize test to run faster

### Issue: Artifact upload fails
**Cause:** Path doesn't exist
**Solution:**
1. Verify path in workflow matches actual test output location
2. Check if tests generated the expected files

---

## Future Enhancements

### Planned:
- [ ] Performance regression testing
- [ ] Native image build testing (GraalVM)
- [ ] Generated project compilation test (requires MC dependencies)
- [ ] Benchmark comparisons between runs
- [ ] Codecov integration for coverage reports
- [ ] Automatic PR comments with test results

### Under Consideration:
- [ ] Daily cron job for extended test suite
- [ ] Separate workflow for performance tests
- [ ] Docker-based testing for full isolation
- [ ] Multi-version matrix testing (test against multiple Gradle versions)
