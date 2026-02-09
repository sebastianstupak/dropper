#!/bin/bash

echo "Quick Edge Case Tests"
echo "====================="
echo ""

cd /d/dev/minecraft-mod-versioning-example/examples/simple-mod

echo "Test 1: Spaces in name"
/d/dev/minecraft-mod-versioning-example/gradlew :src:cli:run --quiet --args="create item test item" 2>&1 | head -20
echo ""

echo "Test 2: Uppercase letters"
/d/dev/minecraft-mod-versioning-example/gradlew :src:cli:run --quiet --args="create item TestItem" 2>&1 | head -20
echo ""

echo "Test 3: Special characters"
/d/dev/minecraft-mod-versioning-example/gradlew :src:cli:run --quiet --args="create item test@item" 2>&1 | head -20
echo ""

echo "Test 4: Empty argument"
/d/dev/minecraft-mod-versioning-example/gradlew :src:cli:run --quiet --args="create item" 2>&1 | head -20
echo ""

echo "Test 5: Valid underscore name (should succeed)"
/d/dev/minecraft-mod-versioning-example/gradlew :src:cli:run --quiet --args="create item valid_test_item_$(date +%s)" 2>&1 | head -20
echo ""

echo "Test 6: Numbers only"
/d/dev/minecraft-mod-versioning-example/gradlew :src:cli:run --quiet --args="create item 12345" 2>&1 | head -20
echo ""

echo "Test 7: Starting with underscore"
/d/dev/minecraft-mod-versioning-example/gradlew :src:cli:run --quiet --args="create item _test_item" 2>&1 | head -20
echo ""

echo "Test 8: Hyphens"
/d/dev/minecraft-mod-versioning-example/gradlew :src:cli:run --quiet --args="create item test-item" 2>&1 | head -20
echo ""
