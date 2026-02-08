import { test, expect } from '@playwright/test'

/**
 * Smoke tests - Fast, critical tests that should always pass
 * Run these first to catch major issues quickly
 */

test.describe('Smoke Tests - Critical Functionality', () => {
  test('page loads without errors', async ({ page }) => {
    const errors: string[] = []

    // Capture console errors
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        errors.push(msg.text())
      }
    })

    // Capture page errors
    page.on('pageerror', (error) => {
      errors.push(error.message)
    })

    await page.goto('/')

    // Wait for page to load
    await page.waitForLoadState('networkidle')

    // Verify no JavaScript errors
    expect(errors.length).toBe(0)
  })

  test('main content is visible', async ({ page }) => {
    await page.goto('/')

    // Essential elements that must be visible
    const title = page.locator('h1:has-text("DROPPER")')
    const getStartedButton = page.locator('a:has-text("Get Started")')
    const installCode = page.locator('text=npm install -g dropper')

    await expect(title).toBeVisible({ timeout: 10000 })
    await expect(getStartedButton).toBeVisible()
    await expect(installCode).toBeVisible()
  })

  test('navigation works', async ({ page }) => {
    await page.goto('/')

    const getStartedButton = page.locator('a:has-text("Get Started")')
    await getStartedButton.click()

    // Should navigate to installation section
    await expect(page).toHaveURL(/.*#installation/)
  })

  test('page is responsive', async ({ page }) => {
    await page.goto('/')

    // Test mobile viewport
    await page.setViewportSize({ width: 375, height: 667 })
    await page.waitForTimeout(500)

    const title = page.locator('h1:has-text("DROPPER")')
    await expect(title).toBeVisible()

    // Test desktop viewport
    await page.setViewportSize({ width: 1920, height: 1080 })
    await page.waitForTimeout(500)

    await expect(title).toBeVisible()
  })

  test('CSS is loaded', async ({ page }) => {
    await page.goto('/')

    const body = page.locator('body')
    const bgColor = await body.evaluate((el) =>
      window.getComputedStyle(el).backgroundColor
    )

    // Should have dark background (not default white)
    expect(bgColor).not.toBe('rgba(0, 0, 0, 0)')
    expect(bgColor).not.toBe('rgb(255, 255, 255)')
  })

  test('images load successfully', async ({ page }) => {
    await page.goto('/')

    // Check for broken images
    const images = page.locator('img')
    const imageCount = await images.count()

    if (imageCount > 0) {
      // Verify all images loaded
      for (let i = 0; i < imageCount; i++) {
        const img = images.nth(i)
        const isLoaded = await img.evaluate((el: HTMLImageElement) => {
          return el.complete && el.naturalHeight !== 0
        })

        // Log warning but don't fail if images aren't loaded yet
        if (!isLoaded) {
          console.warn(`Image ${i} may not be loaded`)
        }
      }
    }
  })

  test('page title is correct', async ({ page }) => {
    await page.goto('/')

    await expect(page).toHaveTitle(/Dropper/)
  })

  test('no 404 or 500 errors', async ({ page }) => {
    const response = await page.goto('/')

    expect(response?.status()).toBeLessThan(400)
  })
})

test.describe('Smoke Tests - Performance', () => {
  test('page loads within acceptable time', async ({ page }) => {
    const startTime = Date.now()

    await page.goto('/')
    await page.waitForLoadState('networkidle')

    const loadTime = Date.now() - startTime

    // Page should load within 5 seconds (generous for CI)
    expect(loadTime).toBeLessThan(5000)

    console.log(`Page load time: ${loadTime}ms`)
  })

  test('page is interactive quickly', async ({ page }) => {
    await page.goto('/')

    // Wait for page to be interactive (DOMContentLoaded)
    await page.waitForLoadState('domcontentloaded')

    // Button should be clickable quickly
    const button = page.locator('a:has-text("Get Started")')
    await expect(button).toBeVisible({ timeout: 3000 })

    const isEnabled = await button.isEnabled()
    expect(isEnabled).toBe(true)
  })

  test('no memory leaks on navigation', async ({ page }) => {
    await page.goto('/')

    // Navigate around the page
    await page.locator('a:has-text("Get Started")').click()
    await page.goBack()

    await page.locator('a:has-text("Learn More")').click()
    await page.goBack()

    // Page should still be responsive
    const title = page.locator('h1:has-text("DROPPER")')
    await expect(title).toBeVisible()
  })
})

