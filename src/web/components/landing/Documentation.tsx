'use client'

import { useState } from 'react'
import { Copy, Check } from 'lucide-react'

interface ArgumentDoc {
  name: string
  description: string
  required: boolean
}

interface OptionDoc {
  name: string
  description: string
  required: boolean
}

interface CommandDoc {
  name: string
  description: string
  usage: string
  arguments: ArgumentDoc[]
  options: OptionDoc[]
  examples: string[]
  subcommands?: CommandDoc[]
}

interface DocsData {
  version: string
  commands: CommandDoc[]
}

interface DocumentationProps {
  docsData: DocsData
}

function CopyableCode({ code, showPrompt = false }: { code: string; showPrompt?: boolean }) {
  const [copied, setCopied] = useState(false)

  const copyToClipboard = async () => {
    await navigator.clipboard.writeText(code)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div
      className="bg-[#1E1E1E] text-[#55FF55] px-3 py-2 rounded flex items-center justify-between font-mono text-sm cursor-pointer hover:brightness-110 transition-all group"
      onClick={copyToClipboard}
    >
      <code>
        {showPrompt && '$ '}
        {code}
      </code>
      <button className="ml-2 opacity-0 group-hover:opacity-100 transition-opacity">
        {copied ? (
          <Check size={16} className="text-green-400" />
        ) : (
          <Copy size={16} className="text-gray-400" />
        )}
      </button>
    </div>
  )
}

function CommandCard({ command }: { command: CommandDoc }) {
  const [expanded, setExpanded] = useState(false)

  return (
    <div className="minecraft-panel mb-6 bg-white">
      <div
        className="p-6 cursor-pointer hover:brightness-105 transition-all"
        onClick={() => setExpanded(!expanded)}
      >
        <div className="flex items-center justify-between">
          <div>
            <h3 className="font-[family-name:var(--font-minecraft)] text-lg mb-2 text-[#000000]">
              dropper {command.name}
            </h3>
            <p className="text-base text-[#000000] font-semibold">{command.description}</p>
          </div>
          <div className="text-2xl text-[#000000] font-bold">
            {expanded ? '−' : '+'}
          </div>
        </div>
      </div>

      {expanded && (
        <div className="px-6 pb-6 space-y-4 border-t-2 border-[#3C3C3C] pt-4">
          {/* Usage */}
          <div>
            <h4 className="font-[family-name:var(--font-minecraft)] text-sm text-[#000000] uppercase font-bold mb-2">
              Usage
            </h4>
            <CopyableCode code={command.usage} />
          </div>

          {/* Arguments */}
          {command.arguments.length > 0 && (
            <div>
              <h4 className="font-[family-name:var(--font-minecraft)] text-sm text-[#000000] uppercase font-bold mb-2">
                Arguments
              </h4>
              <ul className="space-y-2">
                {command.arguments.map((arg) => (
                  <li key={arg.name} className="text-[#000000]">
                    <code className="bg-[#1E1E1E] text-white px-3 py-2 rounded font-mono text-sm font-bold">
                      {arg.name}
                    </code>
                    {arg.required && (
                      <span className="ml-2 text-sm text-red-600 font-bold">required</span>
                    )}
                    <p className="text-[#000000] font-bold mt-2 ml-2 text-base">{arg.description}</p>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Options */}
          {command.options.length > 0 && (
            <div>
              <h4 className="font-[family-name:var(--font-minecraft)] text-sm text-[#000000] uppercase font-bold mb-2">
                Options
              </h4>
              <ul className="space-y-2">
                {command.options.map((opt) => (
                  <li key={opt.name} className="text-[#000000]">
                    <code className="bg-[#1E1E1E] text-white px-3 py-2 rounded font-mono text-sm font-bold">
                      {opt.name}
                    </code>
                    <p className="text-[#000000] font-bold mt-2 ml-2 text-base">{opt.description}</p>
                  </li>
                ))}
              </ul>
            </div>
          )}

          {/* Subcommands */}
          {command.subcommands && command.subcommands.length > 0 && (
            <div>
              <h4 className="font-[family-name:var(--font-minecraft)] text-sm text-[#000000] uppercase font-bold mb-3">
                Subcommands
              </h4>
              <div className="space-y-3 ml-4">
                {command.subcommands.map((sub) => (
                  <CommandCard key={sub.name} command={sub} />
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

export function Documentation({ docsData }: DocumentationProps) {
  return (
    <section id="documentation" className="py-24 bg-minecraft-darker">
      <div className="container mx-auto px-4">
        <div className="text-center mb-16">
          <h2 className="font-[family-name:var(--font-minecraft)] text-3xl sm:text-4xl mb-4 text-[#55FF55]">
            DOCUMENTATION
          </h2>
          <p className="text-gray-300 text-lg max-w-2xl mx-auto">
            Complete command reference for Dropper CLI v{docsData.version}
          </p>
        </div>

        <div className="max-w-4xl mx-auto">
          {docsData.commands.map((command) => (
            <CommandCard key={command.name} command={command} />
          ))}
        </div>

      </div>
    </section>
  )
}
