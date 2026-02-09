#!/bin/bash
set -e

echo "📚 Generating CLI documentation..."

# Build the CLI first if needed
if [ ! -f "src/cli/build/libs/dropper.jar" ]; then
  echo "Building CLI..."
  ./gradlew :src:cli:build
fi

# Get absolute path to output file
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
OUTPUT_PATH="$SCRIPT_DIR/../src/web/public/docs.json"

# Run docs command
echo "Running dropper docs..."
./gradlew :src:cli:run --args="docs --output=$OUTPUT_PATH"

echo "✅ Documentation generated at $OUTPUT_PATH"
