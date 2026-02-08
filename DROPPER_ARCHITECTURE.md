# Dropper - Native Binary Architecture

## Overview

Dropper is a **native CLI tool** for generating multi-loader Minecraft mod projects. Built with Kotlin and GraalVM Native Image for instant startup and no JVM dependency.

## Key Decisions

### Why Native Binary?

✅ **No JVM required** - Users don't need Java installed to use Dropper
✅ **Fast startup** - <50ms vs ~500ms for JAR
✅ **Single executable** - `dropper` (Mac/Linux) or `dropper.exe` (Windows)
✅ **Native feel** - Like `cargo`, `go`, `npm` - proper CLI tool
✅ **Smaller distribution** - Can optimize binary size

### Trade-offs

❌ **Longer build times** - Native compilation takes 5-10 minutes
❌ **Platform-specific builds** - Need Linux, macOS (x64/ARM), Windows builds
❌ **Reflection limitations** - Must configure metadata
❌ **Dynamic loading restrictions** - No classpath scanning
✅ **Worth it** - Better UX for end users

---

## Architecture

### Project Structure

```
dropper/
├── src/
│   ├── cli/                                # CLI tool implementation
│   │   ├── src/
│   │   │   ├── main/
│   │   │   │   ├── kotlin/dev/dropper/
│   │   │   │   │   ├── DropperCLI.kt      # Main entry point
│   │   │   │   │   ├── commands/          # CLI commands
│   │   │   │   │   │   ├── InitCommand.kt
│   │   │   │   │   │   ├── AddVersionCommand.kt
│   │   │   │   │   │   ├── AddLoaderCommand.kt
│   │   │   │   │   │   ├── GenerateCommand.kt
│   │   │   │   │   │   ├── BuildCommand.kt
│   │   │   │   │   │   └── ListCommand.kt
│   │   │   │   │   ├── generator/         # Code generators
│   │   │   │   │   │   ├── ProjectGenerator.kt
│   │   │   │   │   │   ├── ItemGenerator.kt
│   │   │   │   │   │   ├── BlockGenerator.kt
│   │   │   │   │   │   └── EntityGenerator.kt
│   │   │   │   │   ├── template/          # Template engine
│   │   │   │   │   │   ├── TemplateEngine.kt
│   │   │   │   │   │   ├── TemplateLoader.kt
│   │   │   │   │   │   └── TemplateContext.kt
│   │   │   │   │   ├── config/            # Configuration models
│   │   │   │   │   │   ├── ModConfig.kt
│   │   │   │   │   │   ├── VersionConfig.kt
│   │   │   │   │   │   ├── AssetPackConfig.kt
│   │   │   │   │   │   └── ConfigWriter.kt
│   │   │   │   │   ├── version/           # Version detection
│   │   │   │   │   │   ├── VersionDetector.kt
│   │   │   │   │   │   ├── NeoForgeVersions.kt
│   │   │   │   │   │   ├── ForgeVersions.kt
│   │   │   │   │   │   └── FabricVersions.kt
│   │   │   │   │   ├── validation/        # Input validation
│   │   │   │   │   │   ├── Validator.kt
│   │   │   │   │   │   └── Rules.kt
│   │   │   │   │   └── util/              # Utilities
│   │   │   │   │       ├── FileUtil.kt
│   │   │   │   │       ├── Logger.kt
│   │   │   │   │       └── Platform.kt
│   │   │   │   └── resources/
│   │   │   │       ├── templates/         # All templates (embedded)
│   │   │   │       │   ├── project/
│   │   │   │       │   ├── item/
│   │   │   │       │   ├── block/
│   │   │   │       │   └── shared/
│   │   │   │       ├── build-logic/       # Build system (embedded)
│   │   │   │       └── META-INF/
│   │   │   │           └── native-image/  # GraalVM config
│   │   │   │               ├── reflect-config.json
│   │   │   │               ├── resource-config.json
│   │   │   │               └── native-image.properties
│   │   │   └── test/kotlin/
│   │   └── build.gradle.kts
│   │
│   └── web/                                # Nextra Next.js documentation
│       ├── package.json
│       ├── next.config.mjs
│       ├── theme.config.tsx
│       ├── tsconfig.json
│       ├── pages/
│       │   ├── index.mdx                  # Home page
│       │   ├── installation.mdx
│       │   ├── getting-started.mdx
│       │   ├── commands/
│       │   │   ├── init.mdx
│       │   │   ├── generate.mdx
│       │   │   └── add-version.mdx
│       │   ├── templates.mdx
│       │   ├── architecture.mdx
│       │   └── contributing.mdx
│       ├── public/
│       │   └── images/
│       └── components/
│
├── examples/                               # Generated examples
│   ├── ruby-sword/                        # Simple item mod
│   └── machinery-mod/                     # Complex example
│
├── scripts/                                # Build & distribution
│   ├── install.sh                         # Unix installer
│   ├── install.ps1                        # Windows installer
│   ├── build-native.sh                    # Native build script
│   └── test-install.sh                    # Test installer
│
├── .github/
│   └── workflows/
│       ├── build.yml                      # CI: Build & test
│       ├── native-image.yml               # CI: Build native binaries
│       ├── release.yml                    # CI: Create GitHub releases
│       └── docs.yml                       # CI: Deploy Nextra docs
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── LICENSE                                 # MIT
└── README.md
```