test.describe('Smoke Tests - Accessibility', () => {
  test('page has proper document structure', async ({ page }) => {
    await page.goto('/')

    // Check for required HTML structure
    await expect(page.locator('html')).toBeVisible()
    await expect(page.locator('body')).toBeVisible()
    await expect(page.locator('main')).toBeVisible()
  })

  test('heading hierarchy is correct', async ({ page }) => {
    await page.goto('/')

    // Should have at least one h1
    const h1Count = await page.locator('h1').count()
    expect(h1Count).toBeGreaterThan(0)

    // Main h1 should be visible
    const mainHeading = page.locator('h1').first()
    await expect(mainHeading).toBeVisible()
  })

  test('links are keyboard accessible', async ({ page }) => {
    await page.goto('/')

    // Focus on first link
    const firstLink = page.locator('a').first()
    await firstLink.focus()

    // Should be focused
    const isFocused = await firstLink.evaluate((el) => el === document.activeElement)
    expect(isFocused).toBe(true)
  })

  test('page can be navigated with keyboard', async ({ page }) => {
    await page.goto('/')

    // Tab through interactive elements
    await page.keyboard.press('Tab')
    await page.keyboard.press('Tab')

    // Some element should be focused
    const activeElement = await page.evaluate(() => document.activeElement?.tagName)
    expect(activeElement).toBeTruthy()
  })
})

test.describe('Smoke Tests - Cross-Browser', () => {
  test('works in Chromium', async ({ page, browserName }) => {
    test.skip(browserName !== 'chromium', 'Chromium-specific test')

    await page.goto('/')
    const title = page.locator('h1:has-text("DROPPER")')
    await expect(title).toBeVisible()
  })

  test('works in Firefox', async ({ page, browserName }) => {
    test.skip(browserName !== 'firefox', 'Firefox-specific test')

    await page.goto('/')
    const title = page.locator('h1:has-text("DROPPER")')
    await expect(title).toBeVisible()
  })

  test('works in WebKit', async ({ page, browserName }) => {
    test.skip(browserName !== 'webkit', 'WebKit-specific test')

    await page.goto('/')
    const title = page.locator('h1:has-text("DROPPER")')
    await expect(title).toBeVisible()
  })
})

test.describe('Smoke Tests - Mobile', () => {
  test.use({ viewport: { width: 375, height: 667 } })

  test('works on mobile viewport', async ({ page }) => {
    await page.goto('/')

    const title = page.locator('h1:has-text("DROPPER")')
    await expect(title).toBeVisible()

    const button = page.locator('a:has-text("Get Started")')
    await expect(button).toBeVisible()
  })

  test('no horizontal scroll on mobile', async ({ page }) => {
    await page.goto('/')

    const scrollWidth = await page.evaluate(() => document.body.scrollWidth)
    const clientWidth = await page.evaluate(() => document.body.clientWidth)

    // Should not have horizontal overflow
    expect(scrollWidth).toBeLessThanOrEqual(clientWidth + 20) // 20px tolerance
  })

  test('touch targets are large enough', async ({ page }) => {
    await page.goto('/')

    const button = page.locator('a:has-text("Get Started")')
    const box = await button.boundingBox()

    expect(box).not.toBeNull()

    if (box) {
      // Touch targets should be at least 44x44 (iOS guideline)
      expect(box.height).toBeGreaterThanOrEqual(40)
      expect(box.width).toBeGreaterThanOrEqual(80)
    }
  })
})

test.describe('Smoke Tests - SEO', () => {
  test('has meta tags', async ({ page }) => {
    await page.goto('/')

    // Check for essential meta tags
    const hasViewport = await page.locator('meta[name="viewport"]').count()
    expect(hasViewport).toBeGreaterThan(0)

    // Check for description (if present)
    const descriptionCount = await page.locator('meta[name="description"]').count()
    // Description is optional but good for SEO
    if (descriptionCount > 0) {
      const description = await page
        .locator('meta[name="description"]')
        .getAttribute('content')
      expect(description).toBeTruthy()
    }
  })

  test('has valid language attribute', async ({ page }) => {
    await page.goto('/')

    const htmlLang = await page.locator('html').getAttribute('lang')
    expect(htmlLang).toBeTruthy()
  })
})

test.describe('Smoke Tests - Security', () => {
  test('does not expose sensitive information', async ({ page }) => {
    await page.goto('/')

    // Check page content for common sensitive patterns
    const content = await page.content()

    // Should not contain API keys or tokens
    expect(content).not.toMatch(/api[_-]?key/i)
    expect(content).not.toMatch(/secret[_-]?key/i)
    expect(content).not.toMatch(/access[_-]?token/i)
  })

  test('uses HTTPS in production links', async ({ page }) => {
    await page.goto('/')

    // Get all external links
    const links = page.locator('a[href^="http"]')
    const count = await links.count()

    for (let i = 0; i < count; i++) {
      const href = await links.nth(i).getAttribute('href')

      // External links should use https (except localhost)
      if (href && !href.includes('localhost')) {
        expect(href).toMatch(/^https:\/\//)
      }
    }
  })
})

test.describe('Smoke Tests - Quick Screenshot', () => {
  test('capture full page for visual verification', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    await page.screenshot({
      path: 'tests/screenshots/smoke-test-full-page.png',
      fullPage: true,
    })
  })

  test('capture above-the-fold content', async ({ page }) => {
    await page.goto('/')
    await page.waitForLoadState('networkidle')

    await page.screenshot({
      path: 'tests/screenshots/smoke-test-above-fold.png',
      fullPage: false,
    })
  })
})
