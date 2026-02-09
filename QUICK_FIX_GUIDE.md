# Dropper CLI - Quick Fix Guide for Test Execution

**Goal:** Fix compilation errors and run the comprehensive test suite

**Time Required:** 30-60 minutes

---

## Problem

The Dropper CLI project cannot compile due to:
1. ❌ Missing Jackson dependencies (12 files affected)
2. ❌ Missing Gson main dependency (2 files affected)
3. ❌ Conflicting main() functions (3 files affected)

---

## Solution

### Fix 1: Add Missing Dependencies

**File:** `D:\dev\minecraft-mod-versioning-example\src\cli\build.gradle.kts`

**Location:** In the `dependencies` block, add these lines:

```kotlin
dependencies {
    // CLI framework (GraalVM compatible)
    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    // YAML parsing (GraalVM compatible)
    implementation("com.charleskorn.kaml:kaml:0.55.0")

    // HTTP client (GraalVM compatible)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON parsing (GraalVM compatible)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Mustache templates (GraalVM compatible)
    implementation("com.github.spullara.mustache.java:compiler:0.9.11")

    // JSON serialization for advanced features (ADD THESE 4 LINES)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.16.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
    testImplementation(kotlin("test"))
}
```

**Note:** Remove the duplicate `testImplementation("com.google.code.gson:gson:2.10.1")` line if present.

---

### Fix 2: Remove Conflicting main() Functions

#### File 1: `src/cli/src/main/kotlin/dev/dropper/commands/CreateCommand.kt`

Find and **delete or comment out** the `main()` function at the bottom of the file:

```kotlin
// DELETE OR COMMENT OUT THIS:
fun main(args: Array<String>) {
    CreateCommand().main(args)
}
```

#### File 2: `src/cli/src/main/kotlin/dev/dropper/commands/ListCommand.kt`

Find and **delete or comment out** the `main()` function at the bottom of the file:

```kotlin
// DELETE OR COMMENT OUT THIS:
fun main(args: Array<String>) = ListCommand()
    .subcommands(
        // ... subcommands ...
    )
    .main(args)
```

#### File 3: `src/cli/src/main/kotlin/dev/dropper/commands/RemoveCommand.kt`

Find and **delete or comment out** the `main()` function at the bottom of the file:

```kotlin
// DELETE OR COMMENT OUT THIS:
fun main(args: Array<String>) = RemoveCommand()
    .subcommands(
        // ... subcommands ...
    )
    .main(args)
```

---

### Fix 3: Verify Imports

**File:** `src/cli/src/main/kotlin/dev/dropper/commands/publish/PublishHelper.kt`

Ensure this import is present at the top:

```kotlin
import kotlinx.serialization.serializer
```

If missing, add it after the existing `kotlinx.serialization.Serializable` import.

---

## Test Compilation

After making the fixes, test if the project compiles:

```bash
# Change to project directory
cd D:\dev\minecraft-mod-versioning-example

# Clean build
./gradlew :src:cli:clean

# Compile Kotlin code
./gradlew :src:cli:compileKotlin
```

**Expected Output:**
```
BUILD SUCCESSFUL in 25s
```

---

## Run Tests

Once compilation succeeds, run the full test suite:

```bash
# Run all tests
./gradlew :src:cli:test

# Or run with clean build
./gradlew :src:cli:clean :src:cli:test

# Or run with detailed output
./gradlew :src:cli:test --info
```

**Expected Output:**
```
BUILD SUCCESSFUL in 90-120s
90+ tests completed, 0-1 failures
```

---

## View Test Results

### HTML Report

```bash
# Windows
start src\cli\build\reports\tests\test\index.html

# Linux/Mac
open src/cli/build/reports/tests/test/index.html
```

### Console Output

```bash
# Run with better console output
./gradlew :src:cli:test --console=verbose
```

---

## Run Specific Test Categories