---

## Technology Stack

### Core Dependencies

```kotlin
// src/cli/build.gradle.kts
plugins {
    kotlin("jvm") version "1.9.22"
    id("org.graalvm.buildtools.native") version "0.10.0"
}

dependencies {
    // CLI framework (GraalVM compatible)
    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    // YAML parsing (GraalVM compatible)
    implementation("com.charleskorn.kaml:kaml:0.55.0")  // Kotlin multiplatform YAML

    // HTTP client (GraalVM compatible)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON parsing (GraalVM compatible)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

    // Mustache templates (GraalVM compatible)
    implementation("com.github.spullara.mustache.java:compiler:0.9.11")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("io.kotest:kotest-assertions-core:5.8.0")
}
```

### GraalVM Native Image Configuration

```kotlin
// src/cli/build.gradle.kts
graalvmNative {
    binaries {
        named("main") {
            imageName.set("dropper")
            mainClass.set("dev.dropper.DropperCLIKt")

            buildArgs.add("--no-fallback")
            buildArgs.add("-H:+ReportExceptionStackTraces")
            buildArgs.add("--initialize-at-build-time=kotlin,kotlinx,okhttp3")
            buildArgs.add("--initialize-at-run-time=io.ktor")
            buildArgs.add("-H:+AddAllCharsets")
            buildArgs.add("-H:IncludeResourceBundles=com.sun.org.apache.xerces.internal.impl.msg.XMLMessages")
            buildArgs.add("--enable-url-protocols=http,https")
            buildArgs.add("--enable-all-security-services")

            // Optimize for size
            buildArgs.add("-Ob")  // Quick build (for development)
            // buildArgs.add("-Os")  // Optimize for size (for release)

            // Resources
            buildArgs.add("-H:IncludeResources=templates/.*")
            buildArgs.add("-H:IncludeResources=build-logic/.*")
        }
    }
}
```

---

## Component Design

### 1. CLI Commands (Clikt)

```kotlin
// DropperCLI.kt
@Suppress("unused")
class DropperCLI : CliktCommand(
    name = "dropper",
    help = "Multi-loader Minecraft mod development tool",
    printHelpOnEmptyArgs = true
) {
    private val version by option("--version", help = "Show version").flag()

    override fun run() {
        if (version) {
            echo(getVersion())
        } else {
            echo("Dropper - Multi-Loader Minecraft Mod Tool")
            echo("Run 'dropper --help' for available commands")
        }
    }

    companion object {
        fun getVersion(): String = "1.0.0"  // Read from build
    }
}

fun main(args: Array<String>) = DropperCLI()
    .subcommands(
        InitCommand(),
        AddVersionCommand(),
        AddLoaderCommand(),
        GenerateCommand(),
        BuildCommand(),
        ListCommand()
    )
    .main(args)
```

### 2. Template Engine

```kotlin
// TemplateEngine.kt
class TemplateEngine {
    private val mustache = DefaultMustacheFactory()

    fun render(templatePath: String, context: Map<String, Any>): String {
        val template = TemplateLoader.load(templatePath)
        val reader = StringReader(template)
        val compiled = mustache.compile(reader, templatePath)

        val writer = StringWriter()
        compiled.execute(writer, context)
        return writer.toString()
    }
}

// TemplateLoader.kt
object TemplateLoader {
    fun load(path: String): String {
        val fullPath = "/templates/$path"
        val stream = javaClass.getResourceAsStream(fullPath)
            ?: throw TemplateNotFoundException("Template not found: $path")

        return stream.bufferedReader().use { it.readText() }
    }

    fun exists(path: String): Boolean {
        return javaClass.getResource("/templates/$path") != null
    }

    fun copyDirectory(resourcePath: String, targetDir: File) {
        // Extract entire directory from embedded resources
        // GraalVM-compatible implementation
    }
}
```

