# Dropper Development Guide

## Project Structure

```
dropper/
├── src/
│   ├── cli/                    # CLI tool (Kotlin + GraalVM)
│   │   ├── src/main/kotlin/    # CLI source code
│   │   ├── src/main/resources/ # Embedded templates & build-logic
│   │   └── src/test/kotlin/    # E2E tests
│   └── web/                    # Documentation site (Next.js)
├── examples/
│   └── ruby-sword/             # Example generated project
├── scripts/                    # Build & installer scripts
└── docs/                       # Additional documentation
```

## Development Setup

### Prerequisites

- Java 21+ (for building the CLI)
- GraalVM (for native binary compilation)
- pnpm (for web development)

### Building the CLI

```bash
# Build JAR
./gradlew :src:cli:build

# Run via Gradle
./gradlew :src:cli:run --args="init test-mod"

# Run tests
./gradlew :src:cli:test
```

### Building Native Binary

```bash
# Requires GraalVM
./gradlew :src:cli:nativeCompile

# Binary will be at:
# src/cli/build/native/nativeCompile/dropper
```

### Running Tests

```bash
# Unit tests
./gradlew :src:cli:test

# E2E tests (full project generation)
./gradlew :src:cli:test --tests "*.E2ETest"
```

## Adding New Features

### Adding a New Command

1. Create command in `src/cli/src/main/kotlin/dev/dropper/commands/`
2. Extend `CliktCommand`
3. Register in `DropperCLI.kt`
4. Write tests in `src/cli/src/test/kotlin/`

### Adding New Templates

1. Create template in `src/cli/src/main/resources/templates/`
2. Use Mustache syntax: `{{variable}}`
3. Render via `TemplateEngine.render()`
4. Test in E2E tests

### Adding New Generator

1. Create generator in `src/cli/src/main/kotlin/dev/dropper/generator/`
2. Implement generation logic
3. Add corresponding command
4. Write E2E tests

## Code Guidelines

- **Kotlin style**: Follow Kotlin conventions
- **Testing**: E2E tests for all generators
- **Templates**: Use Mustache for all file generation
- **Errors**: Clear, actionable error messages
- **Logging**: Use `Logger.success()`, `Logger.info()`, `Logger.error()`

## Release Process

1. Update version in `build.gradle.kts`
2. Tag release: `git tag v1.0.0`
3. Push: `git push --tags`
4. GitHub Actions will build native binaries
5. Create GitHub release with binaries

## Testing Generated Projects

```bash
# Generate test project
./gradlew :src:cli:run --args="init test-mod --name 'Test' --author 'Test' --description 'Test' --versions '1.20.1' --loaders 'fabric'"

# Build generated project
cd test-mod
./gradlew build
```

## Architecture

See [DROPPER_ARCHITECTURE.md](DROPPER_ARCHITECTURE.md) for detailed architecture.
