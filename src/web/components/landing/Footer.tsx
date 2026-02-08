import { siteConfig } from "@/lib/site-config"
import Image from "next/image"

export function Footer() {
  const links = [
    { name: "Features", href: "#features" },
    { name: "Installation", href: "#installation" },
    { name: "Examples", href: `${siteConfig.github.repo}/tree/main/examples` },
    { name: "GitHub", href: siteConfig.github.repo },
    { name: "Documentation", href: `${siteConfig.github.repo}#readme` },
    { name: "License", href: siteConfig.license.url },
  ]

  return (
    <footer className="bg-minecraft-obsidian border-t-2 border-minecraft-stone py-12">
      <div className="container mx-auto px-4">
        <div className="flex flex-col md:flex-row justify-between items-center gap-8">
          {/* Brand */}
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-minecraft-stone pixel-border flex items-center justify-center overflow-hidden">
              <Image
                src="/minecraft/dropper.png"
                alt="Dropper"
                width={40}
                height={40}
                className="pixelated"
              />
            </div>
            <div>
              <h3 className="font-[family-name:var(--font-minecraft)] text-base">
                DROPPER
              </h3>
              <p className="text-xs text-gray-500">
                Drop versions fast
              </p>
            </div>
          </div>

          {/* Links */}
          <nav className="flex flex-wrap justify-center gap-x-6 gap-y-2">
            {links.map((link) => (
              <a
                key={link.name}
                href={link.href}
                className="text-sm text-gray-400 hover:text-minecraft-lime transition-colors cursor-pointer"
                target={link.href.startsWith('http') ? '_blank' : undefined}
                rel={link.href.startsWith('http') ? 'noopener noreferrer' : undefined}
              >
                {link.name}
              </a>
            ))}
          </nav>

          {/* Copyright */}
          <div className="text-xs text-gray-500 text-center md:text-right">
            © {new Date().getFullYear()} {siteConfig.author.name}
          </div>
        </div>
      </div>
    </footer>
  )
}
