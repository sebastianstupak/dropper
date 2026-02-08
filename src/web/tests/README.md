# Dropper Web E2E Tests

Comprehensive end-to-end tests for the Dropper landing page using Playwright.

## Overview

This test suite validates:
- Landing page functionality and content
- Minecraft theming and custom styling
- Tailwind CSS application
- Responsive design across devices
- Component interactions
- Visual regression testing
- Accessibility

## Test Structure

```
tests/
├── e2e/
│   ├── landing-page.spec.ts      # Basic functionality, theming, interactions
│   ├── responsive.spec.ts        # Responsive design across viewports
│   ├── sections.spec.ts          # Section-by-section validation
│   ├── tailwind-theming.spec.ts  # Tailwind CSS and Minecraft theme
│   └── helpers.ts                # Test utilities and helpers
├── screenshots/                   # Screenshot output (gitignored)
└── playwright-report/            # HTML test reports (gitignored)
```

## Running Tests

### Install Dependencies

```bash
npm install
npx playwright install  # Install browser binaries
```

### Run All Tests

```bash
# Headless mode (CI/CD)
npm run test:e2e

# Headed mode (see browser)
npm run test:e2e:headed

# Interactive UI mode
npm run test:e2e:ui

# Debug mode (step through tests)
npm run test:e2e:debug
```

### Run Specific Tests

```bash
# Run specific test file
npx playwright test landing-page.spec.ts

# Run tests matching a pattern
npx playwright test --grep "Hero section"

# Run tests in specific browser
npx playwright test --project=chromium
npx playwright test --project=webkit
npx playwright test --project=firefox

# Run tests on specific device
npx playwright test --project="Mobile Chrome"
npx playwright test --project="Mobile Safari"
```

## Test Coverage

### Landing Page Tests (`landing-page.spec.ts`)

**Basic Functionality**
- Page loads successfully
- All sections render (Hero, Features, Installation, Examples, Footer)
- Buttons and links work
- Scroll indicator functions

**Minecraft Theming**
- Custom color palette applied
- Pixel borders render correctly
- Minecraft font family loads
- Text shadows applied
- Background patterns display

**Button Interactions**
- Hover effects work
- Click navigation functions
- Button variants styled correctly
- Active states apply

**Animations**
- Float animation on logo
- Pixelate-in animation on content
- Bounce animation on scroll indicator

**Accessibility**
- Proper heading hierarchy
- Semantic HTML structure
- Keyboard navigation

**Content Validation**
- Correct text content
- Stats display properly
- Installation command shown

### Responsive Design Tests (`responsive.spec.ts`)

**Desktop (1920x1080)**
- Desktop layout renders
- Larger text sizes
- Row button layout
- Full-page screenshots

**Tablet (768x1024)**
- Tablet-optimized layout
- Adapted button sizes
- Grid layouts maintained

**Mobile (375x667)**
- Mobile layout stacks correctly
- Buttons vertical
- Smaller text sizes
- No horizontal overflow
- Touch-friendly sizes

**Large Desktop (2560x1440)**
- 4K display support
- Content centering
- Maximum text sizes

**Landscape Mobile (667x375)**
- Landscape orientation
- Content readability

**Cross-Browser Testing**
- Consistent rendering across viewports
- Screenshots at each breakpoint

### Section Tests (`sections.spec.ts`)

**Hero Section**
- Title and subtitle render
- Logo/icon displays
- Quick install code block
- Action buttons
- Stats panel
- Scroll indicator position

**Features Section**
- Section renders (when implemented)

**Installation Section**
- Anchor link works

**Examples Section**
- Section structure

**Footer Section**
- Footer renders at bottom

**Component Screenshots**
- Individual component capture
- Visual regression testing

### Tailwind & Theme Tests (`tailwind-theming.spec.ts`)

**Tailwind CSS**
- CSS loads successfully
- Utility classes apply
- Responsive classes work
- Spacing utilities
- Grid utilities

