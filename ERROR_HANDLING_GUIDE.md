# Dropper CLI Error Handling & Robustness Guide

This document describes the comprehensive error handling and robustness improvements implemented across the Dropper CLI.

## Overview

The Dropper CLI now includes production-grade error handling, validation, and resource management across all commands. The improvements ensure reliable operation, helpful error messages, and graceful failure handling.

## New Utility Components

### 1. ErrorHandler (`util/ErrorHandler.kt`)

Centralized error handling for all CLI operations.

**Features:**
- Structured error handling by category (file, network, process)
- Context-aware error messages with helpful hints
- Platform-specific suggestions (Windows vs Unix/Linux)
- Safe execution wrappers for all operation types

**Usage:**
```kotlin
// File operations
ErrorHandler.safeFileOperation("write file", filePath) {
    file.writeText(content)
}

// Network operations
ErrorHandler.safeNetworkOperation("download asset") {
    httpClient.get(url)
}

// Process operations
ErrorHandler.safeProcessExecution("gradle build") {
    ProcessBuilder(...).start().waitFor()
}
```

**Error Categories:**
- **File Operations**: NoSuchFileException, AccessDeniedException, IOException
- **Network Operations**: SocketTimeoutException, UnknownHostException, SSLException
- **Process Operations**: IOException (start failures), exit code handling

### 2. Validators (`util/Validators.kt`)

Comprehensive validation framework for all input types.

**Validators Available:**

#### Name Validation
- `validateModId(modId: String)` - Mod ID validation (lowercase, hyphens allowed)
- `validateComponentName(name: String, type: String)` - Item/block/entity names
- `validatePackageName(packageName: String)` - Java package names
- `validateVersion(version: String)` - Semantic versioning
- `validateMinecraftVersion(version: String)` - Minecraft version format
- `validateLoader(loader: String)` - Modloader names (fabric/forge/neoforge)

#### File Path Validation
- `validatePathExists(path: File, type: String)` - Check file/directory exists
- `validateIsDirectory(path: File)` - Verify path is a directory
- `validateWritable(path: File)` - Check write permissions
- `validateSafePath(basePath: File, targetPath: File)` - Prevent directory traversal
- `validatePathLength(path: Path)` - Windows MAX_PATH (260 chars)
- `validateFileName(name: String)` - Invalid character checks

#### Resource Validation
- `validateDiskSpace(path: File, requiredBytes: Long)` - Check available space
- `validateDropperProject(directory: File)` - Verify Dropper project structure

**Usage:**
```kotlin
val result = Validators.validateComponentName("ruby_sword", "Item name")
if (!result.isValid) {
    Validators.exitWithError(result)
}
```

### 3. Enhanced FileUtil (`util/FileUtil.kt`)

Safe file operations with automatic validation and error handling.

**Features:**
- Atomic writes (temp file + move)
- Automatic directory creation
- Path safety checks
- Resource cleanup
- Disk space validation
- Human-readable file sizes

**Safe Operations:**
```kotlin
// All operations include validation
FileUtil.writeText(file, content)      // Validates path, permissions, disk space
FileUtil.readText(file)                 // Validates existence, readability
FileUtil.copyDirectory(source, target) // Validates permissions, space
FileUtil.deleteDirectory(dir)          // Safety checks for system directories
```

### 4. FileTransaction (`util/FileTransaction.kt`)

Atomic multi-file operations with rollback support.

**Features:**
- Transaction-based file operations
- Automatic backup creation
- Rollback on failure
- Resource cleanup

**Usage:**
```kotlin
FileTransaction.atomic {
    writeFile(file1, content1)
    writeFile(file2, content2)
    deleteFile(file3)
    copyFile(source, dest)
}
// Auto-commits on success, rolls back on failure
```

### 5. Enhanced Logger (`util/Logger.kt`)

Structured logging with progress indicators.

**New Features:**
- Debug mode (verbose logging)
- Progress indicators/spinners
- Section headers and separators
- Progress with counts
- Automatic cleanup of progress indicators

