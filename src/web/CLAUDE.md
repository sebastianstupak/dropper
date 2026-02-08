# Dropper Web - Claude Code Instructions

This is the **landing page and web presence** for Dropper, a Minecraft mod build automation CLI tool.

## Tech Stack

- **Next.js 15+** (App Router)
- **Tailwind CSS v4** (with CSS-first configuration)
- **shadcn/ui** (Radix UI components)
- **TypeScript** (strict mode)
- **React 19+**

## Project Structure

```
src/web/
├── CLAUDE.md                 # This file
├── app/                      # Next.js App Router
│   ├── layout.tsx           # Root layout
│   ├── page.tsx             # Home page
│   ├── docs/                # Documentation pages
│   ├── globals.css          # Tailwind + custom styles
│   └── fonts/               # Local fonts
├── components/
│   ├── ui/                  # shadcn/ui components
│   ├── landing/             # Landing page sections
│   └── minecraft/           # Minecraft-themed components
├── lib/
│   └── utils.ts             # Utilities (cn, etc.)
├── public/
│   ├── minecraft/           # Minecraft textures, assets
│   └── favicons/
├── tailwind.config.ts       # Tailwind v4 config
├── next.config.js
├── tsconfig.json
└── package.json
```

## Design Philosophy

### Minecraft Aesthetic
- **Pixelated fonts** (Press Start 2P, Monocraft, Minecraft font)
- **Block-based UI elements** (inspired by Minecraft GUI)
- **Color palette**:
  - Primary: `#8B8B8B` (stone gray)
  - Secondary: `#3C3C3C` (dark stone)
  - Accent: `#55FF55` (lime green, enchantment table)
  - Dirt: `#8B6F47`
  - Grass: `#7CBD6B`
- **Textures**: Subtle pixelated backgrounds (dirt, stone, planks)
- **Hover effects**: Mimick Minecraft button hover (brightness increase)

### Modern Web Standards
- **Performance first**: Optimize images, lazy load components
- **Accessibility**: ARIA labels, keyboard navigation, semantic HTML
- **SEO**: Meta tags, Open Graph, structured data
- **Responsive**: Mobile-first design
- **Dark mode**: Default dark theme with light mode option

## Tailwind CSS v4 Guidelines

### Configuration (CSS-first)
```css
/* app/globals.css */
@import "tailwindcss";

@theme {
  /* Colors */
  --color-minecraft-stone: #8B8B8B;
  --color-minecraft-dark: #3C3C3C;
  --color-minecraft-lime: #55FF55;
  --color-minecraft-dirt: #8B6F47;
  --color-minecraft-grass: #7CBD6B;

  /* Fonts */
  --font-family-minecraft: 'Press Start 2P', 'Monocraft', monospace;

  /* Spacing follows 8px grid (like Minecraft blocks) */
  --spacing-block: 16px; /* 1 block = 16px */

  /* Shadows - subtle pixelated effect */
  --shadow-minecraft: 4px 4px 0 rgba(0, 0, 0, 0.3);
}
```

### Usage Patterns
```tsx
// Use semantic color names
<div className="bg-minecraft-stone text-white">

// Responsive with container queries
<div className="@container">
  <div className="@lg:grid-cols-2">

// Custom utilities
<div className="pixel-border"> {/* defined in globals.css */}
```

### Custom Utilities
```css
/* app/globals.css */
@layer utilities {
  .pixel-border {
    border: 2px solid;
    border-image: /* pixelated border image */;
    image-rendering: pixelated;
  }

  .minecraft-button {
    @apply bg-minecraft-stone hover:brightness-110 active:brightness-90;
    @apply pixel-border shadow-minecraft;
    transition: all 0.05s; /* Fast like MC */
  }
}
```

## shadcn/ui Guidelines

### Installation
```bash
npx shadcn@latest init
npx shadcn@latest add button card tabs
```

### Customization
Modify `components/ui/*.tsx` to add Minecraft theming:

```tsx
// components/ui/button.tsx
const buttonVariants = cva(
  "minecraft-button font-minecraft text-sm",
  {
    variants: {
      variant: {
        default: "bg-minecraft-stone text-white",
        creative: "bg-minecraft-lime text-black",
        survival: "bg-minecraft-dirt text-white",
      }
    }
  }
)
```

