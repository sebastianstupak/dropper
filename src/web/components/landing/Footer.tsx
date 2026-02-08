import { siteConfig } from "@/lib/site-config"

export function Footer() {
  const links = {
    product: [
      { name: "Features", href: "#features" },
      { name: "Installation", href: "#installation" },
      { name: "Examples", href: "#examples" },
    ],
    community: [
      { name: "GitHub", href: siteConfig.github.repo },
      { name: "Discord", href: siteConfig.social.discord },
      { name: "Issues", href: `${siteConfig.github.repo}/issues` },
      { name: "Discussions", href: `${siteConfig.github.repo}/discussions` },
    ],
    resources: [
      { name: "Documentation", href: `${siteConfig.github.repo}#readme` },
      { name: "Examples", href: `${siteConfig.github.repo}/tree/main/examples` },
      { name: "License", href: siteConfig.license.url },
    ],
  }

  return (
    <footer className="bg-minecraft-obsidian border-t-4 border-minecraft-stone py-16">
      <div className="container mx-auto px-4">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-12 mb-12">
          {/* Brand */}
          <div>
            <div className="w-12 h-12 bg-minecraft-stone pixel-border mb-4 flex items-center justify-center overflow-hidden">
              <img src="/minecraft/dropper.png" alt="Dropper" className="w-full h-full pixelated" />
            </div>
            <h3 className="font-[family-name:var(--font-minecraft)] text-lg mb-2">
              DROPPER
            </h3>
            <p className="text-sm text-gray-400">
              Minecraft mod build automation for the modern developer
            </p>
          </div>

          {/* Links */}
          <div>
            <h4 className="font-[family-name:var(--font-minecraft)] text-xs uppercase mb-4 text-minecraft-lime">
              Product
            </h4>
            <ul className="space-y-2">
              {links.product.map((link) => (
                <li key={link.name}>
                  <a
                    href={link.href}
                    className="text-sm text-gray-400 hover:text-minecraft-lime transition-colors"
                  >
                    {link.name}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h4 className="font-[family-name:var(--font-minecraft)] text-xs uppercase mb-4 text-minecraft-lime">
              Community
            </h4>
            <ul className="space-y-2">
              {links.community.map((link) => (
                <li key={link.name}>
                  <a
                    href={link.href}
                    className="text-sm text-gray-400 hover:text-minecraft-lime transition-colors"
                  >
                    {link.name}
                  </a>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h4 className="font-[family-name:var(--font-minecraft)] text-xs uppercase mb-4 text-minecraft-lime">
              Resources
            </h4>
            <ul className="space-y-2">
              {links.resources.map((link) => (
                <li key={link.name}>
                  <a
                    href={link.href}
                    className="text-sm text-gray-400 hover:text-minecraft-lime transition-colors"
                  >
                    {link.name}
                  </a>
                </li>
              ))}
            </ul>
          </div>
        </div>

        {/* Bottom */}
        <div className="border-t border-minecraft-stone pt-8 flex flex-col sm:flex-row justify-between items-center gap-4">
          <p className="text-sm text-gray-500">
            © {new Date().getFullYear()} {siteConfig.author.name}. Built with ❤️ for the Minecraft modding community.
          </p>
          <div className="flex gap-4 text-sm text-gray-500">
            <a href={siteConfig.license.url} target="_blank" rel="noopener noreferrer" className="hover:text-minecraft-lime transition-colors">
              {siteConfig.license.name} License
            </a>
          </div>
        </div>
      </div>
    </footer>
  )
}
