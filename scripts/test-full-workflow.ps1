# Dropper Full Workflow Test (Windows)

Write-Host "╔═══════════════════════════════════════════════════════════════╗"
Write-Host "║           Dropper Full Workflow Test                         ║"
Write-Host "╚═══════════════════════════════════════════════════════════════╝"
Write-Host ""
Write-Host "This test will:"
Write-Host "  1. Generate a test project using Dropper"
Write-Host "  2. Build the project with Gradle"
Write-Host "  3. Verify JAR files are created"
Write-Host ""
Write-Host "⚠️  This test takes 5-10 minutes as Gradle downloads dependencies"
Write-Host ""

# Run the full workflow test
$env:RUN_FULL_BUILD = "true"
.\gradlew :src:cli:test --tests "*.FullWorkflowTest.full workflow*"

Write-Host ""
Write-Host "✓ Full workflow test completed successfully!"
