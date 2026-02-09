import { test } from '@playwright/test';

test('screenshot full documentation', async ({ page }) => {
  await page.goto('http://localhost:3000');

  // Scroll to documentation section
  await page.locator('#documentation').scrollIntoViewIfNeeded();
  await page.waitForTimeout(500);

  // Take full page screenshot from docs section onwards
  await page.screenshot({ path: 'screenshot-full-page.png', fullPage: true });

  // Get the full documentation section height
  const docsHeight = await page.locator('#documentation').boundingBox();
  console.log('Documentation section height:', docsHeight);

  // Screenshot just documentation with more context
  await page.screenshot({
    path: 'screenshot-docs-full-height.png',
    clip: {
      x: 0,
      y: docsHeight!.y,
      width: 1280,
      height: Math.min(docsHeight!.height, 2000)
    }
  });
});
