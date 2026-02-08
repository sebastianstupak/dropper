#!/bin/bash
set -e

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║           Dropper Full Workflow Test                         ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "This test will:"
echo "  1. Generate a test project using Dropper"
echo "  2. Build the project with Gradle"
echo "  3. Verify JAR files are created"
echo ""
echo "⚠️  This test takes 5-10 minutes as Gradle downloads dependencies"
echo ""

# Run the full workflow test
export RUN_FULL_BUILD=true
./gradlew :src:cli:test --tests "*.FullWorkflowTest.full workflow*"

echo ""
echo "✓ Full workflow test completed successfully!"
