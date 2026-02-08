'use client'

import { cn } from "@/lib/utils"
import { useState } from "react"

interface CodeBlockProps {
  code: string
  className?: string
  language?: 'bash' | 'powershell'
}

export function CodeBlock({ code, className, language = 'bash' }: CodeBlockProps) {
  const prompt = language === 'powershell' ? '>' : '$'
  const [copied, setCopied] = useState(false)

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(code)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    } catch (err) {
      console.error('Failed to copy:', err)
    }
  }

  return (
    <div
      onClick={copyToClipboard}
      className={cn(
        "bg-minecraft-darker pixel-border-inset p-4",
        "font-mono text-sm",
        "overflow-x-auto text-left relative group cursor-pointer",
        "hover:brightness-110 transition-all duration-150",
        "active:brightness-95",
        className
      )}
      title="Click to copy"
    >
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-start gap-2 flex-1">
          <span className="text-minecraft-lime select-none">{prompt}</span>
          <span className="text-white break-all">{code}</span>
        </div>
        <div className="text-gray-500 text-xs opacity-50 group-hover:opacity-100 transition-opacity flex-shrink-0">
          {copied ? (
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="text-minecraft-lime">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
          ) : (
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
              <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
            </svg>
          )}
        </div>
      </div>
    </div>
  )
}
