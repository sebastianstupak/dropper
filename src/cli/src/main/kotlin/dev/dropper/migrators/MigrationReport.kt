package dev.dropper.migrators

/**
 * Generates comprehensive migration reports
 */
class MigrationReport {
    fun generateReport(result: MigrationResult, plan: MigrationPlan): String {
        val sb = StringBuilder()

        sb.appendLine("╔═══════════════════════════════════════════════════════════════╗")
        sb.appendLine("║               Migration Report                                ║")
        sb.appendLine("╚═══════════════════════════════════════════════════════════════╝")
        sb.appendLine()

        // Status
        sb.appendLine("Status: ${if (result.success) "✓ SUCCESS" else "✗ FAILED"}")
        sb.appendLine("Operations executed: ${result.operationsExecuted}")
        sb.appendLine()

        // Changes
        if (result.changes.isNotEmpty()) {
            sb.appendLine("Changes made:")
            result.changes.forEach { change ->
                sb.appendLine("  ✓ $change")
            }
            sb.appendLine()
        }

        // Warnings
        if (result.warnings.isNotEmpty()) {
            sb.appendLine("Warnings:")
            result.warnings.forEach { warning ->
                sb.appendLine("  ⚠ $warning")
            }
            sb.appendLine()
        }

        // Errors
        if (result.errors.isNotEmpty()) {
            sb.appendLine("Errors:")
            result.errors.forEach { error ->
                sb.appendLine("  ✗ $error")
            }
            sb.appendLine()
        }

        // Manual steps
        if (result.manualStepsRequired.isNotEmpty() || plan.requiredManualSteps.isNotEmpty()) {
            sb.appendLine("Manual steps required:")
            (result.manualStepsRequired + plan.requiredManualSteps).distinct().forEach { step ->
                sb.appendLine("  → $step")
            }
            sb.appendLine()
        }

        return sb.toString()
    }

    fun generateDryRunReport(plan: MigrationPlan): String {
        val sb = StringBuilder()

        sb.appendLine("╔═══════════════════════════════════════════════════════════════╗")
        sb.appendLine("║            Migration Plan (Dry Run)                           ║")
        sb.appendLine("╚═══════════════════════════════════════════════════════════════╝")
        sb.appendLine()

        sb.appendLine("Planned operations: ${plan.operations.size}")
        sb.appendLine()

        // Group operations by type
        val grouped = plan.operations.groupBy { it.javaClass.simpleName }
        grouped.forEach { (type, ops) ->
            sb.appendLine("$type: ${ops.size}")
            ops.take(5).forEach { op ->
                sb.appendLine("  • ${formatOperation(op)}")
            }
            if (ops.size > 5) {
                sb.appendLine("  ... and ${ops.size - 5} more")
            }
            sb.appendLine()
        }

        // Warnings
        if (plan.warnings.isNotEmpty()) {
            sb.appendLine("Warnings:")
            plan.warnings.forEach { warning ->
                sb.appendLine("  ⚠ $warning")
            }
            sb.appendLine()
        }

        // Manual steps
        if (plan.requiredManualSteps.isNotEmpty()) {
            sb.appendLine("Manual steps will be required:")
            plan.requiredManualSteps.forEach { step ->
                sb.appendLine("  → $step")
            }
            sb.appendLine()
        }

        sb.appendLine("Run without --dry-run to execute these changes")

        return sb.toString()
    }

    private fun formatOperation(op: MigrationOperation): String {
        return when (op) {
            is MigrationOperation.CreateDirectory -> "Create: ${op.path}"
            is MigrationOperation.CopyFile -> "Copy: ${op.source} → ${op.destination}"
            is MigrationOperation.MoveFile -> "Move: ${op.source} → ${op.destination}"
            is MigrationOperation.DeleteFile -> "Delete: ${op.path}"
            is MigrationOperation.UpdateFileContent -> "Update: ${op.path} - ${op.description}"
            is MigrationOperation.ReplaceInFile -> "Replace in: ${op.path} - ${op.description}"
            is MigrationOperation.UpdateConfig -> "Update config: ${op.configPath} - ${op.description}"
        }
    }
}
