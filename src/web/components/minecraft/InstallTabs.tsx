'use client'

import { useState } from "react"
import { CodeBlock } from "./CodeBlock"
import { siteConfig } from "@/lib/site-config"
import { cn } from "@/lib/utils"

interface InstallTabsProps {
  className?: string
}

export function InstallTabs({ className }: InstallTabsProps) {
  const [activeTab, setActiveTab] = useState<'unix' | 'windows'>('unix')

  return (
    <div className={cn("w-full max-w-2xl mx-auto", className)}>
      {/* Tab buttons */}
      <div className="flex gap-1 mb-0 bg-gray-900 rounded-t-sm overflow-hidden">
        <button
          onClick={() => setActiveTab('unix')}
          className={`
            flex-1 px-4 py-2 text-xs uppercase transition-all duration-150
            cursor-pointer border-b-2
            ${activeTab === 'unix'
              ? 'bg-gray-700 text-white border-minecraft-lime'
              : 'bg-gray-800 text-gray-400 hover:text-gray-200 hover:bg-gray-750 border-transparent'
            }
          `}
        >
          macOS / Linux
        </button>
        <button
          onClick={() => setActiveTab('windows')}
          className={`
            flex-1 px-4 py-2 text-xs uppercase transition-all duration-150
            cursor-pointer border-b-2
            ${activeTab === 'windows'
              ? 'bg-gray-700 text-white border-minecraft-lime'
              : 'bg-gray-800 text-gray-400 hover:text-gray-200 hover:bg-gray-750 border-transparent'
            }
          `}
        >
          Windows
        </button>
      </div>

      {/* Tab content */}
      <div className="relative min-h-[60px]">
        <div className="w-full">
          {activeTab === 'unix' ? (
            <CodeBlock code={siteConfig.install.unix} language="bash" key="unix" />
          ) : (
            <CodeBlock code={siteConfig.install.windows} language="powershell" key="windows" />
          )}
        </div>
      </div>
    </div>
  )
}
