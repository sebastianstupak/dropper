import { Hero } from "@/components/landing/Hero"
import { Features } from "@/components/landing/Features"
import { Installation } from "@/components/landing/Installation"
import { Documentation } from "@/components/landing/Documentation"
import { Footer } from "@/components/landing/Footer"
import { promises as fs } from 'fs'
import path from 'path'

async function getDocsData() {
  try {
    const docsPath = path.join(process.cwd(), 'public', 'docs.json')
    const fileContents = await fs.readFile(docsPath, 'utf8')
    return JSON.parse(fileContents)
  } catch (error) {
    // Return fallback docs if file doesn't exist
    console.warn('docs.json not found, using fallback')
    return {
      version: "1.0.0",
      commands: []
    }
  }
}

export default async function Home() {
  const docsData = await getDocsData()

  return (
    <main className="relative">
      <Hero />
      <Features />
      <Installation />
      <Documentation docsData={docsData} />
      <Footer />
    </main>
  )
}
