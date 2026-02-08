import { BlockButton } from "@/components/minecraft/BlockButton"
import { CodeBlock } from "@/components/minecraft/CodeBlock"
import { siteConfig } from "@/lib/site-config"
import Image from "next/image"

export function Hero() {
  return (
    <section className="relative min-h-screen flex items-center justify-center overflow-hidden">
      {/* Pixelated background pattern */}
      <div className="absolute inset-0 opacity-10">
        <div className="absolute inset-0" style={{
          backgroundImage: `repeating-linear-gradient(
            0deg,
            transparent,
            transparent 16px,
            rgba(255,255,255,0.03) 16px,
            rgba(255,255,255,0.03) 32px
          ),
          repeating-linear-gradient(
            90deg,
            transparent,
            transparent 16px,
            rgba(255,255,255,0.03) 16px,
            rgba(255,255,255,0.03) 32px
          )`
        }} />
      </div>

      <div className="container mx-auto px-4 relative z-10">
        <div className="max-w-4xl mx-auto text-center space-y-8">
          {/* Logo/Title */}
          <div className="animate-pixelate-in">
            <div className="inline-block">
              <div className="w-24 h-24 sm:w-32 sm:h-32 mx-auto mb-6 pixel-border animate-float relative bg-minecraft-stone">
                <Image
                  src="/minecraft/dropper.png"
                  alt="Dropper Block"
                  width={128}
                  height={128}
                  className="pixelated"
                  priority
                />
              </div>
            </div>
            <h1 className="font-[family-name:var(--font-minecraft)] text-4xl sm:text-6xl lg:text-7xl text-shadow-dark mb-4">
              DROPPER
            </h1>
            <p className="text-xl sm:text-2xl text-minecraft-lime font-[family-name:var(--font-minecraft)] text-shadow-dark">
              Drop versions fast
            </p>
            <p className="text-base sm:text-lg text-gray-400 mt-2">
              Minecraft Mod Build Automation
            </p>
          </div>

          {/* Tagline */}
          <p className="text-lg sm:text-xl text-gray-300 max-w-2xl mx-auto">
            Build multi-loader, multi-version Minecraft mods from a{" "}
            <span className="text-minecraft-lime font-semibold">single codebase</span>.
            Zero configuration required.
          </p>

          {/* Quick Install */}
          <div className="max-w-2xl mx-auto space-y-4">
            <CodeBlock code="pnpm add -g dropper" />
            <div className="flex flex-col sm:flex-row gap-4 justify-center items-center">
              <BlockButton variant="stone" href={siteConfig.github.repo}>
                <span className="flex items-center gap-2">
                  <Image
                    src="/minecraft/nether-star.webp"
                    alt="Star"
                    width={24}
                    height={24}
                    className="pixelated inline-block pb-0.5"
                  />
                  Star on GitHub
                </span>
              </BlockButton>
              <BlockButton variant="lime" href="#installation" className="px-8 py-4 text-base sm:text-lg">
                Get Started
              </BlockButton>
            </div>
          </div>

          {/* Quick Stats */}
          <div className="grid grid-cols-3 gap-4 max-w-2xl mx-auto pt-12">
            {[
              { value: "10+", label: "MC Versions" },
              { value: "3", label: "Loaders" },
              { value: "1", label: "Codebase" },
            ].map((stat) => (
              <div key={stat.label} className="minecraft-panel p-4">
                <div className="font-[family-name:var(--font-minecraft)] text-2xl sm:text-3xl mb-2">
                  {stat.value}
                </div>
                <div className="text-xs sm:text-sm uppercase">
                  {stat.label}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Scroll indicator */}
      <div className="absolute bottom-8 left-1/2 -translate-x-1/2 animate-bounce">
        <div className="w-6 h-10 pixel-border rounded-full flex items-start justify-center p-2">
          <div className="w-1.5 h-1.5 bg-minecraft-lime rounded-full" />
        </div>
      </div>
    </section>
  )
}
