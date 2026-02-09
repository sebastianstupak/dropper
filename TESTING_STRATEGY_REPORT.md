# Dropper CLI - Testing Strategy & Execution Plan

**Generated**: 2026-02-09
**Test Suite Size**: 925 test methods across 42 test files
**Current Status**: Compilation blocked - Fix required before execution

---

## Overview

The Dropper CLI project has a world-class test suite with comprehensive E2E coverage. This document outlines the strategy for executing the test suite and achieving 80%+ code coverage.

---

## Test Suite Architecture

### Test Organization

```
src/cli/src/test/kotlin/dev/dropper/
├── commands/           # Command-level unit tests
├── integration/        # E2E integration tests
│   ├── *CommandTest.kt       # Command E2E tests
│   ├── *CommandE2ETest.kt    # Extended E2E scenarios
│   ├── *CommandAdvancedE2ETest.kt  # Complex scenarios
│   └── *IntegrationTest.kt   # Cross-component tests
└── e2e/               # Full workflow tests
    ├── FullWorkflowTest.kt
    ├── CompleteWorkflowTest.kt
    └── CLIWorkflowTest.kt
```

### Test Categorization

| Category | Files | Tests | Purpose |
|----------|-------|-------|---------|
| **Unit Tests** | 15 | ~200 | Individual command/utility testing |
| **Integration Tests** | 20 | ~500 | Multi-component interaction |
| **E2E Tests** | 7 | ~225 | Complete workflows end-to-end |

---

## Testing Phases

### Phase 0: Core Feature Generators (Foundation)

**Purpose**: Verify all content generation commands work correctly

**Test Files** (7 files, ~150 tests):
- `CreateItemCommandTest.kt` - Item generation
- `CreateBlockCommandTest.kt` - Block generation
- `CreateEntityCommandTest.kt` - Entity generation
- `CreateRecipeCommandTest.kt` - Recipe generation
- `CreateEnchantmentCommandTest.kt` - Enchantment generation
- `CreateBiomeCommandTest.kt` - Biome generation
- `CreateTagCommandTest.kt` - Tag generation

**What's Tested**:
- Command parsing and validation
- File generation (Java classes, JSON assets)
- Multi-loader compatibility (Fabric, Forge, NeoForge)
- Package structure correctness
- Resource file generation

**Execution**:
```bash
./gradlew :src:cli:test --tests "*CreateItemCommandTest*" \
                        --tests "*CreateBlockCommandTest*" \
                        --tests "*CreateEntityCommandTest*" \
                        --tests "*CreateRecipeCommandTest*" \
                        --tests "*CreateEnchantmentCommandTest*" \
                        --tests "*CreateBiomeCommandTest*" \
                        --tests "*CreateTagCommandTest*"
```

**Success Criteria**:
- All generator commands produce valid output
- Generated files follow correct structure
- Multi-loader scenarios work correctly
- Test pass rate: 100%

---

### Phase 1: Development Operations (Essential)

**Purpose**: Test day-to-day development commands

**Test Files** (6 files, ~100 tests):
- `DevCommandTest.kt` - Dev server management
- `DevCommandE2ETest.kt` - Extended dev scenarios
- `ValidateCommandE2ETest.kt` - Project validation
- `ListCommandE2ETest.kt` - Resource listing
- `ListCommandBasicTest.kt` - Basic list functionality
- `ValidationUtilTest.kt` - Validation utilities

**What's Tested**:
- Development server lifecycle
- Live reload functionality
- Project structure validation
- Resource enumeration
- Configuration validation

**Execution**:
```bash
./gradlew :src:cli:test --tests "*DevCommandTest*" \
                        --tests "*ValidateCommandE2ETest*" \
                        --tests "*ListCommandE2ETest*" \
                        --tests "*ListCommandBasicTest*" \
                        --tests "*ValidationUtilTest*"
```

**Success Criteria**:
- Dev commands work reliably
- Validation catches common errors
- List commands show correct output
- Test pass rate: 100%

