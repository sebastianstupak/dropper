# Test Migration Guide

## Problem

Currently, ~34 integration and E2E tests modify `System.setProperty("user.dir", ...)` to change the working directory. This causes JVM crashes on Windows due to file locking and process isolation issues in the Gradle test executor.

## Current State

- ✅ **Unit tests**: Work on all platforms (43+ tests)
- ✅ **Linux/macOS**: All tests work (including integration/e2e)
- ⚠️  **Windows**: Integration/e2e tests excluded to prevent crashes

## Solution Approaches

### Approach 1: TestProjectContext (Partial Solution)

We created `TestProjectContext` to encapsulate project directory management:

```kotlin
class MyTest {
    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("my-test")
        context.createDefaultProject()
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }

    @Test
    fun myTest() {
        context.withProjectDir {
            // Code that uses user.dir
            MyCommand().parse(args)
        }

        // Verify using context.file()
        assertTrue(context.file("some/path").exists())
    }
}
```

**Status**: ✅ Implemented, ⚠️  Still causes crashes on Windows because it temporarily modifies user.dir

**Example**: `AddVersionCommandTest` has been migrated to this pattern

### Approach 2: Command Refactoring (Best Long-term Solution)

Refactor commands to accept an optional `projectDir` parameter instead of always using `user.dir`:

```kotlin
// Current (problematic)
class MyCommand : CliktCommand() {
    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        // ...
    }
}

// Proposed (Windows-safe)
open class DropperCommand : CliktCommand() {
    var projectDir: File = File(System.getProperty("user.dir"))
}

class MyCommand : DropperCommand() {
    override fun run() {
        // Use this.projectDir instead
        // ...
    }
}

// In tests
@Test
fun myTest() {
    val testDir = createTestProject()
    val command = MyCommand()
    command.projectDir = testDir
    command.parse(args)
}
```

**Status**: ⏳ Not started - requires refactoring 20+ command classes

**Benefits**:
- Works on all platforms
- No user.dir modification
- Clean separation of concerns
- Testable design

### Approach 3: Subprocess Execution (Complex)

Run tests in separate processes:

```kotlin
@Test
fun myTest() {
    val result = CommandExecutor.executeGradle(
        workingDir = testProjectDir,
        "dropper", "item", "my_item"
    )
    assertTrue(result.success)
}
```

**Status**: ✅ Implemented (`CommandExecutor`), ⚠️  Slow and complex for unit testing

**Drawbacks**:
- Slow (new JVM for each command)
- Complex setup
- Hard to debug
- Loses in-process benefits

## Recommended Strategy

### Phase 1: Current State (✅ Done)
- Unit tests work on all platforms
- Integration tests work on Linux/macOS
- Windows excludes problematic tests
- CI runs full test suite on Linux

### Phase 2: Progressive Migration (📋 TODO)
1. Create `DropperCommand` base class with configurable `projectDir`
2. Migrate one command at a time
3. Update corresponding tests
4. Remove Windows exclusions as tests are fixed
5. Track progress: `X/34` tests Windows-compatible

### Phase 3: Complete (🎯 Goal)
- All tests work on all platforms
- No user.dir modification
- Clean, testable design

## Current Test Status

### ✅ Windows-Compatible (Working)
- `ValidationUtilTest` (22 tests)
- `PackageNameSanitizationTest` (11 tests)
- `JarValidationUtilsTest` (10 tests)
- **Total**: 43 tests

### ⏳ Linux/macOS Only (To Be Migrated)

**Integration Tests** (~25 tests):
- AddVersionCommandTest (migrated to TestProjectContext, still excluded on Windows)
- AssetPackCommandTest
- BuildCommandTest
- CleanCommandE2ETest
- CompleteWorkflowTest
- CreateCommandTest
- DevCommandTest
- E2ETest
- ExportCommandE2ETest
- FullCLIBuildTest
- FullWorkflowTest
- ImportCommandE2ETest
- ListCommandE2ETest
- ListCommandBasicTest
- MigrateCommandE2ETest
- MigrateCommandAdvancedE2ETest
- PackageCommandE2ETest
- PackageCommandAdvancedE2ETest
- RemoveCommandE2ETest
- RenameCommandE2ETest
- SearchCommandE2ETest
- SyncCommandE2ETest
- TemplateCommandE2ETest
- UpdateCommandE2ETest
- ValidateCommandE2ETest
- CLIWorkflowTest

**Command Tests** (~6 tests):
- CreateBiomeCommandTest
- CreateBlockCommandTest
- CreateEnchantmentCommandTest
- CreateEntityCommandTest
- CreateRecipeCommandTest
- CreateTagCommandTest

**E2E Tests** (~8 tests):
- AssetPackE2ETest
- ComplexModpackE2ETest
- DevCommandE2ETest
- FullCLIBuildTest
- MinecraftVersionsE2ETest
- PackageNameGenerationE2ETest
- SimpleModVersionsTest
- TemplateValidationE2ETest

**Total to migrate**: ~39 tests

## How to Migrate a Test

### Step 1: Use TestProjectContext (Quick, Partial Fix)

```kotlin
// Before
class MyTest {
    private lateinit var testProjectDir: File
    private val originalUserDir = System.getProperty("user.dir")

    @BeforeEach
    fun setup() {
        testProjectDir = File("build/test-my-test/${System.currentTimeMillis()}")
        testProjectDir.mkdirs()
        ProjectGenerator().generate(testProjectDir, config)
        System.setProperty("user.dir", testProjectDir.absolutePath)
    }

    @AfterEach
    fun cleanup() {
        System.setProperty("user.dir", originalUserDir)
        testProjectDir.deleteRecursively()
    }
}

// After
class MyTest {
    private lateinit var context: TestProjectContext

    @BeforeEach
    fun setup() {
        context = TestProjectContext.create("my-test")
        context.createProject(config)
    }

    @AfterEach
    fun cleanup() {
        context.cleanup()
    }
}
```

**Note**: This is still not Windows-safe, but it's cleaner code.

### Step 2: Refactor Command (Full Fix)

Wait for Phase 2 when `DropperCommand` base class is ready.

## CI Strategy

**Linux CI** (ubuntu-latest):
- Run ALL tests including integration/e2e
- This is our source of truth

**Windows CI** (windows-latest):
- Run unit tests only
- Skip integration/e2e tests (for now)
- Document this limitation

**macOS CI** (macos-13):
- Run ALL tests like Linux
- Verify cross-platform compatibility

## Tracking Progress

To see which tests still need migration:

```bash
# Count tests that modify user.dir
find src/cli/src/test -name "*Test.kt" -exec grep -l "System.setProperty.*user.dir" {} \; | wc -l

# Count tests using TestProjectContext
find src/cli/src/test -name "*Test.kt" -exec grep -l "TestProjectContext" {} \; | wc -l
```

Current: **1/39** tests migrated to TestProjectContext (awaiting command refactoring)

## Future Work

1. **Create DropperCommand base class** with configurable projectDir
2. **Migrate commands one-by-one** to use DropperCommand
3. **Update tests** as commands are migrated
4. **Remove Windows exclusions** progressively
5. **Full cross-platform test suite** 🎯

## Questions?

- Why not just run tests in Docker? → Complex setup, slow, not native
- Why not mock File operations? → Too invasive, loses real integration testing
- Why not use @TempDir? → Still requires command refactoring
- Can we use Testcontainers? → Overkill for this problem

The best solution is **Approach 2: Command Refactoring**. It's just a matter of time investment.
