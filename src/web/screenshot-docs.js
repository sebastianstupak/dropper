const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:3000');

  // Scroll to documentation section
  await page.evaluate(() => {
    document.querySelector('#documentation').scrollIntoView();
  });

  // Wait for any animations
  await page.waitForTimeout(500);

  // Take full page screenshot
  await page.screenshot({ path: 'screenshot-docs-full.png', fullPage: true });

  // Take screenshot of just the documentation section
  const docsSection = await page.locator('#documentation');
  await docsSection.screenshot({ path: 'screenshot-docs-section.png' });

  // Click first command to expand it
  await page.click('.minecraft-panel');
  await page.waitForTimeout(300);

  // Screenshot expanded command
  await docsSection.screenshot({ path: 'screenshot-docs-expanded.png' });

  await browser.close();
})();
