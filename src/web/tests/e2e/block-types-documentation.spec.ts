import { test, expect } from '@playwright/test';

test.describe('Block Types Documentation', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:3000');

    // Scroll to documentation section
    await page.locator('#documentation').scrollIntoViewIfNeeded();
    await page.waitForTimeout(500);
  });

  test('should display all main commands', async ({ page }) => {
    // Verify main commands are visible
    await expect(page.locator('text=dropper init')).toBeVisible();
    await expect(page.locator('text=dropper create')).toBeVisible();
    await expect(page.locator('text=dropper build')).toBeVisible();
  });

  test('should display create block subcommand', async ({ page }) => {
    // Click on create command to expand
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Verify subcommands section is visible
    await expect(page.locator('text=SUBCOMMANDS')).toBeVisible();

    // Verify block subcommand is listed
    await expect(page.locator('text=dropper create block')).toBeVisible();
  });

  test('should show block type options', async ({ page }) => {
    // Expand create command
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Find and click the block subcommand
    await page.locator('.minecraft-panel:has-text("dropper create block")').click();
    await page.waitForTimeout(300);

    // Verify block type option is shown
    const blockTypeOption = page.locator('text=--type').first();
    await expect(blockTypeOption).toBeVisible();

    // Verify all block types are listed
    const typeDescription = page.locator('text=basic, ore, pillar, slab, stairs, fence, wall, door, trapdoor, button, pressure_plate, crop');
    await expect(typeDescription).toBeVisible();
  });

  test('should show all block type options in description', async ({ page }) => {
    // Expand create command
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Click block subcommand
    await page.locator('.minecraft-panel:has-text("dropper create block")').click();
    await page.waitForTimeout(300);

    // Get the options section
    const optionsSection = page.locator('h4:has-text("OPTIONS")').locator('..');

    // Verify basic building blocks
    await expect(optionsSection.locator('text=basic')).toBeVisible();
    await expect(optionsSection.locator('text=ore')).toBeVisible();
    await expect(optionsSection.locator('text=pillar')).toBeVisible();

    // Verify structural blocks
    await expect(optionsSection.locator('text=slab')).toBeVisible();
    await expect(optionsSection.locator('text=stairs')).toBeVisible();
    await expect(optionsSection.locator('text=fence')).toBeVisible();
    await expect(optionsSection.locator('text=wall')).toBeVisible();

    // Verify interactive blocks
    await expect(optionsSection.locator('text=door')).toBeVisible();
    await expect(optionsSection.locator('text=trapdoor')).toBeVisible();
    await expect(optionsSection.locator('text=button')).toBeVisible();
    await expect(optionsSection.locator('text=pressure_plate')).toBeVisible();

    // Verify special blocks
    await expect(optionsSection.locator('text=crop')).toBeVisible();
  });

  test('should show drops-self option', async ({ page }) => {
    // Expand create command
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Click block subcommand
    await page.locator('.minecraft-panel:has-text("dropper create block")').click();
    await page.waitForTimeout(300);

    // Verify drops-self option
    await expect(page.locator('text=--drops-self')).toBeVisible();
    await expect(page.locator('text=Block drops itself')).toBeVisible();
  });

  test('should show max-age option for crops', async ({ page }) => {
    // Expand create command
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Click block subcommand
    await page.locator('.minecraft-panel:has-text("dropper create block")').click();
    await page.waitForTimeout(300);

    // Verify max-age option
    await expect(page.locator('text=--max-age')).toBeVisible();
    await expect(page.locator('text=Max age for crops')).toBeVisible();
  });

  test('should have NAME argument listed', async ({ page }) => {
    // Expand create command
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Click block subcommand
    await page.locator('.minecraft-panel:has-text("dropper create block")').click();
    await page.waitForTimeout(300);

    // Verify NAME argument
    await expect(page.locator('text=ARGUMENTS')).toBeVisible();
    await expect(page.locator('code:has-text("NAME")')).toBeVisible();
    await expect(page.locator('text=Block name in snake_case')).toBeVisible();
    await expect(page.locator('text=required')).toBeVisible();
  });

  test('should show usage example', async ({ page }) => {
    // Expand create command
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Click block subcommand
    await page.locator('.minecraft-panel:has-text("dropper create block")').click();
    await page.waitForTimeout(300);

    // Verify usage section
    await expect(page.locator('text=USAGE')).toBeVisible();
    await expect(page.locator('text=dropper create block <NAME>')).toBeVisible();
  });

  test('should allow copying usage text', async ({ page }) => {
    // Expand create command
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Click block subcommand
    await page.locator('.minecraft-panel:has-text("dropper create block")').click();
    await page.waitForTimeout(300);

    // Find and click the usage code block
    const usageBlock = page.locator('text=dropper create block <NAME>').locator('..');
    await usageBlock.click();

    // Verify copy icon appears
    const copyIcon = usageBlock.locator('svg');
    await expect(copyIcon).toBeVisible();
  });

  test('should have proper text contrast', async ({ page }) => {
    // Expand create command
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Click block subcommand
    await page.locator('.minecraft-panel:has-text("dropper create block")').click();
    await page.waitForTimeout(300);

    // Take screenshot for visual verification
    await page.screenshot({ path: 'test-results/block-documentation-contrast.png' });

    // Verify heading has lime green color
    const heading = page.locator('h3:has-text("dropper create block")');
    await expect(heading).toHaveCSS('color', 'rgb(0, 0, 0)'); // Should be black on white background

    // Verify section headers are visible
    const usageHeader = page.locator('h4:has-text("USAGE")');
    await expect(usageHeader).toHaveCSS('color', 'rgb(0, 0, 0)'); // Should be black
  });

  test('should expand and collapse properly', async ({ page }) => {
    const createPanel = page.locator('.minecraft-panel:has-text("dropper create")').first();

    // Initially collapsed - subcommands should not be visible
    await expect(page.locator('text=SUBCOMMANDS')).not.toBeVisible();

    // Click to expand
    await createPanel.click();
    await page.waitForTimeout(300);

    // Now subcommands should be visible
    await expect(page.locator('text=SUBCOMMANDS')).toBeVisible();

    // Click to collapse
    await createPanel.click();
    await page.waitForTimeout(300);

    // Subcommands should be hidden again
    await expect(page.locator('text=SUBCOMMANDS')).not.toBeVisible();
  });

  test('should show all create subcommands', async ({ page }) => {
    // Expand create command
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Verify all create subcommands
    await expect(page.locator('text=dropper create item')).toBeVisible();
    await expect(page.locator('text=dropper create block')).toBeVisible();
    await expect(page.locator('text=dropper create version')).toBeVisible();
    await expect(page.locator('text=dropper create asset-pack')).toBeVisible();
  });

  test('should have responsive layout', async ({ page }) => {
    // Test mobile viewport
    await page.setViewportSize({ width: 375, height: 667 });
    await page.goto('http://localhost:3000');
    await page.locator('#documentation').scrollIntoViewIfNeeded();

    // Verify documentation is still visible
    await expect(page.locator('text=DOCUMENTATION')).toBeVisible();

    // Expand create command
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Verify content is readable on mobile
    await expect(page.locator('text=SUBCOMMANDS')).toBeVisible();
  });

  test('should maintain scroll position when expanding', async ({ page }) => {
    // Scroll to create command
    const createPanel = page.locator('.minecraft-panel:has-text("dropper create")').first();
    await createPanel.scrollIntoViewIfNeeded();

    // Get initial position
    const initialY = await createPanel.boundingBox().then(box => box?.y);

    // Click to expand
    await createPanel.click();
    await page.waitForTimeout(300);

    // Position should be roughly the same (within reasonable tolerance)
    const newY = await createPanel.boundingBox().then(box => box?.y);
    expect(Math.abs((initialY || 0) - (newY || 0))).toBeLessThan(50);
  });

  test('should have accessible markup', async ({ page }) => {
    // Expand create command
    await page.locator('.minecraft-panel:has-text("dropper create")').click();
    await page.waitForTimeout(300);

    // Check for proper heading hierarchy
    const h4Headers = await page.locator('h4').count();
    expect(h4Headers).toBeGreaterThan(0);

    // Check for proper list structure
    const lists = await page.locator('ul').count();
    expect(lists).toBeGreaterThan(0);

    // Check for code blocks
    const codeBlocks = await page.locator('code').count();
    expect(codeBlocks).toBeGreaterThan(0);
  });
});

