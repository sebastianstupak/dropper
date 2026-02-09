# Edge Case Manual Testing Checklist

This document provides a manual testing checklist for edge cases that need to be tested interactively.

## Test Project Location
`/d/dev/minecraft-mod-versioning-example/examples/simple-mod`

## Test Commands

### 1. Invalid Inputs

#### Test 1.1: Spaces in name
```bash
./gradlew :src:cli:run --args="create item test item"
```
**Expected:** Should fail with error message about invalid name format
**Actual:** 

#### Test 1.2: Uppercase letters
```bash
./gradlew :src:cli:run --args="create item TestItem"
```
**Expected:** Should fail (lowercase only)
**Actual:**

#### Test 1.3: Special characters
```bash
./gradlew :src:cli:run --args="create item test@item#123"
```
**Expected:** Should fail
**Actual:**

#### Test 1.4: Very long name (65+ chars)
```bash
./gradlew :src:cli:run --args="create item aaaaaaaaaabbbbbbbbbbccccccccccddddddddddeeeeeeeeeeffffffffff12345"
```
**Expected:** Should fail with length validation error
**Actual:**

#### Test 1.5: Empty string
```bash
./gradlew :src:cli:run --args="create item"
```
**Expected:** Should fail with missing argument error
**Actual:**

#### Test 1.6: Reserved keyword (class)
```bash
./gradlew :src:cli:run --args="create item class"
```
**Expected:** Should fail
**Actual:**

#### Test 1.7: Reserved namespace (minecraft)
```bash
./gradlew :src:cli:run --args="create item minecraft"
```
**Expected:** Should fail
**Actual:**

#### Test 1.8: Numbers only
```bash
./gradlew :src:cli:run --args="create item 12345"
```
**Expected:** Should fail
**Actual:**

### 2. Duplicate Names

#### Test 2.1: Create duplicate item
```bash
# First create
./gradlew :src:cli:run --args="create item duplicate_test"
# Try to create again
./gradlew :src:cli:run --args="create item duplicate_test"
```
**Expected:** Second command should fail
**Actual:**

#### Test 2.2: Block with same name as item
```bash
./gradlew :src:cli:run --args="create item conflict_name"
./gradlew :src:cli:run --args="create block conflict_name"
```
**Expected:** Should fail or warn
**Actual:**

### 3. Special Characters

#### Test 3.1: Hyphens
```bash
./gradlew :src:cli:run --args="create item test-item-name"
```
**Expected:** Should fail (hyphens not allowed)
**Actual:**

#### Test 3.2: Underscores (valid)
```bash
./gradlew :src:cli:run --args="create item valid_test_item"
```
**Expected:** Should succeed
**Actual:**

#### Test 3.3: Multiple underscores
```bash
./gradlew :src:cli:run --args="create item test___item"
```
**Expected:** Should succeed (or warn)
**Actual:**

#### Test 3.4: Starting with underscore
```bash
./gradlew :src:cli:run --args="create item _test_item"
```
**Expected:** Should fail
**Actual:**

#### Test 3.5: Ending with underscore
```bash
./gradlew :src:cli:run --args="create item test_item_"
```
**Expected:** Should fail
**Actual:**

#### Test 3.6: Dots in name
```bash
./gradlew :src:cli:run --args="create item test.item.name"
```
**Expected:** Should fail
**Actual:**

### 4. Missing Dependencies

#### Test 4.1: Run in empty directory
```bash
mkdir /d/dev/test-empty
cd /d/dev/test-empty
/d/dev/minecraft-mod-versioning-example/gradlew :src:cli:run --args="create item test_item"
```
**Expected:** Should fail with clear error
**Actual:**

#### Test 4.2: Missing config.yml
```bash
cd /d/dev/minecraft-mod-versioning-example/examples/simple-mod
mv config.yml config.yml.backup
./gradlew :src:cli:run --args="create item test_item"
mv config.yml.backup config.yml
```
**Expected:** Should fail with "config.yml not found"
**Actual:**

