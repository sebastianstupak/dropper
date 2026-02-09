# Test Compilation Errors - Detailed Report

**Generated**: 2026-02-09
**Status**: 44 compilation errors in 4 test files
**Impact**: Blocking execution of 925-test suite

---

## Summary

| File | Errors | Type | Priority |
|------|--------|------|----------|
| PublishCommandAdvancedE2ETest.kt | 20 | Missing params, removed methods | CRITICAL |
| PublishPackageIntegrationTest.kt | 13 | Missing autoChangelog param | CRITICAL |
| MigrateCommandAdvancedE2ETest.kt | 11 | API signature changes | HIGH |
| PublishCommandE2ETest.kt | 0* | Likely duplicate/resolved | LOW |

*PublishCommandE2ETest.kt errors appear to be from the duplicate MockHttpClient that was already removed

---

## File 1: PublishCommandAdvancedE2ETest.kt (20 errors)

### Location
`src/cli/src/test/kotlin/dev/dropper/integration/PublishCommandAdvancedE2ETest.kt`

### Error Categories

#### 1. Missing `autoChangelog` Parameter (Line 95)
**Error**: `No value passed for parameter 'autoChangelog'`

**Location**: Line 95
```kotlin
val overriddenConfig = helper.buildPublishConfig(
    version = "2.0.0",
    changelog = "CLI changelog",
    gameVersions = "1.21.1",
    loaders = "fabric",
    releaseType = "beta",
    dryRun = false,
    configFile = baseConfig
)
```

**Fix**:
```kotlin
val overriddenConfig = helper.buildPublishConfig(
    version = "2.0.0",
    changelog = "CLI changelog",
    autoChangelog = false,  // ADD THIS LINE
    gameVersions = "1.21.1",
    loaders = "fabric",
    releaseType = "beta",
    dryRun = false,
    configFile = baseConfig
)
```

#### 2. Removed Mock Method: `responses` (4 occurrences)
**Error**: `Unresolved reference: responses`

**Locations**: Lines 511, 513, 565, 566, 567, 695, 696, 722, 723

**Current Code**:
```kotlin
mockHttpClient.responses.add(HttpResponse(...))
```

**Fix**: The `MockHttpClient` in `dev.dropper.publishers` uses `nextResponse` instead:
```kotlin
mockHttpClient.nextResponse = HttpResponse(200, """{"id": "test-123"}""")
```

#### 3. Removed Mock Method: `simulateTimeout` (1 occurrence)
**Error**: `Unresolved reference: simulateTimeout`

**Location**: Line 538

**Current Code**:
```kotlin
mockHttpClient.simulateTimeout = true
```

**Fix**: The real `MockHttpClient` doesn't have this method. Need to either:
- Extend MockHttpClient with error simulation
- Use a different approach to test timeouts
- Mock at a higher level

**Recommended Fix**:
```kotlin
// Remove timeout simulation test or create custom mock
// The real MockHttpClient can return error responses:
mockHttpClient.nextResponse = HttpResponse(0, "Network error: timeout")
```

#### 4. Removed Mock Method: `simulateNetworkError` (1 occurrence)
**Error**: `Unresolved reference: simulateNetworkError`

**Location**: Line 590

**Fix**: Same as simulateTimeout - use error response:
```kotlin
mockHttpClient.nextResponse = HttpResponse(0, "Network error: connection failed")
```

#### 5. Removed Mock Method: `simulateSSLError` (1 occurrence)
**Error**: `Unresolved reference: simulateSSLError`

**Location**: Line 875

**Fix**: Use error response:
```kotlin
mockHttpClient.nextResponse = HttpResponse(0, "SSL certificate error")
```

#### 6. Invalid Parameters: `headers` (2 occurrences)
**Error**: `Cannot find a parameter with this name: headers`

**Locations**: Lines 821, 849

**Current Code**:
```kotlin
val config = PublishConfig(
    version = "1.0.0",
    changelog = "Test",
    headers = mapOf("X-Custom" to "value")  // INVALID
)
```

**Fix**: Check `PublishConfig` constructor - likely doesn't accept `headers` parameter:
```kotlin
// Remove headers parameter or add to config if needed
val config = PublishConfig(
    version = "1.0.0",
    changelog = "Test"
)
```

### Summary of Fixes for PublishCommandAdvancedE2ETest.kt

1. Add `autoChangelog = false` to line 95
2. Replace `mockHttpClient.responses.add()` with `mockHttpClient.nextResponse =`
3. Replace `simulateTimeout = true` with error response
4. Replace `simulateNetworkError = true` with error response
5. Replace `simulateSSLError = true` with error response
6. Remove `headers` parameter from PublishConfig construction

---

## File 2: PublishPackageIntegrationTest.kt (13 errors)

### Location
`src/cli/src/test/kotlin/dev/dropper/integration/PublishPackageIntegrationTest.kt`

### Error Pattern
**Error**: `No value passed for parameter 'autoChangelog'`

