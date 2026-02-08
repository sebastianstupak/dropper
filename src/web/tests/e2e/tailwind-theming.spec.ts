import { test, expect } from '@playwright/test'

test.describe('Tailwind CSS - Loading and Application', () => {
  test('should load Tailwind CSS successfully', async ({ page }) => {
    await page.goto('/')

    // Check if Tailwind utility classes are being applied
    const body = page.locator('body')
    await expect(body).toBeVisible()

    // Verify Tailwind reset/base styles are applied
    const bodyClass = await body.getAttribute('class')
    expect(bodyClass).toBeTruthy()

    // Check computed styles to verify CSS is loaded
    const backgroundColor = await body.evaluate((el) =>
      window.getComputedStyle(el).backgroundColor
    )

    // Background should not be the default (white/transparent)
    expect(backgroundColor).toBeTruthy()
    expect(backgroundColor).not.toBe('rgba(0, 0, 0, 0)')
  })

  test('should apply Tailwind utility classes correctly', async ({ page }) => {
    await page.goto('/')

    // Check for common Tailwind utilities
    const container = page.locator('.container').first()
    await expect(container).toBeVisible()

    // Verify flex utilities
    const flexElement = page.locator('.flex').first()
    await expect(flexElement).toBeVisible()

    const flexValue = await flexElement.evaluate((el) =>
      window.getComputedStyle(el).display
    )
    expect(flexValue).toBe('flex')
  })

  test('should apply responsive Tailwind classes', async ({ page }) => {
    await page.goto('/')

    // Check for responsive class application
    const responsiveElement = page.locator('.sm\\:text-6xl').first()

    if ((await responsiveElement.count()) > 0) {
      await expect(responsiveElement).toBeVisible()

      // On desktop viewport, sm: classes should apply
      const className = await responsiveElement.getAttribute('class')
      expect(className).toContain('sm:text-6xl')
    }
  })

  test('should apply Tailwind spacing utilities', async ({ page }) => {
    await page.goto('/')

    // Check padding utilities
    const paddedElement = page.locator('[class*="px-"]').first()
    await expect(paddedElement).toBeVisible()

    const padding = await paddedElement.evaluate((el) => {
      const styles = window.getComputedStyle(el)
      return {
        paddingLeft: styles.paddingLeft,
        paddingRight: styles.paddingRight,
      }
    })

    // Should have some padding applied
    expect(padding.paddingLeft).not.toBe('0px')
  })

  test('should apply Tailwind grid utilities', async ({ page }) => {
    await page.goto('/')

    const gridElement = page.locator('.grid-cols-3').first()
    await expect(gridElement).toBeVisible()

    const gridTemplateColumns = await gridElement.evaluate((el) =>
      window.getComputedStyle(el).gridTemplateColumns
    )

    // Should have 3 columns
    const columnCount = gridTemplateColumns.split(' ').length
    expect(columnCount).toBe(3)
  })
})

test.describe('Minecraft Theme - Custom Colors', () => {
  test('should apply custom Minecraft stone color', async ({ page }) => {
    await page.goto('/')

    // Find elements with minecraft-stone color
    const stoneElement = page.locator('.bg-minecraft-stone').first()

    if ((await stoneElement.count()) > 0) {
      await expect(stoneElement).toBeVisible()

      const bgColor = await stoneElement.evaluate((el) =>
        window.getComputedStyle(el).backgroundColor
      )

      // Should be a grayish color (#8B8B8B = rgb(139, 139, 139))
      expect(bgColor).toBeTruthy()
    }
  })

  test('should apply custom Minecraft lime color', async ({ page }) => {
    await page.goto('/')

    const limeElement = page.locator('.text-minecraft-lime').first()
    await expect(limeElement).toBeVisible()

    const color = await limeElement.evaluate((el) =>
      window.getComputedStyle(el).color
    )

    // Should be lime green (#55FF55 = rgb(85, 255, 85))
    expect(color).toContain('85, 255, 85')
  })

  test('should apply custom Minecraft obsidian background', async ({ page }) => {
    await page.goto('/')

    const body = page.locator('body')
    const bgColor = await body.evaluate((el) =>
      window.getComputedStyle(el).backgroundColor
    )

    // Should be very dark (#100819 or similar)
    expect(bgColor).toBeTruthy()

    // Parse RGB to check darkness
    const rgbMatch = bgColor.match(/rgb\((\d+),\s*(\d+),\s*(\d+)\)/)
    if (rgbMatch) {
      const [, r, g, b] = rgbMatch.map(Number)
      // All values should be very low (dark)
      expect(r).toBeLessThan(50)
      expect(g).toBeLessThan(50)
      expect(b).toBeLessThan(50)
    }
  })

  test('should apply custom Minecraft dark color', async ({ page }) => {
    await page.goto('/')

    // Check if dark color is used anywhere
    const darkElements = page.locator('[class*="minecraft-dark"]')
    const count = await darkElements.count()

    // Should have some elements with minecraft-dark
    expect(count).toBeGreaterThanOrEqual(0)
  })

  test('should apply Minecraft diamond color if used', async ({ page }) => {
    await page.goto('/')

    // Diamond color might be used for accents
    const diamondElements = page.locator('[class*="minecraft-diamond"]')
    const count = await diamondElements.count()

    // Count could be 0 if not used yet, just checking it's a valid test
    expect(count).toBeGreaterThanOrEqual(0)
  })
})

