# Packaging Guide

This guide explains how to package your Dropper mods for distribution on different platforms.

## Overview

Dropper provides comprehensive packaging commands to create distribution-ready packages for:
- Modrinth
- CurseForge
- Universal bundles
- Universal JARs (planned)

## Quick Start

```bash
# Build your mod first
dropper build

# Package for Modrinth
dropper package modrinth

# Package for CurseForge
dropper package curseforge

# Create a bundle with all versions/loaders
dropper package bundle
```

## Commands

### `dropper package modrinth`

Creates a package optimized for Modrinth distribution.

**Features:**
- Includes `modrinth.json` metadata
- Auto-detects project icon
- Includes README, CHANGELOG, and LICENSE
- Supports version and loader filtering

**Usage:**
```bash
dropper package modrinth [OPTIONS]
```

**Options:**
- `--output <DIR>` - Output directory (default: `build/packages`)
- `--include-sources` - Include source JARs
- `--include-javadoc` - Include javadoc JARs
- `--versions <VERSIONS>` - Comma-separated list of versions to include
- `--loaders <LOADERS>` - Comma-separated list of loaders to include

**Examples:**
```bash
# Basic package
dropper package modrinth

# Include sources and javadoc
dropper package modrinth --include-sources --include-javadoc

# Package only Fabric for 1.20.1
dropper package modrinth --versions 1.20.1 --loaders fabric

# Custom output directory
dropper package modrinth --output dist/modrinth
```

**Output:**
```
build/packages/modrinth/
└── mymod-1.0.0-modrinth.zip
    ├── modrinth.json          # Modrinth metadata
    ├── mymod-1.20.1-fabric.jar
    ├── mymod-1.20.1-forge.jar
    ├── mymod-1.20.1-neoforge.jar
    ├── README.md
    ├── CHANGELOG.md
    ├── LICENSE
    └── icon.png               # If present
```

### `dropper package curseforge`

Creates a package optimized for CurseForge distribution.

**Features:**
- Includes `manifest.json` with CurseForge format
- Supports modpack manifest structure
- Includes README, CHANGELOG, and LICENSE
- Supports version and loader filtering

**Usage:**
```bash
dropper package curseforge [OPTIONS]
```

**Options:**
- `--output <DIR>` - Output directory (default: `build/packages`)
- `--include-sources` - Include source JARs
- `--include-javadoc` - Include javadoc JARs
- `--versions <VERSIONS>` - Comma-separated list of versions to include
- `--loaders <LOADERS>` - Comma-separated list of loaders to include

**Examples:**
```bash
# Basic package
dropper package curseforge

# Package specific version
dropper package curseforge --versions 1.21.1

# Include sources
dropper package curseforge --include-sources
```

**Output:**
```
build/packages/curseforge/
└── mymod-1.0.0-curseforge.zip
    ├── manifest.json           # CurseForge manifest
    ├── mymod-1.20.1-fabric.jar
    ├── mymod-1.20.1-forge.jar
    ├── mymod-1.20.1-neoforge.jar
    ├── README.md
    ├── CHANGELOG.md
    └── LICENSE
```

### `dropper package bundle`

Creates a bundle containing all versions and loaders in a single ZIP file.

**Features:**
- Organized directory structure by version/loader
- Includes `BUNDLE_INFO.txt` with package details
- Includes README, CHANGELOG, and LICENSE
- Perfect for GitHub releases

**Usage:**
```bash
dropper package bundle [OPTIONS]
```

**Options:**
- `--output <DIR>` - Output directory (default: `build/packages`)
- `--include-sources` - Include source JARs
- `--include-javadoc` - Include javadoc JARs
- `--versions <VERSIONS>` - Comma-separated list of versions to include
- `--loaders <LOADERS>` - Comma-separated list of loaders to include

**Examples:**
```bash
# Bundle everything
dropper package bundle

# Bundle only Fabric loaders
dropper package bundle --loaders fabric

# Bundle with sources and javadoc
dropper package bundle --include-sources --include-javadoc
```

