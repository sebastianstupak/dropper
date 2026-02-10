package dev.dropper.removers

import java.io.File

/**
 * Analyzes dependencies between mod components
 */
object DependencyAnalyzer {

    /**
     * Find all recipes that use this item/block
     */
    fun findRecipeReferences(projectDir: File, componentId: String, modId: String): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val recipeDir = File(projectDir, "versions/shared/v1/data/$modId/recipe")

        if (!recipeDir.exists()) return dependencies

        recipeDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .forEach { recipeFile ->
                // Skip the component's own recipe (self-reference)
                if (recipeFile.nameWithoutExtension == componentId) return@forEach

                val content = recipeFile.readText()
                if (content.contains("\"$modId:$componentId\"") ||
                    content.contains("\"item\": \"$modId:$componentId\"") ||
                    content.contains("\"id\": \"$modId:$componentId\"")) {
                    dependencies.add(
                        Dependency(
                            file = recipeFile,
                            type = DependencyType.RECIPE,
                            description = "Recipe ${recipeFile.nameWithoutExtension} uses $componentId"
                        )
                    )
                }
            }

        return dependencies
    }

    /**
     * Find all tags that reference this item/block
     */
    fun findTagReferences(projectDir: File, componentId: String, modId: String): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val tagsDir = File(projectDir, "versions/shared/v1/data/$modId/tags")

        if (!tagsDir.exists()) return dependencies

        tagsDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .forEach { tagFile ->
                val content = tagFile.readText()
                if (content.contains("\"$modId:$componentId\"")) {
                    dependencies.add(
                        Dependency(
                            file = tagFile,
                            type = DependencyType.TAG,
                            description = "Tag ${tagFile.nameWithoutExtension} references $componentId"
                        )
                    )
                }
            }

        return dependencies
    }

    /**
     * Find all loot tables that reference this item/block
     */
    fun findLootTableReferences(projectDir: File, componentId: String, modId: String): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val lootTableDir = File(projectDir, "versions/shared/v1/data/$modId/loot_tables")

        if (!lootTableDir.exists()) return dependencies

        lootTableDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .forEach { lootFile ->
                val content = lootFile.readText()
                if (content.contains("\"$modId:$componentId\"")) {
                    dependencies.add(
                        Dependency(
                            file = lootFile,
                            type = DependencyType.LOOT_TABLE,
                            description = "Loot table ${lootFile.nameWithoutExtension} references $componentId"
                        )
                    )
                }
            }

        return dependencies
    }

    /**
     * Find all advancements that reference this item/block
     */
    fun findAdvancementReferences(projectDir: File, componentId: String, modId: String): List<Dependency> {
        val dependencies = mutableListOf<Dependency>()
        val advancementsDir = File(projectDir, "versions/shared/v1/data/$modId/advancement")

        if (!advancementsDir.exists()) return dependencies

        advancementsDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .forEach { advFile ->
                val content = advFile.readText()
                if (content.contains("\"$modId:$componentId\"")) {
                    dependencies.add(
                        Dependency(
                            file = advFile,
                            type = DependencyType.ADVANCEMENT,
                            description = "Advancement ${advFile.nameWithoutExtension} references $componentId"
                        )
                    )
                }
            }

        return dependencies
    }

    /**
     * Find all dependencies for a component
     */
    fun findAllDependencies(projectDir: File, componentId: String, modId: String): List<Dependency> {
        val allDependencies = mutableListOf<Dependency>()

        allDependencies.addAll(findRecipeReferences(projectDir, componentId, modId))
        allDependencies.addAll(findTagReferences(projectDir, componentId, modId))
        allDependencies.addAll(findLootTableReferences(projectDir, componentId, modId))
        allDependencies.addAll(findAdvancementReferences(projectDir, componentId, modId))

        return allDependencies
    }
}