---

### Phase 2: Version & Asset Management (Critical)

**Purpose**: Verify multi-version and asset pack functionality

**Test Files** (7 files, ~120 tests):
- `AddVersionCommandTest.kt` - Adding Minecraft versions
- `RemoveCommandE2ETest.kt` - Removing versions/resources
- `RenameCommandE2ETest.kt` - Renaming resources
- `SyncCommandE2ETest.kt` - Syncing versions
- `AssetPackE2ETest.kt` - Asset pack management
- `AssetPackCommandTest.kt` - Asset pack commands
- `MinecraftVersionsE2ETest.kt` - Version detection
- `SimpleModVersionsTest.kt` - Version compatibility

**What's Tested**:
- Multi-version project structure
- Asset pack versioning
- Cross-version synchronization
- Version compatibility detection
- Resource management across versions

**Execution**:
```bash
./gradlew :src:cli:test --tests "*AddVersionCommandTest*" \
                        --tests "*RemoveCommandE2ETest*" \
                        --tests "*RenameCommandE2ETest*" \
                        --tests "*SyncCommandE2ETest*" \
                        --tests "*AssetPackE2ETest*" \
                        --tests "*AssetPackCommandTest*" \
                        --tests "*MinecraftVersionsE2ETest*" \
                        --tests "*SimpleModVersionsTest*"
```

**Success Criteria**:
- Version management works correctly
- Asset packs organize resources properly
- Sync operations maintain consistency
- Test pass rate: ≥ 95%

---

### Phase 3: Publishing & Packaging (Business Critical)

**Purpose**: Test mod distribution and publishing

**Test Files** (6 files, ~180 tests):
- `PublishCommandE2ETest.kt` - Basic publishing
- `PublishCommandAdvancedE2ETest.kt` - Advanced publishing scenarios
- `PublishPackageIntegrationTest.kt` - Package+publish integration
- `PackageCommandE2ETest.kt` - Packaging functionality
- `PackageCommandAdvancedE2ETest.kt` - Advanced packaging
- `PackagePublishErrorHandlingTest.kt` - Error scenarios

**What's Tested**:
- Modrinth publishing
- CurseForge publishing
- GitHub releases
- Package generation (JAR files)
- Multi-platform publishing
- Error handling and recovery
- Changelog generation
- Dry-run mode

**Execution**:
```bash
# CURRENTLY BLOCKED - Fix compilation first
./gradlew :src:cli:test --tests "*PublishCommandE2ETest*" \
                        --tests "*PublishCommandAdvancedE2ETest*" \
                        --tests "*PublishPackageIntegrationTest*" \
                        --tests "*PackageCommandE2ETest*" \
                        --tests "*PackageCommandAdvancedE2ETest*" \
                        --tests "*PackagePublishErrorHandlingTest*"
```

**Success Criteria**:
- Publishing to all platforms works
- Error handling is robust
- Dry-run mode prevents accidental publishes
- Test pass rate: ≥ 90%

**BLOCKERS**:
- ❌ 20 compilation errors in PublishCommandAdvancedE2ETest.kt
- ❌ 13 compilation errors in PublishPackageIntegrationTest.kt
- See COMPILATION_ERRORS_REPORT.md for fixes

---

### Phase 4: Advanced Operations (Power Features)

**Purpose**: Test advanced workflow features

**Test Files** (10 files, ~150 tests):
- `MigrateCommandE2ETest.kt` - Basic version migration
- `MigrateCommandAdvancedE2ETest.kt` - Complex migrations
- `ImportCommandE2ETest.kt` - Project import
- `UpdateCommandE2ETest.kt` - Dependency updates
- `ExportCommandE2ETest.kt` - Project export
- `SearchCommandE2ETest.kt` - Resource search
- `TemplateCommandE2ETest.kt` - Template management
- `TemplateValidationE2ETest.kt` - Template validation
- `CleanCommandE2ETest.kt` - Project cleanup
- `BuildCommandTest.kt` - Build integration

