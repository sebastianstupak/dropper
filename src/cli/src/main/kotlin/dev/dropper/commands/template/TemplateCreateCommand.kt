package dev.dropper.commands.template

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import dev.dropper.templates.TemplateRegistry
import dev.dropper.util.FileUtil
import dev.dropper.util.Logger
import java.io.File

/**
 * Create components from template
 */
class TemplateCreateCommand : CliktCommand(
    name = "create",
    help = "Create components from template"
) {
    private val templateName by argument(help = "Template name or component name")
    private val name by option("--name", "-n", help = "Component name")
    private val material by option("--material", "-m", help = "Material name for sets")

    // Item property options
    private val tier by option("--tier", help = "Tool tier (stone, iron, diamond, netherite)")
    private val armorType by option("--armor-type", help = "Armor type (helmet, chestplate, leggings, boots)")
    private val enchantable by option("--enchantable", help = "Make item enchantable").flag()
    private val dyeable by option("--dyeable", help = "Make item dyeable").flag()
    private val repairable by option("--repairable", help = "Make item repairable").flag()
    private val repairMaterial by option("--repair-material", help = "Material used for repair")
    private val stackSize by option("--stack-size", help = "Maximum stack size").default("64")
    private val maxDamage by option("--max-damage", help = "Maximum durability/damage")
    private val attribute by option("--attribute", help = "Custom attribute (name:value)").multiple()
    private val tag by option("--tag", help = "Custom tag").multiple()
    private val recipe by option("--recipe", help = "Recipe type (crafting, smelting, etc.)")
    private val lootTable by option("--loot-table", help = "Generate loot table").flag()
    private val villagerTrade by option("--villager-trade", help = "Add villager trade").flag()
    private val advancement by option("--advancement", help = "Generate advancement").flag()
    private val model by option("--model", help = "Model type (custom, generated, etc.)")

    // Custom template options
    private val variable by option("--var", help = "Template variable (key:value)").multiple()
    private val condition by option("--if", help = "Conditional generation (key:value)")
    private val forEach by option("--foreach", help = "Loop generation (key:val1,val2,...)")
    private val extends by option("--extends", help = "Parent template to extend")
    private val include by option("--include", help = "Templates to compose/include").multiple()
    private val implements by option("--implements", help = "Interface template to implement")

    // Template application options
    private val template by option("--template", help = "Template to apply").multiple()
    private val override by option("--override", help = "Template override (key:value)").multiple()
    private val resolve by option("--resolve", help = "Conflict resolution strategy (merge, replace)")

    // Debug and preview options
    private val debug by option("--debug", help = "Enable debug output").flag()
    private val preview by option("--preview", help = "Preview generated output").flag()
    private val dryRun by option("--dry-run", help = "Show what would be generated without writing").flag()
    private val validate by option("--validate", help = "Validate template before applying").flag()
    private val customModel by option("--custom-model", help = "Use custom model").flag()
    private val minecraft by option("--minecraft", help = "Target Minecraft version")

    /**
     * Build a map of all template variables from CLI options.
     * Combines explicit --var entries with item property options.
     */
    private fun buildTemplateVariables(): Map<String, String> {
        val vars = mutableMapOf<String, String>()

        // Parse explicit --var key:value pairs
        variable.forEach { v ->
            val parts = v.split(":", limit = 2)
            if (parts.size == 2) {
                vars[parts[0]] = parts[1]
            }
        }

        // Add item property options as variables
        tier?.let { vars["tier"] = it }
        armorType?.let { vars["armorType"] = it }
        if (enchantable) vars["enchantable"] = "true"
        if (dyeable) vars["dyeable"] = "true"
        if (repairable) vars["repairable"] = "true"
        repairMaterial?.let { vars["repairMaterial"] = it }
        vars["stackSize"] = stackSize
        maxDamage?.let { vars["maxDamage"] = it }
        if (attribute.isNotEmpty()) vars["attributes"] = attribute.joinToString(",")
        if (tag.isNotEmpty()) vars["tags"] = tag.joinToString(",")
        recipe?.let { vars["recipe"] = it }
        if (lootTable) vars["lootTable"] = "true"
        if (villagerTrade) vars["villagerTrade"] = "true"
        if (advancement) vars["advancement"] = "true"
        model?.let { vars["model"] = it }
        if (customModel) vars["customModel"] = "true"
        minecraft?.let { vars["minecraft"] = it }
        extends?.let { vars["extends"] = it }
        implements?.let { vars["implements"] = it }
        material?.let { vars["material"] = it }

        // Parse overrides
        override.forEach { o ->
            val parts = o.split(":", limit = 2)
            if (parts.size == 2) {
                vars[parts[0]] = parts[1]
            }
        }

        return vars
    }

    /**
     * Resolve the mod ID from config.yml if present.
     */
    private fun resolveModId(projectDir: File): String? {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) return null
        val content = configFile.readText()
        return Regex("id:\\s*([a-z0-9_-]+)").find(content)?.groupValues?.get(1)
    }

    /**
     * Convert snake_case to PascalCase class name.
     */
    private fun toClassName(snakeCase: String): String {
        return snakeCase.split("_").joinToString("") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    override fun run() {
        val projectDir = File(System.getProperty("user.dir"))
        val configFile = File(projectDir, "config.yml")
        val registry = TemplateRegistry()
        val templateVars = buildTemplateVariables()

        if (debug) {
            Logger.info("[DEBUG] Template creation starting...")
            Logger.info("[DEBUG] Template name: $templateName")
            Logger.info("[DEBUG] Material: $material")
            Logger.info("[DEBUG] Templates: $template")
            Logger.info("[DEBUG] Variables: $variable")
            Logger.info("[DEBUG] Resolved variables: $templateVars")
        }

        // --- Dry run: show what would be generated without writing ---
        if (dryRun) {
            Logger.info("[DRY RUN] Would create component: $templateName")
            if (template.isNotEmpty()) {
                Logger.info("[DRY RUN] Using templates: ${template.joinToString(", ")}")
            }
            val modId = resolveModId(projectDir)
            val sanitizedModId = modId?.let { FileUtil.sanitizeModId(it) }
            val className = toClassName(name ?: templateName)
            if (sanitizedModId != null) {
                Logger.info("[DRY RUN] Files that would be generated:")
                Logger.info("[DRY RUN]   shared/common/src/main/java/com/$sanitizedModId/items/$className.java")
                Logger.info("[DRY RUN]   versions/shared/v1/assets/$modId/models/item/${name ?: templateName}.json")
                Logger.info("[DRY RUN]   versions/shared/v1/assets/$modId/textures/item/${name ?: templateName}.png")
            }
            if (templateVars.isNotEmpty()) {
                Logger.info("[DRY RUN] Template variables: ${templateVars.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
            }
            return
        }

        // --- Preview: show what would be generated with options ---
        if (preview) {
            Logger.info("[PREVIEW] Component: $templateName")
            if (template.isNotEmpty()) {
                Logger.info("[PREVIEW] Templates: ${template.joinToString(", ")}")
            }
            val className = toClassName(name ?: templateName)
            Logger.info("[PREVIEW] Class name: $className")
            if (enchantable) Logger.info("[PREVIEW] Enchantable: yes")
            if (dyeable) Logger.info("[PREVIEW] Dyeable: yes")
            if (repairable) Logger.info("[PREVIEW] Repairable: yes")
            if (customModel) Logger.info("[PREVIEW] Custom model: yes")
            if (advancement) Logger.info("[PREVIEW] Advancement: yes")
            tier?.let { Logger.info("[PREVIEW] Tier: $it") }
            armorType?.let { Logger.info("[PREVIEW] Armor type: $it") }
            extends?.let { Logger.info("[PREVIEW] Extends: $it") }
            implements?.let { Logger.info("[PREVIEW] Implements: $it") }
            if (templateVars.isNotEmpty()) {
                Logger.info("[PREVIEW] Variables: ${templateVars.entries.joinToString(", ") { "${it.key}=${it.value}" }}")
            }
            return
        }

        // --- Validate: check templates exist ---
        if (validate) {
            val allTemplates = registry.listTemplates()
            val templateNames = allTemplates.map { it.name }

            if (template.isNotEmpty()) {
                Logger.info("Validating templates: ${template.joinToString(", ")}")
                var allValid = true
                template.forEach { t ->
                    val baseName = t.split(":").first()
                    val found = templateNames.contains(baseName)
                    if (found) {
                        Logger.info("  Validating template: $baseName - found")
                    } else {
                        Logger.info("  Validating template: $baseName - not found in registry")
                        allValid = false
                    }
                }
                if (!allValid) {
                    Logger.warn("Some templates were not found in the registry")
                }
            }

            // Also validate the main templateName against registry
            val mainFound = templateNames.contains(templateName)
            if (mainFound) {
                Logger.info("  Template '$templateName' is valid")
            } else {
                Logger.info("  Template '$templateName' not in registry (will use as component name)")
            }

            // If only validating (no other action), return early
            if (template.isEmpty() && !configFile.exists()) {
                return
            }
        }

        if (!configFile.exists()) {
            Logger.warn("No config.yml found. Running in standalone mode.")
        }

        val componentName = name ?: templateName

        Logger.info("Creating from template: $templateName...")

        // Log configuration details
        material?.let { Logger.info("  Material: $it") }
        tier?.let { Logger.info("  Tier: $it") }
        armorType?.let { Logger.info("  Armor type: $it") }
        if (enchantable) Logger.info("  Enchantable: yes")
        if (dyeable) Logger.info("  Dyeable: yes")
        if (repairable) {
            Logger.info("  Repairable: yes")
            repairMaterial?.let { Logger.info("  Repair material: $it") }
        }
        Logger.info("  Stack size: $stackSize")
        maxDamage?.let { Logger.info("  Max damage: $it") }
        if (attribute.isNotEmpty()) Logger.info("  Attributes: ${attribute.joinToString(", ")}")
        if (tag.isNotEmpty()) Logger.info("  Tags: ${tag.joinToString(", ")}")
        recipe?.let { Logger.info("  Recipe: $it") }
        if (lootTable) Logger.info("  Loot table: yes")
        if (villagerTrade) Logger.info("  Villager trade: yes")
        if (advancement) Logger.info("  Advancement: yes")
        model?.let { Logger.info("  Model: $it") }
        if (variable.isNotEmpty()) Logger.info("  Variables: ${variable.joinToString(", ")}")
        condition?.let { Logger.info("  Condition: $it") }
        forEach?.let { Logger.info("  ForEach: $it") }
        extends?.let { Logger.info("  Extends: $it") }
        if (include.isNotEmpty()) Logger.info("  Includes: ${include.joinToString(", ")}")
        implements?.let { Logger.info("  Implements: $it") }
        if (template.isNotEmpty()) Logger.info("  Templates: ${template.joinToString(", ")}")
        if (override.isNotEmpty()) Logger.info("  Overrides: ${override.joinToString(", ")}")
        resolve?.let { Logger.info("  Resolve strategy: $it") }
        minecraft?.let { Logger.info("  Minecraft version: $it") }
        if (customModel) Logger.info("  Custom model: yes")

        // Try to apply registered templates first (e.g., armor-set, tool-set, ore-set, wood-set, dimension)
        val success = registry.createFromTemplate(projectDir, templateName, componentName, material)

        if (success) {
            Logger.success("Template '$templateName' created successfully!")
        } else {
            // Template not found in registry -- generate a generic component from options
            val generated = generateComponentFromOptions(projectDir, componentName, templateVars)
            if (generated) {
                Logger.success("Component '$componentName' created successfully!")
            } else {
                Logger.warn("Template processing completed for '$templateName'")
            }
        }
    }

    /**
     * Generate a component when no registered template matches.
     * Uses --extends, --implements, --var, and item property options
     * to produce a Java class with real fields, properties, and methods.
     */
    private fun generateComponentFromOptions(projectDir: File, componentName: String, vars: Map<String, String>): Boolean {
        val configFile = File(projectDir, "config.yml")
        if (!configFile.exists()) {
            return false
        }

        val modId = resolveModId(projectDir) ?: return false
        val sanitizedModId = FileUtil.sanitizeModId(modId)
        val className = toClassName(componentName)

        // Determine base class: use --extends if given, otherwise default to Item
        val baseClass = extends?.let { toClassName(it) } ?: "Item"
        val extendsClause = " extends $baseClass"
        val implementsClause = implements?.let { " implements ${toClassName(it)}" } ?: ""

        // Resolve tier constant name for Java
        val tierValue = vars["tier"]
        val tierConstant = tierValue?.let { resolveTierConstant(it) }

        // Build Item.Properties chain
        val propertiesChain = buildItemProperties(vars)

        // Build imports
        val imports = buildImportList(vars, baseClass)

        // Build fields, constructor, and methods
        val fieldLines = buildList {
            add("    public static final String ID = \"$componentName\";")

            // Tier field
            if (tierConstant != null) {
                add("    public static final Tier TIER = Tiers.${tierConstant};")
            }

            // Attribute modifier fields
            if (vars.containsKey("attributes")) {
                vars["attributes"]!!.split(",").forEach { attr ->
                    val parts = attr.split(":", limit = 2)
                    if (parts.size == 2) {
                        val attrName = parts[0].trim().uppercase()
                        val attrValue = parts[1].trim()
                        add("    public static final double ${attrName}_MODIFIER = ${attrValue}D;")
                    }
                }
            }
        }

        val methodLines = buildList {
            // Constructor
            add("    public $className() {")
            add("        super($propertiesChain);")
            add("    }")

            // Enchantable override
            if (vars.containsKey("enchantable") && vars["enchantable"] == "true") {
                add("")
                add("    @Override")
                add("    public boolean isEnchantable(net.minecraft.world.item.ItemStack stack) {")
                add("        return true;")
                add("    }")
            }

            // Repairable + repair material
            if (vars.containsKey("repairable") && vars["repairable"] == "true" && vars.containsKey("repairMaterial")) {
                val repairMat = vars["repairMaterial"]!!
                val repairItemRef = resolveItemReference(repairMat)
                add("")
                add("    @Override")
                add("    public boolean isValidRepairItem(net.minecraft.world.item.ItemStack toRepair, net.minecraft.world.item.ItemStack repair) {")
                add("        return repair.is($repairItemRef) || super.isValidRepairItem(toRepair, repair);")
                add("    }")
            }
        }

        val content = buildString {
            appendLine("package com.$sanitizedModId.items;")
            appendLine()
            imports.forEach { appendLine("import $it;") }
            if (imports.isNotEmpty()) appendLine()
            appendLine("/**")
            appendLine(" * Custom component: $className")
            if (extends != null) appendLine(" * Extends: $extends")
            if (implements != null) appendLine(" * Implements: $implements")
            appendLine(" */")
            appendLine("public class $className$extendsClause$implementsClause {")
            fieldLines.forEach { appendLine(it) }
            if (fieldLines.isNotEmpty() && methodLines.isNotEmpty()) appendLine()
            methodLines.forEach { appendLine(it) }
            appendLine("}")
        }

        val itemFile = File(projectDir, "shared/common/src/main/java/com/$sanitizedModId/items/$className.java")
        try {
            FileUtil.writeText(itemFile, content)
            Logger.info("  Created: shared/common/src/main/java/com/$sanitizedModId/items/$className.java")
        } catch (e: Exception) {
            Logger.error("Failed to write component file: ${e.message}")
            return false
        }

        // Generate item model JSON
        val modelContent = """
            {
              "parent": "item/generated",
              "textures": {
                "layer0": "$modId:item/$componentName"
              }
            }
        """.trimIndent()

        try {
            val modelFile = File(projectDir, "versions/shared/v1/assets/$modId/models/item/$componentName.json")
            FileUtil.writeText(modelFile, modelContent)
            Logger.info("  Created: versions/shared/v1/assets/$modId/models/item/$componentName.json")
        } catch (e: Exception) {
            Logger.warn("Could not create model file: ${e.message}")
        }

        // Generate placeholder texture
        try {
            val textureFile = File(projectDir, "versions/shared/v1/assets/$modId/textures/item/$componentName.png")
            textureFile.parentFile?.mkdirs()
            if (!textureFile.exists()) {
                textureFile.createNewFile()
                Logger.info("  Created: versions/shared/v1/assets/$modId/textures/item/$componentName.png")
            }
        } catch (e: Exception) {
            Logger.warn("Could not create texture placeholder: ${e.message}")
        }

        return true
    }

    /**
     * Resolve a tier name to the corresponding Tiers enum constant.
     */
    private fun resolveTierConstant(tier: String): String {
        return when (tier.lowercase()) {
            "wood", "wooden" -> "WOOD"
            "stone" -> "STONE"
            "iron" -> "IRON"
            "gold", "golden" -> "GOLD"
            "diamond" -> "DIAMOND"
            "netherite" -> "NETHERITE"
            else -> tier.uppercase()
        }
    }

    /**
     * Build the Item.Properties constructor chain from template variables.
     */
    private fun buildItemProperties(vars: Map<String, String>): String {
        val chain = StringBuilder("new Item.Properties()")
        val stackSize = vars["stackSize"]
        if (stackSize != null && stackSize != "64") {
            chain.append(".stacksTo($stackSize)")
        }
        val maxDamage = vars["maxDamage"]
        if (maxDamage != null) {
            chain.append(".durability($maxDamage)")
        }
        return chain.toString()
    }

    /**
     * Build the list of import statements needed for the generated class.
     */
    private fun buildImportList(vars: Map<String, String>, baseClass: String): List<String> {
        val imports = mutableListOf<String>()
        if (baseClass == "Item") {
            imports.add("net.minecraft.world.item.Item")
        }
        if (vars.containsKey("tier")) {
            imports.add("net.minecraft.world.item.Tier")
            imports.add("net.minecraft.world.item.Tiers")
        }
        if (vars.containsKey("repairMaterial")) {
            imports.add("net.minecraft.world.item.Items")
        }
        return imports.sorted()
    }

    /**
     * Resolve an item name to a code reference (e.g., "iron_ingot" -> "Items.IRON_INGOT").
     */
    private fun resolveItemReference(itemName: String): String {
        return "Items.${itemName.uppercase()}"
    }
}
