package dev.dropper.renamers

import dev.dropper.util.FileUtil
import java.io.File

/**
 * Renamer for the entire mod (mod ID change).
 * Handles config.yml, resource directories, JSON references,
 * Java references, and package directory renaming.
 */
class ModRenamer : ComponentRenamer {

    override fun discover(context: RenameContext): List<File> {
        val files = mutableListOf<File>()

        // config.yml
        val configFile = File(context.projectDir, "config.yml")
        if (configFile.exists()) files.add(configFile)

        // Asset directories: assets/{modId}/
        findDirectoriesNamed(context.projectDir, context.oldName).forEach { dir ->
            dir.walkTopDown().filter { it.isFile }.forEach { files.add(it) }
        }

        // Java files referencing the old mod ID
        val javaFiles = RenamerUtil.findFilesContaining(
            context.projectDir, context.oldName, listOf(".java", ".kt")
        )
        files.addAll(javaFiles)

        // JSON files referencing the old mod ID
        val jsonFiles = RenamerUtil.findFilesContaining(
            context.projectDir, context.oldName, listOf(".json")
        )
        files.addAll(jsonFiles)

        return files.distinct()
    }

    override fun findReferences(context: RenameContext, discoveredFiles: List<File>): Map<File, List<String>> {
        val references = mutableMapOf<File, MutableList<String>>()

        discoveredFiles.forEach { file ->
            if (!file.isFile) return@forEach
            try {
                val content = file.readText()
                val refs = mutableListOf<String>()

                if (content.contains("\"${context.oldName}\"")) {
                    refs.add("Mod ID string literal: ${context.oldName}")
                }
                if (content.contains("${context.oldName}:")) {
                    refs.add("Resource location namespace: ${context.oldName}")
                }
                if (refs.isNotEmpty()) {
                    references[file] = refs
                }
            } catch (_: Exception) {
                // Skip unreadable files
            }
        }

        return references
    }

    override fun checkConflicts(context: RenameContext): List<String> {
        val conflicts = mutableListOf<String>()

        // Check if new mod ID directories already exist
        val versionsDir = File(context.projectDir, "versions")
        if (versionsDir.exists()) {
            versionsDir.walkTopDown().forEach { file ->
                if (file.isDirectory && file.name == context.newName &&
                    (file.parentFile?.name == "assets" || file.parentFile?.name == "data")
                ) {
                    conflicts.add("Directory already exists: ${file.relativeTo(context.projectDir)}")
                }
            }
        }

        return conflicts
    }