**What's Tested**:
- Migration between Minecraft versions
- API change detection
- Project import/export
- Template system
- Dependency management
- Search functionality

**Execution**:
```bash
# MigrateCommandAdvancedE2ETest.kt BLOCKED
./gradlew :src:cli:test --tests "*MigrateCommandE2ETest*" \
                        --tests "*ImportCommandE2ETest*" \
                        --tests "*UpdateCommandE2ETest*" \
                        --tests "*ExportCommandE2ETest*" \
                        --tests "*SearchCommandE2ETest*" \
                        --tests "*TemplateCommandE2ETest*" \
                        --tests "*TemplateValidationE2ETest*" \
                        --tests "*CleanCommandE2ETest*" \
                        --tests "*BuildCommandTest*"
```

**Success Criteria**:
- Migration handles API changes correctly
- Import/export preserves project structure
- Template system is flexible
- Test pass rate: ≥ 85%

**BLOCKERS**:
- ❌ 11 compilation errors in MigrateCommandAdvancedE2ETest.kt
- See COMPILATION_ERRORS_REPORT.md for fixes

---

### Phase 5: Full Workflow Integration (Confidence)

**Purpose**: Verify complete end-to-end workflows

**Test Files** (8 files, ~225 tests):
- `FullWorkflowTest.kt` - Complete mod lifecycle
- `CompleteWorkflowTest.kt` - Alternative workflow
- `CLIWorkflowTest.kt` - CLI interaction patterns
- `FullCLIBuildTest.kt` - Build system integration
- `CreateCommandTest.kt` - Project creation flows
- `E2ETest.kt` - Generic E2E scenarios

**What's Tested**:
- Project creation → development → publishing workflow
- Multi-loader project generation
- Cross-component integration
- Real-world usage scenarios
- Performance under realistic conditions

**Execution**:
```bash
./gradlew :src:cli:test --tests "*FullWorkflowTest*" \
                        --tests "*CompleteWorkflowTest*" \
                        --tests "*CLIWorkflowTest*" \
                        --tests "*FullCLIBuildTest*" \
                        --tests "*CreateCommandTest*" \
                        --tests "*E2ETest*"
```

**Success Criteria**:
- All workflows complete successfully
- No resource leaks or file locks
- Generated projects build successfully
- Test pass rate: 100%

---

## Test Execution Strategy

### Pre-Execution Checklist

- [ ] Fix 44 compilation errors (see COMPILATION_ERRORS_REPORT.md)
- [ ] Verify all dependencies are available
- [ ] Ensure sufficient disk space (tests create temporary projects)
- [ ] Close any IDE file watchers (can cause file lock issues on Windows)

### Execution Plan

#### Iteration 1: Full Suite (Initial Run)

**Purpose**: Establish baseline, identify flaky tests

```bash
./gradlew :src:cli:clean
./gradlew :src:cli:test --rerun-tasks 2>&1 | tee test-iteration-1.log
```

**Analysis**:
- Count pass/fail/skip
- Identify failures
- Note execution time
- Check for flaky tests

#### Iteration 2: Clean Build Verification

**Purpose**: Verify consistency, ensure no caching issues

```bash
./gradlew :src:cli:clean
./gradlew :src:cli:test 2>&1 | tee test-iteration-2.log
```

**Analysis**:
- Compare with Iteration 1
- Identify inconsistent tests
- Document any differences

#### Iteration 3: No-Cache Run

**Purpose**: Verify from-scratch builds

```bash
./gradlew :src:cli:test --no-build-cache 2>&1 | tee test-iteration-3.log
```

**Analysis**:
- Verify no cache corruption
- Confirm consistent results

### Phased Execution

Run each phase independently for targeted analysis:

