import { Hero } from "@/components/landing/Hero"
import { Features } from "@/components/landing/Features"
import { Installation } from "@/components/landing/Installation"
import { Examples } from "@/components/landing/Examples"
import { Footer } from "@/components/landing/Footer"

export default function Home() {
  return (
    <main className="relative">
      <Hero />
      <Features />
      <Installation />
      <Examples />
      <Footer />
    </main>
  )
}
