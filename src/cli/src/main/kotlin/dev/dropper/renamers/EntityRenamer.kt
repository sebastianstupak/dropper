package dev.dropper.renamers

import java.io.File

/**
 * Renamer for entity components
 */
class EntityRenamer : ComponentRenamer {

    override fun discover(context: RenameContext): List<File> {
        val files = mutableListOf<File>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // Common entity class
        val commonEntityFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/entities/$oldClassName.java")
        if (commonEntityFile.exists()) files.add(commonEntityFile)

        // Loader-specific registration files
        listOf("fabric", "forge", "neoforge").forEach { loader ->
            val loaderSuffix = RenamerUtil.loaderClassName(loader)
            val loaderFile = File(context.projectDir, "shared/$loader/src/main/java/$packagePath/platform/$loader/${oldClassName}${loaderSuffix}.java")
            if (loaderFile.exists()) files.add(loaderFile)

            // Renderer files
            val rendererFile = File(context.projectDir, "shared/$loader/src/main/java/$packagePath/client/renderer/$loader/${oldClassName}Renderer.java")
            if (rendererFile.exists()) files.add(rendererFile)
        }

        // Spawn egg class
        val spawnEggFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/items/${oldClassName}SpawnEgg.java")
        if (spawnEggFile.exists()) files.add(spawnEggFile)

        // Assets
        val versionPath = if (context.version != null) {
            "versions/${context.version}"
        } else {
            "versions/shared/v1"
        }

        // Entity model
        val modelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/entity/${context.oldName}.json")
        if (modelFile.exists()) files.add(modelFile)

        // Entity texture
        val textureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/entity/${context.oldName}.png")
        if (textureFile.exists()) files.add(textureFile)

        // Spawn egg model
        val spawnEggModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/item/${context.oldName}_spawn_egg.json")
        if (spawnEggModelFile.exists()) files.add(spawnEggModelFile)

        // Spawn egg texture
        val spawnEggTextureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/item/${context.oldName}_spawn_egg.png")
        if (spawnEggTextureFile.exists()) files.add(spawnEggTextureFile)

        return files
    }

    override fun findReferences(context: RenameContext, discoveredFiles: List<File>): Map<File, List<String>> {
        val references = mutableMapOf<File, MutableList<String>>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)

        // Find Java/Kotlin files that reference this entity
        val javaFiles = RenamerUtil.findFilesContaining(
            context.projectDir,
            oldClassName,
            listOf(".java", ".kt")
        )

        javaFiles.forEach { file ->
            val content = file.readText()
            val refs = mutableListOf<String>()

            if (content.contains(oldClassName)) {
                refs.add("Class name: $oldClassName")
            }

            if (content.contains("\"${context.oldName}\"")) {
                refs.add("ID: ${context.oldName}")
            }

            if (content.contains("import ${context.packageName}.entities.$oldClassName")) {
                refs.add("Import: ${context.packageName}.entities.$oldClassName")
            }

            if (refs.isNotEmpty()) {
                references[file] = refs
            }
        }

        // Find JSON files that reference this entity
        val jsonFiles = RenamerUtil.findFilesContaining(
            context.projectDir,
            "${context.modId}:${context.oldName}",
            listOf(".json")
        )

        jsonFiles.forEach { file ->
            val refs = references.getOrPut(file) { mutableListOf() }
            refs.add("Resource location: ${context.modId}:${context.oldName}")
        }

        // Find lang file references
        val langFiles = RenamerUtil.findFilesContaining(
            context.projectDir,
            "entity.${context.modId}.${context.oldName}",
            listOf(".json")
        )

        langFiles.forEach { file ->
            val refs = references.getOrPut(file) { mutableListOf() }
            refs.add("Lang key: entity.${context.modId}.${context.oldName}")
        }