test.describe('Block Types Integration', () => {
  test('should verify docs.json schema', async ({ page }) => {
    // Fetch docs.json
    const response = await page.request.get('http://localhost:3000/docs.json');
    expect(response.ok()).toBeTruthy();

    const docs = await response.json();

    // Verify structure
    expect(docs).toHaveProperty('version');
    expect(docs).toHaveProperty('commands');
    expect(Array.isArray(docs.commands)).toBeTruthy();

    // Find create command
    const createCommand = docs.commands.find((cmd: any) => cmd.name === 'create');
    expect(createCommand).toBeDefined();
    expect(createCommand.subcommands).toBeDefined();

    // Find block subcommand
    const blockSubcommand = createCommand.subcommands.find((sub: any) => sub.name === 'block');
    expect(blockSubcommand).toBeDefined();

    // Verify block options
    const typeOption = blockSubcommand.options.find((opt: any) => opt.name.includes('--type'));
    expect(typeOption).toBeDefined();
    expect(typeOption.description).toContain('basic');
    expect(typeOption.description).toContain('ore');
    expect(typeOption.description).toContain('pillar');
    expect(typeOption.description).toContain('slab');
    expect(typeOption.description).toContain('stairs');
    expect(typeOption.description).toContain('fence');
    expect(typeOption.description).toContain('wall');
    expect(typeOption.description).toContain('door');
    expect(typeOption.description).toContain('trapdoor');
    expect(typeOption.description).toContain('button');
    expect(typeOption.description).toContain('pressure_plate');
    expect(typeOption.description).toContain('crop');

    // Verify drops-self option
    const dropsOption = blockSubcommand.options.find((opt: any) => opt.name.includes('--drops-self'));
    expect(dropsOption).toBeDefined();

    // Verify max-age option
    const maxAgeOption = blockSubcommand.options.find((opt: any) => opt.name.includes('--max-age'));
    expect(maxAgeOption).toBeDefined();
  });
});