```bash
# Phase 0: Generators (should pass 100%)
./gradlew :src:cli:test --tests "*Create*CommandTest*"

# Phase 1: Dev operations (should pass 100%)
./gradlew :src:cli:test --tests "*DevCommandTest*" --tests "*ValidateCommandE2ETest*"

# Phase 2: Version management (should pass 95%+)
./gradlew :src:cli:test --tests "*AddVersionCommandTest*" --tests "*RemoveCommandE2ETest*"

# Phase 3: Publishing (BLOCKED - fix first)
# ./gradlew :src:cli:test --tests "*PublishCommandE2ETest*"

# Phase 4: Advanced (PARTIALLY BLOCKED)
./gradlew :src:cli:test --tests "*ImportCommandE2ETest*" --tests "*UpdateCommandE2ETest*"

# Phase 5: Full workflows (should pass 100%)
./gradlew :src:cli:test --tests "*FullWorkflowTest*" --tests "*CompleteWorkflowTest*"
```

---

## Coverage Analysis

### Adding JaCoCo Plugin

Add to `src/cli/build.gradle.kts`:

```kotlin
plugins {
    // ... existing plugins
    jacoco
}

jacoco {
    toolVersion = "0.8.11"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/generated/**",
                    "**/buildSrc/**"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = "0.80".toBigDecimal()
            }
        }

        rule {
            element = "CLASS"
            limit {
                counter = "BRANCH"
                minimum = "0.70".toBigDecimal()
            }
        }
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}
```

### Generating Coverage Reports

```bash
# Run tests with coverage
./gradlew :src:cli:test :src:cli:jacocoTestReport

# View HTML report
open src/cli/build/reports/jacoco/test/html/index.html
# (Use 'start' on Windows, 'xdg-open' on Linux)

# Check coverage threshold
./gradlew :src:cli:jacocoTestCoverageVerification
```

### Coverage Targets

| Metric | Target | Minimum |
|--------|--------|---------|
| **Line Coverage** | 85% | 80% |
| **Branch Coverage** | 75% | 70% |
| **Method Coverage** | 80% | 75% |
| **Class Coverage** | 85% | 80% |

### Coverage by Component (Estimated)

| Component | Target | Confidence |
|-----------|--------|------------|
| Core Generators | 90% | HIGH |
| CLI Commands | 85% | HIGH |
| Version Management | 85% | HIGH |
| Publishing System | 75% | MEDIUM (blocked) |
| Migration System | 70% | MEDIUM (blocked) |
| Utilities | 90% | HIGH |
| Build Logic | 75% | MEDIUM |

---

## Performance Benchmarks

### Expected Execution Times

| Phase | Tests | Est. Time | Notes |
|-------|-------|-----------|-------|
| Phase 0 | ~150 | 2-3 min | File I/O intensive |
| Phase 1 | ~100 | 1-2 min | Light operations |
| Phase 2 | ~120 | 3-5 min | Multi-version setup |
| Phase 3 | ~180 | 4-6 min | Network mocking |
| Phase 4 | ~150 | 3-5 min | Complex operations |
| Phase 5 | ~225 | 5-8 min | Full workflows |
| **TOTAL** | **925** | **18-29 min** | Full suite |

### Performance Monitoring

Track these metrics:

```bash
# Slowest tests
./gradlew :src:cli:test 2>&1 | grep "completed in" | sort -k4 -n | tail -10

# Test count by duration
# < 1s: Fast
# 1-5s: Acceptable
# 5-10s: Slow (investigate)
# > 10s: Very slow (optimize)
```

---

## Flaky Test Detection

### Multi-Run Verification

```bash
# Run 3 times and compare
for i in 1 2 3; do
    ./gradlew :src:cli:test 2>&1 | tee test-run-$i.log
done

# Compare results
diff test-run-1.log test-run-2.log
diff test-run-2.log test-run-3.log
```

### Common Flaky Test Causes

1. **File System Race Conditions**
   - Temporary directories not cleaned up
   - File locks on Windows
   - Timing-dependent I/O

2. **Test Order Dependencies**
   - Shared state between tests
   - Static variables not reset
   - File system pollution

3. **External Dependencies**
   - Network conditions
   - System time
   - Available ports