        return references
    }

    override fun checkConflicts(context: RenameContext): List<String> {
        val conflicts = mutableListOf<String>()
        val newClassName = RenamerUtil.toClassName(context.newName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // Check if new class name already exists
        val newCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/entities/$newClassName.java")
        if (newCommonFile.exists()) {
            conflicts.add("Entity class already exists: $newClassName")
        }

        // Check if new assets already exist
        val versionPath = if (context.version != null) {
            "versions/${context.version}"
        } else {
            "versions/shared/v1"
        }

        val newModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/entity/${context.newName}.json")
        if (newModelFile.exists()) {
            conflicts.add("Entity model already exists: ${context.newName}.json")
        }

        return conflicts
    }

    override fun planRename(context: RenameContext): List<RenameOperation> {
        val operations = mutableListOf<RenameOperation>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val newClassName = RenamerUtil.toClassName(context.newName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // 1. Rename common entity class
        val commonEntityFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/entities/$oldClassName.java")
        if (commonEntityFile.exists()) {
            val newFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/entities/$newClassName.java")
            operations.add(RenameOperation.FileRename(commonEntityFile, newFile))

            operations.add(
                RenameOperation.ContentReplace(
                    file = newFile,
                    oldContent = oldClassName,
                    newContent = newClassName,
                    description = "Update class name references"
                )
            )

            operations.add(
                RenameOperation.ContentReplace(
                    file = newFile,
                    oldContent = "\"${context.oldName}\"",
                    newContent = "\"${context.newName}\"",
                    description = "Update entity ID"
                )
            )
        }

        // 2. Rename spawn egg class
        val spawnEggFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/items/${oldClassName}SpawnEgg.java")
        if (spawnEggFile.exists()) {
            val newSpawnEggFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/items/${newClassName}SpawnEgg.java")
            operations.add(RenameOperation.FileRename(spawnEggFile, newSpawnEggFile))

            operations.add(
                RenameOperation.ContentReplace(
                    file = newSpawnEggFile,
                    oldContent = oldClassName,
                    newContent = newClassName,
                    description = "Update spawn egg class references"
                )
            )

            operations.add(
                RenameOperation.ContentReplace(
                    file = newSpawnEggFile,
                    oldContent = "${context.oldName}_spawn_egg",
                    newContent = "${context.newName}_spawn_egg",
                    description = "Update spawn egg ID"
                )
            )
        }

        // 3. Rename loader-specific files
        listOf("fabric", "forge", "neoforge").forEach { loader ->
            val loaderSuffix = RenamerUtil.loaderClassName(loader)
            val oldLoaderFile = File(context.projectDir, "shared/$loader/src/main/java/$packagePath/platform/$loader/${oldClassName}${loaderSuffix}.java")
            if (oldLoaderFile.exists()) {
                val newLoaderFile = File(context.projectDir, "shared/$loader/src/main/java/$packagePath/platform/$loader/${newClassName}${loaderSuffix}.java")
                operations.add(RenameOperation.FileRename(oldLoaderFile, newLoaderFile))

                operations.add(
                    RenameOperation.ContentReplace(
                        file = newLoaderFile,
                        oldContent = oldClassName,
                        newContent = newClassName,
                        description = "Update loader class references"
                    )
                )
            }

            // Renderer files
            val oldRendererFile = File(context.projectDir, "shared/$loader/src/main/java/$packagePath/client/renderer/$loader/${oldClassName}Renderer.java")
            if (oldRendererFile.exists()) {
                val newRendererFile = File(context.projectDir, "shared/$loader/src/main/java/$packagePath/client/renderer/$loader/${newClassName}Renderer.java")
                operations.add(RenameOperation.FileRename(oldRendererFile, newRendererFile))

                operations.add(
                    RenameOperation.ContentReplace(
                        file = newRendererFile,
                        oldContent = oldClassName,
                        newContent = newClassName,
                        description = "Update renderer class references"
                    )
                )

                operations.add(
                    RenameOperation.ContentReplace(
                        file = newRendererFile,
                        oldContent = "textures/entity/${context.oldName}",
                        newContent = "textures/entity/${context.newName}",
                        description = "Update texture path"
                    )
                )
            }
        }

        // 4. Rename assets
        val versionPath = if (context.version != null) {
            "versions/${context.version}"
        } else {
            "versions/shared/v1"
        }

        // Entity model
        val oldModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/entity/${context.oldName}.json")
        if (oldModelFile.exists()) {
            val newModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/entity/${context.newName}.json")
            operations.add(RenameOperation.FileRename(oldModelFile, newModelFile))

            operations.add(
                RenameOperation.ContentReplace(
                    file = newModelFile,
                    oldContent = context.oldName,
                    newContent = context.newName,
                    description = "Update entity name in model"
                )
            )
        }

        // Entity texture
        val oldTextureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/entity/${context.oldName}.png")
        if (oldTextureFile.exists()) {
            val newTextureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/entity/${context.newName}.png")
            operations.add(RenameOperation.FileRename(oldTextureFile, newTextureFile))
        }

        // Spawn egg model
        val oldSpawnEggModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/item/${context.oldName}_spawn_egg.json")
        if (oldSpawnEggModelFile.exists()) {
            val newSpawnEggModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/item/${context.newName}_spawn_egg.json")
            operations.add(RenameOperation.FileRename(oldSpawnEggModelFile, newSpawnEggModelFile))
        }

        // Spawn egg texture
        val oldSpawnEggTextureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/item/${context.oldName}_spawn_egg.png")
        if (oldSpawnEggTextureFile.exists()) {
            val newSpawnEggTextureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/item/${context.newName}_spawn_egg.png")
            operations.add(RenameOperation.FileRename(oldSpawnEggTextureFile, newSpawnEggTextureFile))
        }

        // 5. Update lang files
        val langFiles = RenamerUtil.findFilesContaining(
            context.projectDir,
            "entity.${context.modId}.${context.oldName}",
            listOf(".json")
        )

        langFiles.forEach { langFile ->
            operations.add(
                RenameOperation.ContentReplace(
                    file = langFile,
                    oldContent = "entity.${context.modId}.${context.oldName}",
                    newContent = "entity.${context.modId}.${context.newName}",
                    description = "Update entity lang key"
                )
            )

            // Also update spawn egg lang key if present
            val content = langFile.readText()
            if (content.contains("item.${context.modId}.${context.oldName}_spawn_egg")) {
                operations.add(
                    RenameOperation.ContentReplace(
                        file = langFile,
                        oldContent = "item.${context.modId}.${context.oldName}_spawn_egg",
                        newContent = "item.${context.modId}.${context.newName}_spawn_egg",
                        description = "Update spawn egg lang key"
                    )
                )
            }
        }

        return operations
    }

    override fun validate(context: RenameContext): Boolean {
        val newClassName = RenamerUtil.toClassName(context.newName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        val newCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/entities/$newClassName.java")
        if (!newCommonFile.exists()) return false

        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val oldCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/entities/$oldClassName.java")
        if (oldCommonFile.exists()) return false

        return true
    }

    override fun getFilePatterns(context: RenameContext): List<FilePattern> {
        return listOf(
            FilePattern("${context.oldName}.java", FileType.JAVA_CLASS),
            FilePattern("${context.oldName}.json", FileType.JSON_ASSET),
            FilePattern("${context.oldName}.png", FileType.TEXTURE),
            FilePattern("${context.oldName}_spawn_egg.java", FileType.JAVA_CLASS),
            FilePattern("${context.oldName}_spawn_egg.json", FileType.JSON_ASSET),
            FilePattern("${context.oldName}_spawn_egg.png", FileType.TEXTURE)
        )
    }
}
