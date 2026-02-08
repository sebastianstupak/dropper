package dev.dropper.template

/**
 * Builder for creating template context maps
 */
class TemplateContext private constructor(
    private val data: MutableMap<String, Any>
) {

    fun put(key: String, value: Any): TemplateContext {
        data[key] = value
        return this
    }

    fun putAll(map: Map<String, Any>): TemplateContext {
        data.putAll(map)
        return this
    }

    fun build(): Map<String, Any> = data.toMap()

    companion object {
        fun create(): TemplateContext {
            return TemplateContext(mutableMapOf())
        }

        fun from(map: Map<String, Any>): TemplateContext {
            return TemplateContext(map.toMutableMap())
        }
    }
}