    override fun planRename(context: RenameContext): List<RenameOperation> {
        val operations = mutableListOf<RenameOperation>()
        val oldId = context.oldName
        val newId = context.newName
        val oldSanitized = sanitize(oldId)
        val newSanitized = sanitize(newId)
        val oldPackage = "com.$oldSanitized"
        val newPackage = "com.$newSanitized"
        val oldPackagePath = oldPackage.replace(".", "/")
        val newPackagePath = newPackage.replace(".", "/")

        // 1. Update config.yml
        val configFile = File(context.projectDir, "config.yml")
        if (configFile.exists()) {
            val content = configFile.readText()
            if (content.contains("id: $oldId") || content.contains("id: \"$oldId\"")) {
                operations.add(
                    RenameOperation.ContentReplace(
                        file = configFile,
                        oldContent = "id: $oldId",
                        newContent = "id: $newId",
                        description = "Update mod ID in config.yml"
                    )
                )
            }
        }

        // 2. Rename resource directories: assets/{oldId}/ -> assets/{newId}/
        //    and data/{oldId}/ -> data/{newId}/
        val assetDirs = findAssetAndDataDirs(context.projectDir, oldId)
        assetDirs.forEach { dir ->
            val newDir = File(dir.parentFile, newId)
            operations.add(RenameOperation.FileRename(dir, newDir))
        }

        // 3. Update all JSON files that reference the old mod ID
        //    (models, recipes, loot tables, tags, lang files)
        //    After directory renames, files are in new locations
        val jsonFiles = findAllJsonFiles(context.projectDir)
        jsonFiles.forEach { file ->
            try {
                val content = file.readText()
                // Determine the file path after potential directory renames
                val effectiveFile = resolveFileAfterRenames(file, oldId, newId, assetDirs)
                if (content.contains("$oldId:")) {
                    operations.add(
                        RenameOperation.ContentReplace(
                            file = effectiveFile,
                            oldContent = "$oldId:",
                            newContent = "$newId:",
                            description = "Update resource namespace in ${file.name}"
                        )
                    )
                }
            } catch (_: Exception) {
                // Skip unreadable files
            }
        }

        // 4. Update all Java files that reference the old mod ID
        val javaFiles = findAllJavaFiles(context.projectDir)
        javaFiles.forEach { file ->
            try {
                val content = file.readText()
                val effectiveFile = resolveJavaFileAfterPackageRename(
                    file, oldPackagePath, newPackagePath, context.projectDir
                )

                // Update mod ID string literals (e.g., "oldmod" in MOD_ID = "oldmod")
                if (content.contains("\"$oldId\"")) {
                    operations.add(
                        RenameOperation.ContentReplace(
                            file = effectiveFile,
                            oldContent = "\"$oldId\"",
                            newContent = "\"$newId\"",
                            description = "Update mod ID string literal in ${file.name}"
                        )
                    )
                }
            } catch (_: Exception) {
                // Skip unreadable files
            }
        }

        // 5. Rename package directories if the sanitized mod ID changes
        if (oldSanitized != newSanitized) {
            // Collect package rename operations and Java content update operations
            val packageOps = planPackageRename(
                context.projectDir, oldPackage, newPackage
            )
            operations.addAll(packageOps)
        }

        return operations
    }

    override fun validate(context: RenameContext): Boolean {
        val configFile = File(context.projectDir, "config.yml")
        if (!configFile.exists()) return false

        val content = configFile.readText()
        // New ID should be present, old ID should not be (in the id: field)
        if (!content.contains("id: ${context.newName}")) return false
        if (content.contains("id: ${context.oldName}")) return false

        return true
    }

    override fun getFilePatterns(context: RenameContext): List<FilePattern> {
        return listOf(
            FilePattern("config.yml", FileType.JSON_ASSET),
            FilePattern("*.java", FileType.JAVA_CLASS),
            FilePattern("*.json", FileType.JSON_ASSET)
        )
    }

    /**
     * Plan operations for renaming package directories and updating content.
     */
    private fun planPackageRename(
        projectDir: File,
        oldPackage: String,
        newPackage: String
    ): List<RenameOperation> {
        val operations = mutableListOf<RenameOperation>()
        val oldPackagePath = oldPackage.replace(".", "/")
        val newPackagePath = newPackage.replace(".", "/")

        // Find all Java source roots that contain the old package
        val sourceRoots = listOf(
            "shared/common/src/main/java",
            "shared/common/src/test/java",
            "shared/fabric/src/main/java",
            "shared/forge/src/main/java",
            "shared/neoforge/src/main/java"
        )

        // First, collect all Java files that need content updates (before moving)
        val javaFileMoves = mutableListOf<Pair<File, File>>()

        sourceRoots.forEach { root ->
            val oldDir = File(projectDir, "$root/$oldPackagePath")
            if (oldDir.exists() && oldDir.isDirectory) {
                val newDir = File(projectDir, "$root/$newPackagePath")

                // Walk all files in the old directory tree
                oldDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relativePath = file.relativeTo(oldDir).path
                    val newFile = File(newDir, relativePath)
                    javaFileMoves.add(Pair(file, newFile))
                }
            }
        }

