#!/bin/bash

#
# Run Dropper CLI tests in Docker container
# Alternative to WSL for running Linux tests on Windows
#

set -e

echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║           Dropper CLI - Docker Test Runner                   ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo ""

# Configuration
IMAGE_NAME="dropper-test"
CONTAINER_NAME="dropper-test-runner"

# Build Docker image if it doesn't exist
if ! docker images | grep -q "$IMAGE_NAME"; then
    echo "🐳 Building Docker test image..."
    docker build -t "$IMAGE_NAME" -f- . <<'DOCKERFILE'
FROM eclipse-temurin:21-jdk

# Install required tools
RUN apt-get update && apt-get install -y \
    git \
    bash \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /project

# Pre-download Gradle wrapper
COPY gradlew gradlew.bat gradle.properties settings.gradle.kts ./
COPY gradle/ gradle/
RUN ./gradlew --version

CMD ["./gradlew", "clean", ":src:cli:test", "--no-daemon"]
DOCKERFILE
    echo "✅ Docker image built"
else
    echo "✅ Docker image already exists"
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "Running Tests in Docker Container"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Remove old container if it exists
docker rm -f "$CONTAINER_NAME" 2>/dev/null || true

# Run tests in container
echo "🐳 Starting container and running tests..."
echo ""

if docker run --rm \
    --name "$CONTAINER_NAME" \
    -v "$(pwd):/project" \
    -e DROPPER_TEST_ENV=docker \
    "$IMAGE_NAME"; then

    echo ""
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║              ✅ ALL TESTS PASSED IN DOCKER! ✅                ║"
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo ""
    exit 0
else
    echo ""
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║              ❌ SOME TESTS FAILED ❌                          ║"
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo ""
    exit 1
fi
