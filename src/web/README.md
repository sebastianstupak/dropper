# Dropper Landing Page

A Minecraft-themed landing page for Dropper, the Minecraft mod build automation CLI tool.

## Tech Stack

- **Next.js 15** (App Router)
- **Tailwind CSS v4** (CSS-first configuration)
- **TypeScript** (strict mode)
- **Press Start 2P** font (Minecraft-style)

## Quick Start

```bash
# Install dependencies
cd src/web
pnpm install

# Run development server
pnpm dev

# Open http://localhost:3000
```

## Development

```bash
# Build for production
pnpm build

# Start production server
pnpm start

# Lint code
pnpm lint
```

## Project Structure

```
src/web/
├── app/                      # Next.js App Router
│   ├── layout.tsx           # Root layout with fonts
│   ├── page.tsx             # Landing page
│   └── globals.css          # Tailwind + Minecraft theme
├── components/
│   ├── landing/             # Landing page sections
│   │   ├── Hero.tsx
│   │   ├── Features.tsx
│   │   ├── Installation.tsx
│   │   ├── Examples.tsx
│   │   └── Footer.tsx
│   └── minecraft/           # Minecraft-themed components
│       ├── BlockButton.tsx  # Minecraft-style buttons
│       └── CodeBlock.tsx    # Terminal-style code blocks
├── lib/
│   └── utils.ts             # Utility functions
└── public/                  # Static assets
```

## Design System

### Colors

- **Stone Gray**: `#8B8B8B` (primary buttons)
- **Dark Stone**: `#3C3C3C` (secondary)
- **Lime Green**: `#55FF55` (accents, CTAs)
- **Dirt Brown**: `#8B6F47`
- **Grass Green**: `#7CBD6B`
- **Obsidian**: `#100819` (background)

### Typography

- **Headings**: Press Start 2P (pixelated, Minecraft-style)
- **Body**: JetBrains Mono (readable, monospace)

### Components

#### BlockButton
Minecraft-style button with pixel borders and hover effects.

```tsx
<BlockButton variant="lime">Get Started</BlockButton>
<BlockButton variant="stone">Learn More</BlockButton>
```

#### CodeBlock
Terminal-style code display with Minecraft theming.

```tsx
<CodeBlock code="npm install -g dropper" />
```

## Customization

See [CLAUDE.md](./CLAUDE.md) for detailed instructions on:
- Adding new components
- Modifying the Minecraft theme
- Best practices for Next.js + Tailwind v4
- Working with shadcn/ui components

## Deployment

### Vercel (Recommended)

1. Push to GitHub
2. Import project in Vercel
3. Deploy automatically

### Self-hosted

```bash
pnpm build
pnpm start
```

## License

MIT