**Minecraft Custom Colors**
- Stone color (#8B8B8B)
- Lime color (#55FF55)
- Obsidian background (#100819)
- Dark color (#3C3C3C)
- Diamond color (#5DCDE3)

**Custom Utilities**
- `.pixel-border` - 3D border effect
- `.minecraft-button` - Button styling
- `.minecraft-panel` - Panel styling
- `.text-shadow-dark` - Text shadows
- `.pixelated` - Image rendering

**Custom Animations**
- `animate-float` - Floating motion
- `animate-pixelate-in` - Fade in effect
- `animate-bounce` - Bounce effect

**Gradient Utilities**
- `gradient-dirt` - Dirt gradient
- `gradient-grass` - Grass gradient

**Font Application**
- Minecraft font family
- Uppercase transforms

**Hover States**
- Brightness effects on hover
- Active states

**Custom Spacing**
- Block-based spacing variables
- Shadow variables

## Screenshots

Screenshots are captured in `tests/screenshots/` directory:

### Full Page Screenshots
- `desktop-landing-full.png` - Desktop full page
- `tablet-landing-full.png` - Tablet full page
- `mobile-landing-full.png` - Mobile full page
- `4k-landing-full.png` - 4K display full page
- `mobile-landscape-full.png` - Mobile landscape

### Section Screenshots
- `section-hero.png` - Hero section
- `section-footer.png` - Footer section
- `all-sections.png` - All sections together

### Component Screenshots
- `component-logo.png` - Animated logo
- `component-code-block.png` - Code block
- `component-buttons.png` - Button group
- `component-minecraft-panel.png` - Stats panel
- `component-scroll-indicator.png` - Scroll indicator

### Responsive Screenshots
- `desktop-hero.png` - Desktop hero
- `tablet-buttons.png` - Tablet buttons
- `mobile-buttons.png` - Mobile buttons
- `mobile-stats.png` - Mobile stats
- `mobile-{width}x{height}.png` - Various viewports

### Visual Regression Snapshots
- `hero-section.png` - Hero visual baseline
- `stats-panel.png` - Stats visual baseline
- `button-group.png` - Button visual baseline

## Helper Functions

The `helpers.ts` file provides utilities for common test operations:

### Page Interaction
- `waitForPageLoad()` - Wait for full page load
- `scrollToElement()` - Smooth scroll to element
- `isInViewport()` - Check element visibility

### Style Validation
- `hasClass()` - Check CSS class presence
- `getComputedStyle()` - Get computed style property
- `getRGBColor()` - Get color in RGB format
- `hasCSSVariable()` - Check CSS variable exists
- `getCSSVariable()` - Get CSS variable value

### Minecraft Theme Validation
- `hasMinecraftTheming()` - Check Minecraft classes
- `verifyTailwindLoaded()` - Verify Tailwind works

### Screenshots
- `screenshotElement()` - Screenshot with padding
- `testAtViewports()` - Test at multiple viewports

### Performance
- `getPerformanceMetrics()` - Page performance data
- `verifyImagesLoaded()` - Check image loading

### Accessibility
- `checkAccessibility()` - Basic accessibility checks

### Animations
- `waitForAnimations()` - Wait for CSS animations

## Configuration

### Playwright Config (`playwright.config.ts`)

**Test Settings**
- Test directory: `./tests/e2e`
- Parallel execution enabled
- Retries on CI: 2
- Base URL: `http://localhost:3001`

**Browsers**
- Chromium (Desktop)
- Firefox (Desktop)
- WebKit (Safari)
- Mobile Chrome (Pixel 5)
- Mobile Safari (iPhone 12)

**Output**
- HTML report: `tests/playwright-report/`
- Screenshots: On failure
- Videos: On failure
- Traces: On retry

**Web Server**
- Auto-start Next.js dev server on port 3001
- Reuse existing server in development
- Timeout: 2 minutes

## CI/CD Integration

### GitHub Actions Example

```yaml
name: E2E Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-node@v3
        with:
          node-version: 18

      - name: Install dependencies
        run: |
          cd src/web
          npm install
          npx playwright install --with-deps

      - name: Run E2E tests
        run: |
          cd src/web
          npm run test:e2e

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v3
        with:
          name: playwright-report
          path: src/web/tests/playwright-report/

      - name: Upload screenshots
        if: failure()
        uses: actions/upload-artifact@v3
        with:
          name: screenshots
          path: src/web/tests/screenshots/
```

## Best Practices

### Writing Tests

1. **Use descriptive test names**
   ```ts
   test('should apply Minecraft lime color to highlighted text', async ({ page }) => {
     // ...
   })
   ```

2. **Wait for elements properly**
   ```ts
   await page.waitForSelector('h1:has-text("DROPPER")')
   await expect(element).toBeVisible()
   ```

3. **Use helper functions**
   ```ts
   import { waitForPageLoad, hasMinecraftTheming } from './helpers'

   await waitForPageLoad(page)
   const hasTheme = await hasMinecraftTheming(page, '.minecraft-button')
   ```

4. **Group related tests**
   ```ts
   test.describe('Hero Section', () => {
     test('should render title', async ({ page }) => { /* ... */ })
     test('should render subtitle', async ({ page }) => { /* ... */ })
   })
   ```

5. **Take screenshots strategically**
   ```ts
   // Full page
   await page.screenshot({ path: 'full-page.png', fullPage: true })

   // Specific element
   await element.screenshot({ path: 'component.png' })

   // Visual regression
   await expect(element).toHaveScreenshot('baseline.png')
   ```

### Debugging Tests

1. **Run in headed mode**
   ```bash
   npm run test:e2e:headed
   ```

2. **Use debug mode**
   ```bash
   npm run test:e2e:debug
   ```

3. **Add console logs**
   ```ts
   console.log(await element.textContent())
   ```

4. **Pause execution**
   ```ts
   await page.pause()
   ```

5. **View trace**
   ```bash
   npx playwright show-trace trace.zip
   ```

## Troubleshooting

### Tests Fail with "Page not found"

**Issue**: Dev server not running or port conflict

**Solution**:
```bash
# Check if port 3001 is in use
netstat -ano | findstr :3001  # Windows
lsof -i :3001                 # Mac/Linux

# Kill process or change port in playwright.config.ts
```

### Tests Timeout

**Issue**: Page loads slowly or infinite loading

**Solution**:
- Increase timeout in `playwright.config.ts`
- Check network requests in browser DevTools
- Verify server logs

### Screenshots Don't Match

**Issue**: Visual regression tests fail

**Solution**:
- Update baselines: `npx playwright test --update-snapshots`
- Check for font loading issues
- Verify viewport size consistency

### CSS Not Applying

**Issue**: Tailwind styles not detected

**Solution**:
- Verify CSS file loads: Check Network tab
- Check Tailwind config: `tailwind.config.ts`
- Clear Next.js cache: `rm -rf .next`

## Maintenance

### Updating Baselines

When design changes are intentional:

```bash
# Update all visual snapshots
npx playwright test --update-snapshots

# Update specific test
npx playwright test landing-page.spec.ts --update-snapshots
```

### Adding New Tests

1. Create test file in `tests/e2e/`
2. Import test framework and helpers
3. Write descriptive test cases
4. Run tests locally
5. Update this README

### Reviewing Test Reports

After test run:

```bash
# Open HTML report
npx playwright show-report tests/playwright-report
```

## Resources

- [Playwright Documentation](https://playwright.dev)
- [Playwright Best Practices](https://playwright.dev/docs/best-practices)
- [Next.js Testing](https://nextjs.org/docs/testing)
- [Tailwind CSS Documentation](https://tailwindcss.com)

## Support

For issues with tests:
1. Check this README
2. Review Playwright docs
3. Check test output and screenshots
4. Open an issue with:
   - Test file and line number
   - Error message
   - Screenshots (if applicable)
   - Browser and OS version
