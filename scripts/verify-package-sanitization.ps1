#!/usr/bin/env pwsh

Write-Host "╔══════════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  Package Name Sanitization Verification Script                  ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Create temporary test directory
$timestamp = [int][double]::Parse((Get-Date -UFormat %s))
$TEST_DIR = "build/package-sanitization-test-$timestamp"
New-Item -ItemType Directory -Force -Path $TEST_DIR | Out-Null

Write-Host "Test directory: $TEST_DIR"
Write-Host ""

# Test cases: mod_id -> expected_package_name
$TEST_CASES = @{
    "my_mod" = "mymod"
    "cool-mod" = "coolmod"
    "test_123" = "test123"
    "super-cool_mod" = "supercoolmod"
    "my-fancy_mod" = "myfancymod"
}

$PASSED = 0
$FAILED = 0

foreach ($mod_id in $TEST_CASES.Keys) {
    $expected_package = $TEST_CASES[$mod_id]

    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
    Write-Host "Testing mod ID: $mod_id" -ForegroundColor Yellow
    Write-Host "Expected package: com.$expected_package"
    Write-Host ""

    $project_dir = "$TEST_DIR/$mod_id"

    # Build the CLI
    Write-Host "  [1/5] Building CLI..." -ForegroundColor DarkGray
    & ./gradlew :src:cli:build -q 2>&1 | Out-Null

    # Generate project using dropper init
    Write-Host "  [2/5] Generating project..." -ForegroundColor DarkGray
    & ./gradlew :src:cli:run -q --args="init $project_dir --name TestMod --id $mod_id --author Test --description Test --versions 1.20.1 --loaders fabric" 2>&1 | Out-Null

    # Generate an item to test component generation
    Write-Host "  [3/5] Generating test item..." -ForegroundColor DarkGray
    Push-Location $project_dir
    & ../../gradlew :src:cli:run -q --args="item test_item" 2>&1 | Out-Null
    Pop-Location

    # Verify package structure
    Write-Host "  [4/5] Verifying package structure..." -ForegroundColor DarkGray

    $ERRORS = 0

    # Check Services.java
    $services_file = "$project_dir/shared/common/src/main/java/com/$expected_package/Services.java"
    if (-not (Test-Path $services_file)) {
        Write-Host "    ✗ Services.java not found at expected location" -ForegroundColor Red
        $ERRORS++
    } else {
        $content = Get-Content $services_file -Raw
        if ($content -notmatch "package com\.$expected_package;") {
            Write-Host "    ✗ Services.java has incorrect package declaration" -ForegroundColor Red
            $ERRORS++
        }
    }

    # Check Item class
    $item_file = "$project_dir/shared/common/src/main/java/com/$expected_package/items/TestItem.java"
    if (-not (Test-Path $item_file)) {
        Write-Host "    ✗ TestItem.java not found at expected location" -ForegroundColor Red
        $ERRORS++
    } else {
        $content = Get-Content $item_file -Raw
        if ($content -notmatch "package com\.$expected_package\.items;") {
            Write-Host "    ✗ TestItem.java has incorrect package declaration" -ForegroundColor Red
            $ERRORS++
        }
    }

    # Check Fabric registration
    $fabric_file = "$project_dir/shared/fabric/src/main/java/com/$expected_package/platform/fabric/TestItemFabric.java"
    if (-not (Test-Path $fabric_file)) {
        Write-Host "    ✗ Fabric registration not found at expected location" -ForegroundColor Red
        $ERRORS++
    } else {
        $content = Get-Content $fabric_file -Raw
        if ($content -notmatch "package com\.$expected_package\.platform\.fabric;") {
            Write-Host "    ✗ Fabric registration has incorrect package declaration" -ForegroundColor Red
            $ERRORS++
        }
        if ($content -notmatch "import com\.$expected_package\.items\.TestItem;") {
            Write-Host "    ✗ Fabric registration has incorrect import" -ForegroundColor Red
            $ERRORS++
        }
    }

    # Verify assets use original mod ID
    $model_file = "$project_dir/versions/shared/v1/assets/$mod_id/models/item/test_item.json"
    if (-not (Test-Path $model_file)) {
        Write-Host "    ✗ Item model not found (should use original mod ID '$mod_id')" -ForegroundColor Red
        $ERRORS++
    }

    Write-Host "  [5/5] Results..." -ForegroundColor DarkGray
    if ($ERRORS -eq 0) {
        Write-Host "    ✓ All checks passed for mod ID: $mod_id" -ForegroundColor Green
        $PASSED++
    } else {
        Write-Host "    ✗ $ERRORS check(s) failed for mod ID: $mod_id" -ForegroundColor Red
        $FAILED++
    }
    Write-Host ""
}

Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray
Write-Host "Summary:"
Write-Host "  Passed: $PASSED" -ForegroundColor Green
Write-Host "  Failed: $FAILED" $(if ($FAILED -gt 0) { "-ForegroundColor Red" } else { "-ForegroundColor Gray" })
Write-Host ""

# Cleanup
Write-Host "Cleaning up test directory..."
Remove-Item -Recurse -Force $TEST_DIR

if ($FAILED -eq 0) {
    Write-Host "╔══════════════════════════════════════════════════════════════════╗" -ForegroundColor Green
    Write-Host "║  ✓ ALL TESTS PASSED                                             ║" -ForegroundColor Green
    Write-Host "╚══════════════════════════════════════════════════════════════════╝" -ForegroundColor Green
    exit 0
} else {
    Write-Host "╔══════════════════════════════════════════════════════════════════╗" -ForegroundColor Red
    Write-Host "║  ✗ SOME TESTS FAILED                                            ║" -ForegroundColor Red
    Write-Host "╚══════════════════════════════════════════════════════════════════╝" -ForegroundColor Red
    exit 1
}
