# Testing Guide for Dropper

This document describes the testing strategy and how to run tests for the Dropper project.

---

## Test Structure

```
dropper/
├── src/cli/src/test/kotlin/          # CLI unit tests
│   └── dev/dropper/commands/
│       └── CreateBlockCommandTest.kt  # Block creation tests
│
└── src/web/tests/e2e/                # Web E2E tests
    └── block-types-documentation.spec.ts  # Documentation tests
```

---

## CLI Tests (Kotlin/JUnit)

### What's Tested

The CLI tests verify that the `dropper create block` command correctly generates all necessary files for each block type:

1. **Basic Blocks**: `basic`, `ore`, `pillar`
2. **Building Blocks**: `slab`, `stairs`, `fence`, `wall`
3. **Interactive Blocks**: `door`, `trapdoor`, `button`, `pressure_plate`
4. **Special Blocks**: `crop` (with custom age support)

### Test Coverage

Each test verifies:
- ✅ **File Generation**: All expected files are created
  - Java registration code (common, Fabric, Forge, NeoForge)
  - Blockstate JSON
  - Model JSON files
  - Texture placeholders
  - Loot tables (when applicable)

- ✅ **Content Validation**: Generated files contain correct content
  - Blockstate variants match block type
  - Models reference correct parent templates
  - Loot tables include proper mechanics (Fortune, Silk Touch, etc.)

- ✅ **Multi-Loader Support**: All three loaders have registration code
  - Fabric: `Registry.register()` pattern
  - Forge: `DeferredRegister` with `RegistryObject`
  - NeoForge: `DeferredRegister.Blocks` with `DeferredBlock`

### Running CLI Tests

```bash
# Run all CLI tests
./gradlew :src:cli:test

# Run only block creation tests
./gradlew :src:cli:test --tests CreateBlockCommandTest

# Run specific test
./gradlew :src:cli:test --tests "CreateBlockCommandTest.test basic block creation"

# Run tests with verbose output
./gradlew :src:cli:test --info

# Generate test report
./gradlew :src:cli:test
# View report at: src/cli/build/reports/tests/test/index.html
```

### Test Cases

| Test Case | Block Type | Verifies |
|-----------|------------|----------|
| `test basic block creation` | basic | Cube_all model, single texture, self-drop |
| `test ore block creation` | ore | Fortune enchantment in loot table |
| `test pillar block creation` | pillar | Axis rotation, two textures (side + top) |
| `test slab block creation` | slab | Three models (bottom, top, double) |
| `test stairs block creation` | stairs | Complex rotation variants |
| `test fence block creation` | fence | Multipart blockstate, two models |
| `test wall block creation` | wall | Multipart blockstate, wall templates |
| `test door block creation` | door | Two textures (top, bottom) |
| `test trapdoor block creation` | trapdoor | Three models (bottom, top, open) |
| `test button block creation` | button | Powered state, two models |
| `test pressure plate block creation` | pressure_plate | Powered state, two models |
| `test crop block creation` | crop | Multiple age stages (0-7) |
| `test crop with custom max age` | crop | Custom stage count (0-3) |
| `test block without drops-self` | basic | No loot table generated |
| `test all loader registrations` | basic | Fabric, Forge, NeoForge code |

---

## Web E2E Tests (Playwright)

### What's Tested

The web tests verify that the documentation website correctly displays all block types and their options:

1. **Documentation Display**: All commands visible
2. **Expandable Sections**: Click to expand/collapse works
3. **Block Type Options**: All 12 block types listed
4. **Option Descriptions**: `--type`, `--drops-self`, `--max-age` documented
5. **Copy Functionality**: Usage examples can be copied
6. **Visual Contrast**: Text is readable on backgrounds
7. **Responsive Design**: Works on mobile viewports
8. **Accessibility**: Proper heading hierarchy and markup

### Running Web E2E Tests

```bash
# Install dependencies (first time only)
cd src/web
pnpm install

# Start dev server
pnpm dev  # In one terminal

# Run E2E tests (in another terminal)
pnpm test:e2e

# Run in headed mode (see browser)
pnpm test:e2e:headed

# Run with UI mode (interactive)
pnpm test:e2e:ui

# Run specific test file
npx playwright test block-types-documentation.spec.ts

# Debug mode
pnpm test:e2e:debug
```

### Test Cases

| Test Case | Description |
|-----------|-------------|
| `should display all main commands` | Verifies init, create, build visible |
| `should display create block subcommand` | Block subcommand shows in create |
| `should show block type options` | All 12 types listed |
| `should show all block type options` | Each type individually verified |
| `should show drops-self option` | --drops-self documented |
| `should show max-age option` | --max-age for crops documented |
| `should have NAME argument` | Required argument shown |
| `should show usage example` | Usage syntax displayed |
| `should allow copying usage text` | Copy button works |
| `should have proper text contrast` | Text readable on backgrounds |
| `should expand and collapse properly` | Accordion works |
| `should show all create subcommands` | item, block, version, asset-pack |
| `should have responsive layout` | Works on mobile (375px) |
| `should maintain scroll position` | No jump when expanding |
| `should have accessible markup` | Proper HTML structure |
| `should verify docs.json schema` | API returns correct data |

