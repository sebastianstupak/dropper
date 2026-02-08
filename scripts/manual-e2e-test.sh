#!/bin/bash
set -e

echo "=== Manual E2E Test ==="
echo ""

# Step 1: Clean examples
echo "Step 1: Cleaning examples..."
rm -rf examples/test-mod-manual
echo "  ✓ Cleaned"

# Step 2: Create test project using ProjectGenerator
echo ""
echo "Step 2: Creating test project..."
cd examples
mkdir -p test-mod-manual
cd test-mod-manual

# Create a minimal config.yml
cat > config.yml << 'EOF'
mod:
  id: testmodmanual
  name: "Test Mod Manual"
  version: "1.0.0"
  description: "Manual E2E test mod"
  author: "Test"
  license: "MIT"
EOF

# Copy gradle wrapper
echo ""
echo "Step 3: Copying Gradle wrapper..."
cp -r ../../gradle .
cp ../../gradlew* .
chmod +x gradlew
echo "  ✓ Gradle wrapper copied"

# Show what we have
echo ""
echo "Step 4: Checking structure..."
ls -la
echo ""
echo "Gradle wrapper files:"
ls -la gradle/wrapper/

echo ""
echo "=== Manual setup complete ==="
echo "Project location: $(pwd)"
echo ""
echo "To build manually:"
echo "  cd $(pwd)"
echo "  ./gradlew build"
