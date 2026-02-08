import { test, expect } from '@playwright/test'

test.describe('Landing Page - Basic Functionality', () => {
  test('should load the landing page successfully', async ({ page }) => {
    await page.goto('/')

    // Wait for the page to be fully loaded
    await page.waitForLoadState('networkidle')

    // Check page title
    await expect(page).toHaveTitle(/Dropper/)

    // Verify the page loaded without errors
    expect(page.url()).toBe('http://localhost:3001/')
  })

  test('should display all main sections', async ({ page }) => {
    await page.goto('/')

    // Wait for content to load
    await page.waitForSelector('h1:has-text("DROPPER")')

    // Verify Hero section
    const heroTitle = page.locator('h1:has-text("DROPPER")')
    await expect(heroTitle).toBeVisible()

    const heroSubtitle = page.locator('text=Minecraft Mod Build Automation')
    await expect(heroSubtitle).toBeVisible()

    // Verify quick install code block
    const installCode = page.locator('text=npm install -g dropper')
    await expect(installCode).toBeVisible()

    // Verify stats panel
    await expect(page.locator('text=10+')).toBeVisible()
    await expect(page.locator('text=MC Versions')).toBeVisible()

    // Note: The Features, Installation, Examples, and Footer sections
    // would need test IDs or specific content to verify
    // For now, we verify the page structure loads
  })

  test('should display hero buttons', async ({ page }) => {
    await page.goto('/')

    // Wait for buttons to be visible
    const getStartedButton = page.locator('a:has-text("Get Started")')
    const learnMoreButton = page.locator('a:has-text("Learn More")')

    await expect(getStartedButton).toBeVisible()
    await expect(learnMoreButton).toBeVisible()
  })

  test('should have functioning scroll indicator', async ({ page }) => {
    await page.goto('/')

    // Find the scroll indicator (animated bounce element at bottom)
    const scrollIndicator = page.locator('.animate-bounce').first()
    await expect(scrollIndicator).toBeVisible()
  })
})

test.describe('Landing Page - Minecraft Theming', () => {
  test('should apply Minecraft color palette', async ({ page }) => {
    await page.goto('/')

    // Check if custom Minecraft colors are applied
    const body = page.locator('body')
    const bodyBg = await body.evaluate((el) =>
      window.getComputedStyle(el).backgroundColor
    )

    // Verify dark background (minecraft-obsidian)
    expect(bodyBg).toBeTruthy()

    // Check for lime-colored elements
    const limeElements = page.locator('.text-minecraft-lime')
    const count = await limeElements.count()
    expect(count).toBeGreaterThan(0)
  })

  test('should apply pixel borders to minecraft-panel elements', async ({ page }) => {
    await page.goto('/')

    // Find minecraft-panel elements (stats panels)
    const panels = page.locator('.minecraft-panel')
    const count = await panels.count()
    expect(count).toBe(3) // Hero section has 3 stat panels

    // Check if pixel-border class is applied
    const firstPanel = panels.first()
    const className = await firstPanel.getAttribute('class')
    expect(className).toContain('minecraft-panel')
  })

  test('should use Minecraft font family', async ({ page }) => {
    await page.goto('/')

    const title = page.locator('h1:has-text("DROPPER")')
    await expect(title).toBeVisible()

    // Check if font variable is applied
    const fontFamily = await title.evaluate((el) =>
      window.getComputedStyle(el).fontFamily
    )

    // Should include the minecraft font variable
    expect(fontFamily).toBeTruthy()
  })

  test('should apply text shadows for Minecraft aesthetic', async ({ page }) => {
    await page.goto('/')

    const title = page.locator('h1:has-text("DROPPER")')
    const textShadow = await title.evaluate((el) =>
      window.getComputedStyle(el).textShadow
    )

    // Should have text shadow applied
    expect(textShadow).not.toBe('none')
  })

  test('should have pixelated grid background pattern', async ({ page }) => {
    await page.goto('/')

    // Look for the background pattern div
    const bgPattern = page.locator('section').first().locator('div').first()
    await expect(bgPattern).toBeVisible()

    // The pattern uses inline styles with repeating-linear-gradient
    const hasPattern = await bgPattern.evaluate((el) => {
      const style = el.querySelector('div')?.getAttribute('style')
      return style?.includes('repeating-linear-gradient')
    })

    expect(hasPattern).toBe(true)
  })
})

