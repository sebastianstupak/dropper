import { CodeBlock } from "@/components/minecraft/CodeBlock"

const examples = [
  {
    title: "Add a Custom Item",
    description: "Create items that work across all versions and loaders",
    code: "dropper generate item diamond_pickaxe --creative-tab tools"
  },
  {
    title: "Support New Version",
    description: "Add support for the latest Minecraft release",
    code: "dropper version add 1.21.4 --inherit-from v2"
  },
  {
    title: "Run Tests",
    description: "Test your mod across all loaders before release",
    code: "dropper test --loader neoforge --version 1.20.1"
  },
]

export function Examples() {
  return (
    <section className="py-24 bg-gradient-to-b from-minecraft-darker to-minecraft-obsidian">
      <div className="container mx-auto px-4">
        <div className="text-center mb-16">
          <h2 className="font-[family-name:var(--font-minecraft)] text-3xl sm:text-4xl mb-4 text-shadow-dark">
            EXAMPLES
          </h2>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto">
            Common workflows made simple
          </p>
        </div>

        <div className="max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-6">
          {examples.map((example) => (
            <div key={example.title} className="space-y-3">
              <div className="minecraft-panel p-4">
                <h3 className="font-[family-name:var(--font-minecraft)] text-xs mb-2 uppercase">
                  {example.title}
                </h3>
                <p className="text-xs text-[#1E1E1E]">
                  {example.description}
                </p>
              </div>
              <CodeBlock code={example.code} className="text-xs" />
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
