#!/bin/bash

echo "Quick Edge Case Tests"
echo "====================="
echo ""

TEST_DIR="/d/dev/minecraft-mod-versioning-example/examples/simple-mod"

echo "Test 1: Spaces in name"
echo "Command: create item test item"
cd "$TEST_DIR" && ../../gradlew :src:cli:run --quiet --console=plain --args="create item test item" 2>&1 | head -20
echo ""

echo "Test 2: Uppercase letters"
echo "Command: create item TestItem"
cd "$TEST_DIR" && ../../gradlew :src:cli:run --quiet --console=plain --args="create item TestItem" 2>&1 | head -20
echo ""

echo "Test 3: Special characters (@)"
echo "Command: create item test@item"
cd "$TEST_DIR" && ../../gradlew :src:cli:run --quiet --console=plain --args="create item test@item" 2>&1 | head -20
echo ""

echo "Test 4: Empty argument"
echo "Command: create item"
cd "$TEST_DIR" && ../../gradlew :src:cli:run --quiet --console=plain --args="create item" 2>&1 | head -20
echo ""

echo "Test 5: Numbers only"
echo "Command: create item 12345"
cd "$TEST_DIR" && ../../gradlew :src:cli:run --quiet --console=plain --args="create item 12345" 2>&1 | head -20
echo ""

echo "Test 6: Starting with underscore"
echo "Command: create item _test_item"
cd "$TEST_DIR" && ../../gradlew :src:cli:run --quiet --console=plain --args="create item _test_item" 2>&1 | head -20
echo ""

echo "Test 7: Hyphens"
echo "Command: create item test-item"
cd "$TEST_DIR" && ../../gradlew :src:cli:run --quiet --console=plain --args="create item test-item" 2>&1 | head -20
echo ""

echo "Test 8: Dots in name"
echo "Command: create item test.item"
cd "$TEST_DIR" && ../../gradlew :src:cli:run --quiet --console=plain --args="create item test.item" 2>&1 | head -20
echo ""

echo "Test 9: Valid underscore name (should succeed)"
TIMESTAMP=$(date +%s)
echo "Command: create item valid_test_$TIMESTAMP"
cd "$TEST_DIR" && ../../gradlew :src:cli:run --quiet --console=plain --args="create item valid_test_$TIMESTAMP" 2>&1 | tail -5
echo ""

echo "Test 10: Duplicate item"
echo "Command: create item duplicate_test (first time)"
cd "$TEST_DIR" && ../../gradlew :src:cli:run --quiet --console=plain --args="create item duplicate_edge_test" 2>&1 | tail -3
echo "Command: create item duplicate_test (second time - should fail)"
cd "$TEST_DIR" && ../../gradlew :src:cli:run --quiet --console=plain --args="create item duplicate_edge_test" 2>&1 | head -20
echo ""
