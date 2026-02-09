#!/bin/bash
set -e

echo "╔══════════════════════════════════════════════════════════════════╗"
echo "║  Package Name Sanitization Verification Script                  ║"
echo "╚══════════════════════════════════════════════════════════════════╝"
echo ""

# Create temporary test directory
TEST_DIR="build/package-sanitization-test-$(date +%s)"
mkdir -p "$TEST_DIR"

echo "Test directory: $TEST_DIR"
echo ""

# Test cases: mod_id -> expected_package_name
declare -A TEST_CASES=(
    ["my_mod"]="mymod"
    ["cool-mod"]="coolmod"
    ["test_123"]="test123"
    ["super-cool_mod"]="supercoolmod"
    ["my-fancy_mod"]="myfancymod"
)

PASSED=0
FAILED=0

for mod_id in "${!TEST_CASES[@]}"; do
    expected_package="${TEST_CASES[$mod_id]}"

    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Testing mod ID: $mod_id"
    echo "Expected package: com.$expected_package"
    echo ""

    project_dir="$TEST_DIR/$mod_id"

    # Build the CLI
    echo "  [1/5] Building CLI..."
    ./gradlew :src:cli:build -q > /dev/null 2>&1

    # Generate project using dropper init
    echo "  [2/5] Generating project..."
    ./gradlew :src:cli:run -q --args="init $project_dir --name TestMod --id $mod_id --author Test --description Test --versions 1.20.1 --loaders fabric" > /dev/null 2>&1

    # Generate an item to test component generation
    echo "  [3/5] Generating test item..."
    cd "$project_dir"
    ../../gradlew :src:cli:run -q --args="item test_item" > /dev/null 2>&1
    cd - > /dev/null

    # Verify package structure
    echo "  [4/5] Verifying package structure..."

    ERRORS=0

    # Check Services.java
    services_file="$project_dir/shared/common/src/main/java/com/$expected_package/Services.java"
    if [ ! -f "$services_file" ]; then
        echo "    ✗ Services.java not found at expected location"
        ERRORS=$((ERRORS + 1))
    else
        if ! grep -q "package com.$expected_package;" "$services_file"; then
            echo "    ✗ Services.java has incorrect package declaration"
            ERRORS=$((ERRORS + 1))
        fi
    fi

    # Check Item class
    item_file="$project_dir/shared/common/src/main/java/com/$expected_package/items/TestItem.java"
    if [ ! -f "$item_file" ]; then
        echo "    ✗ TestItem.java not found at expected location"
        ERRORS=$((ERRORS + 1))
    else
        if ! grep -q "package com.$expected_package.items;" "$item_file"; then
            echo "    ✗ TestItem.java has incorrect package declaration"
            ERRORS=$((ERRORS + 1))
        fi
    fi

    # Check Fabric registration
    fabric_file="$project_dir/shared/fabric/src/main/java/com/$expected_package/platform/fabric/TestItemFabric.java"
    if [ ! -f "$fabric_file" ]; then
        echo "    ✗ Fabric registration not found at expected location"
        ERRORS=$((ERRORS + 1))
    else
        if ! grep -q "package com.$expected_package.platform.fabric;" "$fabric_file"; then
            echo "    ✗ Fabric registration has incorrect package declaration"
            ERRORS=$((ERRORS + 1))
        fi
        if ! grep -q "import com.$expected_package.items.TestItem;" "$fabric_file"; then
            echo "    ✗ Fabric registration has incorrect import"
            ERRORS=$((ERRORS + 1))
        fi
    fi

    # Verify assets use original mod ID
    model_file="$project_dir/versions/shared/v1/assets/$mod_id/models/item/test_item.json"
    if [ ! -f "$model_file" ]; then
        echo "    ✗ Item model not found (should use original mod ID '$mod_id')"
        ERRORS=$((ERRORS + 1))
    fi

    echo "  [5/5] Results..."
    if [ $ERRORS -eq 0 ]; then
        echo "    ✓ All checks passed for mod ID: $mod_id"
        PASSED=$((PASSED + 1))
    else
        echo "    ✗ $ERRORS check(s) failed for mod ID: $mod_id"
        FAILED=$((FAILED + 1))
    fi
    echo ""
done

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Summary:"
echo "  Passed: $PASSED"
echo "  Failed: $FAILED"
echo ""

# Cleanup
echo "Cleaning up test directory..."
rm -rf "$TEST_DIR"

if [ $FAILED -eq 0 ]; then
    echo "╔══════════════════════════════════════════════════════════════════╗"
    echo "║  ✓ ALL TESTS PASSED                                             ║"
    echo "╚══════════════════════════════════════════════════════════════════╝"
    exit 0
else
    echo "╔══════════════════════════════════════════════════════════════════╗"
    echo "║  ✗ SOME TESTS FAILED                                            ║"
    echo "╚══════════════════════════════════════════════════════════════════╝"
    exit 1
fi