**Occurrences**: 13 times (lines 92, 137, 156, 288, 306, 331, 352, 386, 430, 457, 509, 536, 552)

### Fix Pattern

All errors are the same - missing `autoChangelog` parameter in `buildPublishConfig()` calls.

**Current Code**:
```kotlin
val config = helper.buildPublishConfig(
    version = "1.0.0",
    changelog = "Test changelog",
    gameVersions = "1.21.1",
    loaders = "fabric",
    releaseType = "release",
    dryRun = false,
    configFile = configData
)
```

**Fix**:
```kotlin
val config = helper.buildPublishConfig(
    version = "1.0.0",
    changelog = "Test changelog",
    autoChangelog = false,  // ADD THIS LINE
    gameVersions = "1.21.1",
    loaders = "fabric",
    releaseType = "release",
    dryRun = false,
    configFile = configData
)
```

### Automated Fix

Can be fixed with find-and-replace:

**Find**:
```
changelog = ([^,]+),
    gameVersions
```

**Replace**:
```
changelog = $1,
    autoChangelog = false,
    gameVersions
```

---

## File 3: MigrateCommandAdvancedE2ETest.kt (11 errors)

### Location
`src/cli/src/test/kotlin/dev/dropper/integration/MigrateCommandAdvancedE2ETest.kt`

### Error Categories

#### 1. Type Mismatch in ApiChange Construction (2 errors)
**Lines**: 153, 164

**Error 1**: `Type mismatch: inferred type is Unit but Boolean was expected`
**Error 2**: `Unresolved reference: deprecated` / `Unresolved reference: required`

**Current Code**:
```kotlin
ApiChange(
    type = ChangeType.METHOD_SIGNATURE_CHANGED,
    deprecated = deprecated,  // INVALID
    // ...
)

ApiChange(
    type = ChangeType.CONSTRUCTOR_CHANGED,
    required = required,  // INVALID
    // ...
)
```

**Fix**: Check `ApiChange` data class definition and use correct field names:
```kotlin
// Likely should be:
ApiChange(
    type = ChangeType.METHOD_SIGNATURE_CHANGED,
    isDeprecated = true,  // or similar
    // ...
)
```

#### 2. Too Many Arguments in detectChanges() (2 errors)
**Lines**: 173, 176

**Error**: `Too many arguments for public final fun detectChanges(fromVersion: String, toVersion: String): List<ApiChange>`

**Current Code**:
```kotlin
val changes = detector.detectChanges("1.20.4", "1.21.1", someExtraParam)
```

**Fix**: Remove extra parameter:
```kotlin
val changes = detector.detectChanges("1.20.4", "1.21.1")
```

#### 3. Unresolved Reference: getPackFormat (2 errors)
**Lines**: 187, 188

**Error**: `Unresolved reference: getPackFormat`

**Current Code**:
```kotlin
val format1 = version1.getPackFormat()
val format2 = version2.getPackFormat()
```

**Fix**: Method likely renamed or moved:
```kotlin
// Check MinecraftVersion class for correct method name
val format1 = version1.packFormat  // Might be a property
// or
val format1 = MinecraftVersions.getPackFormat(version1)
```

#### 4. Unresolved Reference: category (1 error)
**Line**: 250

**Error**: `Cannot find a parameter with this name: category`

**Current Code**:
```kotlin
ApiChange(
    type = ChangeType.CLASS_MOVED,
    category = "rendering",  // INVALID
    // ...
)
```

**Fix**: Remove invalid parameter or use correct field name:
```kotlin
ApiChange(
    type = ChangeType.CLASS_MOVED,
    // Remove category or use correct field
)
```

#### 5. Invalid Parameter: backup (1 error)
**Line**: 442

**Error**: `Cannot find a parameter with this name: backup`

**Current Code**:
```kotlin
MigrateCommand(
    fromVersion = "1.20.4",
    toVersion = "1.21.1",
    backup = true,  // INVALID
    // ...
)
```

**Fix**: Parameter was removed from MigrateCommand:
```kotlin
MigrateCommand(
    fromVersion = "1.20.4",
    toVersion = "1.21.1"
    // Remove backup parameter
)
```

### Summary of Fixes for MigrateCommandAdvancedE2ETest.kt

1. Fix `ApiChange` constructor calls - use correct field names
2. Remove extra parameters from `detectChanges()` calls
3. Replace `getPackFormat()` with correct method/property
4. Remove `category` parameter from `ApiChange`
5. Remove `backup` parameter from `MigrateCommand`

---

## File 4: PublishCommandE2ETest.kt (0 errors - RESOLVED)

### Status
This file was listed in initial error output but appears to be resolved after removing the duplicate `MockHttpClient` definition at the bottom of `PublishCommandAdvancedE2ETest.kt`.

The errors were likely due to the conflicting mock definitions in the same package.

---

## Detailed Fix Script

