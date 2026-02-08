# Claude Code Instructions

This is **Dropper** - a native CLI tool for generating multi-loader Minecraft mod projects.

## Git Commit Guidelines

**IMPORTANT: Follow these rules for all commits:**

1. **Use Conventional Commits format:**
   ```
   <type>: <short description>

   [optional body]
   ```

   Types: feat, fix, docs, style, refactor, test, chore

2. **Keep commit messages SHORT:**
   - First line: 50 characters max
   - Description should be concise and clear
   - Examples:
     - ✅ `feat: add item generator`
     - ✅ `fix: template rendering bug`
     - ✅ `docs: update README`
     - ❌ `feat: add item generator with support for custom items and proper package structure`

3. **NEVER co-sign commits:**
   - ❌ Do NOT add `Co-Authored-By: Claude` or similar
   - ❌ Do NOT use `--trailer` or similar flags
   - These are the user's commits, not AI-generated

## Project Overview

Dropper is a **native CLI tool** built with Kotlin and GraalVM Native Image that generates multi-loader Minecraft mod projects.

### What Dropper Does

Dropper generates complete, production-ready Minecraft mod projects with:
- Multi-loader support (Fabric, Forge, NeoForge) from a single codebase
- Multi-version support with asset pack versioning
- Proper Java package structure
- ServiceLoader platform abstraction
- Comprehensive build system
- AGENTS.md documentation with modloader references

### Project Structure

```
dropper/
├── src/
│   ├── cli/                    # Kotlin CLI with GraalVM Native Image
│   │   ├── src/main/kotlin/    # CLI source code
│   │   │   └── dev/dropper/
│   │   │       ├── DropperCLI.kt       # Main entry
│   │   │       ├── commands/            # Clikt commands
│   │   │       ├── generator/           # Generators
│   │   │       ├── template/            # Template engine
│   │   │       ├── config/              # Config models
│   │   │       └── util/                # Utilities
│   │   ├── src/main/resources/  # Templates & build-logic
│   │   │   ├── templates/       # Mustache templates
│   │   │   └── build-logic/     # Embedded build system
│   │   ├── src/test/kotlin/     # E2E tests
│   │   └── build.gradle.kts
│   └── web/                     # Next.js documentation
├── examples/ruby-sword/         # Example generated project
├── scripts/
│   ├── install.sh               # Unix installer
│   └── install.ps1              # Windows installer
├── .github/workflows/
│   ├── ci.yml                   # CI: Build & test
│   └── release.yml              # Multi-platform releases
├── build.gradle.kts
├── settings.gradle.kts
├── .gitignore
├── README.md
├── DEVELOPMENT.md
├── DROPPER_ARCHITECTURE.md
└── LICENSE (MIT)
```

## Development Commands

```bash
# Build the CLI
./gradlew :src:cli:build

# Run the CLI
./gradlew :src:cli:run --args="init my-mod"

# Run E2E tests
./gradlew :src:cli:test

# Build native binary (requires GraalVM)
./gradlew :src:cli:nativeCompile
```

## Working with Dropper CLI

### Adding a New CLI Command

1. **Create command class** in `src/cli/src/main/kotlin/dev/dropper/commands/`:
   ```kotlin
   package dev.dropper.commands

   import com.github.ajalt.clikt.core.CliktCommand

   class MyCommand : CliktCommand(
       name = "my-command",
       help = "Description of command"
   ) {
       override fun run() {
           // Implementation
       }
   }
   ```

2. **Register in DropperCLI.kt**:
   ```kotlin
   .subcommands(
       InitCommand(),
       GenerateCommand(),
       MyCommand()  // Add here
   )
   ```

3. **Write E2E tests** in `src/cli/src/test/kotlin/dev/dropper/integration/`

4. **Commit**:
   ```bash
   git add .
   git commit -m "feat: add my-command"
   ```

### Adding a New Generator

1. **Create generator** in `src/cli/src/main/kotlin/dev/dropper/generator/`:
   ```kotlin
   package dev.dropper.generator

   import dev.dropper.util.FileUtil
   import dev.dropper.util.Logger
   import java.io.File

   class BlockGenerator {
       fun generate(projectDir: File, blockName: String, packageName: String, modId: String) {
           Logger.info("Generating block: $blockName")

           // Generate block class
           generateBlockClass(projectDir, blockName, packageName)

           // Generate blockstate JSON
           generateBlockstate(projectDir, blockName, modId)

           // Generate models
           generateBlockModel(projectDir, blockName, modId)

           Logger.success("Block '$blockName' generated successfully")
       }
   }
   ```

2. **Add to GenerateCommand** or create new command

3. **Create templates** in `src/cli/src/main/resources/templates/block/`

4. **Write comprehensive E2E tests**

5. **Commit**:
   ```bash
   git add .
   git commit -m "feat: add block generator"
   ```

### Adding New Templates

1. **Create Mustache template** in `src/cli/src/main/resources/templates/`:
   ```
   templates/
   ├── project/
   │   ├── config.yml.mustache
   │   ├── build.gradle.kts.mustache
   │   └── README.md.mustache
   ├── item/
   │   ├── Item.java.mustache
   │   └── item_model.json.mustache
   └── block/
       ├── Block.java.mustache
       └── blockstate.json.mustache
   ```