**Usage:**
```kotlin
Logger.setVerbose(true)           // Enable debug mode
Logger.debug("Debug information")  // Only shown in verbose mode
Logger.progress("Processing...")   // Show spinner
Logger.progressComplete(success)   // Complete spinner
Logger.withProgress("Task") { }    // Auto-cleanup wrapper
Logger.progressCount(1, 10, "Processing file")
```

## Enhanced Components

### GradleRunner (`commands/util/GradleRunner.kt`)

**Improvements:**
- Gradle wrapper validation (existence + permissions)
- 30-minute timeout for builds
- Graceful process shutdown
- Background output streaming
- Shutdown hook for cleanup
- Unix execute permission checks
- Task existence checking

**Error Messages:**
- Missing wrapper: "Run 'gradle wrapper' to generate it"
- Permission issues: "Run 'chmod +x gradlew'" (Unix)
- Timeout: Shows duration and suggests checking build

### ProcessManager (`commands/util/ProcessManager.kt`)

**Existing Features (Already Good):**
- Shutdown hook registration
- Graceful + forceful termination
- Process lifecycle tracking
- Background process management

## Error Handling Patterns

### Pattern 1: Input Validation First

```kotlin
override fun run() {
    // 1. Validate all inputs
    val nameCheck = Validators.validateComponentName(name, "Item name")
    if (!nameCheck.isValid) {
        Validators.exitWithError(nameCheck)
    }

    // 2. Validate project state
    val projectCheck = Validators.validateDropperProject(projectDir)
    if (!projectCheck.isValid) {
        Validators.exitWithError(projectCheck)
    }

    // 3. Proceed with operation
    // ...
}
```

### Pattern 2: Safe File Operations

```kotlin
// Use FileUtil for all file operations
FileUtil.writeText(file, content)  // Automatic validation
FileUtil.readText(file)            // Error handling included

// Use FileTransaction for multi-file operations
FileTransaction.atomic {
    writeFile(file1, content1)
    writeFile(file2, content2)
}
```

### Pattern 3: Process Execution

```kotlin
// GradleRunner handles all Gradle operations
val runner = GradleRunner(projectDir)
val exitCode = runner.executeTask(version, loader, "build")
if (exitCode != 0) {
    Logger.error("Build failed")
    return
}
```

### Pattern 4: Progress Reporting

```kotlin
Logger.withProgress("Generating files") {
    // Long operation
    generateFiles()
}

// Or for counted operations
files.forEachIndexed { index, file ->
    Logger.progressCount(index + 1, files.size, "Processing ${file.name}")
    processFile(file)
}
```

## Error Message Guidelines

### 1. Structure

Every error message should have:
1. **What went wrong** - Clear description
2. **Why it happened** - Context (optional)
3. **How to fix it** - Actionable suggestion

Example:
```
✗ File not found: config.yml
Suggestion: Run 'dropper init' or cd into a project directory
```

### 2. Platform-Specific Hints

Provide OS-specific guidance:

```kotlin
if (System.getProperty("os.name").lowercase().contains("windows")) {
    Logger.info("On Windows, try running as Administrator")
} else {
    Logger.info("On Unix/Linux, check permissions with 'ls -l'")
}
```

### 3. Progressive Disclosure

- **Error**: Brief, scannable message
- **Hint**: Suggestion for most common cause
- **Debug**: Detailed info (only in verbose mode)

```kotlin
Logger.error("Build failed")
Logger.info("Hint: Check that all dependencies are available")
Logger.debug("Exit code: $exitCode\nOutput: $output")
```

## Validation Checklist

Before running any operation, validate:

### Input Validation
- [ ] Names (mod ID, component names)
- [ ] Versions (semantic, Minecraft)
- [ ] Paths (length, characters, safety)
- [ ] Enum values (loaders, types)

### File System Validation
- [ ] Paths exist (when reading)
- [ ] Parent directories exist/writable (when writing)
- [ ] Disk space available
- [ ] No directory traversal attempts
- [ ] File name validity