**Output:**
```
build/packages/bundle/
└── mymod-1.0.0-bundle.zip
    ├── BUNDLE_INFO.txt
    ├── README.md
    ├── CHANGELOG.md
    ├── LICENSE
    └── 1_20_1/
        ├── fabric/
        │   └── mymod-1.20.1-fabric.jar
        ├── forge/
        │   └── mymod-1.20.1-forge.jar
        └── neoforge/
            └── mymod-1.20.1-neoforge.jar
```

### `dropper package universal`

Creates a universal JAR with all loaders merged (not yet implemented).

**Status:** Not implemented

This command is planned for future releases. Currently, if you need a universal JAR:
1. Use `dropper package bundle` to get all loaders
2. Manually merge JARs using a shade plugin
3. Or distribute separate JARs for each loader

## Metadata Files

### Modrinth Metadata (`modrinth.json`)

Automatically generated from your `config.yml`:

```json
{
  "project_id": "mymod",
  "version_number": "1.0.0",
  "name": "My Awesome Mod",
  "description": "A cool mod that does cool things",
  "files": [
    {
      "path": "mymod-1.20.1-fabric.jar",
      "file_type": "mod"
    }
  ],
  "loaders": ["fabric", "forge", "neoforge"],
  "game_versions": ["1.20.1", "1.21.1"]
}
```

### CurseForge Manifest (`manifest.json`)

Automatically generated from your `config.yml`:

```json
{
  "minecraft": {
    "version": "1.20.1",
    "modLoaders": [
      {"id": "fabric", "primary": true},
      {"id": "forge", "primary": true},
      {"id": "neoforge", "primary": true}
    ]
  },
  "manifestType": "minecraftModpack",
  "manifestVersion": 1,
  "name": "My Awesome Mod",
  "version": "1.0.0",
  "author": "YourName",
  "files": []
}
```

### Bundle Info (`BUNDLE_INFO.txt`)

Automatically generated:

```
My Awesome Mod v1.0.0
=====================

A cool mod that does cool things

Author: YourName
License: MIT

Included Files:
---------------
  - mymod-1.20.1-fabric.jar
  - mymod-1.20.1-forge.jar
  - mymod-1.20.1-neoforge.jar
  - mymod-1.21.1-fabric.jar
  - mymod-1.21.1-forge.jar
  - mymod-1.21.1-neoforge.jar

This bundle contains all versions and loaders for My Awesome Mod.
Please install the appropriate JAR file for your Minecraft version and mod loader.
```

## Workflow

### Recommended Packaging Workflow

1. **Develop your mod:**
   ```bash
   dropper create item ruby_sword
   dropper create block ruby_ore
   ```

2. **Test your mod:**
   ```bash
   dropper dev client
   ```

3. **Build all versions:**
   ```bash
   dropper build
   ```

4. **Package for distribution:**
   ```bash
   # For Modrinth
   dropper package modrinth

   # For CurseForge
   dropper package curseforge

   # For GitHub releases
   dropper package bundle
   ```

5. **Upload to platforms:**
   - Upload `*-modrinth.zip` to Modrinth
   - Upload `*-curseforge.zip` to CurseForge
   - Attach `*-bundle.zip` to GitHub release

### CI/CD Integration

Example GitHub Actions workflow:

```yaml
name: Release

on:
  push:
    tags:
      - 'v*'

jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Setup Java
        uses: actions/setup-java@v3
        with:
          java-version: '17'

      - name: Build mod
        run: ./gradlew build

      - name: Package for Modrinth
        run: dropper package modrinth

      - name: Package for CurseForge
        run: dropper package curseforge

      - name: Create bundle
        run: dropper package bundle

      - name: Create Release
        uses: softprops/action-gh-release@v1
        with:
          files: |
            build/packages/modrinth/*.zip
            build/packages/curseforge/*.zip
            build/packages/bundle/*.zip
```

## Advanced Usage

