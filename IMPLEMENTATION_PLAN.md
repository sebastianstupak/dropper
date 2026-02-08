# Dropper Implementation Plan

## Parallel Implementation Strategy

We'll use **6 specialized agents** working in parallel to implement Dropper CLI.

---

## Agent 1: Project Structure & Build Setup

**Responsibility**: Foundation, build system, GraalVM configuration

### Tasks

1. **Create new project structure**
   ```
   dropper/
   ├── src/
   │   ├── cli/          # CLI tool (Kotlin + GraalVM)
   │   └── web/          # Nextra docs (Next.js)
   ├── examples/
   ├── scripts/
   └── .github/workflows/
   ```

2. **Set up build.gradle.kts**
   - Configure Kotlin JVM
   - Add GraalVM Native Image plugin
   - Configure dependencies (Clikt, Kaml, OkHttp, Kotlinx-serialization)
   - Set up shadow JAR (fallback)

3. **Configure GraalVM Native Image**
   - Create `native-image.properties`
   - Configure reflection metadata
   - Configure resource inclusion
   - Set up build args for optimization

4. **Create build scripts**
   - `scripts/build-native.sh` - Build native binary
   - `scripts/test-native.sh` - Test binary
   - Platform detection logic

5. **Set up testing framework**
   - JUnit 5 configuration
   - Kotest assertions
   - Integration test structure

### Deliverables

- ✅ Working Gradle build
- ✅ Native image compiles successfully
- ✅ Basic "Hello Dropper" CLI works as native binary
- ✅ Tests run and pass

---

## Agent 2: Template Engine & Resource Embedding

**Responsibility**: Template system, resource loading, GraalVM compatibility

### Tasks

1. **Implement TemplateLoader**
   - Load templates from classpath resources
   - GraalVM-compatible resource access
   - Directory extraction from JAR/native
   - Caching for performance

2. **Implement TemplateEngine**
   - Mustache template rendering
   - Context variable substitution
   - Template validation
   - Error handling with clear messages

3. **Create template system**
   ```
   resources/templates/
   ├── project/
   │   ├── config.yml.mustache
   │   ├── settings.gradle.kts.mustache
   │   ├── build.gradle.kts.mustache
   │   └── README.md.mustache
   ├── item/
   │   ├── Item.java.mustache
   │   ├── ItemTest.java.mustache
   │   └── item_model.json.mustache
   └── shared/
       ├── ExampleMod.java.mustache
       └── Services.java.mustache
   ```