2. **Use Mustache variables**:
   ```mustache
   {{modId}}
   {{modName}}
   {{modVersion}}
   {{packageName}}
   {{packagePath}}
   {{author}}
   {{license}}
   ```

3. **Render in generator**:
   ```kotlin
   val context = TemplateContext.create()
       .put("modId", modId)
       .put("blockName", blockName)
       .build()

   val content = templateEngine.render("block/Block.java.mustache", context)
   FileUtil.writeText(targetFile, content)
   ```

### Writing E2E Tests

All features **must** have E2E tests in `src/cli/src/test/kotlin/dev/dropper/integration/`:

```kotlin
@Test
fun `test block generation creates all files`() {
    val projectDir = File(testDir, "block-test")

    // Generate project
    val config = ModConfig(...)
    ProjectGenerator().generate(projectDir, config)

    // Generate block
    BlockGenerator().generate(projectDir, "stone_block", "com.testmod", "testmod")

    // Verify files exist
    assertTrue(
        File(projectDir, "shared/common/src/main/java/com/testmod/blocks/StoneBlock.java").exists(),
        "Block class should exist"
    )
    assertTrue(
        File(projectDir, "versions/shared/v1/assets/testmod/blockstates/stone_block.json").exists(),
        "Blockstate should exist"
    )

    // Verify content
    val blockContent = File(...).readText()
    assertTrue(blockContent.contains("class StoneBlock"), "Should have correct class name")
}
```

## Common Tasks for Claude

### "Add a new CLI command"
1. Create command class extending `CliktCommand`
2. Implement `run()` method with logic
3. Register in `DropperCLI.kt`
4. Write E2E tests
5. Update README/docs
6. Commit: `git commit -m "feat: add <command>"`

### "Add a new generator"
1. Create generator class in `generator/`
2. Implement generation logic with template rendering
3. Create necessary Mustache templates in `resources/templates/`
4. Add to `GenerateCommand` or create new command
5. Write comprehensive E2E tests
6. Commit: `git commit -m "feat: add <type> generator"`

### "Fix a bug"
1. Identify which component is affected (generator/command/template)
2. Fix the code or template
3. Add regression test to E2E suite
4. Verify test passes: `./gradlew :src:cli:test`
5. Commit: `git commit -m "fix: <description>"`

### "Update templates"
1. Edit Mustache templates in `src/cli/src/main/resources/templates/`
2. Test template rendering in E2E tests
3. Generate test project and verify it builds
4. Commit: `git commit -m "fix: update <template>"`

## Important Files

**CLI Core:**
- `src/cli/src/main/kotlin/dev/dropper/DropperCLI.kt` - Main entry point
- `src/cli/src/main/kotlin/dev/dropper/commands/` - All CLI commands
- `src/cli/src/main/kotlin/dev/dropper/generator/` - All generators
- `src/cli/src/main/kotlin/dev/dropper/template/` - Template engine
- `src/cli/src/main/resources/templates/` - Mustache templates
- `src/cli/src/test/kotlin/dev/dropper/integration/` - E2E tests
- `src/cli/build.gradle.kts` - CLI build config with GraalVM

**CI/CD:**
- `.github/workflows/ci.yml` - Tests on Ubuntu, macOS, Windows
- `.github/workflows/release.yml` - Multi-platform native builds
- `scripts/install.sh` - Unix/Linux/macOS installer
- `scripts/install.ps1` - Windows PowerShell installer

**Documentation:**
- `README.md` - Project README
- `DEVELOPMENT.md` - Development guide
- `DROPPER_ARCHITECTURE.md` - Architecture details
- `CLAUDE.md` - This file

## Testing Philosophy

**Every feature needs E2E tests:**
- Test complete workflows (init → generate → verify)
- Verify generated files exist with correct content
- Test project structure validation
- Verify generated projects would build successfully
- Run tests before every commit: `./gradlew :src:cli:test`
- Never ship untested code

## Release Process

**Semantic Versioning:**
- Major (v2.0.0): Breaking changes to CLI or generated projects
- Minor (v1.1.0): New features (commands, generators)
- Patch (v1.0.1): Bug fixes

**Creating a Release:**
```bash
# 1. Update version in build.gradle.kts
# 2. Commit and tag
git add .
git commit -m "chore: bump version to v1.1.0"
git tag v1.1.0
git push origin main
git push origin v1.1.0

# 3. GitHub Actions automatically:
#    - Builds native binaries for Linux, macOS (x64/ARM64), Windows
#    - Creates draft release
#    - Tests installation on all platforms
#    - Publishes release
```

## Philosophy

Dropper optimizes for:
- **Developer Experience** - Simple, intuitive CLI
- **Zero Configuration** - Convention over configuration
- **Fast Startup** - Native binary, instant execution (<50ms)
- **Multi-Platform** - Works on Linux, macOS, Windows
- **Comprehensive Testing** - E2E tests for everything
- **Automated CI/CD** - GitHub Actions for all builds

**The goal**: Make multi-loader Minecraft mod development effortless.

## When in Doubt

1. Check `DROPPER_ARCHITECTURE.md` for architecture details
2. Check `DEVELOPMENT.md` for workflows
3. Look at existing generators/commands for patterns
4. **Always run E2E tests**: `./gradlew :src:cli:test`
5. Test actual project generation before committing
6. Follow conventional commits format
7. Keep commit messages short and clear
8. Never add co-author attribution
