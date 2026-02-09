#!/bin/bash

# Edge Case Testing Script for Dropper CLI
# Tests error handling and validation for all generation commands

PROJECT_DIR="/d/dev/minecraft-mod-versioning-example/examples/simple-mod"
DROPPER_CMD="./gradlew :src:cli:run --quiet --console=plain --args"
LOG_FILE="/d/dev/minecraft-mod-versioning-example/edge-case-test-results.md"

# Initialize log file
echo "# Edge Case Testing Results" > "$LOG_FILE"
echo "" >> "$LOG_FILE"
echo "Test Date: $(date)" >> "$LOG_FILE"
echo "Project: Dropper CLI" >> "$LOG_FILE"
echo "Test Directory: $PROJECT_DIR" >> "$LOG_FILE"
echo "" >> "$LOG_FILE"
echo "---" >> "$LOG_FILE"
echo "" >> "$LOG_FILE"

echo "======================================"
echo "Dropper CLI Edge Case Testing"
echo "======================================"
echo ""
echo "Results will be logged to: $LOG_FILE"
echo ""

# Test counter
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0

# Function to run test and log results
run_test() {
    local category="$1"
    local test_name="$2"
    local command="$3"
    local expected="$4"
    
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
    
    echo "Test $TOTAL_TESTS: $test_name"
    
    # Run command and capture output
    cd "$PROJECT_DIR"
    OUTPUT=$(eval "$DROPPER_CMD \"$command\"" 2>&1)
    EXIT_CODE=$?
    
    # Determine if test passed
    if [ "$expected" = "should_fail" ]; then
        if [ $EXIT_CODE -ne 0 ]; then
            echo "  PASS - Command correctly failed"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            RESULT="PASS"
        else
            echo "  FAIL - Command should have failed but succeeded"
            FAILED_TESTS=$((FAILED_TESTS + 1))
            RESULT="FAIL"
        fi
    else
        if [ $EXIT_CODE -eq 0 ]; then
            echo "  PASS - Command succeeded"
            PASSED_TESTS=$((PASSED_TESTS + 1))
            RESULT="PASS"
        else
            echo "  FAIL - Command should have succeeded but failed"
            FAILED_TESTS=$((FAILED_TESTS + 1))
            RESULT="FAIL"
        fi
    fi
    
    # Log to file
    echo "## $category - Test $TOTAL_TESTS" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
    echo "**Test:** $test_name" >> "$LOG_FILE"
    echo "**Command:** \`$command\`" >> "$LOG_FILE"
    echo "**Expected:** $expected" >> "$LOG_FILE"
    echo "**Exit Code:** $EXIT_CODE" >> "$LOG_FILE"
    echo "**Result:** $RESULT" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
    echo "**Output:**" >> "$LOG_FILE"
    echo '```' >> "$LOG_FILE"
    echo "$OUTPUT" >> "$LOG_FILE"
    echo '```' >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
    echo "---" >> "$LOG_FILE"
    echo "" >> "$LOG_FILE"
    
    echo ""
}

echo "=== SECTION 1: Invalid Names ==="
echo ""

run_test "Invalid Names" "Mod ID with spaces" "create item test item" "should_fail"
run_test "Invalid Names" "Mod ID with uppercase" "create item TestItem" "should_fail"
run_test "Invalid Names" "Special characters" "create item test@item" "should_fail"
run_test "Invalid Names" "Dots in name" "create item test.item" "should_fail"
run_test "Invalid Names" "Slashes in name" "create item test/item" "should_fail"
run_test "Invalid Names" "Starting with number" "create item 123test" "should_fail"
run_test "Invalid Names" "Empty string" "create item" "should_fail"

echo "=== SECTION 2: Very Long Names ==="
echo ""

LONG_NAME_65=$(printf 'a%.0s' {1..65})
LONG_NAME_100=$(printf 'b%.0s' {1..100})

run_test "Length Validation" "65 characters" "create item $LONG_NAME_65" "should_fail"
run_test "Length Validation" "100 characters" "create item $LONG_NAME_100" "should_fail"

echo "=== SECTION 3: Reserved Keywords ==="
echo ""

run_test "Reserved Keywords" "Java keyword: class" "create item class" "should_fail"
run_test "Reserved Keywords" "Java keyword: public" "create item public" "should_fail"
run_test "Reserved Keywords" "Java keyword: void" "create item void" "should_fail"
run_test "Reserved Keywords" "Reserved: minecraft" "create item minecraft" "should_fail"

echo "=== SECTION 4: Numbers Only ==="
echo ""

run_test "Numbers Only" "Only numbers" "create item 12345" "should_fail"
run_test "Numbers Only" "Valid with numbers" "create item test_item_123" "should_pass"

echo "=== SECTION 5: Duplicate Names ==="
echo ""

run_test "Duplicates" "Create initial item" "create item duplicate_test_item" "should_pass"
run_test "Duplicates" "Create same item again" "create item duplicate_test_item" "should_fail"

echo "=== SECTION 6: Special Characters ==="
echo ""

run_test "Special Chars" "Hyphens (invalid)" "create item test-item" "should_fail"
run_test "Special Chars" "Underscores (valid)" "create item valid_underscore_item" "should_pass"
run_test "Special Chars" "Multiple underscores" "create item test___item" "should_pass"
run_test "Special Chars" "Starting underscore" "create item _test" "should_fail"
run_test "Special Chars" "Ending underscore" "create item test_" "should_fail"

echo ""
echo "======================================"
echo "Test Summary"
echo "======================================"
echo "Total Tests: $TOTAL_TESTS"
echo "Passed: $PASSED_TESTS"
echo "Failed: $FAILED_TESTS"
echo ""
echo "Full results saved to: $LOG_FILE"
echo ""

if [ $FAILED_TESTS -eq 0 ]; then
    echo "All tests passed!"
    exit 0
else
    echo "Some tests failed. Please review the log."
    exit 1
fi
