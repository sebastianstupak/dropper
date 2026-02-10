package dev.dropper.renamers

import java.io.File

/**
 * Renamer for Java package refactoring.
 * Moves files to new package directories and updates
 * package declarations and import statements.
 */
class PackageRenamer : ComponentRenamer {

    override fun discover(context: RenameContext): List<File> {
        val files = mutableListOf<File>()
        val oldPackagePath = context.oldName.replace(".", "/")

        // Find all Java files under the old package path in all source roots
        getSourceRoots(context.projectDir).forEach { root ->
            val oldDir = File(root, oldPackagePath)
            if (oldDir.exists() && oldDir.isDirectory) {
                oldDir.walkTopDown().filter { it.isFile }.forEach { files.add(it) }
            }
        }

        // Also find all Java files that import from the old package
        val javaFiles = RenamerUtil.findFilesContaining(
            context.projectDir,
            "import ${context.oldName}",
            listOf(".java", ".kt")
        )
        files.addAll(javaFiles)

        return files.distinct()
    }

    override fun findReferences(context: RenameContext, discoveredFiles: List<File>): Map<File, List<String>> {
        val references = mutableMapOf<File, MutableList<String>>()

        discoveredFiles.forEach { file ->
            if (!file.isFile) return@forEach
            try {
                val content = file.readText()
                val refs = mutableListOf<String>()

                if (content.contains("package ${context.oldName}")) {
                    refs.add("Package declaration: ${context.oldName}")
                }
                if (content.contains("import ${context.oldName}")) {
                    refs.add("Import statement: ${context.oldName}")
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
        val newPackagePath = context.newName.replace(".", "/")

        getSourceRoots(context.projectDir).forEach { root ->
            val newDir = File(root, newPackagePath)
            if (newDir.exists() && newDir.listFiles()?.isNotEmpty() == true) {
                conflicts.add("Target package directory already exists and is non-empty: ${newDir.relativeTo(context.projectDir)}")
            }
        }

        return conflicts
    }

    override fun planRename(context: RenameContext): List<RenameOperation> {
        val operations = mutableListOf<RenameOperation>()
        val oldPackage = context.oldName
        val newPackage = context.newName
        val oldPackagePath = oldPackage.replace(".", "/")
        val newPackagePath = newPackage.replace(".", "/")

        // Collect all files that will be moved
        val fileMoves = mutableListOf<Pair<File, File>>()

        // 1. Plan directory renames for each source root
        getSourceRoots(context.projectDir).forEach { root ->
            val oldDir = File(root, oldPackagePath)
            if (oldDir.exists() && oldDir.isDirectory) {
                val newDir = File(root, newPackagePath)

                // Track individual files for content updates
                oldDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relativePath = file.relativeTo(oldDir).path
                    val newFile = File(newDir, relativePath)
                    fileMoves.add(Pair(file, newFile))
                }

                // Rename the directory
                operations.add(RenameOperation.FileRename(oldDir, newDir))
            }
        }

        // 2. Update package declarations in moved files
        fileMoves.forEach { (_, newFile) ->
            if (newFile.extension == "java" || newFile.extension == "kt") {
                operations.add(
                    RenameOperation.ContentReplace(
                        file = newFile,
                        oldContent = "package $oldPackage",
                        newContent = "package $newPackage",
                        description = "Update package declaration in ${newFile.name}"
                    )
                )
            }
        }

        // 3. Update import statements in moved files
        fileMoves.forEach { (_, newFile) ->
            if (newFile.extension == "java" || newFile.extension == "kt") {
                operations.add(
                    RenameOperation.ContentReplace(
                        file = newFile,
                        oldContent = "import $oldPackage",
                        newContent = "import $newPackage",
                        description = "Update import statements in ${newFile.name}"
                    )
                )
            }
        }

        // 4. Update import statements in ALL other Java files that reference the old package
        //    These are files that were NOT moved (they live outside the old package)
        val movedFilePaths = fileMoves.map { it.first.absolutePath }.toSet()
        val allJavaFiles = findAllJavaFiles(context.projectDir)

        allJavaFiles.forEach { file ->
            if (file.absolutePath in movedFilePaths) return@forEach
            try {
                val content = file.readText()
                if (content.contains("import $oldPackage")) {
                    operations.add(
                        RenameOperation.ContentReplace(
                            file = file,
                            oldContent = "import $oldPackage",
                            newContent = "import $newPackage",
                            description = "Update import statements in ${file.name}"
                        )
                    )
                }
            } catch (_: Exception) {
                // Skip unreadable files
            }
        }

        return operations
    }

    override fun validate(context: RenameContext): Boolean {
        val newPackagePath = context.newName.replace(".", "/")
        val oldPackagePath = context.oldName.replace(".", "/")

        // Check that at least one new package directory exists
        var newExists = false
        var oldExists = false

        getSourceRoots(context.projectDir).forEach { root ->
            val newDir = File(root, newPackagePath)
            if (newDir.exists()) newExists = true
            val oldDir = File(root, oldPackagePath)
            if (oldDir.exists()) oldExists = true
        }

        return newExists && !oldExists
    }

    override fun getFilePatterns(context: RenameContext): List<FilePattern> {
        return listOf(
            FilePattern("*.java", FileType.JAVA_CLASS),
            FilePattern("*.kt", FileType.JAVA_CLASS)
        )
    }

    /**
     * Get all Java/Kotlin source root directories in the project.
     */
    private fun getSourceRoots(projectDir: File): List<File> {
        val roots = mutableListOf<File>()
        val candidates = listOf(
            "shared/common/src/main/java",
            "shared/common/src/test/java",
            "shared/fabric/src/main/java",
            "shared/forge/src/main/java",
            "shared/neoforge/src/main/java"
        )

        candidates.forEach { path ->
            val dir = File(projectDir, path)
            if (dir.exists()) roots.add(dir)
        }

        return roots
    }

    /**
     * Find all Java/Kotlin files in the project.
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
}
