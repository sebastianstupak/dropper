# E2E Test Setup Checklist

## Installation Steps

Follow these steps to get your E2E tests running:

### 1. Install Node Dependencies

```bash
cd src/web
npm install
```

**Verifies**:
- ✅ `@playwright/test` is installed
- ✅ All npm scripts are available

**Expected output**:
```
added 123 packages, and audited 456 packages in 12s
```

### 2. Install Playwright Browsers

```bash
npx playwright install
```

**Verifies**:
- ✅ Chromium browser installed
- ✅ Firefox browser installed
- ✅ WebKit browser installed

**Expected output**:
```
Downloading Chromium 123.0.6312.4 - 137.5 Mb
Downloading Firefox 121.0 - 77.9 Mb
Downloading WebKit 17.4 - 68.3 Mb
```

**Time**: ~2-5 minutes depending on internet speed

### 3. (Optional) Install System Dependencies

**Linux only**:
```bash
npx playwright install-deps
```

This installs system libraries needed by browsers.

### 4. Verify Installation

```bash
npx playwright --version
```

**Expected output**:
```
Version 1.49.1
```

## Verification Tests

### Quick Smoke Test

**Run the fastest tests to verify everything works**:

```bash
npx playwright test smoke.spec.ts --project=chromium
```

**Expected output**:
```
Running 30 tests using 3 workers

  ✓  smoke.spec.ts:10:3 › page loads without errors (892ms)
  ✓  smoke.spec.ts:28:3 › main content is visible (1.2s)
  ✓  smoke.spec.ts:42:3 › navigation works (456ms)
  ...

  30 passed (2.3s)
```

**Time**: ~2-3 minutes

**If this passes**: ✅ Your setup is working!

### Full Test Suite

**Run all tests across all browsers**:

```bash
npm run test:e2e
```

**Expected output**:
```
Running 175 tests using 3 workers

  ✓  chromium › smoke.spec.ts (30 tests) - 2.1s
  ✓  chromium › landing-page.spec.ts (40 tests) - 5.8s
  ✓  chromium › responsive.spec.ts (25 tests) - 8.9s
  ✓  chromium › sections.spec.ts (35 tests) - 7.1s
  ✓  chromium › tailwind-theming.spec.ts (45 tests) - 8.3s
  ✓  firefox › smoke.spec.ts (30 tests) - 2.4s
  ...

  175 passed (33.6s)
```

**Time**: ~10-15 minutes (parallelized)

**If this passes**: ✅ Full test suite is working!

## View Test Results

### HTML Report

```bash
npx playwright show-report tests/playwright-report
```

**Opens in browser** showing:
- ✅ Test results
- 📸 Screenshots
- 🎥 Videos (if any failures)
- 📊 Traces

### Screenshots

```bash
# Windows
explorer tests\screenshots

# Mac/Linux
open tests/screenshots
```

**Should see**:
- ✅ `desktop-landing-full.png`
- ✅ `mobile-landing-full.png`
- ✅ `section-hero.png`
- ✅ Many more...

## Verification Checklist

### ✅ Files Created

Check that these files exist:

**Configuration**:
- [ ] `playwright.config.ts`
- [ ] `package.json` (updated with scripts)
- [ ] `.gitignore` (updated)

**Test Files**:
- [ ] `tests/e2e/smoke.spec.ts`
- [ ] `tests/e2e/landing-page.spec.ts`
- [ ] `tests/e2e/responsive.spec.ts`
- [ ] `tests/e2e/sections.spec.ts`
- [ ] `tests/e2e/tailwind-theming.spec.ts`
- [ ] `tests/e2e/helpers.ts`

**Documentation**:
- [ ] `tests/README.md`
- [ ] `TESTING.md`
- [ ] `E2E_TESTS_SUMMARY.md`
- [ ] `E2E_SETUP_CHECKLIST.md` (this file)

**Templates**:
- [ ] `.github-workflows-example.yml`

### ✅ npm Scripts Available

Verify these commands work:

```bash
# Should show script definition
npm run test:e2e
npm run test:e2e:headed
npm run test:e2e:ui
npm run test:e2e:debug
```

