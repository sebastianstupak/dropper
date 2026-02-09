# Importing Existing Mods

Dropper can import existing Fabric, Forge, and NeoForge mods into its multi-loader structure.

## Commands

### Import Fabric Mod

```bash
dropper import fabric <PATH> [--target <OUTPUT>]
```

Import an existing Fabric mod project.

**Arguments:**
- `PATH` - Path to existing Fabric mod project

**Options:**
- `--target, -t` - Target directory for imported project (default: `<source>-dropper`)

**Example:**
```bash
dropper import fabric ./my-fabric-mod
cd my-fabric-mod-dropper
./gradlew build
```

### Import Forge Mod

```bash
dropper import forge <PATH> [--target <OUTPUT>]
```

Import an existing Forge mod project.

**Example:**
```bash
dropper import forge ./my-forge-mod --target ./my-dropper-mod
```

### Import NeoForge Mod

```bash
dropper import neoforge <PATH> [--target <OUTPUT>]
```

Import an existing NeoForge mod project.

### Convert to Multi-Loader

```bash
dropper import convert --from <LOADER>
```

Convert an existing Dropper project to support multiple loaders.

**Options:**
- `--from` - Source loader (fabric, forge, neoforge)

**Example:**
```bash
cd my-dropper-mod
dropper import convert --from fabric
```

## What Gets Imported?

When you import a mod, Dropper:

1. **Analyzes** the source project structure
2. **Extracts** metadata (mod ID, version, dependencies)
3. **Maps** files to Dropper structure:
   - Java sources → `shared/common/`
   - Loader-specific code → `shared/fabric/forge/neoforge/`
   - Assets → `versions/shared/v1/assets/`
   - Data → `versions/shared/v1/data/`
4. **Generates** Dropper build system
5. **Creates** config.yml with detected settings

## Project Structure Mapping

### Before (Fabric)
```
my-fabric-mod/
├── src/main/
│   ├── java/com/mymod/
│   │   ├── MyMod.java
│   │   └── items/RubyItem.java
│   └── resources/
│       ├── fabric.mod.json
│       ├── assets/mymod/
│       └── data/mymod/
├── build.gradle
└── gradle.properties
```

### After (Dropper)
```
my-fabric-mod-dropper/
├── config.yml
├── buildSrc/
├── shared/
│   ├── common/
│   │   └── src/main/java/com/mymod/
│   │       ├── MyMod.java
│   │       └── items/RubyItem.java
│   └── fabric/
│       └── src/main/java/com/mymod/platform/
├── versions/
│   ├── shared/v1/
│   │   ├── assets/mymod/
│   │   └── data/mymod/
│   └── 1_20_1/
│       ├── config.yml
│       └── fabric/
└── build.gradle.kts
```

## Import Requirements

### Fabric Mods
- Must have `fabric.mod.json` in `src/main/resources/`
- Source files in `src/main/java/`

### Forge Mods
- Must have `META-INF/mods.toml` in `src/main/resources/`
- Uses ForgeGradle build system

### NeoForge Mods
- Must have `META-INF/neoforge.mods.toml` in `src/main/resources/`
- Uses NeoGradle build system

## After Importing

1. **Review** the imported project structure
2. **Test** that the mod builds: `./gradlew build`
3. **Verify** assets and data were mapped correctly
4. **Add** additional loaders if desired:
   ```bash
   dropper create version 1.20.1 --loaders forge,neoforge
   ```

## Common Issues

### Metadata Not Found

If the importer can't find `fabric.mod.json`, `mods.toml`, or `neoforge.mods.toml`:

```
Error: fabric.mod.json not found
```

**Solution:** Ensure metadata file is in `src/main/resources/`

### Package Detection Failed

If source files don't have package declarations:

```
Warning: Could not detect base package
```

**Solution:** The importer will use a default package. Update `config.yml` manually.

### Complex Build Configuration

For mods with complex Gradle configurations:

**Solution:** Import will create a standard Dropper build system. You may need to manually configure special dependencies in `buildSrc/`.

## Tips

- **Start small**: Import a simple mod first to understand the process
- **Check assets**: Verify textures and models were copied correctly
- **Test builds**: Run `./gradlew build` immediately after importing
- **Review config**: Check `config.yml` has correct mod information
- **Add loaders**: Dropper makes it easy to add Forge/NeoForge after importing Fabric

## Next Steps

After importing:

1. **Build the project**: `./gradlew build`
2. **Test in-game**: `dropper dev run`
3. **Add more loaders**: `dropper create version <MC_VERSION> --loaders forge,neoforge`
4. **Create new content**: `dropper create item my_item`

See [UPDATING.md](./UPDATING.md) for keeping dependencies up to date.
