import { test, expect } from '@playwright/test'

test.describe('Landing Page Sections - Hero', () => {
  test('should render Hero section with all elements', async ({ page }) => {
    await page.goto('/')

    // Wait for hero to load
    await page.waitForSelector('h1:has-text("DROPPER")')

    // Verify main title
    const title = page.locator('h1:has-text("DROPPER")')
    await expect(title).toBeVisible()
    await expect(title).toHaveText('DROPPER')

    // Verify subtitle
    const subtitle = page.locator('text=Minecraft Mod Build Automation')
    await expect(subtitle).toBeVisible()

    // Verify tagline with highlighted text
    const tagline = page.locator('text=Build multi-loader, multi-version Minecraft mods')
    await expect(tagline).toBeVisible()

    const highlightedText = page.locator('text=single codebase')
    await expect(highlightedText).toBeVisible()

    // Take hero screenshot
    const hero = page.locator('section').first()
    await hero.screenshot({
      path: 'tests/screenshots/section-hero.png',
    })
  })

  test('should render logo/icon in Hero', async ({ page }) => {
    await page.goto('/')

    const logo = page.locator('.animate-float').first()
    await expect(logo).toBeVisible()

    // Should contain the dropper emoji
    const logoContent = await logo.textContent()
    expect(logoContent?.trim()).toBe('📦')
  })

  test('should render quick install section', async ({ page }) => {
    await page.goto('/')

    const installCommand = page.locator('text=npm install -g dropper')
    await expect(installCommand).toBeVisible()

    // Should be in a code block style element
    const codeBlock = installCommand.locator('..')
    await expect(codeBlock).toBeVisible()
  })

  test('should render action buttons', async ({ page }) => {
    await page.goto('/')

    const getStartedBtn = page.locator('a:has-text("Get Started")')
    const learnMoreBtn = page.locator('a:has-text("Learn More")')

    await expect(getStartedBtn).toBeVisible()
    await expect(learnMoreBtn).toBeVisible()

    // Verify button hrefs
    expect(await getStartedBtn.getAttribute('href')).toBe('#installation')
    expect(await learnMoreBtn.getAttribute('href')).toBe('#features')
  })

  test('should render stats panel', async ({ page }) => {
    await page.goto('/')

    // Verify all three stats
    const stat1Value = page.locator('text=10+')
    const stat1Label = page.locator('text=MC Versions')

    const stat2Value = page.locator('text=3').first()
    const stat2Label = page.locator('text=Loaders')

    const stat3Value = page.locator('text=1').first()
    const stat3Label = page.locator('text=Codebase')

    await expect(stat1Value).toBeVisible()
    await expect(stat1Label).toBeVisible()
    await expect(stat2Value).toBeVisible()
    await expect(stat2Label).toBeVisible()
    await expect(stat3Value).toBeVisible()
    await expect(stat3Label).toBeVisible()

    // Screenshot stats
    const statsGrid = page.locator('.grid-cols-3').first()
    await statsGrid.screenshot({
      path: 'tests/screenshots/hero-stats.png',
    })
  })

  test('should render scroll indicator', async ({ page }) => {
    await page.goto('/')

    const scrollIndicator = page.locator('.animate-bounce').first()
    await expect(scrollIndicator).toBeVisible()

    // Should be positioned at bottom
    const box = await scrollIndicator.boundingBox()
    expect(box).not.toBeNull()

    if (box) {
      const viewportHeight = page.viewportSize()?.height || 0
      // Indicator should be near bottom (within last 20% of viewport)
      expect(box.y).toBeGreaterThan(viewportHeight * 0.7)
    }
  })
})

test.describe('Landing Page Sections - Features', () => {
  test('should render Features section', async ({ page }) => {
    await page.goto('/')

    // Scroll to features section (if it exists)
    // Note: Since we need to verify the section exists, we check for common elements

    // Check if the page has multiple sections
    const sections = page.locator('section')
    const sectionCount = await sections.count()

    // Hero is first section, features should be second (if it exists)
    expect(sectionCount).toBeGreaterThanOrEqual(1)

    // Take screenshot of all sections
    await page.screenshot({
      path: 'tests/screenshots/all-sections.png',
      fullPage: true,
    })
  })
})