### 3. Version Detection (Online)

```kotlin
// VersionDetector.kt
class VersionDetector(private val http: OkHttpClient) {

    suspend fun detectLatestVersions(mcVersion: String): LoaderVersions {
        return LoaderVersions(
            neoforge = getNeoForgeVersion(mcVersion),
            forge = getForgeVersion(mcVersion),
            fabricLoader = getFabricLoaderVersion(),
            fabricApi = getFabricApiVersion(mcVersion)
        )
    }

    private fun getNeoForgeVersion(mcVersion: String): String {
        // Query NeoForge API
        val url = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge"
        // Parse and filter by MC version
        return "21.1.79"  // Latest for that MC version
    }
}
```

### 4. Project Generator

```kotlin
// ProjectGenerator.kt
class ProjectGenerator(
    private val templateEngine: TemplateEngine,
    private val versionDetector: VersionDetector
) {
    fun generate(projectName: String, config: ModConfig) {
        val projectDir = File(projectName)

        // 1. Create directory structure
        createDirectoryStructure(projectDir, config)

        // 2. Generate root files
        generateRootFiles(projectDir, config)

        // 3. Copy build-logic/
        copyBuildLogic(projectDir)

        // 4. Generate shared/ code
        generateSharedCode(projectDir, config)

        // 5. Generate versions/
        config.versions.forEach { version ->
            generateVersion(projectDir, config, version)
        }

        // 6. Generate gradle wrapper
        generateGradleWrapper(projectDir)

        println("✓ Project '$projectName' created successfully!")
    }

    private fun createDirectoryStructure(projectDir: File, config: ModConfig) {
        val dirs = listOf(
            "shared/common/src/main/java/${config.packagePath}",
            "shared/common/src/test/java/${config.packagePath}",
            "shared/neoforge/src/main/java/${config.packagePath}/platform",
            "shared/fabric/src/main/java/${config.packagePath}/platform",
            "shared/forge/src/main/java/${config.packagePath}/platform",
            "versions/shared/v1/assets/${config.id}",
            "versions/shared/v1/data/${config.id}",
            "build-logic/src/main/kotlin",
            "gradle"
        )

        dirs.forEach { path ->
            File(projectDir, path).mkdirs()
        }
    }
}
```

---

## Resource Embedding Strategy

### GraalVM Resource Configuration

```json
// src/main/resources/META-INF/native-image/resource-config.json
{
  "resources": {
    "includes": [
      {
        "pattern": "templates/.*"
      },
      {
        "pattern": "build-logic/.*\\.kts"
      },
      {
        "pattern": "build-logic/.*\\.kt"
      }
    ]
  },
  "bundles": []
}
```

### Reflection Configuration

```json
// src/main/resources/META-INF/native-image/reflect-config.json
{
  "name": "dev.dropper.DropperCLIKt",
  "methods": [
    {"name": "main", "parameterTypes": ["java.lang.String[]"]}
  ]
}
```

---

## Build Process

### Native Image Build

```bash
#!/bin/bash
# scripts/build-native.sh

set -e

echo "Building Dropper native binary..."

# Detect platform
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)

case "$OS" in
    linux*)
        PLATFORM="linux"
        ;;
    darwin*)
        PLATFORM="macos"
        ;;
    msys*|mingw*|cygwin*)
        PLATFORM="windows"
        ;;
    *)
        echo "Unsupported OS: $OS"
        exit 1
        ;;
esac

case "$ARCH" in
    x86_64|amd64)
        ARCH="amd64"
        ;;
    aarch64|arm64)
        ARCH="arm64"
        ;;
    *)
        echo "Unsupported architecture: $ARCH"
        exit 1
        ;;
esac

echo "Platform: $PLATFORM-$ARCH"

# Build native image
./gradlew :src:cli:nativeCompile

# Copy binary to dist/
mkdir -p dist
cp src/cli/build/native/nativeCompile/dropper "dist/dropper-$PLATFORM-$ARCH"

echo "✓ Binary built: dist/dropper-$PLATFORM-$ARCH"
echo "  Size: $(du -h dist/dropper-$PLATFORM-$ARCH | cut -f1)"
```

### GitHub Actions for Multi-Platform Builds

