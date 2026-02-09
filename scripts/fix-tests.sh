#!/bin/bash

# Script to automatically refactor tests to use TestProjectContext
# This fixes Windows compatibility issues by not modifying user.dir

set -e

echo "Fixing test files to use TestProjectContext..."

# Find all test files that use System.setProperty("user.dir"
test_files=$(find src/cli/src/test/kotlin/dev/dropper -name "*Test.kt" -exec grep -l "System.setProperty.*user.dir" {} \;)

count=0
for file in $test_files; do
    echo "Processing: $file"

    # Check if already fixed
    if grep -q "TestProjectContext" "$file"; then
        echo "  ✓ Already uses TestProjectContext, skipping"
        continue
    fi

    # Backup
    cp "$file" "$file.bak"

    # Replace imports - add TestProjectContext import after existing imports
    if ! grep -q "import dev.dropper.util.TestProjectContext" "$file"; then
        sed -i '/^import/a import dev.dropper.util.TestProjectContext' "$file"
    fi

    # Replace field declarations
    sed -i 's/private lateinit var testProjectDir: File/private lateinit var context: TestProjectContext/' "$file"
    sed -i 's/private val originalUserDir = System.getProperty("user.dir")//' "$file"

    # Replace setup methods
    sed -i 's/testProjectDir = File("build\/.*")/context = TestProjectContext.create("test-project")/' "$file"
    sed -i 's/testProjectDir\.mkdirs()//' "$file"
    sed -i 's/System\.setProperty("user\.dir", testProjectDir\.absolutePath)//' "$file"

    # Replace cleanup methods
    sed -i 's/System\.setProperty("user\.dir", originalUserDir)//' "$file"
    sed -i 's/testProjectDir\.deleteRecursively()/context.cleanup()/' "$file"

    # Replace File(testProjectDir, with context.file(
    sed -i 's/File(testProjectDir, \(.*\))/context.file(\1)/' "$file"

    count=$((count + 1))
done

echo ""
echo "✅ Fixed $count test files"
echo ""
echo "Next steps:"
echo "1. Review changes: git diff"
echo "2. Run tests: ./gradlew :src:cli:test"
echo "3. Commit if successful: git add -A && git commit -m 'test: refactor tests to use TestProjectContext'"