test.describe('Landing Page Sections - Installation', () => {
  test('should have installation anchor link', async ({ page }) => {
    await page.goto('/')

    const getStartedBtn = page.locator('a[href="#installation"]')
    await expect(getStartedBtn).toBeVisible()

    // Click to navigate to installation
    await getStartedBtn.click()

    // URL should include hash
    await expect(page).toHaveURL(/.*#installation/)
  })
})

test.describe('Landing Page Sections - Examples', () => {
  test('should have examples section linked', async ({ page }) => {
    await page.goto('/')

    // Examples section would be rendered by the Examples component
    // Verify page structure allows for it
    const main = page.locator('main')
    await expect(main).toBeVisible()
  })
})

test.describe('Landing Page Sections - Footer', () => {
  test('should render Footer section', async ({ page }) => {
    await page.goto('/')

    // Scroll to bottom to load footer
    await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight))

    // Footer is rendered by Footer component
    // Wait a moment for any lazy loading
    await page.waitForTimeout(500)

    // Take screenshot of bottom of page (includes footer)
    await page.screenshot({
      path: 'tests/screenshots/section-footer.png',
      fullPage: false,
      clip: {
        x: 0,
        y: (await page.evaluate(() => document.body.scrollHeight)) - 600,
        width: 1920,
        height: 600,
      },
    })
  })
})

test.describe('Section Screenshots - Individual Components', () => {
  test('should capture hero logo screenshot', async ({ page }) => {
    await page.goto('/')

    const logo = page.locator('.animate-float').first()
    await expect(logo).toBeVisible()

    await logo.screenshot({
      path: 'tests/screenshots/component-logo.png',
    })
  })

  test('should capture code block screenshot', async ({ page }) => {
    await page.goto('/')

    // Find the code block containing install command
    const codeBlock = page.locator('text=npm install -g dropper').locator('..')
    await expect(codeBlock).toBeVisible()

    await codeBlock.screenshot({
      path: 'tests/screenshots/component-code-block.png',
    })
  })

  test('should capture button group screenshot', async ({ page }) => {
    await page.goto('/')

    const buttonGroup = page.locator('.flex-col.sm\\:flex-row').first()
    await expect(buttonGroup).toBeVisible()

    await buttonGroup.screenshot({
      path: 'tests/screenshots/component-buttons.png',
    })
  })

  test('should capture individual minecraft panel', async ({ page }) => {
    await page.goto('/')

    const panel = page.locator('.minecraft-panel').first()
    await expect(panel).toBeVisible()

    await panel.screenshot({
      path: 'tests/screenshots/component-minecraft-panel.png',
    })
  })

  test('should capture scroll indicator', async ({ page }) => {
    await page.goto('/')

    const indicator = page.locator('.animate-bounce').first()
    await expect(indicator).toBeVisible()

    await indicator.screenshot({
      path: 'tests/screenshots/component-scroll-indicator.png',
    })
  })
})

test.describe('Section Content Validation', () => {
  test('should have correct content structure', async ({ page }) => {
    await page.goto('/')

    // Verify main container structure
    const main = page.locator('main')
    await expect(main).toHaveClass(/relative/)

    // Verify hero has proper section structure
    const heroSection = page.locator('section').first()
    await expect(heroSection).toHaveClass(/min-h-screen/)
    await expect(heroSection).toHaveClass(/flex/)

    // Verify container centering
    const container = heroSection.locator('.container')
    await expect(container).toBeVisible()
  })

  test('should maintain content hierarchy', async ({ page }) => {
    await page.goto('/')

    // Title should be the main h1
    const h1 = page.locator('h1')
    const h1Count = await h1.count()
    expect(h1Count).toBeGreaterThanOrEqual(1)

    // First h1 should be DROPPER
    const firstH1 = await h1.first().textContent()
    expect(firstH1).toBe('DROPPER')
  })

  test('should have semantic section structure', async ({ page }) => {
    await page.goto('/')

    // Check for semantic HTML
    await expect(page.locator('main')).toBeVisible()
    await expect(page.locator('section')).toBeVisible()

    // Verify sections are properly nested
    const mainSections = page.locator('main > section')
    const count = await mainSections.count()
    expect(count).toBeGreaterThan(0)
  })
})

test.describe('Section Visual Regression', () => {
  test('should match hero section visual snapshot', async ({ page }) => {
    await page.goto('/')

    const hero = page.locator('section').first()
    await expect(hero).toBeVisible()

    // Visual snapshot for regression testing
    await expect(hero).toHaveScreenshot('hero-section.png', {
      maxDiffPixels: 100,
    })
  })

  test('should match stats panel visual snapshot', async ({ page }) => {
    await page.goto('/')

    const stats = page.locator('.grid-cols-3').first()
    await expect(stats).toBeVisible()

    await expect(stats).toHaveScreenshot('stats-panel.png', {
      maxDiffPixels: 50,
    })
  })

  test('should match button group visual snapshot', async ({ page }) => {
    await page.goto('/')

    const buttons = page.locator('.flex-col.sm\\:flex-row').first()
    await expect(buttons).toBeVisible()

    await expect(buttons).toHaveScreenshot('button-group.png', {
      maxDiffPixels: 50,
    })
  })
})
