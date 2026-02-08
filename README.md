# Dropper

**A native CLI tool for generating multi-loader Minecraft mod projects.**

Dropper creates optimized monorepo structures that support multiple Minecraft versions and mod loaders (Fabric, Forge, NeoForge) with maximum code and asset reuse. No JVM required—just a single native binary.

## Features

- **Native Binary** - No Java installation required, instant startup (<50ms)
- **Multi-Loader Support** - Generate projects supporting Fabric, Forge, and NeoForge from one codebase
- **Multi-Version Support** - Manage multiple Minecraft versions simultaneously with asset pack versioning
- **Intelligent Code Generation** - Generate items, blocks, entities with proper loader abstractions
- **Automatic Version Detection** - Fetches latest loader versions for your Minecraft version
- **Config-Driven** - Single `config.yml` generates all loader-specific metadata files
- **Best Practices Built-In** - Follows Gradle conventions with proper incremental builds

## About

I personally don't write mods nor do I play Minecraft anymore (besides the 2 week phases). Made this for my dear friend [SketchMacaw](https://github.com/sketchmacaw) as he was telling me about the struggles of maintaining multiple mods, versions and modloaders and I though I could apply my software development, devops and other expertise and optimize modding dev experience.

I don't plan to actively maintain it - might consider it if there is big popularity or demand - but feel free to fork this and make a thing from this :).

## Installation

### One-Line Install

**macOS / Linux:**
```bash
curl -fsSL https://raw.githubusercontent.com/sebastianstupak/dropper/main/scripts/install.sh | sh
```

**Windows (PowerShell):**
```powershell
iwr https://raw.githubusercontent.com/sebastianstupak/dropper/main/scripts/install.ps1 -useb | iex
```

This will:
- Download the appropriate binary for your platform
- Install to `~/.dropper/` (Unix) or `%LOCALAPPDATA%\Dropper\` (Windows)
- Add to your PATH automatically

### Manual Installation

1. Download the latest binary for your platform from [Releases](https://github.com/sebastianstupak/dropper/releases):
   - **Linux x64:** `dropper-linux-amd64`
   - **macOS x64:** `dropper-macos-amd64`
   - **macOS ARM64:** `dropper-macos-arm64` (Apple Silicon)
   - **Windows x64:** `dropper-windows-amd64.exe`

2. **Unix/Linux/macOS:**
   ```bash
   # Make executable and move to PATH
   chmod +x dropper-*
   sudo mv dropper-* /usr/local/bin/dropper
   ```

3. **Windows:**
   - Rename to `dropper.exe`
   - Move to a directory in your PATH (e.g., `C:\Program Files\Dropper\`)
   - Or use the PowerShell script: `.\scripts\install.ps1`

### Verify Installation

```bash
dropper --version
dropper --help
```

## Quick Start

### Initialize a New Mod

```bash
dropper init my-awesome-mod
```

You'll be prompted for:
- Mod name (display name)
- Mod ID (internal identifier)
- Author
- Description
- License (default: MIT)
- Minecraft versions (e.g., `1.20.1,1.21.1`)
- Mod loaders (default: `fabric,forge,neoforge`)

### Generate Content

```bash
cd my-awesome-mod

# Generate an item
dropper generate item ruby_sword

# More generators coming soon:
# dropper generate block ruby_ore
# dropper generate entity friendly_robot
```

### Build Your Mod

```bash
# Build all versions and loaders
./gradlew build

# Build specific version-loader
./gradlew :1_20_1-neoforge:build

# Output JARs will be in build/
```

### Testing In-Game

JARs are output to `build/<version>/<loader>.jar`:
```
build/
├── 1_20_1/
│   ├── fabric.jar
│   ├── forge.jar
│   └── neoforge.jar
└── 1_21_1/
    ├── fabric.jar
    ├── forge.jar
    └── neoforge.jar
```

Copy the appropriate JAR to your `.minecraft/mods/` folder.

## Project Structure

Dropper generates a layered monorepo structure optimized for code reuse:

```
mod-name/
├── config.yml                    # Mod metadata (single source of truth)
│
├── shared/                       # Version-agnostic code
│   ├── common/                   # Platform-agnostic (all loaders)
│   ├── neoforge/                 # NeoForge abstractions
│   ├── forge/                    # Forge abstractions
│   └── fabric/                   # Fabric abstractions
│
├── versions/
│   ├── shared/
│   │   ├── v1/                   # Asset pack for MC 1.20.x
│   │   │   ├── common/           # Code for 1.20.x
│   │   │   ├── assets/           # Textures, models, sounds
│   │   │   └── data/             # Recipes, tags, loot tables
│   │   └── v2/                   # Asset pack for MC 1.21.x (inherits v1)
│   │
│   └── 1_20_1/                   # MC 1.20.1 specific
│       ├── config.yml            # Version configuration
│       ├── common/               # Shared code (all loaders)
│       ├── neoforge/             # NeoForge-specific
│       ├── fabric/               # Fabric-specific
│       └── forge/                # Forge-specific
│
├── build-logic/                  # Gradle convention plugins
│
└── build/                        # Output JARs
    └── 1_20_1/
        ├── fabric.jar
        ├── forge.jar
        └── neoforge.jar
