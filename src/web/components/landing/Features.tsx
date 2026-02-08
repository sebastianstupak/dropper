import Image from "next/image"
import { getAssetPath } from "@/lib/get-asset-path"

interface Feature {
  icon: string
  title: string
  description: string
}

const features: Feature[] = [
  {
    icon: "/minecraft/grass_block.png",
    title: "Multi-Version",
    description: "Support 10+ Minecraft versions from one codebase. Share code and assets across versions with intelligent inheritance."
  },
  {
    icon: "/minecraft/crafting_table.png",
    title: "Multi-Loader",
    description: "Build for Fabric, Forge, and NeoForge simultaneously. Platform abstractions keep your code clean and portable."
  },
  {
    icon: "/minecraft/chest.png",
    title: "Asset Packs",
    description: "Define textures, models, and data once. Inherit and override across versions. No more copy-paste hell."
  },
  {
    icon: "/minecraft/furnace.png",
    title: "Zero Config",
    description: "Convention over configuration. Sensible defaults get you building immediately. Customize when needed."
  },
  {
    icon: "/minecraft/stone.png",
    title: "Fast Builds",
    description: "Incremental compilation with Gradle caching. Only rebuild what changed. Deploy in seconds, not minutes."
  },
  {
    icon: "/minecraft/diamond_block.png",
    title: "Testing Built-in",
    description: "Co-located tests with your code. Run tests across all loaders and versions. Catch bugs before release."
  },
]

function FeatureCard({ icon, title, description }: Feature) {
  return (
    <div className="minecraft-panel p-6 hover:brightness-105 transition-all duration-200 group">
      <div className="w-16 h-16 mb-4 group-hover:scale-110 transition-transform duration-200 mx-auto">
        <Image
          src={getAssetPath(icon)}
          alt={title}
          width={64}
          height={64}
          className="pixelated"
        />
      </div>
      <h3 className="font-[family-name:var(--font-minecraft)] text-lg mb-3 uppercase">
        {title}
      </h3>
      <p className="text-sm text-[#1E1E1E] leading-relaxed">
        {description}
      </p>
    </div>
  )
}

export function Features() {
  return (
    <section id="features" className="py-24 bg-gradient-to-b from-minecraft-obsidian to-minecraft-darker">
      <div className="container mx-auto px-4">
        <div className="text-center mb-16">
          <h2 className="font-[family-name:var(--font-minecraft)] text-3xl sm:text-4xl mb-4 text-shadow-dark">
            FEATURES
          </h2>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto">
            Everything you need to build professional Minecraft mods
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 max-w-6xl mx-auto">
          {features.map((feature) => (
            <FeatureCard key={feature.title} {...feature} />
          ))}
        </div>
      </div>
    </section>
  )
}