### Step 1: Fix PublishCommandAdvancedE2ETest.kt

```kotlin
// 1. Line 95 - Add autoChangelog parameter
val overriddenConfig = helper.buildPublishConfig(
    version = "2.0.0",
    changelog = "CLI changelog",
    autoChangelog = false,  // ← ADD THIS
    gameVersions = "1.21.1",
    loaders = "fabric",
    releaseType = "beta",
    dryRun = false,
    configFile = baseConfig
)

// 2. Lines 511, 513, 565-567, 695-696, 722-723
// Replace: mockHttpClient.responses.add(HttpResponse(...))
// With:
mockHttpClient.nextResponse = HttpResponse(200, """{"id": "test"}""")

// 3. Line 538 - Remove or replace simulateTimeout
// Remove the test or use:
mockHttpClient.nextResponse = HttpResponse(0, "Network error: timeout")

// 4. Line 590 - Remove or replace simulateNetworkError
mockHttpClient.nextResponse = HttpResponse(0, "Network error")

// 5. Line 875 - Remove or replace simulateSSLError
mockHttpClient.nextResponse = HttpResponse(0, "SSL error")

// 6. Lines 821, 849 - Remove headers parameter
val config = PublishConfig(
    version = "1.0.0",
    changelog = "Test"
    // Remove: headers = mapOf(...)
)
```

### Step 2: Fix PublishPackageIntegrationTest.kt

```kotlin
// Add autoChangelog = false after every changelog parameter
// Lines: 92, 137, 156, 288, 306, 331, 352, 386, 430, 457, 509, 536, 552

val config = helper.buildPublishConfig(
    version = "1.0.0",
    changelog = "Test changelog",
    autoChangelog = false,  // ← ADD THIS TO ALL 13 LOCATIONS
    gameVersions = "1.21.1",
    loaders = "fabric",
    releaseType = "release",
    dryRun = false,
    configFile = configData
)
```

### Step 3: Fix MigrateCommandAdvancedE2ETest.kt

```kotlin
// 1. Lines 153, 164 - Fix ApiChange constructor
// Check ApiChange data class for correct field names

// 2. Lines 173, 176 - Remove extra parameters
val changes = detector.detectChanges("1.20.4", "1.21.1")

// 3. Lines 187, 188 - Fix getPackFormat
val format1 = version1.packFormat  // or correct method

// 4. Line 250 - Remove category parameter
ApiChange(type = ChangeType.CLASS_MOVED, ...)

// 5. Line 442 - Remove backup parameter
MigrateCommand(fromVersion = "1.20.4", toVersion = "1.21.1")
```

---

## Verification Steps

After applying fixes:

```bash
# 1. Clean build
./gradlew :src:cli:clean

# 2. Compile tests
./gradlew :src:cli:compileTestKotlin

# 3. If successful, run tests
./gradlew :src:cli:test

# 4. Check for remaining errors
./gradlew :src:cli:compileTestKotlin 2>&1 | grep "^e: file:" | wc -l
# Should output: 0
```

---

## Estimated Effort

| Task | Time | Difficulty |
|------|------|------------|
| PublishCommandAdvancedE2ETest.kt | 45 min | Medium |
| PublishPackageIntegrationTest.kt | 15 min | Easy (repetitive) |
| MigrateCommandAdvancedE2ETest.kt | 60 min | Hard (API investigation) |
| Verification & testing | 30 min | Easy |
| **TOTAL** | **2.5 hours** | Medium |

---

## Priority Order

1. **PublishPackageIntegrationTest.kt** (15 min) - Easy, quick win
2. **PublishCommandAdvancedE2ETest.kt** (45 min) - Critical publishing tests
3. **MigrateCommandAdvancedE2ETest.kt** (60 min) - More complex API fixes

---

## Root Cause Analysis

### Why These Errors Exist

1. **API Evolution**: Code evolved but tests weren't updated
   - `autoChangelog` parameter added to `buildPublishConfig()`
   - `ApiChange` data class fields changed
   - `MigrateCommand` parameters simplified

2. **Mock Cleanup**: Duplicate mocks removed
   - Local test mocks conflicted with production mocks
   - Removed methods like `responses`, `simulateTimeout`

3. **Method Refactoring**: API methods simplified
   - `detectChanges()` signature changed
   - `getPackFormat()` renamed or moved

### Prevention

- Run `./gradlew build` after API changes
- Add pre-commit hook to compile tests
- CI should fail on test compilation errors
- Document API changes in CHANGELOG

---

## Next Steps

1. **Apply fixes** using the detailed script above
2. **Verify compilation**: `./gradlew :src:cli:compileTestKotlin`
3. **Run full test suite**: `./gradlew :src:cli:test`
4. **Generate coverage**: `./gradlew :src:cli:test :src:cli:jacocoTestReport`
5. **Document results** in TEST_COVERAGE_REPORT.md

Once these 44 errors are fixed, all 925 tests should compile and run successfully!
