# Dropper Prerequisites

This guide explains what you need to use Dropper and how everything works behind the scenes.

## Quick Answer: What Do I Need?

**Required:**
- ✅ Java Development Kit (JDK) 21 or newer
- ✅ Internet connection (for downloading dependencies)

**NOT Required:**
- ❌ Minecraft installation (downloads automatically!)
- ❌ Mod loaders (download automatically via Gradle)
- ❌ IDE (optional but recommended)

That's it! Dropper and Gradle handle everything else.

---

## Detailed Requirements

### 1. Java Development Kit (JDK) 21+

Minecraft 1.20+ requires Java 21 or newer. You need the full JDK (not just JRE) to compile mods.

#### Check if you have Java:
```bash
java -version
```

You should see something like:
```
openjdk version "21.0.1" 2023-10-17
```

If the version is below 21, or you get "command not found", install Java 21:

#### Installation:

**Windows:**
```powershell
# Using winget (Windows 11 / Windows 10 with App Installer)
winget install Microsoft.OpenJDK.21

# OR download from: https://adoptium.net/
```

**macOS:**
```bash
# Using Homebrew
brew install openjdk@21

# Add to PATH (add this to ~/.zshrc or ~/.bash_profile)
export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install openjdk-21-jdk

# Verify installation
java -version
```

**Linux (Fedora/RHEL):**
```bash
sudo dnf install java-21-openjdk-devel

# Verify installation
java -version
```

**Linux (Arch):**
```bash
sudo pacman -S jdk21-openjdk

# Verify installation
java -version
```

### 2. Internet Connection

Required for:
- Downloading Minecraft (first time per version)
- Downloading mod loader dependencies (Fabric, Forge, NeoForge)
- Downloading libraries and mappings

**First run:** 5-10 minutes (downloads ~500MB-1GB)
**Subsequent runs:** Fast (everything cached)

### 3. Disk Space

Approximate space needed:
- **Gradle cache:** 500MB - 1GB per Minecraft version
- **Your project:** 10-50MB
- **Build outputs:** 5-20MB per version/loader combo

Example for a mod supporting 3 versions + 2 loaders:
- Cache: ~2GB (shared across all projects!)
- Your project: 30MB
- Total: ~2GB (but cache is reused)

**Cache location:**
- Windows: `C:\Users\<YourName>\.gradle\caches\`
- macOS/Linux: `~/.gradle/caches/`

You can clean the cache if needed (forces re-download):
```bash
./gradlew clean cleanCache
```

---

## How Minecraft Gets Installed

### The Magic: Automatic Setup

When you run `dropper dev run`, here's what happens:

```
┌─────────────────────────────────────────────────────────┐
│ 1. Dropper reads your config.yml                       │
│    - Minecraft version: 1.20.1                         │
│    - Mod loader: fabric                                │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│ 2. Dropper calls Gradle:                               │
│    ./gradlew :1_20_1-fabric:runClient                  │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│ 3. Gradle Plugin (Fabric Loom / ForgeGradle):          │
│    - Downloads Minecraft 1.20.1 (if not cached)        │
│    - Applies Minecraft mappings (deobfuscation)        │
│    - Sets up mod development environment               │
│    - Compiles your mod                                 │
│    - Injects mod into Minecraft classpath              │
└─────────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────────┐
│ 4. Minecraft launches with your mod loaded!            │
└─────────────────────────────────────────────────────────┘
```

### First Run vs Subsequent Runs

**First Run (per Minecraft version):**
```
[Dropper] Starting Minecraft 1.20.1 with Fabric...
[Gradle] Downloading Minecraft 1.20.1... (300MB)
[Gradle] Downloading Fabric loader... (10MB)
[Gradle] Downloading Fabric API... (5MB)
[Gradle] Applying Minecraft mappings... (2-3 minutes)
[Gradle] Decompiling Minecraft... (2-3 minutes)
[Gradle] Compiling mod... (30 seconds)
[Gradle] Launching Minecraft... (30 seconds)
[Minecraft] Game window opens! 🎮

Total time: ~5-10 minutes
```

**Subsequent Runs (everything cached):**
```
[Dropper] Starting Minecraft 1.20.1 with Fabric...
[Gradle] Using cached Minecraft
[Gradle] Compiling mod... (if changed)
[Gradle] Launching Minecraft...
[Minecraft] Game window opens! 🎮

Total time: ~30-60 seconds
```

### What Gets Cached?

The following are cached in `~/.gradle/caches/` and **shared across all projects:**

- ✅ Minecraft JARs (per version)
- ✅ Decompiled Minecraft source (per version)
- ✅ Mappings (Yarn, MCP, etc.)
- ✅ Mod loader dependencies
- ✅ Library JARs

This means:
- **First project:** Takes 5-10 minutes per version
- **Second project:** Instant setup (cache hit!)
- **Multiple projects:** All share the same cache

---

## Optional but Recommended: IDE

While not required, an IDE makes development much easier.

### IntelliJ IDEA (Recommended)

**Why:** Best Java/Kotlin support, excellent Gradle integration

**Installation:**
```bash
# Download from: https://www.jetbrains.com/idea/download/
# Community Edition is free and sufficient

