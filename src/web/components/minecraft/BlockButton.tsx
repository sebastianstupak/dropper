import { cn } from "@/lib/utils"
import { type ReactNode } from "react"

interface BlockButtonProps {
  variant?: "stone" | "lime" | "dirt" | "grass"
  children: ReactNode
  className?: string
  onClick?: () => void
  href?: string
}

const variantStyles = {
  stone: "minecraft-button bg-minecraft-stone",
  lime: "minecraft-button-lime",
  dirt: "minecraft-button gradient-dirt",
  grass: "minecraft-button gradient-grass",
}

export function BlockButton({
  variant = "stone",
  children,
  className,
  onClick,
  href
}: BlockButtonProps) {
  const baseClasses = cn(
    "px-6 py-3",
    "font-[family-name:var(--font-minecraft)] text-xs sm:text-sm uppercase",
    "cursor-pointer select-none",
    "transition-all duration-75",
    "hover:brightness-110 active:brightness-90",
    variantStyles[variant],
    className
  )

  if (href) {
    return (
      <a href={href} className={baseClasses}>
        {children}
      </a>
    )
  }

  return (
    <button onClick={onClick} className={baseClasses}>
      {children}
    </button>
  )
}
