package dev.dropper.renamers

import java.io.File

/**
 * Renamer for item components
 */
class ItemRenamer : ComponentRenamer {

    override fun discover(context: RenameContext): List<File> {
        val files = mutableListOf<File>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // Common item class
        val commonItemFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/items/$oldClassName.java")
        if (commonItemFile.exists()) files.add(commonItemFile)

        // Loader-specific registration files
        listOf("fabric", "forge", "neoforge").forEach { loader ->
            val loaderFile = File(context.projectDir, "shared/$loader/src/main/java/$packagePath/platform/$loader/${oldClassName}${loader.capitalize()}.java")
            if (loaderFile.exists()) files.add(loaderFile)
        }

        // Assets
        val versionPath = if (context.version != null) {
            "versions/${context.version}"
        } else {
            "versions/shared/v1"
        }

        // Item model
        val modelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/item/${context.oldName}.json")
        if (modelFile.exists()) files.add(modelFile)

        // Texture
        val textureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/item/${context.oldName}.png")
        if (textureFile.exists()) files.add(textureFile)

        // Recipe
        val recipeFile = File(context.projectDir, "$versionPath/data/${context.modId}/recipe/${context.oldName}.json")
        if (recipeFile.exists()) files.add(recipeFile)

        return files
    }

    override fun findReferences(context: RenameContext, discoveredFiles: List<File>): Map<File, List<String>> {
        val references = mutableMapOf<File, MutableList<String>>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)

        // Find Java/Kotlin files that import or reference this item
        val javaFiles = RenamerUtil.findFilesContaining(
            context.projectDir,
            oldClassName,
            listOf(".java", ".kt")
        )

        javaFiles.forEach { file ->
            val content = file.readText()
            val refs = mutableListOf<String>()

            // Class name references
            if (content.contains(oldClassName)) {
                refs.add("Class name: $oldClassName")
            }

            // ID references
            if (content.contains("\"${context.oldName}\"")) {
                refs.add("ID: ${context.oldName}")
            }

            // Import references
            if (content.contains("import ${context.packageName}.items.$oldClassName")) {
                refs.add("Import: ${context.packageName}.items.$oldClassName")
            }

            if (refs.isNotEmpty()) {
                references[file] = refs
            }
        }

        // Find JSON files that reference this item
        val jsonFiles = RenamerUtil.findFilesContaining(
            context.projectDir,
            "${context.modId}:${context.oldName}",
            listOf(".json")
        )

        jsonFiles.forEach { file ->
            val refs = references.getOrPut(file) { mutableListOf() }
            refs.add("Resource location: ${context.modId}:${context.oldName}")
        }

        // Find model references
        val modelRefs = RenamerUtil.findFilesContaining(
            context.projectDir,
            "${context.modId}:item/${context.oldName}",
            listOf(".json")
        )

        modelRefs.forEach { file ->
            val refs = references.getOrPut(file) { mutableListOf() }
            refs.add("Texture reference: ${context.modId}:item/${context.oldName}")
        }