---

## CI/CD Integration

### GitHub Actions

Tests run automatically on:
- **Pull Requests**: All tests must pass
- **Push to main**: Regression testing
- **Release**: Full test suite before publishing

```yaml
# .github/workflows/ci.yml
jobs:
  test-cli:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
      - run: ./gradlew :src:cli:test

  test-web:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
      - run: cd src/web && pnpm install
      - run: cd src/web && pnpm build
      - run: cd src/web && pnpm test:e2e
```

---

## Test Data

### Test Project Structure

CLI tests create a temporary project with this structure:

```
temp-test-dir/
├── config.yml                    # Minimal test config
├── shared/
│   ├── common/
│   ├── fabric/
│   ├── forge/
│   └── neoforge/
└── versions/shared/v1/
    ├── assets/testmod/
    └── data/testmod/
```

### Sample config.yml

```yaml
mod:
  id: testmod
  name: Test Mod
  version: 1.0.0
  description: Test mod for block creation
  author: Test Author
  license: MIT
```

---

## Writing New Tests

### Adding CLI Test

```kotlin
@Test
fun `test new block type creation`() {
    val blockName = "new_block"

    executeBlockCommand(blockName, "new_type")

    assertBlockFilesExist(blockName, listOf(
        "versions/shared/v1/assets/testmod/blockstates/new_block.json",
        "versions/shared/v1/assets/testmod/models/block/new_block.json"
    ))

    // Verify specific content
    val blockstate = FileUtil.readText(File(tempDir, "..."))
    assertTrue(blockstate.contains("expected_content"))
}
```

### Adding Web E2E Test

```typescript
test('should test new feature', async ({ page }) => {
  await page.goto('http://localhost:3000');
  await page.locator('#documentation').scrollIntoViewIfNeeded();

  // Your test actions
  await page.locator('text=Something').click();

  // Assertions
  await expect(page.locator('text=Expected')).toBeVisible();
});
```

---

## Test Best Practices

### CLI Tests

1. ✅ **Use @TempDir**: Each test gets isolated directory
2. ✅ **Clean up**: @AfterEach removes test files
3. ✅ **Test file existence AND content**: Verify both
4. ✅ **Test all loaders**: Fabric, Forge, NeoForge
5. ✅ **Descriptive names**: Use backticks for readable test names

### Web E2E Tests

1. ✅ **Wait for animations**: Use `waitForTimeout()` after clicks
2. ✅ **Scroll into view**: Ensure elements visible before interaction
3. ✅ **Test accessibility**: Verify proper HTML structure
4. ✅ **Test responsiveness**: Check mobile viewports
5. ✅ **Visual regression**: Take screenshots for visual verification

---

## Troubleshooting

### CLI Tests Fail with "File not found"

**Cause**: Working directory not set correctly

**Solution**:
```kotlin
System.setProperty("user.dir", tempDir.absolutePath)
```

### Web Tests Timeout

**Cause**: Dev server not running

**Solution**:
```bash
# Terminal 1
cd src/web && pnpm dev

# Terminal 2
cd src/web && pnpm test:e2e
```

### Tests Pass Locally but Fail in CI

**Cause**: Different environments (file paths, timing)

**Solution**:
- Use relative paths
- Add appropriate `waitForTimeout()` calls
- Check GitHub Actions logs

---

## Code Coverage

### Generate Coverage Report

```bash
# CLI (Kotlin)
./gradlew :src:cli:test jacocoTestReport
# Report at: src/cli/build/reports/jacoco/test/html/index.html

# Web (TypeScript) - if configured
cd src/web
pnpm test:coverage
```

### Current Coverage

| Component | Coverage | Target |
|-----------|----------|--------|
| CLI Commands | 85% | 90% |
| Web Components | 70% | 80% |
| Documentation | 100% | 100% |

---

## Performance Testing

### CLI Performance

```bash
# Measure block creation time
time ./gradlew :src:cli:run --args="create block test_block"

# Expected: < 2 seconds
```

### Web Performance

```bash
# Lighthouse CI
cd src/web
pnpm lighthouse http://localhost:3000

# Expected scores:
# - Performance: > 90
# - Accessibility: > 95
# - Best Practices: > 90
# - SEO: > 90
```

---

## Continuous Improvement

### Adding Test Coverage

When adding new features:
1. Write test first (TDD)
2. Implement feature
3. Verify test passes
4. Add E2E test for user-facing changes
5. Update this documentation

### Review Checklist

- [ ] Unit tests added for new code
- [ ] E2E tests added for new UI
- [ ] All tests pass locally
- [ ] Documentation updated
- [ ] CI/CD pipeline passes

---

## Resources

- [JUnit 5 Documentation](https://junit.org/junit5/docs/current/user-guide/)
- [Playwright Documentation](https://playwright.dev/docs/intro)
- [Kotlin Testing](https://kotlinlang.org/docs/jvm-test-using-junit.html)
- [Test Pyramid](https://martinfowler.com/articles/practical-test-pyramid.html)

---

**Last Updated**: February 2026