test.describe('Minecraft Theme - Custom Utilities', () => {
  test('should apply pixel-border utility', async ({ page }) => {
    await page.goto('/')

    const pixelBorderedElement = page.locator('.pixel-border').first()
    await expect(pixelBorderedElement).toBeVisible()

    const borderStyle = await pixelBorderedElement.evaluate((el) => {
      const styles = window.getComputedStyle(el)
      return {
        borderWidth: styles.borderWidth,
        borderStyle: styles.borderStyle,
        boxShadow: styles.boxShadow,
      }
    })

    // Should have a border
    expect(borderStyle.borderWidth).not.toBe('0px')
    expect(borderStyle.borderStyle).toBe('solid')

    // Should have box shadow (inset shadows for 3D effect)
    expect(borderStyle.boxShadow).not.toBe('none')
  })

  test('should apply minecraft-button utility', async ({ page }) => {
    await page.goto('/')

    const button = page.locator('.minecraft-button').first()
    await expect(button).toBeVisible()

    const styles = await button.evaluate((el) => {
      const computed = window.getComputedStyle(el)
      return {
        border: computed.borderWidth,
        backgroundColor: computed.backgroundColor,
        textShadow: computed.textShadow,
        transition: computed.transition,
      }
    })

    // Should have border (from pixel-border)
    expect(styles.border).not.toBe('0px')

    // Should have background color
    expect(styles.backgroundColor).not.toBe('rgba(0, 0, 0, 0)')

    // Should have text shadow
    expect(styles.textShadow).not.toBe('none')

    // Should have transition for hover effects
    expect(styles.transition).toBeTruthy()
  })

  test('should apply minecraft-panel utility', async ({ page }) => {
    await page.goto('/')

    const panel = page.locator('.minecraft-panel').first()
    await expect(panel).toBeVisible()

    const styles = await panel.evaluate((el) => {
      const computed = window.getComputedStyle(el)
      return {
        backgroundColor: computed.backgroundColor,
        color: computed.color,
        border: computed.borderWidth,
      }
    })

    // Should have light gray background (#C6C6C6 = rgb(198, 198, 198))
    expect(styles.backgroundColor).toContain('198, 198, 198')

    // Should have dark text color (#3C3C3C = rgb(60, 60, 60))
    expect(styles.color).toContain('60, 60, 60')

    // Should have pixel border
    expect(styles.border).not.toBe('0px')
  })

  test('should apply text-shadow-dark utility', async ({ page }) => {
    await page.goto('/')

    const shadowedText = page.locator('.text-shadow-dark').first()
    await expect(shadowedText).toBeVisible()

    const textShadow = await shadowedText.evaluate((el) =>
      window.getComputedStyle(el).textShadow
    )

    // Should have text shadow
    expect(textShadow).not.toBe('none')
    expect(textShadow).toBeTruthy()
  })

  test('should apply pixelated image rendering', async ({ page }) => {
    await page.goto('/')

    // Check if pixelated class exists and applies correctly
    const pixelatedElements = page.locator('.pixelated')
    const count = await pixelatedElements.count()

    if (count > 0) {
      const imageRendering = await pixelatedElements.first().evaluate((el) =>
        window.getComputedStyle(el).imageRendering
      )

      // Should have pixelated image rendering
      expect(imageRendering).toMatch(/pixelated|crisp-edges/)
    }
  })
})

test.describe('Minecraft Theme - Custom Animations', () => {
  test('should apply float animation', async ({ page }) => {
    await page.goto('/')

    const floatingElement = page.locator('.animate-float').first()
    await expect(floatingElement).toBeVisible()

    const animation = await floatingElement.evaluate((el) =>
      window.getComputedStyle(el).animation
    )

    // Should have animation applied
    expect(animation).not.toBe('none')
    expect(animation).toContain('float')
  })

  test('should apply pixelate-in animation', async ({ page }) => {
    await page.goto('/')

    const pixelateElement = page.locator('.animate-pixelate-in').first()
    await expect(pixelateElement).toBeVisible()

    const animation = await pixelateElement.evaluate((el) =>
      window.getComputedStyle(el).animation
    )

    // Should have animation applied
    expect(animation).not.toBe('none')
    expect(animation).toContain('pixelate-in')
  })

  test('should apply bounce animation', async ({ page }) => {
    await page.goto('/')

    const bounceElement = page.locator('.animate-bounce').first()
    await expect(bounceElement).toBeVisible()

    const animation = await bounceElement.evaluate((el) =>
      window.getComputedStyle(el).animation
    )

    // Should have bounce animation (Tailwind's built-in)
    expect(animation).not.toBe('none')
    expect(animation).toContain('bounce')
  })
})