# Open your Dropper project:
# File → Open → Select your project directory
# IDEA will auto-detect Gradle and import the project
```

**Run configuration:**
1. Run your mod: Use Gradle tasks directly
2. Debug: Set breakpoints, use `dropper dev run --debug`

### VS Code

**Why:** Lightweight, extensible

**Required extensions:**
- Extension Pack for Java
- Gradle for Java

**Installation:**
```bash
# Install VS Code: https://code.visualstudio.com/

# Install extensions
code --install-extension vscjava.vscode-java-pack
code --install-extension vscjava.vscode-gradle
```

### Eclipse

**Why:** Classic Java IDE

**Required plugins:**
- Buildship Gradle Integration

**Installation:**
- Download Eclipse IDE for Java Developers
- Help → Eclipse Marketplace → Search "Buildship"

---

## Common Issues & Solutions

### "Java version is too old"

**Problem:** Your Java is below version 21
```
Error: This project requires Java 21 or higher
Current version: 17
```

**Solution:**
1. Install Java 21+ (see installation section above)
2. Verify with `java -version`
3. Restart your terminal/IDE

### "JAVA_HOME not set"

**Problem:** Java is installed but not in PATH

**Solution (macOS/Linux):**
```bash
# Add to ~/.zshrc or ~/.bashrc
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH="$JAVA_HOME/bin:$PATH"

# Reload shell config
source ~/.zshrc  # or source ~/.bashrc
```

**Solution (Windows):**
```powershell
# Add to System Environment Variables
# 1. Search "Environment Variables" in Start Menu
# 2. Edit System Variables
# 3. Add JAVA_HOME pointing to JDK install directory
# 4. Add %JAVA_HOME%\bin to PATH
# 5. Restart terminal
```

### "Could not resolve dependencies"

**Problem:** Internet connection issues or repository down

**Solution:**
```bash
# Retry with verbose output
./gradlew build --refresh-dependencies --debug

# Check your internet connection
# Check firewall/proxy settings

# If behind corporate proxy, configure Gradle proxy:
# Edit ~/.gradle/gradle.properties:
systemProp.http.proxyHost=yourproxy.com
systemProp.http.proxyPort=8080
systemProp.https.proxyHost=yourproxy.com
systemProp.https.proxyPort=8080
```

### "Permission denied: ./gradlew"

**Problem:** Gradle wrapper not executable (Unix/macOS/Linux)

**Solution:**
```bash
chmod +x gradlew
```

### "Out of memory"

**Problem:** Gradle runs out of memory during build

**Solution:**
```bash
# Edit gradle.properties in your project:
org.gradle.jvmargs=-Xmx4G

# Or set globally in ~/.gradle/gradle.properties
```

### Slow download speeds

**Problem:** Downloading from default Maven repositories is slow

**Solution (use mirror):**
```kotlin
// In buildSrc/build.gradle.kts, add mirrors:
repositories {
    mavenCentral {
        url = uri("https://repo1.maven.org/maven2/")
    }
    // Or use Aliyun mirror (fast in Asia)
    maven("https://maven.aliyun.com/repository/public")
}
```

---

## System Requirements

### Minimum:
- **CPU:** Dual-core 2.0+ GHz
- **RAM:** 4GB (Minecraft needs 2GB, Gradle needs 1-2GB)
- **Storage:** 5GB free space
- **OS:** Windows 10+, macOS 10.14+, or modern Linux

### Recommended:
- **CPU:** Quad-core 2.5+ GHz
- **RAM:** 8GB+ (smooth development experience)
- **Storage:** 10GB+ free space
- **OS:** Windows 11, macOS 12+, or Ubuntu 22.04+

### For running Minecraft:
- **GPU:** Any OpenGL 3.2+ compatible GPU
- **Display:** 1280x720 minimum resolution

---

## Next Steps

Once you have Java 21+ installed:

1. **Install Dropper:**
   ```bash
   # From source (for now):
   git clone https://github.com/YOUR_USERNAME/dropper
   cd dropper
   ./gradlew :src:cli:build
   ```

2. **Create your first mod:**
   ```bash
   dropper init my-awesome-mod
   cd my-awesome-mod
   ```

3. **Launch Minecraft:**
   ```bash
   dropper dev run
   ```

4. **Start developing:**
   - See [DEV_COMMAND.md](DEV_COMMAND.md) for dev workflow
   - See [DEV_COMMAND_QUICK_REFERENCE.md](DEV_COMMAND_QUICK_REFERENCE.md) for commands

---

## Getting Help

**Check versions:**
```bash
java -version          # Should be 21+
./gradlew --version    # Shows Gradle version
dropper --version      # Shows Dropper version
```

**Troubleshooting:**
- Check the [DEV_COMMAND.md](DEV_COMMAND.md) troubleshooting section
- Search existing issues on GitHub
- Ask in the community Discord

**Report bugs:**
```bash
# Include this info in bug reports:
java -version
./gradlew --version
dropper --version
# Plus: error messages and dropper.log
```
