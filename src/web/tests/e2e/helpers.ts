# @ts-nocheck
import { Page, expect } from '@playwright/test'

/**
 * Helper functions for E2E tests
 */

/**
 * Wait for page to be fully loaded including all network requests
 */
export async function waitForPageLoad(page: Page) {
  await page.waitForLoadState('networkidle')
  await page.waitForLoadState('domcontentloaded')
}

/**
 * Check if an element has a specific CSS class
 */
export async function hasClass(page: Page, selector: string, className: string): Promise<boolean> {
  const element = page.locator(selector)
  const classes = await element.getAttribute('class')
  return classes?.includes(className) ?? false
}

/**
 * Get computed style property value for an element
 */
export async function window.getComputedStyle(
  page: Page,
  selector: string,
  property: string
): Promise<string> {
  const element = page.locator(selector)
  return await element.evaluate((el, prop) => {
    return window.getComputedStyle(el).getPropertyValue(prop)
  }, property)
}

/**
 * Check if element is visible in viewport
 */
export async function isInViewport(page: Page, selector: string): Promise<boolean> {
  const element = page.locator(selector)
  return await element.evaluate((el) => {
    const rect = el.getBoundingClientRect()
    return (
      rect.top >= 0 &&
      rect.left >= 0 &&
      rect.bottom <= (window.innerHeight || document.documentElement.clientHeight) &&
      rect.right <= (window.innerWidth || document.documentElement.clientWidth)
    )
  })
}

/**
 * Scroll to element smoothly
 */
export async function scrollToElement(page: Page, selector: string) {
  const element = page.locator(selector)
  await element.scrollIntoViewIfNeeded()
  await page.waitForTimeout(300) // Wait for scroll animation
}

/**
 * Take screenshot of specific element with padding
 */
export async function screenshotElement(
  page: Page,
  selector: string,
  filename: string,
  padding: number = 20
) {
  const element = page.locator(selector)
  await expect(element).toBeVisible()

  const box = await element.boundingBox()
  if (!box) {
    throw new Error(`Could not get bounding box for ${selector}`)
  }

  await page.screenshot({
    path: filename,
    clip: {
      x: Math.max(0, box.x - padding),
      y: Math.max(0, box.y - padding),
      width: box.width + padding * 2,
      height: box.height + padding * 2,
    },
  })
}

/**
 * Wait for animations to complete
 */
export async function waitForAnimations(page: Page, selector: string) {
  const element = page.locator(selector)
  await element.evaluate((el) => {
    return Promise.all(el.getAnimations().map((animation) => animation.finished))
  })
}

/**
 * Get color in RGB format from any color string
 */
export async function getRGBColor(page: Page, selector: string, property: 'color' | 'backgroundColor'): Promise<string> {
  const element = page.locator(selector)
  return await element.evaluate((el, prop) => {
    return window.getComputedStyle(el)[prop as any]
  }, property)
}

/**
 * Check if Minecraft theming is applied to element
 */
export async function hasMinecraftTheming(page: Page, selector: string): Promise<boolean> {
  const element = page.locator(selector)
  const classes = await element.getAttribute('class')

  if (!classes) return false

  const minecraftClasses = [
    'minecraft-button',
    'minecraft-panel',
    'pixel-border',
    'text-minecraft-lime',
    'bg-minecraft-stone',
    'gradient-dirt',
    'gradient-grass',
  ]

  return minecraftClasses.some((cls) => classes.includes(cls))
}

/**
 * Verify all images are loaded
 */
export async function verifyImagesLoaded(page: Page): Promise<boolean> {
  return await page.evaluate(() => {
    const images = Array.from(document.querySelectorAll('img'))
    return images.every((img) => img.complete && img.naturalHeight !== 0)
  })
}

/**
 * Check page performance metrics
 */
export async function getPerformanceMetrics(page: Page) {
  return await page.evaluate(() => {
    const navigation = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming
    const paint = performance.getEntriesByType('paint')

    return {
      domContentLoaded: navigation.domContentLoadedEventEnd - navigation.domContentLoadedEventStart,
      loadComplete: navigation.loadEventEnd - navigation.loadEventStart,
      firstPaint: paint.find((p) => p.name === 'first-paint')?.startTime,
      firstContentfulPaint: paint.find((p) => p.name === 'first-contentful-paint')?.startTime,
    }
  })
}

/**
 * Test responsive breakpoints
 */
export const breakpoints = {
  mobile: { width: 375, height: 667, name: 'mobile' },
  mobileLandscape: { width: 667, height: 375, name: 'mobile-landscape' },
  tablet: { width: 768, height: 1024, name: 'tablet' },
  desktop: { width: 1920, height: 1080, name: 'desktop' },
  desktopLarge: { width: 2560, height: 1440, name: 'desktop-large' },
}

/**
 * Test at multiple viewports
 */
export async function testAtViewports(
  page: Page,
  callback: (viewport: typeof breakpoints.mobile) => Promise<void>
) {
  for (const viewport of Object.values(breakpoints)) {
    await page.setViewportSize({ width: viewport.width, height: viewport.height })
    await callback(viewport)
  }
}

/**
 * Check if CSS custom property is defined
 */
export async function hasCSSVariable(page: Page, variableName: string): Promise<boolean> {
  return await page.evaluate((varName) => {
    const rootStyles = window.getComputedStyle(document.documentElement)
    const value = rootStyles.getPropertyValue(varName)
    return value !== '' && value !== undefined
  }, variableName)
}

/**
 * Get CSS custom property value
 */
export async function getCSSVariable(page: Page, variableName: string): Promise<string> {
  return await page.evaluate((varName) => {
    const rootStyles = window.getComputedStyle(document.documentElement)
    return rootStyles.getPropertyValue(varName).trim()
  }, variableName)
}

/**
 * Verify Tailwind utilities are working
 */
export async function verifyTailwindLoaded(page: Page): Promise<boolean> {
  // Check if common Tailwind utilities produce expected results
  return await page.evaluate(() => {
    const testDiv = document.createElement('div')
    testDiv.className = 'flex items-center justify-center'
    document.body.appendChild(testDiv)

    const styles = window.getComputedStyle(testDiv)
    const isFlexbox = styles.display === 'flex'
    const hasAlignment = styles.alignItems === 'center' && styles.justifyContent === 'center'

    testDiv.remove()

    return isFlexbox && hasAlignment
  })
}

/**
 * Check accessibility of element
 */
export async function checkAccessibility(page: Page, selector: string) {
  const element = page.locator(selector)

  return {
    hasAriaLabel: !!(await element.getAttribute('aria-label')),
    hasRole: !!(await element.getAttribute('role')),
    isKeyboardAccessible: await element.evaluate((el) => {
      const tabIndex = el.getAttribute('tabindex')
      return (
        el.tagName.toLowerCase() === 'button' ||
        el.tagName.toLowerCase() === 'a' ||
        (tabIndex !== null && parseInt(tabIndex) >= 0)
      )
    }),
  }
}