        return references
    }

    override fun checkConflicts(context: RenameContext): List<String> {
        val conflicts = mutableListOf<String>()
        val newClassName = RenamerUtil.toClassName(context.newName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // Check if new class name already exists
        val newCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/items/$newClassName.java")
        if (newCommonFile.exists()) {
            conflicts.add("Item class already exists: $newClassName")
        }

        // Check if new assets already exist
        val versionPath = if (context.version != null) {
            "versions/${context.version}"
        } else {
            "versions/shared/v1"
        }

        val newModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/item/${context.newName}.json")
        if (newModelFile.exists()) {
            conflicts.add("Item model already exists: ${context.newName}.json")
        }

        return conflicts
    }

    override fun planRename(context: RenameContext): List<RenameOperation> {
        val operations = mutableListOf<RenameOperation>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val newClassName = RenamerUtil.toClassName(context.newName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // 1. Rename Java classes
        val commonItemFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/items/$oldClassName.java")
        if (commonItemFile.exists()) {
            val newFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/items/$newClassName.java")
            operations.add(RenameOperation.FileRename(commonItemFile, newFile))

            // Update class name in file
            operations.add(
                RenameOperation.ContentReplace(
                    file = newFile,
                    oldContent = "class $oldClassName",
                    newContent = "class $newClassName",
                    description = "Update class name"
                )
            )

            // Update ID constant
            operations.add(
                RenameOperation.ContentReplace(
                    file = newFile,
                    oldContent = "public static final String ID = \"${context.oldName}\"",
                    newContent = "public static final String ID = \"${context.newName}\"",
                    description = "Update ID constant"
                )
            )
        }

        // 2. Rename loader-specific files
        listOf("fabric", "forge", "neoforge").forEach { loader ->
            val oldLoaderFile = File(context.projectDir, "shared/$loader/src/main/java/$packagePath/platform/$loader/${oldClassName}${loader.capitalize()}.java")
            if (oldLoaderFile.exists()) {
                val newLoaderFile = File(context.projectDir, "shared/$loader/src/main/java/$packagePath/platform/$loader/${newClassName}${loader.capitalize()}.java")
                operations.add(RenameOperation.FileRename(oldLoaderFile, newLoaderFile))

                // Update class name
                operations.add(
                    RenameOperation.ContentReplace(
                        file = newLoaderFile,
                        oldContent = "class ${oldClassName}${loader.capitalize()}",
                        newContent = "class ${newClassName}${loader.capitalize()}",
                        description = "Update loader class name"
                    )
                )

                // Update import
                operations.add(
                    RenameOperation.ContentReplace(
                        file = newLoaderFile,
                        oldContent = "import ${context.packageName}.items.$oldClassName",
                        newContent = "import ${context.packageName}.items.$newClassName",
                        description = "Update import statement"
                    )
                )

                // Update class references
                operations.add(
                    RenameOperation.ContentReplace(
                        file = newLoaderFile,
                        oldContent = "$oldClassName.ID",
                        newContent = "$newClassName.ID",
                        description = "Update class references"
                    )
                )
            }
        }

        // 3. Rename assets
        val versionPath = if (context.version != null) {
            "versions/${context.version}"
        } else {
            "versions/shared/v1"
        }

        // Model
        val oldModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/item/${context.oldName}.json")
        if (oldModelFile.exists()) {
            val newModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/item/${context.newName}.json")
            operations.add(RenameOperation.FileRename(oldModelFile, newModelFile))

            // Update texture reference in model
            operations.add(
                RenameOperation.ContentReplace(
                    file = newModelFile,
                    oldContent = "${context.modId}:item/${context.oldName}",
                    newContent = "${context.modId}:item/${context.newName}",
                    description = "Update texture reference"
                )
            )
        }

        // Texture
        val oldTextureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/item/${context.oldName}.png")
        if (oldTextureFile.exists()) {
            val newTextureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/item/${context.newName}.png")
            operations.add(RenameOperation.FileRename(oldTextureFile, newTextureFile))
        }

        // 4. Update recipes that reference this item
        val recipeDir = File(context.projectDir, "$versionPath/data/${context.modId}/recipe")
        if (recipeDir.exists()) {
            recipeDir.listFiles()?.filter { it.extension == "json" }?.forEach { recipeFile ->
                val content = recipeFile.readText()
                if (content.contains("${context.modId}:${context.oldName}")) {
                    operations.add(
                        RenameOperation.ContentReplace(
                            file = recipeFile,
                            oldContent = "${context.modId}:${context.oldName}",
                            newContent = "${context.modId}:${context.newName}",
                            description = "Update item reference in recipe"
                        )
                    )
                }
            }
        }

        // 5. Update tags
        val tagDir = File(context.projectDir, "$versionPath/data/${context.modId}/tags/item")
        if (tagDir.exists()) {
            tagDir.listFiles()?.filter { it.extension == "json" }?.forEach { tagFile ->
                val content = tagFile.readText()
                if (content.contains("${context.modId}:${context.oldName}")) {
                    operations.add(
                        RenameOperation.ContentReplace(
                            file = tagFile,
                            oldContent = "${context.modId}:${context.oldName}",
                            newContent = "${context.modId}:${context.newName}",
                            description = "Update item reference in tag"
                        )
                    )
                }
            }
        }

        return operations
    }

    override fun validate(context: RenameContext): Boolean {
        val newClassName = RenamerUtil.toClassName(context.newName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // Check that new files exist
        val newCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/items/$newClassName.java")
        if (!newCommonFile.exists()) return false

        // Check that old files don't exist
        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val oldCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/items/$oldClassName.java")
        if (oldCommonFile.exists()) return false

        return true
    }

    override fun getFilePatterns(context: RenameContext): List<FilePattern> {
        return listOf(
            FilePattern("${context.oldName}.java", FileType.JAVA_CLASS),
            FilePattern("${context.oldName}.json", FileType.JSON_ASSET),
            FilePattern("${context.oldName}.png", FileType.TEXTURE)
        )
    }
}