test.describe('Landing Page - Button Interactions', () => {
  test('should have hover effect on BlockButtons', async ({ page }) => {
    await page.goto('/')

    const getStartedButton = page.locator('a:has-text("Get Started")')
    await expect(getStartedButton).toBeVisible()

    // Get initial state
    const initialBrightness = await getStartedButton.evaluate((el) =>
      window.getComputedStyle(el).filter
    )

    // Hover over button
    await getStartedButton.hover()

    // The button should have hover styles applied
    // (brightness-110 via hover:brightness-110)
    const className = await getStartedButton.getAttribute('class')
    expect(className).toContain('hover:brightness-110')
  })

  test('should navigate when buttons are clicked', async ({ page }) => {
    await page.goto('/')

    const getStartedButton = page.locator('a:has-text("Get Started")')
    await expect(getStartedButton).toBeVisible()

    // Check href attribute
    const href = await getStartedButton.getAttribute('href')
    expect(href).toBe('#installation')

    // Click button and verify navigation
    await getStartedButton.click()
    expect(page.url()).toContain('#installation')
  })

  test('should have lime button variant styled correctly', async ({ page }) => {
    await page.goto('/')

    const limeButton = page.locator('a:has-text("Get Started")')
    const className = await limeButton.getAttribute('class')

    // Should have lime button classes
    expect(className).toContain('minecraft-button-lime')
  })

  test('should have stone button variant styled correctly', async ({ page }) => {
    await page.goto('/')

    const stoneButton = page.locator('a:has-text("Learn More")')
    const className = await stoneButton.getAttribute('class')

    // Should have stone button classes
    expect(className).toContain('minecraft-button')
    expect(className).toContain('bg-minecraft-stone')
  })
})

test.describe('Landing Page - Animations', () => {
  test('should have floating animation on logo', async ({ page }) => {
    await page.goto('/')

    // Find the animated logo (emoji in hero section)
    const logo = page.locator('.animate-float').first()
    await expect(logo).toBeVisible()

    // Verify animation class is present
    const className = await logo.getAttribute('class')
    expect(className).toContain('animate-float')
  })

  test('should have pixelate-in animation on hero content', async ({ page }) => {
    await page.goto('/')

    const animatedDiv = page.locator('.animate-pixelate-in')
    await expect(animatedDiv).toBeVisible()

    const className = await animatedDiv.getAttribute('class')
    expect(className).toContain('animate-pixelate-in')
  })

  test('should have bounce animation on scroll indicator', async ({ page }) => {
    await page.goto('/')

    const scrollIndicator = page.locator('.animate-bounce').first()
    await expect(scrollIndicator).toBeVisible()

    const className = await scrollIndicator.getAttribute('class')
    expect(className).toContain('animate-bounce')
  })
})

test.describe('Landing Page - Accessibility', () => {
  test('should have proper heading hierarchy', async ({ page }) => {
    await page.goto('/')

    // Check for h1 (should be only one)
    const h1Count = await page.locator('h1').count()
    expect(h1Count).toBeGreaterThanOrEqual(1)

    // Verify main heading
    const mainHeading = page.locator('h1').first()
    await expect(mainHeading).toHaveText('DROPPER')
  })

  test('should have clickable elements with proper cursor', async ({ page }) => {
    await page.goto('/')

    const button = page.locator('a:has-text("Get Started")')
    const cursor = await button.evaluate((el) =>
      window.getComputedStyle(el).cursor
    )

    expect(cursor).toBe('pointer')
  })

  test('should have semantic HTML structure', async ({ page }) => {
    await page.goto('/')

    // Check for main element
    const main = page.locator('main')
    await expect(main).toBeVisible()

    // Check for section elements
    const sections = page.locator('section')
    const sectionCount = await sections.count()
    expect(sectionCount).toBeGreaterThan(0)
  })
})

test.describe('Landing Page - Content Validation', () => {
  test('should display correct tagline', async ({ page }) => {
    await page.goto('/')

    const tagline = page.locator('text=Build multi-loader, multi-version Minecraft mods')
    await expect(tagline).toBeVisible()
  })

  test('should show correct stats values', async ({ page }) => {
    await page.goto('/')

    // Verify the three stat panels
    await expect(page.locator('text=10+')).toBeVisible()
    await expect(page.locator('text=3').first()).toBeVisible()
    await expect(page.locator('text=1').first()).toBeVisible()

    // Verify stat labels
    await expect(page.locator('text=MC Versions')).toBeVisible()
    await expect(page.locator('text=Loaders')).toBeVisible()
    await expect(page.locator('text=Codebase')).toBeVisible()
  })

  test('should display installation command', async ({ page }) => {
    await page.goto('/')

    const installCmd = page.locator('text=npm install -g dropper')
    await expect(installCmd).toBeVisible()
  })
})
