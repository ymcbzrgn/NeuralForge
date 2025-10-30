#!/bin/bash
# Test script for ModelLoader

echo "Building backend..."
cd "$(dirname "$0")/backend"
./gradlew build --no-daemon -q

echo ""
echo "Running ModelLoader test..."
echo "=================================="

# Run test with proper classpath
java -cp "build/classes/java/main:$(./gradlew printClasspath -q)" \
  dev.neuralforge.model.ModelLoaderTest
