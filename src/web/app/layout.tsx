import type { Metadata } from "next"
import { Press_Start_2P, JetBrains_Mono } from "next/font/google"
import "./globals.css"

const minecraftFont = Press_Start_2P({
  weight: "400",
  subsets: ["latin"],
  variable: "--font-minecraft",
  display: "swap",
})

const mono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
  display: "swap",
})

export const metadata: Metadata = {
  title: "Dropper - Drop Versions Fast",
  description: "Drop multi-loader, multi-version Minecraft mods faster than redstone. Build for Fabric, Forge, and NeoForge from a single codebase with zero config.",
  icons: {
    icon: '/minecraft/dropper.png',
    apple: '/minecraft/dropper.png',
  },
  openGraph: {
    title: "Dropper - Drop Versions Fast",
    description: "Drop multi-loader, multi-version Minecraft mods faster than redstone. Zero configuration.",
    type: "website",
    images: ['/minecraft/dropper.png'],
  },
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" className={`${minecraftFont.variable} ${mono.variable}`}>
      <body className="font-mono antialiased">
        {children}
      </body>
    </html>
  )
}
