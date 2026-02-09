# Dropper Dev Command - Quick Reference

## Prerequisites

Before using `dropper dev`:

1. **Java 21 or newer** - [Installation guide](docs/PREREQUISITES.md)
2. **Internet connection** - For downloading Minecraft and dependencies
3. **Disk space** - ~1-2GB per Minecraft version (cached in `~/.gradle/`)

**Note:** You do NOT need Minecraft installed separately! The mod loaders download
and set up Minecraft automatically when you run `dropper dev run`.

See [docs/PREREQUISITES.md](docs/PREREQUISITES.md) for detailed installation instructions.

## First Time Setup

The first time you run `dropper dev run`:
- Takes 5-10 minutes (downloads Minecraft, applies mappings)
- Requires internet connection
- Caches everything in `~/.gradle/caches/`

Subsequent runs are fast (30-60 seconds) as everything is cached.

See [docs/DEV_COMMAND.md](docs/DEV_COMMAND.md) for complete documentation.

---

## Commands

### Launch Minecraft
```bash
# Launch with auto-detection
dropper dev run

# Launch specific version and loader
dropper dev run --version 1.20.1 --loader fabric

# Launch in debug mode
dropper dev run --debug --port 5005

# Launch with clean world
dropper dev run --clean
```

### Client Only
```bash
# Launch client
dropper dev client

# Launch client with specific settings
dropper dev client --version 1.21.1 --loader neoforge --debug
```

### Server Only
```bash
# Launch server
dropper dev server

# Launch server with specific settings
dropper dev server --version 1.20.1 --loader fabric
```

### Run Tests
```bash
# Run tests
dropper dev test

# Run tests with clean build
dropper dev test --clean

# Run tests for specific version
dropper dev test --version 1.20.1 --loader fabric
```

### Hot Reload
```bash
# Show reload instructions
dropper dev reload
```

## Common Workflows

### Quick Start
```bash
# Initialize project
dropper init my-mod

# Create an item
dropper create item diamond_sword

# Launch for testing
dropper dev run
```

### Debug a Crash
```bash
# Start in debug mode
dropper dev run --debug

# Connect debugger to localhost:5005
# Set breakpoints in your IDE
```

### Test Changes
```bash
# Make code changes
# Run tests
dropper dev test

# If tests pass, launch game
dropper dev run
```

### Fresh Start
```bash
# Clean world and rebuild
dropper dev run --clean
```

## Options Reference

### Global Options (run, client, server)
- `--version, -v <VERSION>` - Minecraft version (e.g., 1.20.1)
- `--loader, -l <LOADER>` - Mod loader (fabric, forge, neoforge)
- `--debug, -d` - Enable remote debugging
- `--port, -p <PORT>` - Debug port (default: 5005)
- `--clean, -c` - Start with fresh data

### Test Options
- `--version, -v <VERSION>` - Minecraft version
- `--loader, -l <LOADER>` - Mod loader
- `--clean, -c` - Clean before testing

## File Structure

```
your-mod/
├── config.yml                 # Mod configuration
├── versions/
│   ├── 1_20_1/
│   │   ├── config.yml        # Version config (loaders listed here)
│   │   ├── common/           # Common code
│   │   ├── fabric/           # Fabric-specific
│   │   ├── forge/            # Forge-specific
│   │   └── neoforge/         # NeoForge-specific
│   └── shared/
│       └── v1/               # Asset pack
└── shared/
    └── common/               # Version-agnostic code
```

## Troubleshooting

### "No config.yml found"
- Make sure you're in a Dropper project directory
- Run `dropper init` to create a new project

### "Version X.X.X not found"
- Check available versions with `ls versions/`
- Add version with `dropper create version <version>`

### "Loader X not available"
- Check `versions/<version>/config.yml`
- Make sure loader is in the `loaders:` list

### "Gradle wrapper not found"
- Run `gradle wrapper` in project directory
- Or reinitialize project

### Debug port already in use
- Use different port: `--port 5006`
- Or stop other debug sessions

## IDE Integration

### IntelliJ IDEA
1. Run: `dropper dev run --debug`
2. Create "Remote JVM Debug" configuration
3. Host: `localhost`, Port: `5005`
4. Click "Debug"

### VS Code
1. Run: `dropper dev run --debug`
2. Add to `.vscode/launch.json`:
```json
{
  "type": "java",
  "request": "attach",
  "name": "Attach to Minecraft",
  "hostName": "localhost",
  "port": 5005
}
```
3. Start debugging (F5)

### Eclipse
1. Run: `dropper dev run --debug`
2. Run → Debug Configurations
3. Remote Java Application
4. Host: `localhost`, Port: `5005`
5. Click "Debug"

## Tips

1. **Default Version/Loader**: The first version and loader in your config are used by default

2. **Multiple Versions**: Switch between versions easily:
   ```bash
   dropper dev run --version 1.20.1
   dropper dev run --version 1.21.1
   ```

3. **Keep Game Running**: For data/resource changes only:
   ```bash
   # 1. Keep Minecraft running
   # 2. Make changes to JSON/PNG files
   # 3. Run in-game: /reload
   ```

4. **Code Changes**: Code changes require full restart:
   ```bash
   # 1. Stop Minecraft (Ctrl+C)
   # 2. Make code changes
   # 3. dropper dev run
   ```

5. **Test First**: Always run tests before launching:
   ```bash
   dropper dev test && dropper dev run
   ```

## Exit Codes

- `0` - Success
- `1` - Configuration error (missing config, invalid version)
- `>1` - Gradle/Minecraft error

## Output Prefixes

- `[Dropper]` - CLI messages
- `[Gradle]` - Gradle build output
- `[Minecraft]` - Minecraft game output
- `[Test]` - Test output

## Getting Help

```bash
# Command help
dropper dev --help

# Subcommand help
dropper dev run --help
dropper dev test --help

# Documentation
dropper docs
```
