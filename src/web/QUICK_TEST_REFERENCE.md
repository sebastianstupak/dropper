# Quick Test Reference Card

**Bookmark this for daily E2E testing!** 📌

## 🚀 Most Used Commands

```bash
# Run all tests (headless)
npm run test:e2e

# Run with visible browser (debugging)
npm run test:e2e:headed

# Interactive UI (best for development)
npm run test:e2e:ui

# Debug specific test
npx playwright test --debug --grep "test name"

# View last test report
npx playwright show-report tests/playwright-report
```

## 🎯 Quick Testing Workflows

### Before Committing Code
```bash
npx playwright test smoke.spec.ts --project=chromium
```
**Time**: 2-3 minutes | **Tests**: Critical functionality only

### During Development
```bash
npm run test:e2e:headed
```
**Time**: 10-15 minutes | **Tests**: All tests, visible browsers

### Before Pull Request
```bash
npm run test:e2e
```
**Time**: 10-15 minutes | **Tests**: Full suite, all browsers

### Investigating Bug
```bash
npm run test:e2e:ui
# Then select specific test and click "Show trace"
```

## 📝 Test Files Quick Guide

| File | Purpose | When to Run | Time |
|------|---------|-------------|------|
| `smoke.spec.ts` | Critical checks | Always | 2-3 min |
| `landing-page.spec.ts` | Core features | After UI changes | 5-7 min |
| `responsive.spec.ts` | Mobile/tablet | After layout changes | 8-10 min |
| `sections.spec.ts` | Section validation | After content changes | 6-8 min |
| `tailwind-theming.spec.ts` | CSS/theme | After style changes | 7-9 min |

## 🔧 Common Test Commands

### Run Specific Test File
```bash
npx playwright test landing-page.spec.ts
npx playwright test responsive.spec.ts
```

### Run Tests Matching Pattern
```bash
npx playwright test --grep "Hero section"
npx playwright test --grep "button"
npx playwright test --grep "mobile"
```

### Run Single Browser
```bash
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit
```

### Run Mobile Tests Only
```bash
npx playwright test --project="Mobile Chrome"
npx playwright test --project="Mobile Safari"
```

### Update Visual Baselines
```bash
npx playwright test --update-snapshots
```

## 🐛 Debugging Commands

### Step Through Test
```bash
npx playwright test --debug
```

### Pause Test Execution
Add to test code:
```typescript
await page.pause()
```

### See Browser Console
```bash
npm run test:e2e:headed
# Console output shows in terminal
```

### Generate Test Code
```bash
npx playwright codegen http://localhost:3000
```
**Tip**: Records your actions as test code!

### View Trace File
```bash
npx playwright show-trace trace.zip
```

## 📊 View Results

### HTML Report (Interactive)
```bash
npx playwright show-report tests/playwright-report
```

### Screenshots Folder
```bash
# Windows
explorer tests\screenshots

# Mac
open tests/screenshots

# Linux
xdg-open tests/screenshots
```

### Latest Test Output
```bash
# In terminal after test run
# Scroll up to see results
```

## 🔍 Filtering Tests

### By Browser
```bash
--project=chromium          # Desktop Chrome
--project=firefox           # Desktop Firefox
--project=webkit            # Desktop Safari
--project="Mobile Chrome"   # Android
--project="Mobile Safari"   # iOS
```

### By Test Name
```bash
--grep "should load"        # Contains "should load"
--grep "Hero"              # Contains "Hero"
--grep "responsive"        # Contains "responsive"
```

### Invert Grep (Exclude)
```bash
--grep-invert "slow"       # Exclude tests with "slow"
```

### Run Only Failed Tests
```bash
--last-failed
```

## ⚡ Performance Tips

### Run Faster (Single Worker)
```bash
npx playwright test --workers=1
```

### Run Faster (Chromium Only)
```bash
npx playwright test --project=chromium
```

### Run Faster (No Video)
Edit `playwright.config.ts`:
```typescript
video: 'off'
```

### Run Only Smoke Tests
```bash
npx playwright test smoke.spec.ts
```

## 🔄 Common Update Tasks

### Update Playwright
```bash
npm update @playwright/test
npx playwright install
```

### Clear Next.js Cache
```bash
rm -rf .next
npm run dev
```

### Clear Test Artifacts
```bash
rm -rf tests/screenshots tests/playwright-report
```

### Regenerate Screenshots
```bash
npx playwright test --update-snapshots
```