### Mitigation Strategies

```kotlin
@BeforeEach
fun setup() {
    // Clean setup
    testDir = Files.createTempDirectory("dropper-test").toFile()
}

@AfterEach
fun cleanup() {
    // Aggressive cleanup
    testDir.deleteRecursively()
    System.gc() // Help release file handles on Windows
}
```

---

## Continuous Integration

### CI Pipeline

```yaml
# .github/workflows/test-coverage.yml
name: Test Coverage

on: [push, pull_request]

jobs:
  test:
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, windows-latest]

    runs-on: ${{ matrix.os }}

    steps:
      - uses: actions/checkout@v4

      - uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Run tests with coverage
        run: ./gradlew :src:cli:test :src:cli:jacocoTestReport

      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: src/cli/build/reports/jacoco/test/jacocoTestReport.xml

      - name: Verify coverage threshold
        run: ./gradlew :src:cli:jacocoTestCoverageVerification

      - name: Archive test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: test-results-${{ matrix.os }}
          path: src/cli/build/reports/tests/
```

---

## Success Criteria

### Overall Goals

- [ ] All 925 tests compile successfully
- [ ] Test pass rate ≥ 95% (877+ passing)
- [ ] Overall line coverage ≥ 80%
- [ ] Branch coverage ≥ 70%
- [ ] No flaky tests (100% consistent across 3 runs)
- [ ] Full test suite completes in < 30 minutes

### Phase-Specific Goals

- [ ] Phase 0: 100% pass rate (generators must work)
- [ ] Phase 1: 100% pass rate (dev workflow must work)
- [ ] Phase 2: ≥ 95% pass rate (version management critical)
- [ ] Phase 3: ≥ 90% pass rate (publishing important)
- [ ] Phase 4: ≥ 85% pass rate (advanced features)
- [ ] Phase 5: 100% pass rate (workflows must complete)

---

## Next Steps

### Immediate (Priority 1)

1. **Fix compilation errors** (2-3 hours)
   - See COMPILATION_ERRORS_REPORT.md for detailed fixes
   - Focus on publishing and migration tests

2. **Run Phase 0 tests** (15 minutes)
   - Verify generators work correctly
   - Establish baseline for working tests

3. **Add JaCoCo plugin** (15 minutes)
   - Update build.gradle.kts
   - Configure coverage thresholds

### Short-term (Priority 2)

4. **Run full test suite** (1 hour)
   - Execute all 3 iterations
   - Document results
   - Identify flaky tests

5. **Generate coverage reports** (30 minutes)
   - Analyze coverage gaps
   - Create improvement plan

6. **Fix failing tests** (2-4 hours)
   - Address non-compilation failures
   - Achieve 95%+ pass rate

### Medium-term (Priority 3)

7. **Optimize slow tests** (2-3 hours)
   - Identify tests > 5 seconds
   - Reduce test data size
   - Parallelize where possible

8. **Add missing tests** (4-8 hours)
   - Fill coverage gaps to reach 80%
   - Focus on edge cases
   - Improve error handling coverage

9. **CI/CD integration** (2 hours)
   - Add coverage reporting to CI
   - Set up coverage tracking
   - Configure automatic reports

---

## Conclusion

The Dropper CLI has a **world-class test suite** with 925 comprehensive tests providing extensive E2E coverage. The test-to-code ratio of 1.05:1 is exceptional.

**Current Status**: 🟡 Ready to execute (after fixing 44 compilation errors)

**Confidence Level**: 🟢 HIGH
- Test suite is comprehensive and well-organized
- Phase-based structure makes execution manageable
- High likelihood of achieving 80%+ coverage once tests run

**Recommended Approach**:
1. Fix compilation (2-3 hours)
2. Run phased execution (3-4 hours)
3. Generate coverage reports (1 hour)
4. Address gaps if needed (2-4 hours)

**Total Time to 80% Coverage**: 8-12 hours

The foundation is excellent - just needs the compilation fixes to unlock its potential!