```

**Key Concept: Layer Inheritance**

Code and assets flow from general to specific:
```
shared/common/ → shared/{loader}/ → versions/shared/v1/ → versions/1_20_1/common/ → versions/1_20_1/{loader}/
```

Later layers override earlier ones during build. This means you:
- Define common code once in `shared/common/`
- Override only what's different per loader in `shared/{loader}/`
- Share assets across versions in `versions/shared/v1/`
- Add version-specific changes only where needed

## Commands

### `dropper init`

Initialize a new mod project:

```bash
dropper init [project-name] [options]

Options:
  --name <name>              Mod display name (default: project name)
  --id <id>                  Mod ID (default: kebab-case of name)
  --package <package>        Base package (default: com.{id})
  --author <author>          Mod author (default: git user.name)
  --license <license>        License (default: MIT)
  --mc-version <version>     Minecraft version (default: latest stable)
  --loaders <loaders>        Comma-separated loaders (default: fabric,forge,neoforge)
  --description <text>       Mod description
```

**Example:**
```bash
dropper init my-mod --name "My Awesome Mod" --author "YourName" --mc-version 1.20.1
```

### `dropper generate`

Generate mod components:

```bash
# Items
dropper generate item <name> [options]
  --type <type>              sword, axe, pickaxe, shovel, hoe, food, basic (default: basic)
  --layer <layer>            shared, version, loader-specific (default: shared)

# Blocks
dropper generate block <name> [options]
  --type <type>              ore, stone, wood, plant, basic (default: basic)
  --layer <layer>            shared, version, loader-specific (default: shared)

# Entities
dropper generate entity <name> [options]
  --type <type>              mob, projectile, vehicle (default: mob)
  --layer <layer>            shared, version, loader-specific (default: shared)
```

**Examples:**
```bash
dropper generate item ruby --type basic
dropper generate item ruby_sword --type sword
dropper generate block ruby_ore --type ore
dropper generate entity ruby_golem --type mob
```

### `dropper add-version`

Add support for a new Minecraft version:

```bash
dropper add-version <version> [options]
  --asset-pack <pack>        Reuse existing asset pack (v1, v2, etc.)
  --loaders <loaders>        Comma-separated loaders
  --auto-detect              Automatically detect loader versions (default: true)
```

**Example:**
```bash
dropper add-version 1.21.1 --asset-pack v2 --loaders fabric,neoforge
```

### `dropper list`

List available versions and loaders in the project:

```bash
dropper list [type]
  versions                   List all supported MC versions
  loaders                    List all supported loaders
  asset-packs                List all asset pack versions
```

## Generated Project Features

### Config-Driven Metadata

All mod metadata lives in `config.yml`:

```yaml
mod:
  id: my-mod
  name: "My Awesome Mod"
  version: "1.0.0"
  description: "Does cool things"
  author: "YourName"
  license: "MIT"

versions:
  - minecraft_version: "1.20.1"
    asset_pack: "v1"
    loaders: [fabric, forge, neoforge]
    java_version: 17
```

Dropper automatically generates:
- `fabric.mod.json` for Fabric
- `mods.toml` for Forge/NeoForge
- `META-INF/services/` for ServiceLoader
- Gradle build files with proper dependencies

### Asset Pack Versioning

Reuse assets across similar Minecraft versions:

```yaml
# versions/shared/v1/config.yml
asset_pack:
  version: "v1"
  minecraft_versions: [1.20.1, 1.20.4, 1.20.6]

# versions/shared/v2/config.yml
asset_pack:
  version: "v2"
  minecraft_versions: [1.21.1]
  inherits: "v1"  # Inherits all v1 assets, override only what changed
```

**Benefits:**
- Update a texture once in `v1`, all 1.20.x versions get it
- Override only assets that changed in newer versions
- Clear migration path when Mojang changes formats

### Platform Abstraction

Generated code uses ServiceLoader pattern for clean platform-specific implementations:

```java
// Use platform-specific code anywhere
if (Services.PLATFORM.isModLoaded("jei")) {
    // Do JEI integration
}

// Get platform name
String platform = Services.PLATFORM.getPlatformName(); // "NeoForge", "Fabric", etc.
```

## Example Project

See the complete example in [`examples/ruby-sword/`](examples/ruby-sword/) - a simple mod showcasing the structure:

- Custom sword item (Ruby Sword)
- Multi-loader support (Fabric, Forge, NeoForge)
- Single Minecraft version (1.20.1)
- Asset pack v1 with textures and models
- ServiceLoader platform abstractions
- Co-located tests

Build the example:
```bash
cd examples/ruby-sword
./gradlew build