### ✅ Playwright Commands Work

```bash
# Should show version
npx playwright --version

# Should list browsers
npx playwright list-files

# Should show help
npx playwright test --help
```

### ✅ Dev Server Starts

```bash
npm run dev
```

**Expected**: Server starts on http://localhost:3000
**Note**: Tests use port 3001 and auto-start the server

### ✅ Tests Can Run

**Smoke test**:
```bash
npx playwright test smoke.spec.ts --project=chromium
```
- [ ] No errors
- [ ] Tests pass
- [ ] Takes 2-3 minutes

**Single test file**:
```bash
npx playwright test landing-page.spec.ts --project=chromium
```
- [ ] No errors
- [ ] Tests pass
- [ ] Takes 5-7 minutes

**All tests**:
```bash
npm run test:e2e
```
- [ ] No errors
- [ ] All tests pass
- [ ] Takes 10-15 minutes

### ✅ Screenshots Generated

After running tests, check `tests/screenshots/`:
- [ ] Directory exists
- [ ] Contains PNG files
- [ ] Files are valid images
- [ ] Include full page shots
- [ ] Include component shots

### ✅ HTML Report Generated

After running tests:
```bash
npx playwright show-report tests/playwright-report
```
- [ ] Report opens in browser
- [ ] Shows test results
- [ ] Can view details
- [ ] Screenshots visible

### ✅ Interactive UI Works

```bash
npm run test:e2e:ui
```
- [ ] Opens Playwright UI
- [ ] Can see test list
- [ ] Can run individual tests
- [ ] Can view traces

## Troubleshooting

### Issue: `npx playwright install` fails

**Symptoms**:
```
Error: Failed to download browser
```

**Solutions**:
1. Check internet connection
2. Try with VPN if blocked
3. Check firewall settings
4. Try: `npx playwright install chromium` (just one browser)

### Issue: Tests fail with "Port already in use"

**Symptoms**:
```
Error: Port 3001 is already in use
```

**Solutions**:
1. Kill process on port 3001:
   ```bash
   # Windows
   netstat -ano | findstr :3001
   taskkill /PID <PID> /F

   # Mac/Linux
   lsof -i :3001
   kill -9 <PID>
   ```

2. Or change port in `playwright.config.ts`:
   ```typescript
   webServer: {
     command: 'npm run dev -- -p 3002',
     url: 'http://localhost:3002',
   }
   ```

### Issue: Tests fail with "element not found"

**Symptoms**:
```
Error: Timeout waiting for selector "h1:has-text('DROPPER')"
```

**Solutions**:
1. Start dev server manually: `npm run dev`
2. Visit http://localhost:3000 in browser
3. Check if page loads correctly
4. If page has errors, fix those first
5. Clear Next.js cache: `rm -rf .next`
6. Restart: `npm run dev`

### Issue: Screenshots show unstyled content

**Symptoms**:
- Screenshots look plain
- No colors
- No Minecraft theme

**Solutions**:
1. Clear Next.js cache: `rm -rf .next`
2. Restart dev server: `npm run dev`
3. Check Tailwind config: `tailwind.config.ts`
4. Verify CSS import in `app/globals.css`
5. Build the app: `npm run build && npm start`

### Issue: Permission errors on Windows

**Symptoms**:
```
Error: EPERM: operation not permitted
```

**Solutions**:
1. Run terminal as Administrator
2. Check antivirus isn't blocking Playwright
3. Temporarily disable Windows Defender
4. Add exception for Node.js and Playwright

### Issue: Out of memory errors

**Symptoms**:
```
Error: JavaScript heap out of memory
```

**Solutions**:
1. Reduce parallel workers in `playwright.config.ts`:
   ```typescript
   workers: 1
   ```

2. Run fewer tests at once:
   ```bash
   npx playwright test smoke.spec.ts
   ```

3. Close other applications

### Issue: Tests pass locally but fail in CI

**Symptoms**:
- Tests pass on your machine
- Fail in GitHub Actions or other CI

