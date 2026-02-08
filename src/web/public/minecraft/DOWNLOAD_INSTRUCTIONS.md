# How to Download Minecraft Block Textures

The automated downloads didn't work due to wiki access restrictions. Here's how to manually download textures:

## Method 1: Direct from Minecraft Wiki

Visit these pages and right-click → Save Image:

1. **Dropper**: https://minecraft.wiki/w/Dropper
   - Find the block image, right-click, save as `dropper.png`

2. **Crafting Table**: https://minecraft.wiki/w/Crafting_Table
   - Save as `crafting_table.png`

3. **Furnace**: https://minecraft.wiki/w/Furnace
   - Save as `furnace.png`

4. **Chest**: https://minecraft.wiki/w/Chest
   - Save as `chest.png`

5. **Grass Block**: https://minecraft.wiki/w/Grass_Block
   - Save as `grass_block.png`

6. **Stone**: https://minecraft.wiki/w/Stone
   - Save as `stone.png`

7. **Diamond Block**: https://minecraft.wiki/w/Block_of_Diamond
   - Save as `diamond_block.png`

## Method 2: From Resource Pack

1. Download default Minecraft resource pack from https://mcasset.cloud/
2. Extract the ZIP file
3. Navigate to `assets/minecraft/textures/block/`
4. Copy the PNG files you need to this directory

## Method 3: Use Existing SVG

The site currently uses `dropper.svg` which works well. You can keep using it or replace with PNG.

## File Locations

Place all downloaded images in:
```
src/web/public/minecraft/
```

Images will be accessible at:
```
http://localhost:3000/minecraft/dropper.png
http://localhost:3000/minecraft/crafting_table.png
etc.
```

## Update Components

Once images are downloaded, update Hero.tsx:
```tsx
<Image
  src="/minecraft/dropper.png"  // Change from dropper.svg
  alt="Dropper Block"
  width={64}
  height={64}
  className="pixelated"
/>
```
