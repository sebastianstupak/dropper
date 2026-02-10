package dev.dropper.util

object StringUtil {
    fun toClassName(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { word -> word.replaceFirstChar { it.uppercase() } }
    }

    fun toSnakeCase(className: String): String {
        return className.replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()
    }
}