4. **Embed build-logic/**
   - Copy current build-logic/ to resources/
   - Template-ize configurable parts
   - Ensure GraalVM can extract it

5. **Write resource tests**
   - Test template loading
   - Test rendering
   - Test directory extraction

### Deliverables

- ✅ TemplateEngine working with Mustache
- ✅ All templates embedded and loadable
- ✅ build-logic/ extractable from native binary
- ✅ Tests for template system

---

## Agent 3: CLI Commands Implementation

**Responsibility**: All Clikt commands, user interaction

### Tasks

1. **Implement DropperCLI (main)**
   ```kotlin
   class DropperCLI : CliktCommand()
   fun main(args: Array<String>)
   ```

2. **Implement InitCommand**
   - Interactive prompts (mod name, ID, author, etc.)
   - Validation of inputs
   - Call ProjectGenerator
   - Success/error messages

3. **Implement AddVersionCommand**
   - Select new MC version
   - Auto-detect loader versions
   - Update version configs
   - Create version directory

4. **Implement AddLoaderCommand**
   - Add loader to existing project
   - Generate platform helper
   - Update configs

5. **Implement GenerateCommand**
   - Subcommands: item, block, entity
   - Interactive prompts for each
   - Call appropriate generator
   - Update registration code

6. **Implement BuildCommand**
   - Wrapper around ./gradlew
   - Smart target selection
   - Progress display

7. **Implement ListCommand**
   - Display project structure
   - Show versions, loaders, asset packs

### Deliverables

- ✅ All CLI commands working
- ✅ Interactive prompts functional
- ✅ Input validation
- ✅ Help text and examples
- ✅ Error handling with clear messages

---

## Agent 4: Code Generators

**Responsibility**: Generate project files, items, blocks, etc.

### Tasks

1. **Implement ProjectGenerator**
   - Generate full project structure
   - Create proper Java package directories
   - Generate config files
   - Copy build-logic/
   - Generate shared/ code
   - Generate versions/
   - Generate Gradle wrapper

2. **Implement ItemGenerator**
   - Generate Item.java with proper package
   - Generate ItemTest.java
   - Generate item model JSON
   - Generate recipe JSON
   - Add to lang file

3. **Implement BlockGenerator**
   - Generate Block.java
   - Generate blockstate JSON
   - Generate block model JSON
   - Generate loot table
   - Add to lang file

4. **Implement EntityGenerator (optional/future)**
   - Generate entity class
   - Generate entity model
   - Generate spawn logic

5. **Implement FileUtil**
   - Safe file operations
   - Directory creation
   - Path validation
   - Prevent directory traversal

### Deliverables

- ✅ ProjectGenerator creates valid projects
- ✅ Generated projects build successfully
- ✅ ItemGenerator creates working items
- ✅ BlockGenerator creates working blocks
- ✅ All generators have tests

---

## Agent 5: Version Detection & APIs

**Responsibility**: Auto-detect loader versions, API integration

### Tasks

1. **Implement VersionDetector**
   - Detect latest NeoForge version for MC version
   - Detect latest Forge version
   - Detect latest Fabric Loader
   - Detect latest Fabric API
   - Cache results

2. **Implement NeoForgeVersions**
   - Query NeoForge Maven API
   - Parse version list
   - Filter by MC version
   - Return latest compatible

3. **Implement ForgeVersions**
   - Query Forge files API
   - Parse version list
   - Filter by MC version

4. **Implement FabricVersions**
   - Query Fabric Meta API
   - Get loader versions
   - Get API versions by MC version

5. **Implement caching**
   - Cache API responses
   - Refresh after 24 hours
   - Offline fallback with embedded defaults

6. **Handle errors gracefully**
   - Network timeouts
   - API unavailable
   - Invalid responses
   - Fallback to defaults

### Deliverables

- ✅ VersionDetector auto-detects all loader versions
- ✅ Works offline with cached/embedded defaults
- ✅ Handles API failures gracefully
- ✅ Tests with mocked APIs

---

## Agent 6: Documentation & Distribution

**Responsibility**: Comprehensive docs, installers, CI/CD

### Tasks

1. **Write user documentation**
   - `docs/installation.md`
   - `docs/getting-started.md`
   - `docs/commands/*.md` (one per command)
   - `docs/templates.md`
   - `docs/troubleshooting.md`

2. **Write contributor documentation**
   - `docs/contributing.md`
   - `docs/architecture.md`
   - `docs/development.md`
   - `docs/testing.md`

3. **Create README.md**
   - Project overview
   - Quick start
   - Feature list
   - Installation instructions
   - Example usage
   - Links to docs

4. **Create installer scripts**
   - `scripts/install.sh` (Unix)
   - `scripts/install.ps1` (Windows)
   - Auto-detect platform
   - Download from GitHub releases
   - Set up PATH

5. **Create GitHub Actions workflows**
   - `.github/workflows/build.yml` - CI: test on every push
   - `.github/workflows/native-image.yml` - Build native for all platforms
   - `.github/workflows/release.yml` - Create releases with binaries

6. **Write CHANGELOG.md**
   - Version history format
   - Keep up to date

7. **Create LICENSE**
   - MIT license text

### Deliverables

- ✅ Complete user documentation
- ✅ Complete contributor documentation
- ✅ README.md with examples
- ✅ Working installer scripts
- ✅ GitHub Actions CI/CD
- ✅ Release automation

---

## Integration & Testing Phase

After all agents complete, we'll integrate and test:

1. **End-to-end testing**
   ```bash
   # Test complete workflow
   dropper init test-mod
   cd test-mod
   ./gradlew build
   # Should produce 6 working JARs
   ```

2. **Cross-platform testing**
   - Test on Linux (Ubuntu)
   - Test on macOS (x64 and ARM)
   - Test on Windows

3. **Performance testing**
   - Measure startup time
   - Measure init time
   - Measure binary size

4. **Integration fixes**
   - Fix cross-agent integration issues
   - Resolve conflicts
   - Optimize

---

## Timeline Estimate

| Phase | Duration | Agents |
|-------|----------|--------|
| Phase 1: Setup | 2-3 days | Agent 1 |
| Phase 2: Parallel Development | 5-7 days | All 6 agents |
| Phase 3: Integration | 2-3 days | All agents |
| Phase 4: Testing | 2-3 days | All agents |
| Phase 5: Documentation | 1-2 days | Agent 6 |
| **Total** | **2-3 weeks** | |

---

## Agent Dependencies

```
Agent 1 (Foundation)
    ↓
Agent 2 (Templates) ─────→ Agent 4 (Generators)
    ↓                           ↓
Agent 3 (CLI) ←────────────────┘
    ↑
Agent 5 (Versions)

Agent 6 (Docs) - Independent, can start anytime
```

**Critical path**: Agent 1 → Agent 2 → Agent 4 → Agent 3

**Parallel work**:
- Agent 5 can work independently
- Agent 6 can start immediately
- Agent 3 and Agent 4 can overlap significantly

---

## Success Criteria

### Must Have (MVP)

- ✅ Native binary builds for Linux, macOS, Windows
- ✅ `dropper init` creates working project
- ✅ Generated project builds successfully (all 6 JARs)
- ✅ Generated project has proper Java package structure
- ✅ `dropper generate item` creates working item
- ✅ Auto-detects latest loader versions
- ✅ Complete documentation
- ✅ Installer works on all platforms
- ✅ GitHub releases with binaries

### Should Have (v1.1)

- ✅ `dropper add-version` command
- ✅ `dropper generate block` command
- ✅ `dropper build` wrapper
- ✅ `dropper list` command
- ✅ Offline mode with embedded defaults

### Nice to Have (v2.0)

- ✅ `dropper update-versions` command
- ✅ `dropper validate` command
- ✅ Custom template support
- ✅ Plugin system
- ✅ TUI interface
- ✅ Homebrew formula
- ✅ Chocolatey package

---

## Risk Mitigation

### Risk 1: GraalVM Compatibility Issues

**Mitigation**:
- Use only GraalVM-compatible libraries
- Test native compilation early
- Have reflection config ready
- Fallback to JAR if native fails

### Risk 2: Resource Embedding Complexity

**Mitigation**:
- Test resource loading early
- Use GraalVM's resource API correctly
- Have integration tests for extraction

### Risk 3: Cross-Platform Issues

**Mitigation**:
- Test on all platforms via GitHub Actions
- Use platform-agnostic file operations
- Handle path separators correctly

### Risk 4: API Failures (Version Detection)

**Mitigation**:
- Embed default versions
- Cache API responses
- Graceful degradation
- Clear error messages

---

## Ready to Start!

All agents have clear responsibilities and deliverables. Let's parallelize this implementation!

**Next**: Spawn agents and assign tasks.