### Filtering by Version and Loader

Package only specific combinations:

```bash
# Only 1.20.1 Fabric
dropper package bundle --versions 1.20.1 --loaders fabric

# Multiple versions, single loader
dropper package bundle --versions 1.20.1,1.21.1 --loaders fabric

# Multiple loaders, single version
dropper package bundle --versions 1.20.1 --loaders fabric,forge
```

### Including Development Artifacts

```bash
# Include source code
dropper package modrinth --include-sources

# Include API documentation
dropper package modrinth --include-javadoc

# Include both
dropper package modrinth --include-sources --include-javadoc
```

### Custom Output Directories

```bash
# Package to custom directory
dropper package modrinth --output dist/releases

# Package to version-specific directory
dropper package bundle --output releases/v1.0.0
```

## Project Structure Requirements

For packaging to work correctly, ensure:

1. **Build directory exists:**
   ```
   build/
   └── <version>/
       └── <loader>/
           └── libs/
               └── mymod-<version>-<loader>.jar
   ```

2. **Optional files (auto-included if present):**
   ```
   README.md
   CHANGELOG.md
   LICENSE
   icon.png
   ```

3. **Config file:**
   ```yaml
   # config.yml
   id: mymod
   name: My Awesome Mod
   version: 1.0.0
   description: A cool mod
   author: YourName
   license: MIT
   ```

## Troubleshooting

### Package is empty or missing JARs

**Problem:** Package created but contains no JARs.

**Solution:**
1. Build first: `dropper build`
2. Verify JARs exist in `build/` directory
3. Check version/loader filters

### Metadata is incorrect

**Problem:** `modrinth.json` or `manifest.json` has wrong values.

**Solution:**
1. Check `config.yml` has correct metadata
2. Ensure `id`, `name`, `version`, `author`, `license` are set
3. Re-run packaging command

### Sources/Javadoc not included

**Problem:** Source or javadoc JARs missing from package.

**Solution:**
1. Build with sources/javadoc: `./gradlew build sourcesJar javadocJar`
2. Use `--include-sources` or `--include-javadoc` flags
3. Verify source/javadoc JARs exist in build directory

### Universal package error

**Problem:** `dropper package universal` throws error.

**Solution:**
Universal packaging is not yet implemented. Use `dropper package bundle` instead to get all loaders in a single ZIP.

## Best Practices

1. **Always build before packaging:**
   ```bash
   dropper build && dropper package modrinth
   ```

2. **Version your releases:**
   - Update `version` in `config.yml`
   - Update `CHANGELOG.md`
   - Tag releases: `git tag v1.0.0`

3. **Test packages before upload:**
   ```bash
   # Extract and verify contents
   unzip -l build/packages/modrinth/mymod-1.0.0-modrinth.zip
   ```

4. **Include documentation:**
   - Keep `README.md` up to date
   - Document changes in `CHANGELOG.md`
   - Include `LICENSE` file

5. **Use consistent naming:**
   - Follow semver: `1.0.0`, `1.1.0`, `2.0.0`
   - Keep mod ID lowercase and hyphenated
   - Use descriptive mod names

## Platform-Specific Tips

### Modrinth

- Set `project_id` to match your Modrinth project slug
- Include icon (128x128 PNG recommended)
- Add gallery images to project root (auto-detected)
- Use markdown formatting in description

### CurseForge

- Ensure `manifestType` is `minecraftModpack`
- Set correct `manifestVersion` (usually 1)
- Add dependencies to `files` section manually if needed
- Include screenshots in ZIP root

### GitHub Releases

- Use bundle package for GitHub releases
- Include `BUNDLE_INFO.txt` for user guidance
- Tag releases consistently: `v1.0.0`
- Add release notes from `CHANGELOG.md`

## See Also

- [Build Guide](BUILD.md) - Building your mod
- [Development Guide](DEVELOPMENT.md) - Development workflow
- [Distribution Guide](DISTRIBUTION.md) - Uploading to platforms