### Component Patterns
```tsx
// Use composition
<Card className="pixel-border">
  <CardHeader>
    <CardTitle className="font-minecraft">Dropper</CardTitle>
  </CardHeader>
  <CardContent>
    {/* content */}
  </CardContent>
</Card>

// Server components by default
export default async function Page() {
  return <LandingHero />
}

// Client components when needed
'use client'
export function InteractiveDemo() {
  const [open, setOpen] = useState(false)
  // ...
}
```

## Next.js Best Practices

### App Router Structure
```tsx
// app/page.tsx - Landing page
export default function Home() {
  return (
    <>
      <Hero />
      <Features />
      <Installation />
      <Examples />
      <Footer />
    </>
  )
}

// app/docs/page.tsx - Documentation
export default function Docs() {
  // ...
}
```

### Performance
```tsx
// Use next/image for all images
import Image from 'next/image'
<Image
  src="/minecraft/dropper.png"
  alt="Dropper block"
  width={64}
  height={64}
  className="pixelated"
/>

// Lazy load heavy components
const MinecraftCanvas = dynamic(() => import('@/components/MinecraftCanvas'), {
  ssr: false,
  loading: () => <Skeleton />
})

// Use server components for static content
export default async function Features() {
  return <div>...</div>
}
```

### Metadata
```tsx
// app/layout.tsx
export const metadata: Metadata = {
  title: 'Dropper - Minecraft Mod Build Automation',
  description: 'Build multi-loader, multi-version Minecraft mods with zero config',
  openGraph: {
    images: ['/og-image.png'],
  }
}
```

### Fonts
```tsx
// app/layout.tsx
import { Press_Start_2P } from 'next/font/google'
import localFont from 'next/font/local'

const minecraftFont = Press_Start_2P({
  weight: '400',
  subsets: ['latin'],
  variable: '--font-minecraft',
})

const monocraft = localFont({
  src: './fonts/Monocraft.otf',
  variable: '--font-monocraft',
})

export default function RootLayout({ children }) {
  return (
    <html className={`${minecraftFont.variable} ${monocraft.variable}`}>
      <body>{children}</body>
    </html>
  )
}
```

## Component Architecture

### Landing Page Structure
```tsx
// components/landing/Hero.tsx
export function Hero() {
  return (
    <section className="relative h-screen">
      <MinecraftBackground />
      <div className="container">
        <h1 className="font-minecraft text-6xl">Dropper</h1>
        <p className="text-xl">Build Minecraft mods. Zero config.</p>
        <InstallButton />
      </div>
    </section>
  )
}

// components/landing/Features.tsx
const features = [
  { icon: '🎯', title: 'Multi-Version', desc: '...' },
  { icon: '🔄', title: 'Multi-Loader', desc: '...' },
  { icon: '📦', title: 'Asset Packs', desc: '...' },
]

export function Features() {
  return (
    <section className="py-24">
      <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
        {features.map(feature => (
          <FeatureCard key={feature.title} {...feature} />
        ))}
      </div>
    </section>
  )
}
```

### Minecraft-Themed Components
```tsx
// components/minecraft/BlockButton.tsx
interface BlockButtonProps {
  variant: 'stone' | 'dirt' | 'grass' | 'wood'
  children: React.ReactNode
}

export function BlockButton({ variant, children }: BlockButtonProps) {
  return (
    <button
      className={cn(
        'minecraft-button',
        'font-minecraft text-sm uppercase',
        'px-6 py-3',
        variantStyles[variant]
      )}
    >
      {children}
    </button>
  )
}

// components/minecraft/CodeBlock.tsx
// Styled like Minecraft command blocks
export function CodeBlock({ code, language }: CodeBlockProps) {
  return (
    <div className="bg-[#1E1E1E] pixel-border p-4 font-mono">
      <div className="text-minecraft-lime">$ {code}</div>
    </div>
  )
}

// components/minecraft/InventoryGrid.tsx
// Display features in a 3x3 inventory-style grid
export function InventoryGrid({ items }: InventoryGridProps) {
  return (
    <div className="grid grid-cols-3 gap-1 bg-[#C6C6C6] p-2 pixel-border">
      {items.map(item => (
        <InventorySlot key={item.id} {...item} />
      ))}
    </div>
  )
}
```

