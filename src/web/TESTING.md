# E2E Testing Setup Guide

## Quick Start

### 1. Install Dependencies

```bash
cd src/web
npm install
```

This will install Playwright and all required dependencies from `package.json`.

### 2. Install Playwright Browsers

```bash
npx playwright install
```

This downloads browser binaries (Chromium, Firefox, WebKit) for testing.

For a specific browser only:
```bash
npx playwright install chromium
```

### 3. Start Development Server (if not running)

```bash
npm run dev
```

The tests are configured to auto-start the dev server on port 3001, but you can run it manually if preferred.

### 4. Run Tests

```bash
# Run all tests (headless)
npm run test:e2e

# Run with visible browser
npm run test:e2e:headed

# Open interactive UI
npm run test:e2e:ui

# Debug mode
npm run test:e2e:debug
```

## First Time Setup

### Prerequisites

- Node.js 18+ installed
- npm or yarn package manager
- At least 2GB free disk space (for browser binaries)

### Installation Steps

1. **Navigate to web directory**
   ```bash
   cd src/web
   ```

2. **Install Node dependencies**
   ```bash
   npm install
   ```

3. **Install Playwright browsers**
   ```bash
   npx playwright install --with-deps
   ```

   The `--with-deps` flag installs system dependencies needed by browsers (Linux only).

4. **Verify installation**
   ```bash
   npx playwright --version
   ```

   Should output something like: `Version 1.49.1`

5. **Run smoke tests**
   ```bash
   npx playwright test smoke.spec.ts --project=chromium
   ```

   This runs quick validation tests to ensure everything works.

## Running Tests

### All Tests

```bash
npm run test:e2e
```

This runs all test files across all configured browsers and viewports.

### Specific Test File

```bash
npx playwright test landing-page.spec.ts
npx playwright test responsive.spec.ts
npx playwright test sections.spec.ts
npx playwright test tailwind-theming.spec.ts
npx playwright test smoke.spec.ts
```

### Specific Test Case

```bash
npx playwright test --grep "should load the landing page"
npx playwright test --grep "Hero section"
```

### Specific Browser

```bash
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit
npx playwright test --project="Mobile Chrome"
npx playwright test --project="Mobile Safari"
```

### Multiple Browsers

```bash
npx playwright test --project=chromium --project=firefox
```

### Debug Mode

```bash
npm run test:e2e:debug
```

Opens Playwright Inspector for step-by-step debugging.

### Headed Mode

```bash
npm run test:e2e:headed
```

Runs tests with visible browser windows.

### Interactive UI

```bash
npm run test:e2e:ui
```

Opens Playwright Test UI for exploring tests, debugging, and viewing traces.

## Test Organization

### Test Files

- **`smoke.spec.ts`** - Fast critical tests (run first)
- **`landing-page.spec.ts`** - Main landing page functionality
- **`responsive.spec.ts`** - Responsive design across viewports
- **`sections.spec.ts`** - Individual section validation
- **`tailwind-theming.spec.ts`** - CSS and theming tests
- **`helpers.ts`** - Shared test utilities

### Running by Priority

**Smoke tests first** (fastest, catches major issues):
```bash
npx playwright test smoke.spec.ts
```

**Core functionality next**:
```bash
npx playwright test landing-page.spec.ts
```

**Visual and responsive last**:
```bash
npx playwright test responsive.spec.ts sections.spec.ts
```

## Viewing Test Results

### HTML Report

After tests complete:
```bash
npx playwright show-report tests/playwright-report
```

Opens an interactive HTML report with:
- Test results
- Screenshots
- Videos (on failure)
- Traces (on retry)

### Screenshots

Located in `tests/screenshots/`:
- Full page screenshots
- Section screenshots
- Component screenshots
- Responsive screenshots
- Visual regression baselines

### Test Output

Console output shows:
```
Running 45 tests using 3 workers

  ✓  landing-page.spec.ts:5:3 › should load the landing page (1.2s)
  ✓  landing-page.spec.ts:15:3 › should display all main sections (892ms)
  ✗  responsive.spec.ts:10:3 › should display desktop layout (234ms)
```

## Debugging Failed Tests

### View Error Details

```bash
npx playwright show-report tests/playwright-report
```

Click on failed test to see:
- Error message
- Screenshot at failure
- Video recording
- Network requests
- Console logs

### Debug Specific Test

```bash
npx playwright test landing-page.spec.ts --debug
```

Opens Playwright Inspector with:
- Test code
- Browser window
- Step-by-step controls
- Console output
- Network activity

### Add Breakpoints

In test file:
```typescript
test('my test', async ({ page }) => {
  await page.goto('/')

  // Pause execution here
  await page.pause()

  // Continue test...
})
```

### View Trace

If test was retried, trace files are captured:

```bash
npx playwright show-trace trace.zip
```

Shows timeline of all actions, screenshots, network, and console.

## Updating Visual Baselines

When design changes are intentional:

```bash
# Update all visual snapshots
npx playwright test --update-snapshots

# Update specific test
npx playwright test sections.spec.ts --update-snapshots

# Update specific browser
npx playwright test --project=chromium --update-snapshots
```

This regenerates baseline screenshots for visual regression tests.

