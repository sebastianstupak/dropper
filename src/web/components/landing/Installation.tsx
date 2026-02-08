import { CodeBlock } from "@/components/minecraft/CodeBlock"
import { InstallTabs } from "@/components/minecraft/InstallTabs"
import { BlockButton } from "@/components/minecraft/BlockButton"
import { siteConfig } from "@/lib/site-config"

const steps = [
  {
    title: "2. Initialize Your Mod",
    code: "dropper init my-awesome-mod",
    description: "Create a new mod project with sensible defaults"
  },
  {
    title: "3. Add Version Support",
    code: "dropper version add 1.20.1 1.21.1",
    description: "Support multiple Minecraft versions instantly"
  },
  {
    title: "4. Build Everything",
    code: "dropper build --all",
    description: "Compile JARs for all loaders and versions"
  },
]

export function Installation() {
  return (
    <section id="installation" className="py-24 bg-minecraft-darker">
      <div className="container mx-auto px-4">
        <div className="text-center mb-16">
          <h2 className="font-[family-name:var(--font-minecraft)] text-3xl sm:text-4xl mb-4 text-shadow-dark">
            GET STARTED
          </h2>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto">
            From zero to building in under 60 seconds
          </p>
        </div>

        <div className="max-w-3xl mx-auto space-y-8">
          {/* Step 1: Install */}
          <div className="animate-pixelate-in">
            <div className="mb-3">
              <h3 className="font-[family-name:var(--font-minecraft)] text-sm sm:text-base text-minecraft-lime mb-2">
                1. Install Dropper
              </h3>
              <p className="text-sm text-gray-400">
                Install the Dropper CLI for your platform
              </p>
            </div>
            <InstallTabs className="max-w-none" />
          </div>

          {/* Remaining steps */}
          {steps.map((step, index) => (
            <div
              key={step.title}
              className="animate-pixelate-in"
              style={{ animationDelay: `${(index + 1) * 100}ms` }}
            >
              <div className="mb-3">
                <h3 className="font-[family-name:var(--font-minecraft)] text-sm sm:text-base text-minecraft-lime mb-2">
                  {step.title}
                </h3>
                <p className="text-sm text-gray-400">
                  {step.description}
                </p>
              </div>
              <CodeBlock code={step.code} />
            </div>
          ))}
        </div>

        <div className="text-center mt-12">
          <BlockButton variant="lime" href={siteConfig.github.repo}>
            View on GitHub
          </BlockButton>
        </div>
      </div>
    </section>
  )
}