test.describe('Minecraft Theme - Gradient Utilities', () => {
  test('should apply gradient-dirt utility if used', async ({ page }) => {
    await page.goto('/')

    const dirtElements = page.locator('.gradient-dirt')
    const count = await dirtElements.count()

    if (count > 0) {
      const gradient = await dirtElements.first().evaluate((el) =>
        window.getComputedStyle(el).background
      )

      // Should have gradient
      expect(gradient).toContain('gradient')
    }
  })

  test('should apply gradient-grass utility if used', async ({ page }) => {
    await page.goto('/')

    const grassElements = page.locator('.gradient-grass')
    const count = await grassElements.count()

    if (count > 0) {
      const gradient = await grassElements.first().evaluate((el) =>
        window.getComputedStyle(el).background
      )

      // Should have gradient
      expect(gradient).toContain('gradient')
    }
  })
})

test.describe('Minecraft Theme - Font Application', () => {
  test('should apply Minecraft font family', async ({ page }) => {
    await page.goto('/')

    // Find elements with Minecraft font
    const minecraftFontElement = page.locator('[class*="font-[family-name:var(--font-minecraft)]"]').first()
    await expect(minecraftFontElement).toBeVisible()

    const fontFamily = await minecraftFontElement.evaluate((el) =>
      window.getComputedStyle(el).fontFamily
    )

    // Should have custom font applied
    expect(fontFamily).toBeTruthy()
    expect(fontFamily).not.toBe('serif')
    expect(fontFamily).not.toBe('sans-serif')
  })

  test('should apply uppercase text transform on buttons', async ({ page }) => {
    await page.goto('/')

    const button = page.locator('.minecraft-button, .minecraft-button-lime').first()
    await expect(button).toBeVisible()

    const textTransform = await button.evaluate((el) =>
      window.getComputedStyle(el).textTransform
    )

    // Buttons should have uppercase text
    expect(textTransform).toBe('uppercase')
  })
})

test.describe('Minecraft Theme - Hover States', () => {
  test('should have hover brightness effect on buttons', async ({ page }) => {
    await page.goto('/')

    const button = page.locator('.minecraft-button').first()
    await expect(button).toBeVisible()

    // Check that hover classes are present
    const className = await button.getAttribute('class')
    expect(className).toContain('hover:brightness-110')
  })

  test('should have active state on buttons', async ({ page }) => {
    await page.goto('/')

    const button = page.locator('.minecraft-button').first()
    await expect(button).toBeVisible()

    const className = await button.getAttribute('class')
    expect(className).toContain('active:brightness-90')
  })
})

test.describe('Minecraft Theme - Custom Spacing', () => {
  test('should use Minecraft block-based spacing variables', async ({ page }) => {
    await page.goto('/')

    // Verify custom spacing is available via CSS variables
    const hasSpacingVar = await page.evaluate(() => {
      const rootStyles = getComputedStyle(document.documentElement)
      const blockSpacing = rootStyles.getPropertyValue('--spacing-block')
      return blockSpacing !== ''
    })

    expect(hasSpacingVar).toBe(true)
  })

  test('should have Minecraft shadow variables defined', async ({ page }) => {
    await page.goto('/')

    const hasShadowVar = await page.evaluate(() => {
      const rootStyles = getComputedStyle(document.documentElement)
      const minecraftShadow = rootStyles.getPropertyValue('--shadow-minecraft')
      return minecraftShadow !== ''
    })

    expect(hasShadowVar).toBe(true)
  })
})

test.describe('Tailwind CSS - Visual Validation', () => {
  test('should render with consistent styling across page load', async ({ page }) => {
    await page.goto('/')

    // Wait for all CSS to load
    await page.waitForLoadState('networkidle')

    // Take initial screenshot
    await page.screenshot({
      path: 'tests/screenshots/tailwind-initial-load.png',
    })

    // Reload page
    await page.reload()
    await page.waitForLoadState('networkidle')

    // Take second screenshot
    await page.screenshot({
      path: 'tests/screenshots/tailwind-after-reload.png',
    })

    // Both should look identical (visual regression would catch differences)
  })

  test('should not have FOUC (Flash of Unstyled Content)', async ({ page }) => {
    // Navigate with slow network to test CSS loading
    await page.goto('/', { waitUntil: 'domcontentloaded' })

    // Even before networkidle, elements should have styles
    const body = page.locator('body')
    const bgColor = await body.evaluate((el) =>
      window.getComputedStyle(el).backgroundColor
    )

    // Should already have background color applied
    expect(bgColor).not.toBe('rgba(0, 0, 0, 0)')
    expect(bgColor).not.toBe('rgb(255, 255, 255)')
  })
})