#### Test 4.3: Missing buildSrc
```bash
cd /d/dev/minecraft-mod-versioning-example/examples/simple-mod
mv buildSrc buildSrc.backup
./gradlew :src:cli:run --args="create item test_item"
mv buildSrc.backup buildSrc
```
**Expected:** Should fail with build error
**Actual:**

### 5. Version Edge Cases

#### Test 5.1: Add existing version
```bash
./gradlew :src:cli:run --args="add-version 1_21_4 fabric,forge,neoforge"
```
**Expected:** Should fail (version already exists)
**Actual:**

#### Test 5.2: Invalid version format (dots instead of underscores)
```bash
./gradlew :src:cli:run --args="add-version 1.22.0 fabric"
```
**Expected:** Should fail or auto-convert
**Actual:**

#### Test 5.3: Version with letters
```bash
./gradlew :src:cli:run --args="add-version abc_def fabric"
```
**Expected:** Should fail
**Actual:**

#### Test 5.4: Unsupported loader
```bash
./gradlew :src:cli:run --args="add-version 1_22_0 bukkit"
```
**Expected:** Should fail with "unsupported loader" error
**Actual:**

#### Test 5.5: Empty loaders list
```bash
./gradlew :src:cli:run --args="add-version 1_22_0"
```
**Expected:** Should fail or prompt for loaders
**Actual:**

### 6. Block-Specific Tests

#### Test 6.1: Invalid block type
```bash
./gradlew :src:cli:run --args="create block test_block --type invalid_type"
```
**Expected:** Should fail or list valid types
**Actual:**

#### Test 6.2: Block with very long name
```bash
./gradlew :src:cli:run --args="create block $(printf 'a%.0s' {1..70})"
```
**Expected:** Should fail
**Actual:**

### 7. Unicode and Encoding

#### Test 7.1: Unicode characters
```bash
./gradlew :src:cli:run --args="create item test_ñ_item"
```
**Expected:** Should fail
**Actual:**

#### Test 7.2: Emoji
```bash
./gradlew :src:cli:run --args="create item test_😀_item"
```
**Expected:** Should fail
**Actual:**

### 8. Asset Pack Edge Cases

#### Test 8.1: Add asset pack that already exists
```bash
./gradlew :src:cli:run --args="add-asset-pack v1"
```
**Expected:** Should fail
**Actual:**

#### Test 8.2: Invalid asset pack name
```bash
./gradlew :src:cli:run --args="add-asset-pack 123"
```
**Expected:** Should fail or warn
**Actual:**

### 9. File System Edge Cases

#### Test 9.1: Read-only directory
```bash
# Make directory read-only
chmod -w /d/dev/minecraft-mod-versioning-example/examples/simple-mod/shared
./gradlew :src:cli:run --args="create item readonly_test"
# Restore permissions
chmod +w /d/dev/minecraft-mod-versioning-example/examples/simple-mod/shared
```
**Expected:** Should fail with permission error
**Actual:**

#### Test 9.2: Path traversal attempt
```bash
./gradlew :src:cli:run --args="create item ../../malicious_item"
```
**Expected:** Should fail or sanitize
**Actual:**

### 10. Concurrent Operations

#### Test 10.1: Run two create commands simultaneously
```bash
./gradlew :src:cli:run --args="create item concurrent1" &
./gradlew :src:cli:run --args="create item concurrent2" &
wait
```
**Expected:** Both should succeed or proper locking
**Actual:**

## Summary Template

After testing, fill in this summary:

### Critical Issues Found
1. 
2. 
3. 

### Missing Validations
1. 
2. 
3. 

### Unclear Error Messages
1. 
2. 
3. 

### Crashes/Exceptions
1. 
2. 
3. 

### Recommendations
1. 
2. 
3. 
