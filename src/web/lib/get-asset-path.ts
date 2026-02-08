/**
 * Get asset path with basePath for static export
 * In production (GitHub Pages), prepends /dropper
 * In development, returns the path as-is
 */
export function getAssetPath(path: string): string {
  // Ensure path starts with /
  const normalizedPath = path.startsWith('/') ? path : `/${path}`

  // Get base path from environment (set during build)
  const basePath = process.env.NEXT_PUBLIC_BASE_PATH || ''

  // Return path with basePath prepended
  return `${basePath}${normalizedPath}`
}
