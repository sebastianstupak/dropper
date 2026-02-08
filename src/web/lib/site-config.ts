/**
 * Site Configuration
 * Central configuration for URLs, metadata, and site-wide settings
 */

export const siteConfig = {
  name: "Dropper",
  title: "Dropper - Drop Versions Fast",
  description: "Drop multi-loader, multi-version Minecraft mods faster than redstone.",

  // URLs
  url: process.env.NEXT_PUBLIC_SITE_URL || "https://dropper.dev",
  github: {
    repo: "https://github.com/sebastianstupak/dropper",
    owner: "sebastianstupak",
    repoName: "dropper",
  },

  // Author
  author: {
    name: "Sebastián Stupák",
    url: "https://github.com/sstupak",
  },

  // Social
  social: {
    github: "https://github.com/sebastianstupak/dropper",
    discord: "#", // Add when available
  },

  // License
  license: {
    name: "MIT",
    url: "https://github.com/sebastianstupak/dropper/blob/main/LICENSE",
  },

  // Installation
  install: {
    unix: "curl -fsSL https://sebastianstupak.github.io/dropper/install.sh | sh",
    windows: "iwr https://sebastianstupak.github.io/dropper/install.ps1 -useb | iex",
  },
}

export default siteConfig