# Output: build/1_20_1/{fabric,forge,neoforge}.jar
```

## Documentation

For detailed information about the generated project structure and architecture:

- **[DROPPER_ARCHITECTURE.md](DROPPER_ARCHITECTURE.md)** - Complete architecture documentation
- **[CLAUDE.md](CLAUDE.md)** - Instructions for AI assistants working with generated projects

Generated projects also include extensive documentation:
- `docs/architecture.md` - System design and decisions
- `docs/build-system.md` - Gradle build implementation
- `docs/development.md` - Day-to-day development workflow
- `docs/quick-reference.md` - Cheat sheet for commands

## Building Dropper from Source

### Prerequisites

- Java 21+ (with GraalVM for native builds)
- Gradle 8.5+

### Development Build

```bash
# Clone repository
git clone https://github.com/sebastian-stupak/dropper.git
cd dropper

# Run via Gradle (fast for development)
./gradlew :src:cli:run --args="init test-mod"

# Run tests
./gradlew :src:cli:test
```

### Native Binary Build

```bash
# Build native binary (requires GraalVM, takes 5-10 minutes)
./gradlew :src:cli:nativeCompile

# Binary output: src/cli/build/native/nativeCompile/dropper

# Run native binary
./src/cli/build/native/nativeCompile/dropper --version
```

### Multi-Platform Builds

Use the provided build script:

```bash
# Build for current platform
./scripts/build-native.sh

# Binary output: dist/dropper-{platform}-{arch}
```

For cross-platform builds, see [`.github/workflows/native-image.yml`](.github/workflows/native-image.yml).

## Technology Stack

- **Language:** Kotlin (JVM)
- **CLI Framework:** [Clikt](https://github.com/ajalt/clikt) - GraalVM compatible
- **Template Engine:** [Mustache.java](https://github.com/spullara/mustache.java)
- **YAML Parser:** [Kaml](https://github.com/charleskorn/kaml) - Kotlin multiplatform
- **HTTP Client:** [OkHttp](https://square.github.io/okhttp/)
- **Native Compilation:** [GraalVM Native Image](https://www.graalvm.org/native-image/)

## Design Philosophy

Dropper generates projects optimized for:

- **Maximum Reuse** - Define once, use everywhere
- **Clear Separation** - Each layer has a specific purpose
- **Easy Navigation** - Flat structure, no deep nesting
- **Maintainability** - Add versions/loaders without touching existing code
- **Build Performance** - Incremental builds, proper caching
- **Scalability** - Support 10+ versions × 3+ loaders with minimal duplication

## Comparison to Other Tools

### vs. Architectury

| Feature | Dropper | Architectury |
|---------|---------|--------------|
| External dependencies | None | Architectury API required |
| Multi-version support | Asset packs | Preprocessor/branches |
| Setup time | Instant (native binary) | Requires Gradle/Java |
| Asset reuse | Built-in versioning | Manual duplication |

### vs. MultiLoader Template

| Feature | Dropper | MultiLoader |
|---------|---------|-------------|
| Structure | Custom layered | Standard 3-project |
| Multi-version | Asset packs | Separate branches |
| Setup | CLI generator | Manual clone & configure |
| Asset sharing | Version packs | Duplicated |

### vs. Stonecutter

| Feature | Dropper | Stonecutter |
|---------|---------|-------------|
| Multi-version | Separate dirs | Preprocessor directives |
| Single codebase | No (layered) | Yes (with `#if` comments) |
| Debugging | Normal Java | Preprocessor artifacts |
| Setup | CLI generator | Manual setup |

## Performance

### Dropper CLI

| Metric | Performance |
|--------|-------------|
| Startup time | ~30ms |
| Binary size | ~45MB |
| Memory usage | ~30MB |
| Project initialization | ~1.5s |

### Generated Projects

| Configuration | Clean Build | Incremental |
|---------------|-------------|-------------|
| Single version-loader | ~30s | ~5s |
| All 3 loaders (1 version) | ~90s | ~15s |
| 3 versions × 3 loaders | ~5min | ~1min |

*Tested on modern hardware (8-core CPU, SSD)*

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Development Workflow

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Make changes and test: `./gradlew :src:cli:test`
4. Build native binary: `./gradlew :src:cli:nativeCompile`
5. Test the binary: `./src/cli/build/native/nativeCompile/dropper init test-project`
6. Commit changes: `git commit -m "Add feature: description"`
7. Push to your fork: `git push origin feature/my-feature`
8. Create a Pull Request

## License

[MIT License](LICENSE) - Open source, permissive

## Support

- **Documentation:** See [DROPPER_ARCHITECTURE.md](DROPPER_ARCHITECTURE.md)
- **Issues:** [GitHub Issues](https://github.com/sebastian-stupak/dropper/issues)
- **Discussions:** [GitHub Discussions](https://github.com/sebastian-stupak/dropper/discussions)