### Foundation Tests (Create Commands)

```bash
./gradlew :src:cli:test --tests "*CreateItemCommandTest"
./gradlew :src:cli:test --tests "*CreateBlockCommandTest"
./gradlew :src:cli:test --tests "*CreateEntityCommandTest"
./gradlew :src:cli:test --tests "*CreateRecipeCommandTest"
./gradlew :src:cli:test --tests "*CreateEnchantmentCommandTest"
./gradlew :src:cli:test --tests "*CreateBiomeCommandTest"
./gradlew :src:cli:test --tests "*CreateTagCommandTest"
```

### Dev Workflow Tests

```bash
./gradlew :src:cli:test --tests "*DevCommandTest"
```

### Quality of Life Tests

```bash
./gradlew :src:cli:test --tests "*ValidateCommandE2ETest"
./gradlew :src:cli:test --tests "*ListCommandE2ETest"
./gradlew :src:cli:test --tests "*SyncCommandE2ETest"
./gradlew :src:cli:test --tests "*RenameCommandE2ETest"
./gradlew :src:cli:test --tests "*RemoveCommandE2ETest"
```

### Integration Tests

```bash
./gradlew :src:cli:test --tests "*CompleteWorkflowTest"
./gradlew :src:cli:test --tests "*FullCLIBuildTest"
./gradlew :src:cli:test --tests "*CreateCommandTest"
```

---

## Known Issues

### Issue: CreateBlockCommandTest Failure

One test may fail:

```
CreateBlockCommandTest > test ore block creation() FAILED
    org.opentest4j.AssertionFailedError at CreateBlockCommandTest.kt:81
```

**Action:** This is a known issue and should be investigated separately. The test suite can still provide valuable information even with this one failure.

---

## Troubleshooting

### Problem: "Unresolved reference: jacksonObjectMapper"

**Solution:** Make sure you added all 4 Jackson dependencies from Fix 1.

### Problem: "Unresolved reference: GsonBuilder"

**Solution:** Make sure Gson is in `implementation`, not just `testImplementation`.

### Problem: "Conflicting overloads: public fun main()"

**Solution:** Make sure you removed the main() functions from Fix 2.

### Problem: Gradle daemon issues

**Solution:** Stop all daemons and retry:
```bash
./gradlew --stop
./gradlew :src:cli:test
```

### Problem: Build cache issues

**Solution:** Clean and disable cache:
```bash
./gradlew :src:cli:clean :src:cli:test --no-build-cache
```

---

## Success Criteria

After applying all fixes, you should see:

✅ **Compilation:** `BUILD SUCCESSFUL` with 0 errors
✅ **Test Execution:** 90+ tests run successfully
✅ **Test Report:** Generated at `src/cli/build/reports/tests/test/index.html`
✅ **Coverage:** ~47% of commands tested (20/43 commands)

---

## Next Steps After Tests Pass

1. **Review test results** in HTML report
2. **Investigate any failures** (especially CreateBlockCommandTest)
3. **Run tests multiple times** to check for flaky tests
4. **Document any new issues** found during testing

---

## Quick Command Reference

```bash
# Fix, build, and test in one command
./gradlew :src:cli:clean :src:cli:test --rerun-tasks

# Run tests 3 times to check for flaky tests
./gradlew :src:cli:test --rerun-tasks
./gradlew :src:cli:clean :src:cli:test
./gradlew :src:cli:test --no-build-cache

# View test report
start src\cli\build\reports\tests\test\index.html  # Windows
open src/cli/build/reports/tests/test/index.html   # Mac/Linux
```

---

## Files Referenced

- `TEST_EXECUTION_REPORT.md` - Detailed 9,000+ word analysis
- `TEST_EXECUTION_SUMMARY.md` - Executive summary
- `QUICK_FIX_GUIDE.md` - This file

---

**Last Updated:** 2026-02-09
**Status:** Ready to apply fixes
