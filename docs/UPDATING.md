# Updating Dependencies

Keep your Dropper project up to date with the latest Minecraft versions, mod loaders, and dependencies.

## Commands

### Check for Updates

```bash
dropper update check
```

Check for available updates without applying them.

**Example output:**
```
Found 3 update(s) available:

  ✓ Fabric Loader (1_20_1)
     Current: 0.14.0
     Latest:  0.16.9

  ✓ Fabric API (1_20_1)
     Current: 0.80.0+1.20.1
     Latest:  0.92.2+1.20.1

  ⚠️ Minecraft (1_20_1)
     Current: 1.20.1
     Latest:  1.21.1
     ⚠️ Warning: This is a breaking change

Run 'dropper update apply --all' to apply all updates
```

### Update Minecraft

```bash
dropper update minecraft
```

Update Minecraft to the latest stable version.

**Warning:** Minecraft updates are often breaking changes. Test thoroughly after updating.

### Update Loaders

```bash
dropper update loaders
```

Update all mod loaders (Fabric, Forge, NeoForge) to their latest versions.

**Individual loaders:**
```bash
# Update only Fabric Loader
dropper update loaders --fabric

# Update only Forge
dropper update loaders --forge

# Update only NeoForge
dropper update loaders --neoforge
```

### Update Dependencies

```bash
dropper update dependencies
```

Update all dependencies to their latest compatible versions.

**Example:**
```bash
# Update all dependencies
dropper update dependencies

# Update only Fabric API
dropper update dependencies --fabric-api
```

### Apply All Updates

```bash
dropper update apply --all
```

Apply all available updates at once.

**Example:**
```bash
dropper update check          # See what's available
dropper update apply --all    # Apply everything
```

## What Gets Updated?

Dropper manages versions in `versions/{mc_version}/config.yml`:

```yaml
minecraft_version: "1.20.1"
asset_pack: "v1"
loaders: [fabric, forge, neoforge]
java_version: 21

# Loader versions
neoforge_version: "21.1.0"
forge_version: "51.0.0"
fabric_loader_version: "0.16.9"
fabric_api_version: "0.92.2+1.20.1"
```

## Update Types

### Non-Breaking Updates

These are safe to apply:

- **Fabric Loader**: Patch/minor versions (0.14.0 → 0.16.9)
- **Fabric API**: Patch versions for same MC version
- **Forge**: Patch versions (51.0.0 → 51.0.5)
- **NeoForge**: Patch versions

### Breaking Updates

These require testing:

- **Minecraft**: Any version change (1.20.1 → 1.21.0)
- **Major loader versions**: API changes
- **Java version**: Requires code changes

## Version Resolution

Dropper queries these sources for latest versions:

- **Minecraft**: [Mojang Version Manifest](https://launchermeta.mojang.com/mc/game/version_manifest.json)
- **Fabric Loader**: [Fabric Meta API](https://meta.fabricmc.net/v2/versions/loader)
- **Fabric API**: [Modrinth API](https://api.modrinth.com/v2/project/fabric-api/version)
- **Forge**: [MinecraftForge Files](https://files.minecraftforge.net/)
- **NeoForge**: [NeoForged Maven](https://maven.neoforged.net/releases/)

## Update Workflow

### Recommended Process

1. **Check** for updates:
   ```bash
   dropper update check
   ```

2. **Review** the output - note breaking changes

3. **Update non-breaking** first:
   ```bash
   dropper update loaders
   dropper update dependencies
   ```

4. **Test** the mod:
   ```bash
   dropper dev run
   ./gradlew test
   ```

5. **Update breaking** (e.g., Minecraft):
   ```bash
   dropper update minecraft
   ```

6. **Test thoroughly** after breaking updates

### Automated Updates

For CI/CD pipelines:

```bash
# Check if updates available (exit code 0 = no updates, 1 = updates available)
dropper update check --quiet

# Apply all non-breaking updates
dropper update loaders
dropper update dependencies

# Run tests
./gradlew test
```

## Version Constraints

### Minecraft Version Compatibility

Different Minecraft versions require specific loader versions:

| Minecraft | Fabric Loader | Forge         | NeoForge      |
|-----------|---------------|---------------|---------------|
| 1.20.1    | 0.14.0+       | 47.0.0+       | 47.1.0+       |
| 1.20.4    | 0.15.0+       | 49.0.0+       | 20.4.0+       |
| 1.21.0    | 0.15.0+       | 51.0.0+       | 21.0.0+       |
| 1.21.1    | 0.16.0+       | 51.0.0+       | 21.1.0+       |

Dropper automatically resolves compatible versions.

### Java Version Requirements

| Minecraft | Java Version |
|-----------|--------------|
| 1.20.1    | 17+          |
| 1.20.4    | 17+          |
| 1.21.0    | 21+          |
| 1.21.1    | 21+          |

Update `java_version` in `versions/{mc_version}/config.yml` if needed.

## Handling Update Failures

### Rollback

If an update breaks your mod:

1. **Restore** from git:
   ```bash
   git checkout versions/{mc_version}/config.yml
   ```

2. **Or** manually edit `config.yml` to previous versions

### Partial Failures

If some updates succeed and others fail:

```bash
dropper update check  # See what's still outdated
```

Dropper applies updates atomically when possible.

## Common Issues

### Incompatible Versions

```
Error: Forge 51.0.0 is not compatible with Minecraft 1.20.1
```

**Solution:** Dropper will suggest compatible versions. Update Minecraft first or use an older Forge version.

### Missing Dependencies

```
Error: Fabric API version not found for Minecraft 1.21.5
```

**Solution:** You're using an unsupported Minecraft version. Stick to stable releases.

### Network Issues

```
Error: Failed to fetch version information
```

**Solution:** Check internet connection. Dropper caches version data locally.

## Best Practices

1. **Check regularly**: Run `dropper update check` weekly
2. **Update incrementally**: Don't jump multiple MC versions at once
3. **Test after updates**: Always run `./gradlew test` and `dropper dev run`
4. **Read changelogs**: Check loader/API changelogs for breaking changes
5. **Use version control**: Commit before updating
6. **CI/CD integration**: Automate update checks in CI

## Version Pinning

To prevent automatic updates of specific dependencies:

Edit `versions/{mc_version}/config.yml`:

```yaml
# Pin Fabric Loader to specific version
fabric_loader_version: "0.15.0"  # Won't auto-update
```

Comment in `config.yml` to indicate pinning:

```yaml
# Pinned to 0.15.0 due to compatibility issue
fabric_loader_version: "0.15.0"
```

## Next Steps

- See [IMPORTING.md](./IMPORTING.md) for importing existing mods
- Read [DEVELOPMENT.md](../DEVELOPMENT.md) for development workflows
- Check [README.md](../README.md) for general documentation

## Update Schedule

**Recommended update frequency:**

- **Minecraft**: When new stable version releases
- **Loaders**: Monthly (check for security fixes)
- **Dependencies**: Monthly
- **Fabric API**: As needed (often tied to Minecraft updates)

**Before major releases:**

```bash
dropper update check
dropper update apply --all
./gradlew build
./gradlew test
dropper dev run
```

Run the full test suite before publishing updates.