## Content Guidelines

### Copy Writing
- **Tone**: Technical but approachable, Minecraft-inspired puns OK
- **Headline**: Clear benefit statement
- **CTAs**: Action-oriented ("Start Building", "Install Dropper", "View Docs")

### Code Examples
Show real Dropper commands:
```bash
# Initialize new mod
dropper init my-awesome-mod

# Add version support
dropper version add 1.20.1 1.21.1

# Build for all loaders
dropper build --all
```

### Feature Highlights
1. **Multi-Version Support** - "Support 10+ MC versions with one codebase"
2. **Multi-Loader** - "Fabric, Forge, NeoForge from shared code"
3. **Asset Pack System** - "Define textures once, inherit across versions"
4. **Zero Config** - "Convention over configuration"
5. **Incremental Builds** - "Fast iteration with Gradle caching"

## Development Workflow

### Setup
```bash
cd src/web
pnpm install
pnpm dev  # http://localhost:3000
```

### Adding shadcn Components
```bash
npx shadcn@latest add [component]
# Then customize in components/ui/
```

### Adding New Page
```bash
# Create app/[page]/page.tsx
export default function Page() {
  return <div>...</div>
}
```

### Styling
1. Use Tailwind utilities first
2. Custom utilities in `globals.css` for repeated patterns
3. Component variants with `cva()` from `class-variance-authority`
4. CSS modules only for complex animations

## Deployment

### Vercel (Recommended)
```bash
# Connect GitHub repo
# Auto-deploy on push to main
```

### Environment Variables
```bash
# .env.local
NEXT_PUBLIC_SITE_URL=https://dropper.dev
NEXT_PUBLIC_GITHUB_REPO=username/dropper
```

### Build Optimization
```js
// next.config.js
module.exports = {
  images: {
    formats: ['image/avif', 'image/webp'],
  },
  experimental: {
    optimizeCss: true,
  },
}
```

## Common Tasks for Claude

### "Add a new landing page section"
1. Create component in `components/landing/NewSection.tsx`
2. Import and add to `app/page.tsx`
3. Style with Tailwind + Minecraft theme
4. Ensure responsive (mobile-first)

### "Add a shadcn component"
1. Run `npx shadcn@latest add [component]`
2. Customize in `components/ui/[component].tsx`
3. Add Minecraft theming (pixel borders, colors, fonts)
4. Export from `components/ui/index.ts`

### "Update Minecraft theming"
1. Edit color variables in `app/globals.css` `@theme` block
2. Update custom utilities if needed
3. Test dark/light mode variants

### "Add documentation page"
1. Create `app/docs/[topic]/page.tsx`
2. Use MDX or React components
3. Add to navigation
4. Ensure proper metadata for SEO

### "Optimize performance"
1. Check with Lighthouse
2. Optimize images with `next/image`
3. Lazy load heavy components with `dynamic()`
4. Use server components where possible

## Important Files

- `app/globals.css` - Tailwind config, Minecraft theme variables
- `components/ui/` - shadcn components (customized)
- `components/landing/` - Landing page sections
- `components/minecraft/` - Minecraft-themed UI elements
- `lib/utils.ts` - Utility functions (cn, etc.)
- `tailwind.config.ts` - Tailwind v4 configuration
- `next.config.js` - Next.js configuration

## When in Doubt

1. Check shadcn/ui docs: https://ui.shadcn.com
2. Check Next.js docs: https://nextjs.org/docs
3. Check Tailwind v4 docs: https://tailwindcss.com/docs
4. Look at existing components for patterns
5. Test on mobile viewport
6. Verify accessibility with screen reader

## Resources

- **Minecraft Font**: Press Start 2P (Google Fonts) or Monocraft
- **Minecraft Textures**: Use public domain or create pixel art
- **Color Reference**: Minecraft Wiki color palette
- **Icons**: Lucide React (styled to match theme)

## Philosophy

This landing page should feel like:
- Opening a Minecraft GUI
- Clean, functional, pixelated aesthetic
- Fast, smooth interactions (no janky animations)
- Clear communication of Dropper's value

**The goal**: Convert developers looking for Minecraft mod build tools into Dropper users.
