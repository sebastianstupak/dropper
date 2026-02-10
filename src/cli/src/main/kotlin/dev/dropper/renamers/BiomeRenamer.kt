package dev.dropper.renamers

import java.io.File

/**
 * Renamer for biome components
 */
class BiomeRenamer : ComponentRenamer {

    override fun discover(context: RenameContext): List<File> {
        val files = mutableListOf<File>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // Common biome class
        val commonBiomeFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/biomes/$oldClassName.java")
        if (commonBiomeFile.exists()) files.add(commonBiomeFile)

        // Loader-specific registration files
        listOf("fabric", "forge", "neoforge").forEach { loader ->
            val loaderSuffix = RenamerUtil.loaderClassName(loader)
            val loaderFile = File(context.projectDir, "shared/$loader/src/main/java/$packagePath/platform/$loader/${oldClassName}${loaderSuffix}.java")
            if (loaderFile.exists()) files.add(loaderFile)
        }

        // Data files
        val versionPath = if (context.version != null) {
            "versions/${context.version}"
        } else {
            "versions/shared/v1"
        }

        // Worldgen biome data
        val biomeDataFile = File(context.projectDir, "$versionPath/data/${context.modId}/worldgen/biome/${context.oldName}.json")
        if (biomeDataFile.exists()) files.add(biomeDataFile)

        return files
    }

    override fun findReferences(context: RenameContext, discoveredFiles: List<File>): Map<File, List<String>> {
        val references = mutableMapOf<File, MutableList<String>>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)

        // Find Java/Kotlin files that reference this biome
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

        // Find JSON files that reference this biome
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
            "biome.${context.modId}.${context.oldName}",
            listOf(".json")
        )

        langFiles.forEach { file ->
            val refs = references.getOrPut(file) { mutableListOf() }
            refs.add("Lang key: biome.${context.modId}.${context.oldName}")
        }

        return references
    }

    override fun checkConflicts(context: RenameContext): List<String> {
        val conflicts = mutableListOf<String>()
        val newClassName = RenamerUtil.toClassName(context.newName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        val newCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/biomes/$newClassName.java")
        if (newCommonFile.exists()) {
            conflicts.add("Biome class already exists: $newClassName")
        }

        val versionPath = if (context.version != null) {
            "versions/${context.version}"
        } else {
            "versions/shared/v1"
        }

        val newDataFile = File(context.projectDir, "$versionPath/data/${context.modId}/worldgen/biome/${context.newName}.json")
        if (newDataFile.exists()) {
            conflicts.add("Biome data already exists: ${context.newName}.json")
        }

        return conflicts
    }

    override fun planRename(context: RenameContext): List<RenameOperation> {
        val operations = mutableListOf<RenameOperation>()
        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val newClassName = RenamerUtil.toClassName(context.newName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        // 1. Rename common biome class
        val commonBiomeFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/biomes/$oldClassName.java")
        if (commonBiomeFile.exists()) {
            val newFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/biomes/$newClassName.java")
            operations.add(RenameOperation.FileRename(commonBiomeFile, newFile))

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
                    description = "Update biome ID"
                )
            )
        }

        // 2. Rename loader-specific files
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
        }

        // 3. Rename data files
        val versionPath = if (context.version != null) {
            "versions/${context.version}"
        } else {
            "versions/shared/v1"
        }

        val oldDataFile = File(context.projectDir, "$versionPath/data/${context.modId}/worldgen/biome/${context.oldName}.json")
        if (oldDataFile.exists()) {
            val newDataFile = File(context.projectDir, "$versionPath/data/${context.modId}/worldgen/biome/${context.newName}.json")
            operations.add(RenameOperation.FileRename(oldDataFile, newDataFile))
        }

        // 4. Update lang files
        val langFiles = RenamerUtil.findFilesContaining(
            context.projectDir,
            "biome.${context.modId}.${context.oldName}",
            listOf(".json")
        )

        langFiles.forEach { langFile ->
            operations.add(
                RenameOperation.ContentReplace(
                    file = langFile,
                    oldContent = "biome.${context.modId}.${context.oldName}",
                    newContent = "biome.${context.modId}.${context.newName}",
                    description = "Update biome lang key"
                )
            )
        }

        // 5. Update JSON references
        val jsonFiles = RenamerUtil.findFilesContaining(
            context.projectDir,
            "${context.modId}:${context.oldName}",
            listOf(".json")
        )

        jsonFiles.forEach { jsonFile ->
            operations.add(
                RenameOperation.ContentReplace(
                    file = jsonFile,
                    oldContent = "${context.modId}:${context.oldName}",
                    newContent = "${context.modId}:${context.newName}",
                    description = "Update biome resource location"
                )
            )
        }

        return operations
    }

    override fun validate(context: RenameContext): Boolean {
        val newClassName = RenamerUtil.toClassName(context.newName)
        val packagePath = RenamerUtil.getPackagePath(context.packageName)

        val newCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/biomes/$newClassName.java")
        if (!newCommonFile.exists()) return false

        val oldClassName = RenamerUtil.toClassName(context.oldName)
        val oldCommonFile = File(context.projectDir, "shared/common/src/main/java/$packagePath/biomes/$oldClassName.java")
        if (oldCommonFile.exists()) return false

        return true
    }

    override fun getFilePatterns(context: RenameContext): List<FilePattern> {
        return listOf(
            FilePattern("${context.oldName}.java", FileType.JAVA_CLASS),
            FilePattern("${context.oldName}.json", FileType.JSON_DATA)
        )
    }
}