        // Plan file moves (move from old package dir to new package dir)
        // We need to rename directories, not individual files
        sourceRoots.forEach { root ->
            val oldDir = File(projectDir, "$root/$oldPackagePath")
            if (oldDir.exists() && oldDir.isDirectory) {
                val newDir = File(projectDir, "$root/$newPackagePath")
                operations.add(RenameOperation.FileRename(oldDir, newDir))
            }
        }

        // Plan content updates for moved Java files
        javaFileMoves.forEach { (_, newFile) ->
            // Update package declaration
            operations.add(
                RenameOperation.ContentReplace(
                    file = newFile,
                    oldContent = "package $oldPackage",
                    newContent = "package $newPackage",
                    description = "Update package declaration in ${newFile.name}"
                )
            )
        }

        // Update import statements in all moved Java files
        javaFileMoves.forEach { (_, newFile) ->
            operations.add(
                RenameOperation.ContentReplace(
                    file = newFile,
                    oldContent = "import $oldPackage",
                    newContent = "import $newPackage",
                    description = "Update import statements in ${newFile.name}"
                )
            )
        }

        return operations
    }

    /**
     * Find all directories named [name] under assets/ or data/ within the project.
     */
    private fun findAssetAndDataDirs(projectDir: File, name: String): List<File> {
        val dirs = mutableListOf<File>()
        projectDir.walkTopDown().forEach { file ->
            if (file.isDirectory && file.name == name &&
                (file.parentFile?.name == "assets" || file.parentFile?.name == "data")
            ) {
                dirs.add(file)
            }
        }
        return dirs
    }

    /**
     * Find directories with a specific name anywhere under the project dir.
     */
    private fun findDirectoriesNamed(projectDir: File, name: String): List<File> {
        val dirs = mutableListOf<File>()
        projectDir.walkTopDown().forEach { file ->
            if (file.isDirectory && file.name == name) {
                dirs.add(file)
            }
        }
        return dirs
    }

    /**
     * Find all JSON files in the project.
     */
    private fun findAllJsonFiles(projectDir: File): List<File> {
        val files = mutableListOf<File>()
        projectDir.walkTopDown().forEach { file ->
            if (file.isFile && file.extension == "json") {
                files.add(file)
            }
        }
        return files
    }

    /**
     * Find all Java files in the project.
     */
    private fun findAllJavaFiles(projectDir: File): List<File> {
        val files = mutableListOf<File>()
        projectDir.walkTopDown().forEach { file ->
            if (file.isFile && (file.extension == "java" || file.extension == "kt")) {
                files.add(file)
            }
        }
        return files
    }

    /**
     * Given a file that currently lives under an old asset/data directory,
     * resolve its path after the directory has been renamed.
     */
    private fun resolveFileAfterRenames(
        file: File,
        oldId: String,
        newId: String,
        renamedDirs: List<File>
    ): File {
        for (dir in renamedDirs) {
            val dirPath = dir.absolutePath
            val filePath = file.absolutePath
            if (filePath.startsWith(dirPath + File.separator) || filePath == dirPath) {
                val relative = filePath.removePrefix(dirPath)
                val newDir = File(dir.parentFile, newId)
                return File(newDir.absolutePath + relative)
            }
        }
        return file
    }

    /**
     * Resolve where a Java file will be after a package rename.
     */
    private fun resolveJavaFileAfterPackageRename(
        file: File,
        oldPackagePath: String,
        newPackagePath: String,
        projectDir: File
    ): File {
        val filePath = file.absolutePath
        val projectPath = projectDir.absolutePath
        if (filePath.startsWith(projectPath)) {
            val relative = filePath.removePrefix(projectPath + File.separator)
                .replace("\\", "/")
            if (relative.contains(oldPackagePath)) {
                val newRelative = relative.replace(oldPackagePath, newPackagePath)
                return File(projectDir, newRelative)
            }
        }
        return file
    }

    /**
     * Sanitize a mod ID for use in Java package names.
     * Removes hyphens and underscores to match ModConfig.basePackage behavior.
     */
    private fun sanitize(modId: String): String {
        return FileUtil.sanitizeModId(modId)
    }
}