## 🚨 Troubleshooting Quick Fixes

### Port Already in Use
```bash
# Windows
netstat -ano | findstr :3001
taskkill /PID <PID> /F

# Mac/Linux
lsof -i :3001
kill -9 <PID>
```

### Tests Timing Out
```bash
# Increase timeout
npx playwright test --timeout=60000
```

### CSS Not Loading
```bash
rm -rf .next
npm run dev
```

### Browser Not Found
```bash
npx playwright install
```

## 📖 Documentation Files

| File | Purpose |
|------|---------|
| `TESTING.md` | Setup and quick start |
| `tests/README.md` | Comprehensive guide |
| `E2E_TESTS_SUMMARY.md` | What was created |
| `E2E_SETUP_CHECKLIST.md` | Verification steps |
| `QUICK_TEST_REFERENCE.md` | This file |

## 🎨 Screenshot Locations

**Full page screenshots**:
- `tests/screenshots/desktop-landing-full.png`
- `tests/screenshots/mobile-landing-full.png`
- `tests/screenshots/tablet-landing-full.png`

**Section screenshots**:
- `tests/screenshots/section-hero.png`
- `tests/screenshots/section-footer.png`

**Component screenshots**:
- `tests/screenshots/component-logo.png`
- `tests/screenshots/component-buttons.png`

**Responsive screenshots**:
- `tests/screenshots/mobile-{width}x{height}.png`

## 🎯 Test Priorities

### High Priority (Always Run)
```bash
npx playwright test smoke.spec.ts
npx playwright test landing-page.spec.ts
```

### Medium Priority (Before PR)
```bash
npx playwright test responsive.spec.ts
npx playwright test sections.spec.ts
```

### Low Priority (Weekly)
```bash
npx playwright test tailwind-theming.spec.ts
```

## 💡 Pro Tips

### 1. Use UI Mode for Development
```bash
npm run test:e2e:ui
```
- See all tests
- Run individually
- View traces
- Time travel debugging

### 2. Generate Tests Automatically
```bash
npx playwright codegen localhost:3000
```
- Click around your app
- Playwright writes the test code
- Copy generated code to test file

### 3. Quick Screenshot of Element
```typescript
await page.locator('.hero').screenshot({ path: 'hero.png' })
```

### 4. Check Element Exists
```typescript
await expect(page.locator('h1')).toBeVisible()
```

### 5. Wait for Network Idle
```typescript
await page.waitForLoadState('networkidle')
```

## 🔗 Quick Links

- **Playwright Docs**: https://playwright.dev
- **API Reference**: https://playwright.dev/docs/api/class-test
- **Best Practices**: https://playwright.dev/docs/best-practices
- **Selectors Guide**: https://playwright.dev/docs/selectors

## 📞 Getting Help

1. Check error message in terminal
2. Look at screenshot in HTML report
3. Review test code
4. Check documentation files
5. Search Playwright docs
6. Search GitHub issues

## 🎬 Common Scenarios

### "I changed the hero section"
```bash
npx playwright test landing-page.spec.ts --grep "Hero"
```

### "I updated mobile styles"
```bash
npx playwright test responsive.spec.ts --project="Mobile Chrome"
```

### "I added a new button"
```bash
npx playwright test landing-page.spec.ts --grep "button"
```

### "I changed colors"
```bash
npx playwright test tailwind-theming.spec.ts --grep "color"
```

### "Tests are failing and I don't know why"
```bash
npm run test:e2e:ui
# Click on failing test
# Click "Show trace"
# See exactly what happened
```

## ⏱️ Expected Run Times

| Command | Time | Use Case |
|---------|------|----------|
| Smoke tests | 2-3 min | Quick validation |
| Single file | 5-10 min | Specific area |
| Single browser | 10-15 min | Fast full run |
| All browsers | 30-40 min | Complete validation |
| UI mode (interactive) | Variable | Development |

## 📋 Checklist Template

Copy this for your workflow:

```
Before Commit:
[ ] Run smoke tests
[ ] All pass

Before PR:
[ ] Run all tests
[ ] All pass
[ ] Review HTML report
[ ] Check screenshots

After Deploy:
[ ] Run smoke tests on production
[ ] All critical paths work
```

---

**Last Updated**: 2026-02-08
**Playwright Version**: 1.49.1
**Test Count**: 175+ tests

**💡 Tip**: Keep this file open in your editor for quick reference!