## CI/CD Integration

### GitHub Actions

Copy `.github-workflows-example.yml` to `.github/workflows/e2e-tests.yml`:

```bash
cp .github-workflows-example.yml ../.github/workflows/e2e-tests.yml
```

This sets up automated testing on:
- Push to main/develop
- Pull requests
- Manual workflow dispatch

### Environment Variables

Tests use these environment variables:

- `CI` - Set to `true` in CI environments
- `BASE_URL` - Override default localhost:3001

Example:
```bash
BASE_URL=https://staging.dropper.dev npm run test:e2e
```

## Troubleshooting

### Port Already in Use

**Error**: `Port 3001 is already in use`

**Solution**:
```bash
# Windows
netstat -ano | findstr :3001
taskkill /PID <PID> /F

# Mac/Linux
lsof -i :3001
kill -9 <PID>
```

Or change port in `playwright.config.ts`.

### Browsers Not Installed

**Error**: `Executable doesn't exist at ...`

**Solution**:
```bash
npx playwright install
```

### Tests Timeout

**Error**: `Test timeout of 30000ms exceeded`

**Solution**:
1. Check if dev server is running
2. Increase timeout in test:
   ```typescript
   test('my test', async ({ page }) => {
     // ...
   })
   test.setTimeout(60000) // 60 seconds
   ```

### CSS Not Loading

**Error**: Tests fail with "element not styled correctly"

**Solution**:
1. Clear Next.js cache: `rm -rf .next`
2. Restart dev server: `npm run dev`
3. Check Tailwind config: `tailwind.config.ts`
4. Verify CSS import in `app/globals.css`

### Screenshots Don't Match

**Error**: Visual regression tests fail with pixel differences

**Solution**:
```bash
# View difference
npx playwright show-report tests/playwright-report

# If change is intentional, update baseline
npx playwright test --update-snapshots
```

### Network Issues

**Error**: Tests fail with network errors

**Solution**:
1. Check internet connection
2. Verify dev server is accessible: `curl http://localhost:3001`
3. Check for proxy issues
4. Try offline mode: Remove external dependencies

### Memory Issues

**Error**: Out of memory errors

**Solution**:
1. Reduce parallel workers in `playwright.config.ts`:
   ```typescript
   workers: 1
   ```
2. Run fewer projects:
   ```bash
   npx playwright test --project=chromium
   ```

### Permission Errors (Windows)

**Error**: Permission denied errors

**Solution**:
1. Run as Administrator
2. Check antivirus isn't blocking Playwright
3. Disable Windows Defender real-time protection temporarily

## Best Practices

### Writing Tests

1. **Use descriptive names**
   ```typescript
   test('should display Minecraft-themed hero section with lime accents', ...)
   ```

2. **Wait properly**
   ```typescript
   await page.waitForSelector('h1')
   await expect(element).toBeVisible()
   ```

3. **Use helpers**
   ```typescript
   import { waitForPageLoad } from './helpers'
   await waitForPageLoad(page)
   ```

4. **Group related tests**
   ```typescript
   test.describe('Hero Section', () => {
     // Related tests...
   })
   ```

5. **Take strategic screenshots**
   ```typescript
   await page.screenshot({ path: 'screenshot.png', fullPage: true })
   ```

### Test Maintenance

1. **Run smoke tests regularly**
   ```bash
   npm run test:e2e smoke.spec.ts
   ```

2. **Update baselines after design changes**
   ```bash
   npx playwright test --update-snapshots
   ```

3. **Review HTML reports**
   ```bash
   npx playwright show-report
   ```

4. **Keep dependencies updated**
   ```bash
   npm update @playwright/test
   npx playwright install
   ```

## Performance Tips

### Faster Test Runs

1. **Run smoke tests first**
   ```bash
   npx playwright test smoke.spec.ts && npx playwright test
   ```

2. **Use single browser for development**
   ```bash
   npx playwright test --project=chromium
   ```

3. **Run only changed tests**
   ```bash
   npx playwright test landing-page.spec.ts
   ```

4. **Disable video recording**
   In `playwright.config.ts`:
   ```typescript
   video: 'off'
   ```

5. **Reduce retries in dev**
   ```typescript
   retries: 0
   ```

### Parallel Execution

```bash
# Use more workers (default is CPU count)
npx playwright test --workers=4

# Or single worker for stability
npx playwright test --workers=1
```

## Resources

- **Playwright Docs**: https://playwright.dev
- **Best Practices**: https://playwright.dev/docs/best-practices
- **API Reference**: https://playwright.dev/docs/api/class-test
- **Test README**: `tests/README.md`

## Getting Help

1. Check this guide
2. Review `tests/README.md`
3. Check Playwright documentation
4. Review test output and screenshots
5. Open issue with:
   - Test file and line
   - Error message
   - Screenshots
   - Browser/OS version

## Next Steps

After setup:

1. ✅ Run smoke tests to verify setup
2. ✅ Run all tests to establish baseline
3. ✅ Review HTML report
4. ✅ Explore Playwright UI mode
5. ✅ Set up CI/CD workflow
6. ✅ Add project-specific tests
7. ✅ Configure screenshot baselines

Happy testing! 🎭