### Process Validation
- [ ] Executable exists
- [ ] Execute permissions (Unix)
- [ ] Timeout configured
- [ ] Shutdown hooks registered

### Network Validation
- [ ] URLs valid
- [ ] Timeout configured
- [ ] Retry logic for transient failures
- [ ] Certificate validation

## Resource Management

### File Handles

Always use `.use` for automatic cleanup:

```kotlin
file.bufferedReader().use { reader ->
    // Use reader
} // Automatically closed
```

### Processes

Register shutdown hooks for cleanup:

```kotlin
Runtime.getRuntime().addShutdownHook(Thread {
    process?.destroy()
})
```

### Transactions

Use `FileTransaction` for multi-file operations:

```kotlin
FileTransaction().use { tx ->
    tx.writeFile(file1, content1)
    tx.commit()
} // Auto-rollback if not committed
```

## Testing Error Handling

### Unit Tests

Test all validation paths:

```kotlin
@Test
fun `test invalid mod ID shows helpful error`() {
    val result = Validators.validateModId("Invalid-Name!")
    assertFalse(result.isValid)
    assertNotNull(result.suggestion)
}
```

### Integration Tests

Test complete error flows:

```kotlin
@Test
fun `test command fails gracefully with missing files`() {
    // Remove required file
    configFile.delete()

    // Run command
    val exitCode = runCommand("create", "item", "test")

    // Verify helpful error
    assertEquals(1, exitCode)
    assertTrue(output.contains("config.yml not found"))
    assertTrue(output.contains("Suggestion"))
}
```

## Migration Guide

### Updating Existing Commands

1. **Add input validation** at the start of `run()`:
```kotlin
override fun run() {
    // Add validation
    val nameCheck = Validators.validateComponentName(name, "Item name")
    if (!nameCheck.isValid) {
        Validators.exitWithError(nameCheck)
    }

    // Existing code...
}
```

2. **Replace file operations** with `FileUtil`:
```kotlin
// Before
file.writeText(content)

// After
FileUtil.writeText(file, content)
```

3. **Add progress indicators** for long operations:
```kotlin
Logger.withProgress("Generating item") {
    generateItem()
}
```

4. **Use transactions** for multi-file operations:
```kotlin
FileTransaction.atomic {
    writeFile(file1, content1)
    writeFile(file2, content2)
}
```

## Best Practices

### DO:
- ✅ Validate all inputs before operations
- ✅ Use `FileUtil` for file operations
- ✅ Use `FileTransaction` for atomic multi-file changes
- ✅ Provide helpful error messages with suggestions
- ✅ Add progress indicators for long operations
- ✅ Register shutdown hooks for cleanup
- ✅ Use debug logging for troubleshooting
- ✅ Handle platform differences (Windows vs Unix)

### DON'T:
- ❌ Use raw `File.writeText()` directly
- ❌ Ignore validation errors
- ❌ Show stack traces to users (use debug mode)
- ❌ Use generic error messages
- ❌ Forget to clean up resources
- ❌ Hard-code platform-specific behavior
- ❌ Leave processes running on exit

## Future Improvements

- [ ] Retry logic for network operations
- [ ] Exponential backoff for transient failures
- [ ] Detailed logging to file (with rotation)
- [ ] Metrics collection (operation timing, success rates)
- [ ] Telemetry for error patterns (opt-in)
- [ ] Configuration validation on init
- [ ] Health checks for dependencies (Java, Gradle)
- [ ] Automatic issue reporting template generation

## References

- `ErrorHandler.kt` - Centralized error handling
- `Validators.kt` - Input validation framework
- `FileUtil.kt` - Safe file operations
- `FileTransaction.kt` - Atomic multi-file operations
- `Logger.kt` - Enhanced logging
- `GradleRunner.kt` - Gradle execution with error handling
- `ProcessManager.kt` - Process lifecycle management

---

**Remember:** Good error handling is as important as features. Users should never be left wondering what went wrong or how to fix it.
