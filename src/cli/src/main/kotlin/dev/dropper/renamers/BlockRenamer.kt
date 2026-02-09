package dev.dropper.renamers

import java.io.File

/**
 * Renamer for block components
 */
class BlockRenamer : ComponentRenamer {

    override fun discover(context: RenameContext): List<File> {
        val files = mutableListOf<File>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // Common block class
        val commonBlockFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/blocks/$oldClassName.java")
        if (commonBlockFile.exists()) files.add(commonBlockFile)

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

        // Blockstate
        val blockstateFile = File(context.projectDir, "$versionPath/assets/${context.modId}/blockstates/${context.oldName}.json")
        if (blockstateFile.exists()) files.add(blockstateFile)

        // Block model
        val blockModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/block/${context.oldName}.json")
        if (blockModelFile.exists()) files.add(blockModelFile)

        // Item model
        val itemModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/item/${context.oldName}.json")
        if (itemModelFile.exists()) files.add(itemModelFile)

        // Texture
        val textureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/block/${context.oldName}.png")
        if (textureFile.exists()) files.add(textureFile)

        // Loot table
        val lootTableFile = File(context.projectDir, "$versionPath/data/${context.modId}/loot_table/blocks/${context.oldName}.json")
        if (lootTableFile.exists()) files.add(lootTableFile)

        return files
    }

    override fun findReferences(context: RenameContext, discoveredFiles: List<File>): Map<File, List<String>> {
        val references = mutableMapOf<File, MutableList<String>>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)

        // Find Java/Kotlin files that reference this block
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

            if (refs.isNotEmpty()) {
                references[file] = refs
            }
        }

        // Find JSON files that reference this block
        val jsonFiles = RenamerUtil.findFilesContaining(
            context.projectDir,
            "${context.modId}:${context.oldName}",
            listOf(".json")
        )

        jsonFiles.forEach { file ->
            val refs = references.getOrPut(file) { mutableListOf() }
            refs.add("Resource location: ${context.modId}:${context.oldName}")
        }

        return references
    }

    override fun checkConflicts(context: RenameContext): List<String> {
        val conflicts = mutableListOf<String>()
        val newClassName = RenamerUtil.toClassName(context.newName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // Check if new class already exists
        val newCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/blocks/$newClassName.java")
        if (newCommonFile.exists()) {
            conflicts.add("Block class already exists: $newClassName")
        }

        return conflicts
    }

    override fun planRename(context: RenameContext): List<RenameOperation> {
        val operations = mutableListOf<RenameOperation>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val newClassName = RenamerUtil.toClassName(context.newName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // 1. Rename Java classes
        val commonBlockFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/blocks/$oldClassName.java")
        if (commonBlockFile.exists()) {
            val newFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/blocks/$newClassName.java")
            operations.add(RenameOperation.FileRename(commonBlockFile, newFile))

            operations.add(
                RenameOperation.ContentReplace(
                    file = newFile,
                    oldContent = "class $oldClassName",
                    newContent = "class $newClassName",
                    description = "Update class name"
                )
            )

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

                operations.add(
                    RenameOperation.ContentReplace(
                        file = newLoaderFile,
                        oldContent = "class ${oldClassName}${loader.capitalize()}",
                        newContent = "class ${newClassName}${loader.capitalize()}",
                        description = "Update loader class name"
                    )
                )

                operations.add(
                    RenameOperation.ContentReplace(
                        file = newLoaderFile,
                        oldContent = "import ${context.packageName}.blocks.$oldClassName",
                        newContent = "import ${context.packageName}.blocks.$newClassName",
                        description = "Update import statement"
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

        // Blockstate
        val oldBlockstateFile = File(context.projectDir, "$versionPath/assets/${context.modId}/blockstates/${context.oldName}.json")
        if (oldBlockstateFile.exists()) {
            val newBlockstateFile = File(context.projectDir, "$versionPath/assets/${context.modId}/blockstates/${context.newName}.json")
            operations.add(RenameOperation.FileRename(oldBlockstateFile, newBlockstateFile))

            operations.add(
                RenameOperation.ContentReplace(
                    file = newBlockstateFile,
                    oldContent = "${context.modId}:block/${context.oldName}",
                    newContent = "${context.modId}:block/${context.newName}",
                    description = "Update model reference in blockstate"
                )
            )
        }

        // Block model
        val oldBlockModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/block/${context.oldName}.json")
        if (oldBlockModelFile.exists()) {
            val newBlockModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/block/${context.newName}.json")
            operations.add(RenameOperation.FileRename(oldBlockModelFile, newBlockModelFile))

            operations.add(
                RenameOperation.ContentReplace(
                    file = newBlockModelFile,
                    oldContent = "${context.modId}:block/${context.oldName}",
                    newContent = "${context.modId}:block/${context.newName}",
                    description = "Update texture reference in block model"
                )
            )
        }

        // Item model
        val oldItemModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/item/${context.oldName}.json")
        if (oldItemModelFile.exists()) {
            val newItemModelFile = File(context.projectDir, "$versionPath/assets/${context.modId}/models/item/${context.newName}.json")
            operations.add(RenameOperation.FileRename(oldItemModelFile, newItemModelFile))

            operations.add(
                RenameOperation.ContentReplace(
                    file = newItemModelFile,
                    oldContent = "${context.modId}:block/${context.oldName}",
                    newContent = "${context.modId}:block/${context.newName}",
                    description = "Update block model reference in item model"
                )
            )
        }

        // Texture
        val oldTextureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/block/${context.oldName}.png")
        if (oldTextureFile.exists()) {
            val newTextureFile = File(context.projectDir, "$versionPath/assets/${context.modId}/textures/block/${context.newName}.png")
            operations.add(RenameOperation.FileRename(oldTextureFile, newTextureFile))
        }

        // Loot table
        val oldLootTableFile = File(context.projectDir, "$versionPath/data/${context.modId}/loot_table/blocks/${context.oldName}.json")
        if (oldLootTableFile.exists()) {
            val newLootTableFile = File(context.projectDir, "$versionPath/data/${context.modId}/loot_table/blocks/${context.newName}.json")
            operations.add(RenameOperation.FileRename(oldLootTableFile, newLootTableFile))

            operations.add(
                RenameOperation.ContentReplace(
                    file = newLootTableFile,
                    oldContent = "${context.modId}:${context.oldName}",
                    newContent = "${context.modId}:${context.newName}",
                    description = "Update block reference in loot table"
                )
            )
        }

        // 4. Update tags
        val tagDir = File(context.projectDir, "$versionPath/data/${context.modId}/tags/block")
        if (tagDir.exists()) {
            tagDir.listFiles()?.filter { it.extension == "json" }?.forEach { tagFile ->
                val content = tagFile.readText()
                if (content.contains("${context.modId}:${context.oldName}")) {
                    operations.add(
                        RenameOperation.ContentReplace(
                            file = tagFile,
                            oldContent = "${context.modId}:${context.oldName}",
                            newContent = "${context.modId}:${context.newName}",
                            description = "Update block reference in tag"
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

        val newCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/blocks/$newClassName.java")
        if (!newCommonFile.exists()) return false

        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val oldCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/blocks/$oldClassName.java")
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
