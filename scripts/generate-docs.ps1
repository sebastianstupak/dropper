# Generate CLI documentation for Windows
$ErrorActionPreference = "Stop"

Write-Host "📚 Generating CLI documentation..." -ForegroundColor Green

# Build the CLI first if needed
if (-Not (Test-Path "src/cli/build/libs/dropper.jar")) {
  Write-Host "Building CLI..." -ForegroundColor Yellow
  .\gradlew.bat :src:cli:build
}

# Get absolute path to output file
$OutputPath = Join-Path $PSScriptRoot "..\src\web\public\docs.json" | Resolve-Path -ErrorAction SilentlyContinue
if (-not $OutputPath) {
  $OutputPath = Join-Path $PSScriptRoot "..\src\web\public\docs.json"
}

# Run docs command
Write-Host "Running dropper docs..." -ForegroundColor Yellow
.\gradlew.bat :src:cli:run --args="docs --output=$OutputPath"

Write-Host "[OK] Documentation generated at $OutputPath" -ForegroundColor Green
