package dev.dropper.migrators

/**
 * Detects API changes between Minecraft versions
 */
class ApiChangeDetector {
    /**
     * Known API changes between versions
     */
    private val knownChanges = mapOf(
        "1.20.1" to "1.20.4" to listOf(
            ApiChange(
                type = ApiChangeType.RENAME,
                oldPattern = "Registry.register",
                newPattern = "Registry.register",
                description = "Registry API minor changes",
                autoFixable = false
            )
        ),
        "1.20.4" to "1.21.1" to listOf(
            ApiChange(
                type = ApiChangeType.PARAMETER_CHANGE,
                oldPattern = "Block.Properties.of",
                newPattern = "Block.Properties.of",
                description = "Block properties builder changes",
                autoFixable = true,
                autoFix = { content ->
                    content.replace(
                        ".strength(1.5f)",
                        ".destroyTime(1.5f)"
                    )
                }
            ),
            ApiChange(
                type = ApiChangeType.PACKAGE_MOVE,
                oldPattern = "net.minecraft.world.item.CreativeModeTab",
                newPattern = "net.minecraft.world.item.CreativeModeTabs",
                description = "Creative tab reorganization",
                autoFixable = true,
                autoFix = { content ->
                    content.replace(
                        "CreativeModeTab.",
                        "CreativeModeTabs."
                    )
                }
            ),
            ApiChange(
                type = ApiChangeType.METHOD_SIGNATURE,
                oldPattern = "Item.Properties.stacksTo",
                newPattern = "Item.Properties.stacksTo",
                description = "Item properties changes",
                autoFixable = false
            )
        )
    )

    /**
     * Detect API changes between two versions
     */
    fun detectChanges(fromVersion: String, toVersion: String): List<ApiChange> {
        val key = fromVersion to toVersion
        return knownChanges[key] ?: emptyList()
    }

    /**
     * Analyze file content for potential breaking changes
     */
    fun analyzeContent(content: String, fromVersion: String, toVersion: String): List<DetectedChange> {
        val changes = detectChanges(fromVersion, toVersion)
        val detected = mutableListOf<DetectedChange>()

        changes.forEach { change ->
            if (content.contains(change.oldPattern)) {
                detected.add(
                    DetectedChange(
                        change = change,
                        location = "Found pattern: ${change.oldPattern}",
                        canAutoFix = change.autoFixable
                    )
                )
            }
        }

        return detected
    }

    /**
     * Apply auto-fixes to content
     */
    fun applyAutoFixes(content: String, changes: List<DetectedChange>): String {
        var result = content
        changes.filter { it.canAutoFix }.forEach { detected ->
            detected.change.autoFix?.let { fix ->
                result = fix(result)
            }
        }
        return result
    }
}

enum class ApiChangeType {
    RENAME,
    PARAMETER_CHANGE,
    PACKAGE_MOVE,
    METHOD_SIGNATURE,
    REMOVED,
    DEPRECATED
}

data class ApiChange(
    val type: ApiChangeType,
    val oldPattern: String,
    val newPattern: String,
    val description: String,
    val autoFixable: Boolean,
    val autoFix: ((String) -> String)? = null
)

data class DetectedChange(
    val change: ApiChange,
    val location: String,
    val canAutoFix: Boolean
)
