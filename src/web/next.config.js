/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'export', // Enable static export for GitHub Pages
  images: {
    unoptimized: true, // Required for static export
  },
  // Base path for GitHub Pages (set during deployment)
  basePath: process.env.NEXT_PUBLIC_BASE_PATH || '',
  // Asset prefix needed for proper static asset paths with basePath
  assetPrefix: process.env.NEXT_PUBLIC_BASE_PATH || '',
  trailingSlash: true,
}

module.exports = nextConfig
