# Publishing Guide

Complete guide to publishing mods with Dropper to Modrinth, CurseForge, and GitHub.

## Table of Contents

- [Quick Start](#quick-start)
- [Configuration](#configuration)
- [Publishing Platforms](#publishing-platforms)
- [Command Reference](#command-reference)
- [Changelog Generation](#changelog-generation)
- [Best Practices](#best-practices)
- [Troubleshooting](#troubleshooting)

## Quick Start

### 1. Configure Publish Settings

Create `.dropper/publish-config.yml` in your project:

```yaml
modrinth:
  projectId: "your-project-id"
  apiToken: "${MODRINTH_TOKEN}"

curseforge:
  projectId: 123456
  apiToken: "${CURSEFORGE_TOKEN}"

github:
  repository: "owner/repo"
  apiToken: "${GITHUB_TOKEN}"

defaults:
  releaseType: "release"
  autoChangelog: true
  gitTag: true
```

### 2. Set Environment Variables

```bash
# Linux/macOS
export MODRINTH_TOKEN="your-token"
export CURSEFORGE_TOKEN="your-token"
export GITHUB_TOKEN="your-token"

# Windows PowerShell
$env:MODRINTH_TOKEN="your-token"
$env:CURSEFORGE_TOKEN="your-token"
$env:GITHUB_TOKEN="your-token"
```

### 3. Build Your Mod

```bash
dropper build
```

### 4. Publish

```bash
# Publish to all platforms
dropper publish all

# Or publish to specific platforms
dropper publish modrinth
dropper publish curseforge
dropper publish github
```

## Configuration

### Publish Config File

Location: `.dropper/publish-config.yml`

#### Modrinth Configuration

```yaml
modrinth:
  projectId: "abc123"          # Your Modrinth project ID
  apiToken: "${MODRINTH_TOKEN}" # API token (use env vars)
```

**Getting Modrinth Credentials:**
1. Go to https://modrinth.com/dashboard
2. Create a project or select existing one
3. Note the project ID from the URL
4. Generate API token in Settings > API Tokens

#### CurseForge Configuration

```yaml
curseforge:
  projectId: 123456             # Your CurseForge project ID
  apiToken: "${CURSEFORGE_TOKEN}" # API token (use env vars)
```

**Getting CurseForge Credentials:**
1. Go to https://authors.curseforge.com/
2. Create or select your project
3. Note the numeric project ID from the URL
4. Generate API token in Account Settings > API Keys

#### GitHub Configuration

```yaml
github:
  repository: "owner/repo"      # GitHub repository (owner/name format)
  apiToken: "${GITHUB_TOKEN}"   # Personal access token or repo token
```

**Getting GitHub Credentials:**
1. Go to https://github.com/settings/tokens
2. Generate new token with `repo` scope
3. Note your repository in `owner/name` format

#### Defaults

```yaml
defaults:
  releaseType: "release"  # release, beta, or alpha
  autoChangelog: true     # Generate changelog from git commits
  gitTag: true            # Create git tag on GitHub releases
```

### Environment Variables

Use environment variables for sensitive tokens:

```yaml
# Reference environment variables with ${VAR_NAME}
apiToken: "${MODRINTH_TOKEN}"
```

The config loader automatically substitutes environment variables.

## Publishing Platforms

### Modrinth

**Features:**
- Multi-version support
- Multiple loader support
- Dependency management
- Automatic version detection
- Changelog support

**Version Metadata:**
- Game versions (e.g., "1.20.1", "1.21")
- Loaders (fabric, forge, neoforge)
- Release type (release, beta, alpha)
- Dependencies (required, optional)

**Example:**
```bash
dropper publish modrinth \
  --version 1.2.0 \
  --game-versions "1.20.1,1.21" \
  --loaders "fabric,forge" \
  --release-type release \
  --changelog CHANGELOG.md
```

### CurseForge

**Features:**
- Game version mapping
- Loader detection
- Dependency relations
- Multiple file uploads
- Release type support

**Version Metadata:**
- Game versions (mapped to CurseForge IDs)
- Loaders (Fabric, Forge, NeoForge)
- Release type (release, beta, alpha)
- Relations (required, optional, incompatible, embedded)

**Example:**
```bash
dropper publish curseforge \
  --version 1.2.0 \
  --game-versions "1.20.1" \
  --loaders "fabric" \
  --release-type beta
```

### GitHub Releases

**Features:**
- Create releases with tags
- Upload release assets
- Prerelease flag for alpha/beta
- Markdown changelog
- Multiple file uploads

**Version Metadata:**
- Tag name (e.g., "v1.2.0")
- Release type (release vs prerelease)
- Changelog as release notes

**Example:**
```bash
dropper publish github \
  --version 1.2.0 \
  --changelog CHANGELOG.md \
  --release-type release
```

## Command Reference

### `dropper publish all`

Publish to all configured platforms.

```bash
dropper publish all [OPTIONS]
```

**Options:**
- `--version <VERSION>` - Release version (default: from config.yml)
- `--changelog <FILE>` - Path to changelog file (markdown)
- `--auto-changelog` - Generate changelog from git commits
- `--game-versions <VERSIONS>` - Minecraft versions (comma-separated)
- `--loaders <LOADERS>` - Mod loaders (comma-separated)
- `--release-type <TYPE>` - alpha, beta, or release (default: release)
- `--dry-run` - Preview without actually publishing
- `--continue-on-error` - Continue if one platform fails

**Example:**
```bash
dropper publish all \
  --version 1.2.0 \
  --auto-changelog \
  --game-versions "1.20.1,1.21" \
  --loaders "fabric,forge,neoforge" \
  --release-type release
```

### `dropper publish modrinth`

Publish to Modrinth only.

```bash
dropper publish modrinth [OPTIONS]
```

Same options as `publish all`.

### `dropper publish curseforge`

Publish to CurseForge only.

```bash
dropper publish curseforge [OPTIONS]
```

Same options as `publish all`.

### `dropper publish github`

Publish to GitHub Releases only.

```bash
dropper publish github [OPTIONS]
```

Same options as `publish all`.

## Changelog Generation

### Auto-Generated from Git

Enable `autoChangelog: true` in config or use `--auto-changelog` flag:

```bash
dropper publish all --auto-changelog
```

**Changelog Format:**

Commits are categorized by conventional commit type:

```
## Features
- Add new item type
- Implement crafting system

## Bug Fixes
- Fix item duplication bug
- Resolve crash on startup

## Documentation
- Update README
- Add API docs
```

**Supported Commit Types:**
- `feat:` - Features
- `fix:` - Bug Fixes
- `docs:` - Documentation
- `refactor:` - Refactoring
- `test:` - Testing
- `chore:` - Maintenance
- `perf:` - Performance

### Custom Changelog File

Provide a markdown file:

```bash
dropper publish all --changelog CHANGELOG.md
```

**Changelog File Format:**
```markdown
# Version 1.2.0

## What's New
- Added new sword type
- Improved crafting recipes

## Bug Fixes
- Fixed item rendering
- Resolved multiplayer sync issue

## Breaking Changes
- Changed item IDs (migration required)
```

## Best Practices

### 1. Use Semantic Versioning

Follow [Semantic Versioning](https://semver.org/):

```
MAJOR.MINOR.PATCH

1.0.0 - Initial release
1.1.0 - New features (backwards compatible)
1.1.1 - Bug fixes
2.0.0 - Breaking changes
```

### 2. Test with Dry Run

Always test before publishing:

```bash
dropper publish all --dry-run
```

This shows what would be published without actually publishing.

### 3. Use Conventional Commits

Write clear commit messages:

```bash
git commit -m "feat: add ruby sword item"
git commit -m "fix: resolve crafting recipe bug"
git commit -m "docs: update installation guide"
```

### 4. Write Good Changelogs

**Good:**
```markdown
## Features
- Added Ruby Sword with custom enchantment support
- Implemented new crafting system for gems

## Bug Fixes
- Fixed item duplication exploit
- Resolved crash when using Ruby Sword in creative mode
```

**Bad:**
```markdown
- did stuff
- fixed bug
- updated code
```

### 5. Tag Releases

Create git tags for versions:

```bash
git tag v1.2.0
git push origin v1.2.0
```

### 6. Manage Dependencies

Specify dependencies in publish config:

```yaml
dependencies:
  - id: "fabric-api"
    type: "required"
    version: "0.92.0"
  - id: "jei"
    type: "optional"
```

Then load in code:

```kotlin
val config = PublishConfig(
    // ...
    dependencies = loadDependencies()
)
```

### 7. Use Environment Variables

Never commit tokens to version control:

```yaml
# Good
apiToken: "${MODRINTH_TOKEN}"

# Bad - DO NOT DO THIS
apiToken: "abc123xyz"
```

### 8. Continue on Error

For multi-platform publishing, use `--continue-on-error`:

```bash
dropper publish all --continue-on-error
```

This ensures all platforms are attempted even if one fails.

## Troubleshooting

### "No publish config found"

**Problem:** `.dropper/publish-config.yml` doesn't exist

**Solution:**
```bash
mkdir .dropper
# Create publish-config.yml with your credentials
```

### "No JAR files found"

**Problem:** Mod hasn't been built

**Solution:**
```bash
dropper build
```

### "Invalid project ID"

**Problem:** Incorrect project ID in config

**Solution:**
- Modrinth: Check project URL (last part is project ID)
- CurseForge: Check project URL (numeric ID)
- GitHub: Use `owner/repo` format

### "API token invalid"

**Problem:** Token expired or incorrect

**Solution:**
1. Generate new token from platform
2. Update environment variable
3. Verify token has correct permissions

### "Game version not supported"

**Problem:** CurseForge doesn't recognize game version

**Solution:**
- Use exact version numbers: "1.20.1", not "1.20"
- Check supported versions in CurseForge documentation

### "Failed to create git tag"

**Problem:** Tag already exists or no git repository

**Solution:**
```bash
# Check if git repo
git status

# Delete existing tag if needed
git tag -d v1.0.0
git push origin :refs/tags/v1.0.0
```

### "Validation errors"

**Problem:** Missing required fields

**Solution:**
Check validation errors and provide:
- Version number
- Game versions
- Loaders
- Platform-specific IDs and tokens

### Dry Run Shows Success but Real Publish Fails

**Problem:** Configuration issue not caught by dry run

**Solution:**
- Verify API tokens are correct
- Check network connectivity
- Verify project IDs exist
- Check platform status pages

## Advanced Usage

### Publishing Specific Versions

Publish only certain Minecraft versions:

```bash
dropper publish all --game-versions "1.20.1"
```

### Publishing Specific Loaders

Publish only certain loaders:

```bash
dropper publish all --loaders "fabric"
```

### Beta Releases

Mark releases as beta:

```bash
dropper publish all --release-type beta
```

### Custom Version Numbers

Override version from config.yml:

```bash
dropper publish all --version 2.0.0-alpha.1
```

### Automated CI/CD

Example GitHub Actions workflow:

```yaml
name: Publish

on:
  release:
    types: [created]

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3

      - name: Build
        run: dropper build

      - name: Publish
        env:
          MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}
          CURSEFORGE_TOKEN: ${{ secrets.CURSEFORGE_TOKEN }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          dropper publish all \
            --auto-changelog \
            --version ${{ github.ref_name }}
```

## Security

### Token Security

- Never commit tokens to git
- Use environment variables
- Rotate tokens regularly
- Use minimal required scopes

### Rate Limiting

All platforms have rate limits:
- Modrinth: 300 requests/minute
- CurseForge: 100 requests/minute
- GitHub: 5000 requests/hour

The CLI automatically respects rate limits.

## Support

- **Documentation:** https://dropper.dev/docs
- **Issues:** https://github.com/owner/dropper/issues
- **Discord:** https://discord.gg/dropper

## See Also

- [Build Command](./BUILD.md)
- [Project Structure](./PROJECT_STRUCTURE.md)
- [CI/CD Guide](./CICD.md)
