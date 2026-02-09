import { test } from '@playwright/test';

test('verify documentation contrast', async ({ page }) => {
  await page.goto('http://localhost:3000', { waitUntil: 'networkidle' });

  // Scroll to documentation
  await page.locator('#documentation').scrollIntoViewIfNeeded();
  await page.waitForTimeout(1000);

  // Click first command to expand it
  await page.locator('.minecraft-panel').first().click();
  await page.waitForTimeout(500);

  // Take screenshot of expanded documentation
  await page.locator('#documentation').screenshot({
    path: 'screenshot-contrast-check.png'
  });
});
