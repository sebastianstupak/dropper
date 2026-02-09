import { test } from '@playwright/test';

test('screenshot documentation section', async ({ page }) => {
  await page.goto('http://localhost:3000');

  // Scroll to documentation section
  await page.locator('#documentation').scrollIntoViewIfNeeded();
  await page.waitForTimeout(500);

  // Screenshot the documentation section
  await page.locator('#documentation').screenshot({ path: 'screenshot-docs-section.png' });

  // Click first command to expand it
  await page.locator('.minecraft-panel').first().click();
  await page.waitForTimeout(300);

  // Screenshot expanded command
  await page.locator('#documentation').screenshot({ path: 'screenshot-docs-expanded.png' });
});
