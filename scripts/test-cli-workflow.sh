#!/bin/bash
set -e

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║           Dropper CLI Workflow Test                          ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""
echo "This test will:"
echo "  1. Clean examples directory"
echo "  2. Create a mod using CLI commands"
echo "  3. Build for 2 MC versions (1.20.1 and 1.21.1)"
echo "  4. Build all loaders (Fabric, Forge, NeoForge) for each version"
echo "  5. Verify all 6 JAR files are created"
echo ""
echo "⚠️  Full build takes 10-15 minutes as Gradle downloads dependencies"
echo "   for multiple versions and loaders"
echo ""

# Lightweight test (always runs)
echo "Running lightweight test (structure validation only)..."
./gradlew :src:cli:test --tests "*.CLIWorkflowTest.lightweight*"

echo ""
read -p "Run full build test? This will take 10-15 minutes. (y/N) " -n 1 -r
echo ""

if [[ $REPLY =~ ^[Yy]$ ]]
then
    echo ""
    echo "Starting full build test..."
    export RUN_CLI_BUILD=true
    ./gradlew :src:cli:test --tests "*.CLIWorkflowTest.full*"

    echo ""
    echo "✓ Full CLI workflow test completed successfully!"
else
    echo ""
    echo "Skipping full build test."
    echo "To run it later, use: RUN_CLI_BUILD=true ./gradlew :src:cli:test --tests '*.CLIWorkflowTest.full*'"
fi
