import { cn } from "@/lib/utils"

interface CodeBlockProps {
  code: string
  className?: string
}

export function CodeBlock({ code, className }: CodeBlockProps) {
  return (
    <div className={cn(
      "bg-minecraft-darker pixel-border-inset p-4",
      "font-mono text-sm",
      "overflow-x-auto",
      className
    )}>
      <div className="flex items-start gap-2">
        <span className="text-minecraft-lime select-none">$</span>
        <span className="text-white">{code}</span>
      </div>
    </div>
  )
}
