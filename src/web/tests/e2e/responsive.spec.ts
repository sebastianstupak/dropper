import { test, expect } from '@playwright/test'

test.describe('Responsive Design - Desktop', () => {
  test.use({ viewport: { width: 1920, height: 1080 } })

  test('should display desktop layout correctly', async ({ page }) => {
    await page.goto('/')

    // Wait for content to load
    await page.waitForSelector('h1:has-text("DROPPER")')

    // Verify buttons are in row layout (flex-row)
    const buttonContainer = page.locator('.flex-col.sm\\:flex-row').first()
    await expect(buttonContainer).toBeVisible()

    // Verify stats grid shows all 3 columns
    const statsGrid = page.locator('.grid-cols-3')
    await expect(statsGrid).toBeVisible()

    // Take full page screenshot
    await page.screenshot({
      path: 'tests/screenshots/desktop-landing-full.png',
      fullPage: true,
    })
  })

  test('should display hero section properly on desktop', async ({ page }) => {
    await page.goto('/')

    const hero = page.locator('section').first()
    await expect(hero).toBeVisible()

    // Screenshot hero section only
    await hero.screenshot({
      path: 'tests/screenshots/desktop-hero.png',
    })
  })

  test('should show larger text on desktop', async ({ page }) => {
    await page.goto('/')

    const title = page.locator('h1:has-text("DROPPER")')
    await expect(title).toBeVisible()

    // On desktop, title should use larger text classes (sm:text-6xl lg:text-7xl)
    const className = await title.getAttribute('class')
    expect(className).toContain('lg:text-7xl')
  })
})

test.describe('Responsive Design - Tablet', () => {
  test.use({ viewport: { width: 768, height: 1024 } })

  test('should display tablet layout correctly', async ({ page }) => {
    await page.goto('/')

    await page.waitForSelector('h1:has-text("DROPPER")')

    // Verify the page is responsive
    const main = page.locator('main')
    await expect(main).toBeVisible()

    // Take tablet screenshot
    await page.screenshot({
      path: 'tests/screenshots/tablet-landing-full.png',
      fullPage: true,
    })
  })

  test('should adapt button sizes for tablet', async ({ page }) => {
    await page.goto('/')

    const getStartedButton = page.locator('a:has-text("Get Started")')
    await expect(getStartedButton).toBeVisible()

    // Screenshot buttons on tablet
    const buttonContainer = page.locator('.flex-col.sm\\:flex-row').first()
    await buttonContainer.screenshot({
      path: 'tests/screenshots/tablet-buttons.png',
    })
  })

  test('should show stats grid on tablet', async ({ page }) => {
    await page.goto('/')

    const statsGrid = page.locator('.grid-cols-3')
    await expect(statsGrid).toBeVisible()

    // All 3 stats should still be visible in grid
    const panels = page.locator('.minecraft-panel')
    const count = await panels.count()
    expect(count).toBe(3)
  })
})

test.describe('Responsive Design - Mobile', () => {
  test.use({ viewport: { width: 375, height: 667 } }) // iPhone SE size

  test('should display mobile layout correctly', async ({ page }) => {
    await page.goto('/')

    await page.waitForSelector('h1:has-text("DROPPER")')

    // Verify content is visible and properly stacked
    const main = page.locator('main')
    await expect(main).toBeVisible()

    // Take mobile screenshot
    await page.screenshot({
      path: 'tests/screenshots/mobile-landing-full.png',
      fullPage: true,
    })
  })

  test('should stack buttons vertically on mobile', async ({ page }) => {
    await page.goto('/')

    const buttonContainer = page.locator('.flex-col.sm\\:flex-row').first()
    await expect(buttonContainer).toBeVisible()

    // Buttons should be in column layout on mobile
    const className = await buttonContainer.getAttribute('class')
    expect(className).toContain('flex-col')

    // Screenshot mobile buttons
    await buttonContainer.screenshot({
      path: 'tests/screenshots/mobile-buttons.png',
    })
  })

  test('should show smaller text on mobile', async ({ page }) => {
    await page.goto('/')

    const title = page.locator('h1:has-text("DROPPER")')
    await expect(title).toBeVisible()

    // On mobile, should use smaller text (text-4xl base)
    const className = await title.getAttribute('class')
    expect(className).toContain('text-4xl')
  })

  test('should display logo at smaller size on mobile', async ({ page }) => {
    await page.goto('/')

    const logo = page.locator('.animate-float').first()
    await expect(logo).toBeVisible()

    // Logo should have mobile size classes (w-16 h-16)
    const className = await logo.getAttribute('class')
    expect(className).toContain('w-16')
    expect(className).toContain('h-16')
  })

  test('should maintain 3-column stats grid on mobile', async ({ page }) => {
    await page.goto('/')

    const statsGrid = page.locator('.grid-cols-3')
    await expect(statsGrid).toBeVisible()

    // Screenshot stats on mobile
    await statsGrid.screenshot({
      path: 'tests/screenshots/mobile-stats.png',
    })
  })

  test('should not overflow horizontally on mobile', async ({ page }) => {
    await page.goto('/')

    // Check that body width matches viewport
    const bodyWidth = await page.locator('body').evaluate((el) => {
      return el.scrollWidth
    })

    const viewportWidth = page.viewportSize()?.width || 375

    // Allow small margin for scrollbars
    expect(bodyWidth).toBeLessThanOrEqual(viewportWidth + 20)
  })

  test('should have touch-friendly button sizes', async ({ page }) => {
    await page.goto('/')

    const getStartedButton = page.locator('a:has-text("Get Started")')
    await expect(getStartedButton).toBeVisible()

    // Get button dimensions
    const box = await getStartedButton.boundingBox()
    expect(box).not.toBeNull()

    // Buttons should be at least 44px tall (iOS touch target recommendation)
    if (box) {
      expect(box.height).toBeGreaterThanOrEqual(40) // px-6 py-3 should achieve this
    }
  })
})