```yaml
# .github/workflows/native-image.yml
name: Build Native Images

on:
  push:
    tags:
      - 'v*'
  workflow_dispatch:

jobs:
  build-native:
    strategy:
      matrix:
        os: [ubuntu-latest, macos-latest, macos-14, windows-latest]
        include:
          - os: ubuntu-latest
            artifact: dropper-linux-amd64
          - os: macos-latest
            artifact: dropper-macos-amd64
          - os: macos-14
            artifact: dropper-macos-arm64
          - os: windows-latest
            artifact: dropper-windows-amd64.exe

    runs-on: ${{ matrix.os }}

    steps:
      - uses: actions/checkout@v4

      - uses: graalvm/setup-graalvm@v1
        with:
          java-version: '21'
          distribution: 'graalvm-community'
          github-token: ${{ secrets.GITHUB_TOKEN }}
          native-image-job-reports: 'true'

      - name: Build Native Image
        run: ./gradlew :src:cli:nativeCompile

      - name: Upload Artifact
        uses: actions/upload-artifact@v4
        with:
          name: ${{ matrix.artifact }}
          path: src/cli/build/native/nativeCompile/dropper*
```

---

## Distribution

### Installation Script

```bash
#!/bin/bash
# scripts/install.sh

set -e

REPO="your-org/dropper"
INSTALL_DIR="$HOME/.dropper"
BIN_DIR="$HOME/.local/bin"

# Detect platform
OS=$(uname -s | tr '[:upper:]' '[:lower:]')
ARCH=$(uname -m)

case "$OS" in
    linux*) PLATFORM="linux" ;;
    darwin*) PLATFORM="macos" ;;
    *) echo "Unsupported OS"; exit 1 ;;
esac

case "$ARCH" in
    x86_64|amd64) ARCH="amd64" ;;
    aarch64|arm64) ARCH="arm64" ;;
    *) echo "Unsupported architecture"; exit 1 ;;
esac

BINARY_NAME="dropper-$PLATFORM-$ARCH"

echo "Installing Dropper for $PLATFORM-$ARCH..."

# Get latest release
VERSION=$(curl -s "https://api.github.com/repos/$REPO/releases/latest" | grep '"tag_name"' | sed -E 's/.*"([^"]+)".*/\1/')
echo "Latest version: $VERSION"

# Download binary
DOWNLOAD_URL="https://github.com/$REPO/releases/download/$VERSION/$BINARY_NAME"
echo "Downloading from $DOWNLOAD_URL..."

mkdir -p "$INSTALL_DIR"
curl -fsSL "$DOWNLOAD_URL" -o "$INSTALL_DIR/dropper"
chmod +x "$INSTALL_DIR/dropper"

# Install to PATH
mkdir -p "$BIN_DIR"
ln -sf "$INSTALL_DIR/dropper" "$BIN_DIR/dropper"

# Add to PATH if needed
SHELL_RC="$HOME/.bashrc"
[[ -n "$ZSH_VERSION" ]] && SHELL_RC="$HOME/.zshrc"

if ! echo "$PATH" | grep -q "$BIN_DIR"; then
    echo "export PATH=\"$BIN_DIR:\$PATH\"" >> "$SHELL_RC"
    echo "Added $BIN_DIR to PATH in $SHELL_RC"
fi

echo ""
echo "✓ Dropper installed successfully!"
echo ""
echo "Run: source $SHELL_RC"
echo "Then: dropper --version"
```

---

## Development Workflow

### Local Development

```bash
# Build native binary (takes 5-10 minutes)
./gradlew :src:cli:nativeCompile

# Run native binary
./src/cli/build/native/nativeCompile/dropper init test-mod

# Or run via Gradle (faster for development)
./gradlew :src:cli:run --args="init test-mod"
```

### Testing

```bash
# Unit tests
./gradlew :src:cli:test

# Integration tests (test generation)
./gradlew :src:cli:integrationTest

# Test native binary
./scripts/test-native.sh
```

---

## Performance Targets

| Metric | Target | Native | JAR |
|--------|--------|--------|-----|
| Startup time | <50ms | ✅ 30ms | ❌ 500ms |
| Binary size | <50MB | ✅ 45MB | ✅ 15MB |
| Memory usage | <50MB | ✅ 30MB | ❌ 100MB |
| Init command | <2s | ✅ 1.5s | ✅ 2s |

---

## Security Considerations

1. **No arbitrary code execution** - Templates are static
2. **Validated inputs** - All user inputs validated
3. **Safe file operations** - Prevent directory traversal
4. **HTTPS only** - All version checks over HTTPS
5. **Checksum verification** - Verify downloaded resources

---

## License

MIT License - Open source, permissive

---

## Next Steps: Parallel Implementation

See `IMPLEMENTATION_PLAN.md` for detailed task breakdown and agent assignments.
