# Dropper CLI Workflow Test (Windows)

Write-Host "╔═══════════════════════════════════════════════════════════════╗"
Write-Host "║           Dropper CLI Workflow Test                          ║"
Write-Host "╚═══════════════════════════════════════════════════════════════╝"
Write-Host ""
Write-Host "This test will:"
Write-Host "  1. Clean examples directory"
Write-Host "  2. Create a mod using CLI commands"
Write-Host "  3. Build for 2 MC versions (1.20.1 and 1.21.1)"
Write-Host "  4. Build all loaders (Fabric, Forge, NeoForge) for each version"
Write-Host "  5. Verify all 6 JAR files are created"
Write-Host ""
Write-Host "⚠️  Full build takes 10-15 minutes as Gradle downloads dependencies"
Write-Host "   for multiple versions and loaders"
Write-Host ""

# Lightweight test (always runs)
Write-Host "Running lightweight test (structure validation only)..."
.\gradlew :src:cli:test --tests "*.CLIWorkflowTest.lightweight*"

Write-Host ""
$response = Read-Host "Run full build test? This will take 10-15 minutes. (y/N)"

if ($response -eq 'y' -or $response -eq 'Y') {
    Write-Host ""
    Write-Host "Starting full build test..."
    $env:RUN_CLI_BUILD = "true"
    .\gradlew :src:cli:test --tests "*.CLIWorkflowTest.full*"

    Write-Host ""
    Write-Host "✓ Full CLI workflow test completed successfully!"
} else {
    Write-Host ""
    Write-Host "Skipping full build test."
    Write-Host "To run it later, use: `$env:RUN_CLI_BUILD='true'; .\gradlew :src:cli:test --tests '*.CLIWorkflowTest.full*'"
}
