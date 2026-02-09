#!/bin/bash

#
# Run Dropper CLI tests in WSL (Windows Subsystem for Linux)
# This bypasses Windows Gradle test executor issues by running in a Linux environment
#

set -e

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║           Dropper CLI - WSL Test Runner                      ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Check if running in WSL
if grep -qi microsoft /proc/version 2>/dev/null; then
    echo "✅ Running in WSL"
    IN_WSL=true
else
    echo "⚠️  Not running in WSL - will attempt to launch WSL"
    IN_WSL=false
fi

echo ""

# Get the project directory (convert Windows path to WSL path if needed)
if [ "$IN_WSL" = true ]; then
    # Already in WSL, use current directory
    PROJECT_DIR="$(pwd)"
else
    # Not in WSL, need to convert path and run in WSL
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

    echo "Launching WSL from Windows..."
    echo "Project: $PROJECT_DIR"
    echo ""

    # Convert Windows path to WSL path (e.g., D:\foo -> /mnt/d/foo)
    WSL_PATH=$(echo "$PROJECT_DIR" | sed 's|^\([A-Za-z]\):|/mnt/\L\1|' | sed 's|\\|/|g')

    exec wsl bash -c "cd '$WSL_PATH' && bash scripts/test-in-wsl.sh"
fi

# Now we're definitely in WSL
echo "═══════════════════════════════════════════════════════════════"
echo "Environment Check"
echo "═══════════════════════════════════════════════════════════════"
echo ""

echo "📁 Project Directory: $PROJECT_DIR"
echo "🐧 Distribution: $(lsb_release -d | cut -f2)"
echo "☕ Java Version:"
java -version 2>&1 | head -n 3
echo ""

echo "═══════════════════════════════════════════════════════════════"
echo "Running Full Test Suite"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Run tests with test filtering disabled (all tests enabled)
# Set environment variable to indicate we're in WSL
export DROPPER_TEST_ENV=wsl

echo "Running: ./gradlew :src:cli:test --no-daemon"
echo ""

if ./gradlew :src:cli:test --no-daemon; then
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║              ✅ ALL TESTS PASSED IN WSL! ✅                   ║"
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo ""

    # Show test summary
    echo "Test Summary:"
    echo "  ✓ All unit tests"
    echo "  ✓ All integration tests"
    echo "  ✓ All E2E tests"
    echo "  ✓ All command tests"
    echo ""
    exit 0
else
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║              ❌ SOME TESTS FAILED ❌                          ║"
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo ""
    echo "Check the test output above for details."
    echo ""
    exit 1
fi