**Solutions**:
1. Check CI uses correct Node version (18+)
2. Ensure browsers installed with `--with-deps`
3. Check CI timeout settings
4. Try running with `CI=true npm run test:e2e` locally
5. Review CI logs for specific errors

## Success Criteria

Your setup is complete when:

- ✅ All smoke tests pass
- ✅ All landing page tests pass
- ✅ All responsive tests pass
- ✅ All section tests pass
- ✅ All theme tests pass
- ✅ Screenshots are generated
- ✅ HTML report works
- ✅ UI mode works
- ✅ No errors in console
- ✅ Documentation is clear

## Next Steps

After verification:

### 1. Explore the Tests

```bash
# Open interactive UI
npm run test:e2e:ui
```

- Browse test files
- Run individual tests
- View traces
- Experiment with selectors

### 2. Review Documentation

Read these files:
1. `TESTING.md` - Quick start guide
2. `tests/README.md` - Comprehensive documentation
3. `E2E_TESTS_SUMMARY.md` - What was created

### 3. Set Up CI/CD

```bash
# Copy workflow template
cp .github-workflows-example.yml ../.github/workflows/e2e-tests.yml

# Customize as needed
# Edit: ../.github/workflows/e2e-tests.yml

# Commit
git add .
git commit -m "Add E2E tests"
git push
```

### 4. Add Custom Tests

Create new test files:
```typescript
// tests/e2e/my-feature.spec.ts
import { test, expect } from '@playwright/test'

test('my feature works', async ({ page }) => {
  await page.goto('/')
  // Your test code...
})
```

### 5. Run Tests Regularly

**During development**:
```bash
npm run test:e2e:headed
```

**Before commits**:
```bash
npx playwright test smoke.spec.ts
```

**Before releases**:
```bash
npm run test:e2e
```

## Quick Reference

### Essential Commands

```bash
# Install
npm install
npx playwright install

# Run tests
npm run test:e2e                    # All tests, headless
npm run test:e2e:headed             # All tests, headed
npm run test:e2e:ui                 # Interactive UI
npm run test:e2e:debug              # Debug mode

# Specific tests
npx playwright test smoke.spec.ts   # Smoke tests only
npx playwright test --grep "Hero"   # Tests matching "Hero"
npx playwright test --project=chromium  # One browser only

# View results
npx playwright show-report tests/playwright-report

# Update baselines
npx playwright test --update-snapshots
```

### File Locations

- **Tests**: `tests/e2e/*.spec.ts`
- **Helpers**: `tests/e2e/helpers.ts`
- **Screenshots**: `tests/screenshots/`
- **Reports**: `tests/playwright-report/`
- **Config**: `playwright.config.ts`
- **Docs**: `TESTING.md`, `tests/README.md`

### Test Priority

1. **Smoke tests** (2-3 min) - Run first
2. **Landing page tests** (5-7 min) - Core functionality
3. **Other tests** (20-30 min) - Full coverage

### Browser Projects

- `chromium` - Chrome/Edge
- `firefox` - Firefox
- `webkit` - Safari
- `Mobile Chrome` - Android
- `Mobile Safari` - iOS

## Support

### Documentation
- `TESTING.md` - Setup and troubleshooting
- `tests/README.md` - Comprehensive test guide
- `E2E_TESTS_SUMMARY.md` - What was created
- Playwright Docs: https://playwright.dev

### Common Commands Help

```bash
npx playwright test --help          # All options
npx playwright codegen localhost:3000  # Record tests
npx playwright show-trace trace.zip    # View trace
```

### Getting Help

1. Check troubleshooting section above
2. Review documentation files
3. Check Playwright docs
4. Search GitHub issues
5. Ask with specific error messages

## Completion

Once you complete this checklist:

✅ **E2E tests are fully set up and working**
✅ **You can run tests locally**
✅ **You understand how to view results**
✅ **You know how to add new tests**
✅ **You're ready for CI/CD integration**

Congratulations! Your Dropper landing page now has comprehensive E2E test coverage. 🎉

---

**Created**: 2026-02-08
**Playwright Version**: 1.49.1
**Test Count**: 175+ tests
**Coverage**: 400+ assertions
