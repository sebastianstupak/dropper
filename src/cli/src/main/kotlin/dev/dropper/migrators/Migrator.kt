package dev.dropper.migrators

import java.io.File

/**
 * Base interface for all migrators
 */
interface Migrator {
    /**
     * Plan the migration and return operations to perform
     */
    fun planMigration(context: MigrationContext): MigrationPlan

    /**
     * Execute the migration
     */
    fun executeMigration(plan: MigrationPlan, dryRun: Boolean = false): MigrationResult
}

/**
 * Migration context containing project information and migration parameters
 */
data class MigrationContext(
    val projectDir: File,
    val modId: String,
    val packageName: String,
    val force: Boolean = false,
    val autoFix: Boolean = false,
    val dryRun: Boolean = false,
    val params: Map<String, String> = emptyMap()
)

/**
 * Migration plan containing all operations to be performed
 */
data class MigrationPlan(
    val operations: List<MigrationOperation>,
    val warnings: List<String> = emptyList(),
    val requiredManualSteps: List<String> = emptyList()
)

/**
 * Single migration operation
 */
sealed class MigrationOperation {
    data class CreateDirectory(val path: String) : MigrationOperation()
    data class CopyFile(val source: String, val destination: String) : MigrationOperation()
    data class MoveFile(val source: String, val destination: String) : MigrationOperation()
    data class DeleteFile(val path: String) : MigrationOperation()
    data class UpdateFileContent(
        val path: String,
        val oldContent: String,
        val newContent: String,
        val description: String
    ) : MigrationOperation()
    data class ReplaceInFile(
        val path: String,
        val replacements: List<Pair<String, String>>,
        val description: String
    ) : MigrationOperation()
    data class UpdateConfig(
        val configPath: String,
        val updates: Map<String, Any>,
        val description: String
    ) : MigrationOperation()
}

/**
 * Result of migration execution
 */
data class MigrationResult(
    val success: Boolean,
    val operationsExecuted: Int,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val changes: List<String> = emptyList(),
    val manualStepsRequired: List<String> = emptyList()
)
