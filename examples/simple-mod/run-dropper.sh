#!/bin/bash
cd "$(dirname "$0")"
../../gradlew -p ../.. :src:cli:run --args="$*" --console=plain
