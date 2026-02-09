# Dropper Dev Command - Complete Guide

This guide explains how the `dropper dev` command works, what happens behind the scenes, and how to use it effectively.

## Table of Contents

- [Quick Start](#quick-start)
- [How It Works](#how-it-works)
- [Commands](#commands)
- [Behind the Scenes](#behind-the-scenes)
- [First Run vs Subsequent Runs](#first-run-vs-subsequent-runs)
- [Caching and Storage](#caching-and-storage)
- [Debug Mode](#debug-mode)
- [Troubleshooting](#troubleshooting)

---

## Quick Start

```bash
# Initialize a project
dropper init my-mod

# Create an item
dropper create item diamond_pickaxe --type tool

# Launch Minecraft with your mod
dropper dev run

# That's it! Minecraft will launch with your mod loaded.
```

**First run takes 5-10 minutes** (downloads Minecraft, decompiles, sets up environment).
**Subsequent runs take 30-60 seconds** (everything cached).

---

## How It Works

### The Flow

When you run `dropper dev run`, here's what happens:

```
┌────────────────────────────────────────────────────────────┐
│ Step 1: Project Detection                                 │
├────────────────────────────────────────────────────────────┤
│ • Reads config.yml                                         │
│ • Detects available versions (e.g., 1.20.1, 1.21.1)       │
│ • Detects loaders per version (fabric, forge, neoforge)   │
│ • Selects first version/loader (or uses your --flags)     │
└────────────────────────────────────────────────────────────┘
                           ↓
┌────────────────────────────────────────────────────────────┐
│ Step 2: Gradle Task Construction                          │
├────────────────────────────────────────────────────────────┤
│ • Converts version format: 1.20.1 → 1_20_1                │
│ • Constructs module ID: 1_20_1-fabric                     │
│ • Builds Gradle command:                                   │
│   ./gradlew :1_20_1-fabric:runClient                      │
└────────────────────────────────────────────────────────────┘
                           ↓
┌────────────────────────────────────────────────────────────┐
│ Step 3: Gradle Execution                                  │
├────────────────────────────────────────────────────────────┤
│ • Gradle plugin takes over (Fabric Loom / ForgeGradle)    │
│ • Downloads Minecraft (if not cached)                     │
│ • Applies mappings (deobfuscation)                        │
│ • Decompiles Minecraft source (first time)                │
│ • Compiles your mod                                       │
│ • Sets up run configuration                               │
└────────────────────────────────────────────────────────────┘
                           ↓
┌────────────────────────────────────────────────────────────┐
│ Step 4: Minecraft Launch                                  │
├────────────────────────────────────────────────────────────┤
│ • Minecraft launches with your mod in classpath           │
│ • Mod is loaded by the mod loader                         │
│ • Game window opens!                                      │
└────────────────────────────────────────────────────────────┘
```

### What Dropper Does

Dropper is a **smart wrapper** around Gradle:

1. **Reads your project config** → Knows what versions and loaders you have
2. **Constructs correct Gradle command** → Targets the right module and task
3. **Passes through to Gradle** → Gradle plugin does the heavy lifting

### What Gradle Does

The Gradle plugins (Fabric Loom, ForgeGradle, NeoGradle) handle:

- ✅ Downloading Minecraft
- ✅ Deobfuscating Minecraft (applying mappings)
- ✅ Decompiling Minecraft source code
- ✅ Setting up the development environment
- ✅ Compiling your mod
- ✅ Injecting your mod into Minecraft's classpath
- ✅ Launching Minecraft

**You don't manage any of this manually!**

---

## Commands

### `dropper dev run`

Launch Minecraft with your mod (auto-detects version/loader).

```bash
# Auto-detect (uses first version/loader in config)
dropper dev run

# Specific version and loader
dropper dev run --version 1.20.1 --loader fabric

# With debug mode
dropper dev run --debug --port 5005

# Clean build and run
dropper dev run --clean

# Server instead of client
dropper dev run --server
```

**Options:**
- `--version, -v <VERSION>` - Minecraft version (e.g., 1.20.1)
- `--loader, -l <LOADER>` - Mod loader (fabric, forge, neoforge)
- `--debug, -d` - Enable remote debugging
- `--port, -p <PORT>` - Debug port (default: 5005)
- `--clean, -c` - Clean build before running
- `--server, -s` - Launch server instead of client

### `dropper dev client`

Launch Minecraft client (same as `dropper dev run`).

```bash
dropper dev client
dropper dev client --version 1.21.1 --loader neoforge
```

### `dropper dev server`

Launch Minecraft dedicated server.

```bash
dropper dev server
dropper dev server --version 1.20.1 --loader fabric
```

**Note:** Server requires accepting EULA on first run.

### `dropper dev test`

Run your mod's unit tests.

```bash
# Run tests
dropper dev test

# Specific version/loader
dropper dev test --version 1.20.1 --loader fabric

# Clean and test
dropper dev test --clean
```

### `dropper dev reload`

Show hot reload instructions (for resource-only changes).

```bash
dropper dev reload
```

Output:
```
Hot Reload (Resource Changes Only)
==================================

For JSON/PNG/asset changes (no code changes):
1. Keep Minecraft running
2. Edit your files in versions/shared/v1/
3. In-game, press F3 + T or run: /reload

For code changes (.java files):
1. Stop Minecraft (Ctrl+C in terminal)
2. Make your changes
3. Run: dropper dev run
```

---

## Behind the Scenes

### What Happens on First Run

```
[Dropper] Reading config.yml...
[Dropper] Detected versions: 1.20.1, 1.21.1
[Dropper] Detected loaders for 1.20.1: fabric, neoforge
[Dropper] Using: 1.20.1 with fabric
[Dropper] Running: ./gradlew :1_20_1-fabric:runClient

[Gradle] Downloading Minecraft 1.20.1... (300MB)
[Gradle] > Download complete (3 minutes)

[Gradle] Downloading Fabric loader... (10MB)
[Gradle] > Download complete (10 seconds)

[Gradle] Downloading Fabric API... (5MB)
[Gradle] > Download complete (5 seconds)

[Gradle] Applying Minecraft mappings...
[Gradle] > Remapping Minecraft with Yarn mappings (2 minutes)

[Gradle] Decompiling Minecraft...
[Gradle] > Decompiling with FernFlower (2 minutes)

[Gradle] Compiling mod...
[Gradle] > Compiling Java sources (30 seconds)

[Gradle] Setting up run configuration...
[Gradle] > Preparing Minecraft launch (10 seconds)

[Gradle] Starting Minecraft...

[Minecraft] Loading mod: my-mod 1.0.0
[Minecraft] Mod initialization complete
[Minecraft] Opening game window...
```

**Total time: ~5-10 minutes** (only once per Minecraft version!)

### What Happens on Subsequent Runs

```
[Dropper] Using: 1.20.1 with fabric
[Dropper] Running: ./gradlew :1_20_1-fabric:runClient

[Gradle] Using cached Minecraft 1.20.1
[Gradle] Using cached mappings
[Gradle] Using cached dependencies

[Gradle] Compiling mod...
[Gradle] > Checking for changes... (5 seconds)
[Gradle] > No changes detected, using cached build

[Gradle] Starting Minecraft...

[Minecraft] Loading mod: my-mod 1.0.0
[Minecraft] Opening game window...
```

**Total time: ~30-60 seconds** (incremental builds!)

---

## First Run vs Subsequent Runs

### First Run (per Minecraft version)

**Time:** 5-10 minutes
**Network:** ~500MB - 1GB download
**CPU:** High (decompiling Minecraft)
**Disk:** Writes to `~/.gradle/caches/`

What happens:
1. Downloads Minecraft JAR
2. Downloads mod loader and dependencies
3. Downloads and applies mappings (deobfuscation)
4. Decompiles Minecraft source code
5. Compiles your mod
6. Launches Minecraft

**Cache location:**
- Windows: `C:\Users\<You>\.gradle\caches\fabric-loom\` (or `forge` / `neoforge`)
- macOS: `~/.gradle/caches/fabric-loom/`
- Linux: `~/.gradle/caches/fabric-loom/`

### Subsequent Runs (same version)

**Time:** 30-60 seconds
**Network:** None (cache hit!)
**CPU:** Low (incremental compilation only)
**Disk:** Reads from cache

What happens:
1. ✅ Minecraft cached (skip download)
2. ✅ Mappings cached (skip remapping)
3. ✅ Decompiled source cached (skip decompiling)
4. Recompile mod (only if changed)
5. Launch Minecraft

**Incremental builds:** If you didn't change any code, even compilation is skipped!

---

## Caching and Storage

### What Gets Cached?

**Gradle cache** (`~/.gradle/caches/`):
- ✅ Minecraft JARs (per version)
- ✅ Decompiled Minecraft source
- ✅ Mappings (Yarn, MCP, Mojmap)
- ✅ Mod loader dependencies
- ✅ Libraries (Guava, LWJGL, etc.)

**Build cache** (your project's `build/` directory):
- ✅ Compiled mod classes
- ✅ Processed resources
- ✅ Run configurations

### Cache Size

Approximate sizes:

| Item | Size | Location |
|------|------|----------|
| Minecraft JAR (per version) | ~50MB | `~/.gradle/caches/fabric-loom/minecraft-merged.jar` |
| Decompiled source | ~200MB | `~/.gradle/caches/fabric-loom/minecraft-sources.jar` |
| Mappings | ~10MB | `~/.gradle/caches/fabric-loom/mappings/` |
| Dependencies | ~100MB | `~/.gradle/caches/modules-2/` |
| **Total per version** | **~500MB** | |

**Multiple projects share the same cache!**
- 1st project: 500MB setup
- 2nd project: 0MB (cache reused!)

### Clearing Cache

If you encounter issues, clean the cache:

```bash
# Clean your project's build output
./gradlew clean

# Clean Fabric Loom cache
./gradlew cleanCache

# Clean all Gradle caches (nuclear option, forces re-download)
rm -rf ~/.gradle/caches/fabric-loom/
rm -rf ~/.gradle/caches/forge_gradle/
```

**When to clear cache:**
- Minecraft updates (e.g., 1.20.1 → 1.20.2)
- Mod loader updates
- Corrupted downloads
- Mapping changes

---

## Debug Mode

Debug mode allows you to attach a debugger to Minecraft and step through your code.

### Start in Debug Mode

```bash
dropper dev run --debug --port 5005
```

Output:
```
[Dropper] Starting in debug mode on port 5005
[Dropper] Waiting for debugger to attach...
[Dropper] Connect your IDE debugger to: localhost:5005

Listening for transport dt_socket at address: 5005
```

**Minecraft will pause until you attach a debugger!**

### Attach Debugger (IntelliJ IDEA)

1. **Run dropper in debug mode:**
   ```bash
   dropper dev run --debug --port 5005
   ```

2. **In IntelliJ IDEA:**
   - Run → Edit Configurations
   - Click `+` → Remote JVM Debug
   - Name: `Minecraft Debug`
   - Host: `localhost`
   - Port: `5005`
   - Click OK

3. **Start debugging:**
   - Set breakpoints in your mod code
   - Click Debug (Shift+F9)
   - Minecraft will continue and stop at breakpoints

### Attach Debugger (VS Code)

1. **Run dropper in debug mode:**
   ```bash
   dropper dev run --debug --port 5005
   ```

2. **Add to `.vscode/launch.json`:**
   ```json
   {
     "version": "0.2.0",
     "configurations": [
       {
         "type": "java",
         "name": "Attach to Minecraft",
         "request": "attach",
         "hostName": "localhost",
         "port": 5005
       }
     ]
   }
   ```

3. **Start debugging:**
   - Set breakpoints
   - Press F5 (Run → Start Debugging)
   - Select "Attach to Minecraft"

### Attach Debugger (Eclipse)

1. **Run dropper in debug mode:**
   ```bash
   dropper dev run --debug --port 5005
   ```

2. **In Eclipse:**
   - Run → Debug Configurations
   - Right-click Remote Java Application → New
   - Project: Select your project
   - Host: `localhost`
   - Port: `5005`
   - Click Debug

### Custom Debug Port

If port 5005 is in use, use a different port:

```bash
dropper dev run --debug --port 9999
```

Then configure your IDE debugger to use port `9999` instead.

---

## Troubleshooting

### "No config.yml found in current directory"

**Cause:** Not in a Dropper project directory

**Solution:**
```bash
# Check if you're in the right directory
ls config.yml

# If not found, navigate to your project:
cd path/to/my-mod

# Or create a new project:
dropper init my-new-mod
cd my-new-mod
```

### "Version 1.20.1 not found in project"

**Cause:** Requested version not configured

**Solution:**
```bash
# Check available versions
ls versions/

# Add the version if needed
dropper add version 1.20.1

# Or use an existing version
dropper dev run --version 1.21.1
```

### "Loader fabric not available for version 1.20.1"

**Cause:** Loader not configured for that version

**Solution:**
```bash
# Check version config
cat versions/1_20_1/config.yml

# Add loader to loaders list:
# loaders: [fabric, forge, neoforge]

# Or use a different loader
dropper dev run --version 1.20.1 --loader neoforge
```

### "Gradle wrapper not found"

**Cause:** Missing `gradlew` / `gradlew.bat`

**Solution:**
```bash
# Regenerate wrapper (requires Gradle installed)
gradle wrapper

# Or re-initialize project
dropper init my-mod --force
```

### "Address already in use: 5005"

**Cause:** Debug port already occupied

**Solution:**
```bash
# Use a different port
dropper dev run --debug --port 5006

# Or kill the process using port 5005:
# Linux/macOS:
lsof -i :5005
kill <PID>

# Windows:
netstat -ano | findstr :5005
taskkill /PID <PID> /F
```

### "OutOfMemoryError: Java heap space"

**Cause:** Gradle or Minecraft ran out of memory

**Solution:**
```bash
# Increase Gradle memory in gradle.properties:
echo "org.gradle.jvmargs=-Xmx4G" >> gradle.properties

# Or globally in ~/.gradle/gradle.properties
```

### "Could not resolve dependencies"

**Cause:** Network issues or repository down

**Solution:**
```bash
# Retry with refresh
./gradlew build --refresh-dependencies

# Check internet connection
ping maven.fabricmc.net

# Check firewall/proxy settings

# If behind proxy, configure in ~/.gradle/gradle.properties:
systemProp.http.proxyHost=yourproxy.com
systemProp.http.proxyPort=8080
```

### "Minecraft crashes on launch"

**Possible causes:**
1. Incompatible Minecraft version
2. Mod loader version mismatch
3. Missing dependencies
4. Code errors in your mod

**Solution:**
```bash
# Check Minecraft version matches your mod
cat versions/1_20_1/config.yml

# Check crash report in logs:
cat logs/latest.log

# Run with verbose output
dropper dev run --debug

# Test with clean world
dropper dev run --clean
```

### Slow first-time setup

**Cause:** Downloading ~500MB - 1GB

**Solutions:**
- **Be patient:** First run takes 5-10 minutes (one-time per version)
- **Better internet:** Use wired connection if possible
- **Maven mirror:** Configure faster Maven repository (if in Asia, use Aliyun)
- **Background task:** Run `dropper dev run` and work on something else

### Slow subsequent runs

**Cause:** Usually disk I/O or antivirus

**Solutions:**
```bash
# Clean old build artifacts
./gradlew clean

# Disable antivirus scan for:
# - ~/.gradle/caches/
# - Your project's build/ directory

# Use SSD instead of HDD (if possible)

# Enable Gradle daemon (faster builds)
echo "org.gradle.daemon=true" >> gradle.properties
```

---

## Tips and Best Practices

### 1. Default Version/Loader

The first version and loader in your config are used by default:

```yaml
# config.yml
minecraft_versions: [1.20.1, 1.21.1, 1.19.4]
loaders: [fabric, neoforge, forge]
```

Running `dropper dev run` → Uses `1.20.1` with `fabric`

### 2. Switching Versions Easily

```bash
# Test in 1.20.1
dropper dev run --version 1.20.1

# Test in 1.21.1
dropper dev run --version 1.21.1

# Test different loaders
dropper dev run --version 1.20.1 --loader fabric
dropper dev run --version 1.20.1 --loader neoforge
```

### 3. Hot Reload (Resource Changes)

For **JSON/PNG/asset-only changes** (no code):

```bash
# 1. Keep Minecraft running
# 2. Edit files in versions/shared/v1/assets/
# 3. In-game: Press F3 + T  (or run /reload)
```

For **code changes** (`.java` files):
```bash
# 1. Stop Minecraft (Ctrl+C)
# 2. Edit code
# 3. dropper dev run  (recompiles and relaunches)
```

### 4. Test First

Always run tests before launching:

```bash
# Run tests, then launch
dropper dev test && dropper dev run
```

### 5. Clean Builds

If you encounter weird issues:

```bash
# Clean build artifacts
dropper dev run --clean

# Or manually:
./gradlew clean
dropper dev run
```

### 6. Multiple Minecraft Instances

Run different versions simultaneously:

```bash
# Terminal 1: Launch 1.20.1
dropper dev run --version 1.20.1

# Terminal 2: Launch 1.21.1 (different port if debugging)
dropper dev run --version 1.21.1 --debug --port 5006
```

---

## Performance Tips

### Speed Up Gradle

Add to `gradle.properties`:
```properties
# Use Gradle daemon (faster builds)
org.gradle.daemon=true

# Enable caching
org.gradle.caching=true

# Parallel builds (if multi-module)
org.gradle.parallel=true

# More memory = faster builds
org.gradle.jvmargs=-Xmx4G
```

### Speed Up Minecraft Launch

Add JVM arguments for faster Minecraft startup:
```bash
# In your run configuration (buildSrc handles this automatically)
# But you can customize in buildSrc/src/main/kotlin/ModLoaderPlugin.kt
```

### Use SSD

- Store `~/.gradle/caches/` on SSD (faster I/O)
- Store your project on SSD

---

## Getting Help

### Documentation

- [PREREQUISITES.md](PREREQUISITES.md) - Installation requirements
- [DEV_COMMAND_QUICK_REFERENCE.md](DEV_COMMAND_QUICK_REFERENCE.md) - Command cheat sheet
- [DROPPER_ARCHITECTURE.md](../DROPPER_ARCHITECTURE.md) - Architecture details

### Check Versions

```bash
java -version           # Java version
./gradlew --version     # Gradle version
dropper --version       # Dropper version
```

### Community

- GitHub Issues: Report bugs
- Discord: Get help from community

### Reporting Bugs

Include this information:
```bash
java -version
./gradlew --version
dropper --version

# Include error messages and logs
cat logs/latest.log
```

---

## Next Steps

Now that you understand how `dropper dev` works:

1. **Create items/blocks:**
   ```bash
   dropper create item my_item
   dropper create block my_block
   ```

2. **Launch and test:**
   ```bash
   dropper dev run
   ```

3. **Debug your mod:**
   ```bash
   dropper dev run --debug
   # Attach debugger from your IDE
   ```

4. **Run tests:**
   ```bash
   dropper dev test
   ```

Happy modding!