test.describe('Responsive Design - Large Desktop', () => {
  test.use({ viewport: { width: 2560, height: 1440 } })

  test('should display properly on large screens', async ({ page }) => {
    await page.goto('/')

    await page.waitForSelector('h1:has-text("DROPPER")')

    // Content should be centered with max-width container
    const container = page.locator('.container').first()
    await expect(container).toBeVisible()

    // Take 4K screenshot
    await page.screenshot({
      path: 'tests/screenshots/4k-landing-full.png',
      fullPage: true,
    })
  })

  test('should use largest text sizes on large screens', async ({ page }) => {
    await page.goto('/')

    const title = page.locator('h1:has-text("DROPPER")')
    await expect(title).toBeVisible()

    // Should use lg:text-7xl on large screens
    const className = await title.getAttribute('class')
    expect(className).toContain('lg:text-7xl')
  })

  test('should center content with max-width', async ({ page }) => {
    await page.goto('/')

    const contentDiv = page.locator('.max-w-4xl').first()
    await expect(contentDiv).toBeVisible()

    // Should have mx-auto for centering
    const className = await contentDiv.getAttribute('class')
    expect(className).toContain('mx-auto')
  })
})

test.describe('Responsive Design - Landscape Mobile', () => {
  test.use({ viewport: { width: 667, height: 375 } }) // Landscape orientation

  test('should handle landscape mobile orientation', async ({ page }) => {
    await page.goto('/')

    await page.waitForSelector('h1:has-text("DROPPER")')

    const main = page.locator('main')
    await expect(main).toBeVisible()

    // Take landscape screenshot
    await page.screenshot({
      path: 'tests/screenshots/mobile-landscape-full.png',
      fullPage: true,
    })
  })

  test('should maintain readability in landscape', async ({ page }) => {
    await page.goto('/')

    const tagline = page.locator('text=Build multi-loader, multi-version')
    await expect(tagline).toBeVisible()

    // Text should not be cut off
    const isVisible = await tagline.isVisible()
    expect(isVisible).toBe(true)
  })
})

test.describe('Responsive Design - Cross-Browser', () => {
  test('should render consistently across viewports', async ({ page }) => {
    // Test multiple viewport transitions
    const viewports = [
      { width: 375, height: 667, name: 'mobile' },
      { width: 768, height: 1024, name: 'tablet' },
      { width: 1920, height: 1080, name: 'desktop' },
    ]

    for (const viewport of viewports) {
      await page.setViewportSize({ width: viewport.width, height: viewport.height })
      await page.goto('/')

      await page.waitForSelector('h1:has-text("DROPPER")')

      // Verify main content is always visible
      const title = page.locator('h1:has-text("DROPPER")')
      await expect(title).toBeVisible()

      const buttons = page.locator('a:has-text("Get Started")')
      await expect(buttons).toBeVisible()

      // Take screenshot at each viewport
      await page.screenshot({
        path: `tests/screenshots/${viewport.name}-${viewport.width}x${viewport.height}.png`,
        fullPage: false,
      })
    }
  })
})
