#!/bin/bash
# Test IPC inference integration
# Usage: ./test-ipc-inference.sh

set -e

cd "$(dirname "$0")"

echo "🔨 Building backend..."
./gradlew build -x test --no-daemon -q

echo ""
echo "🚀 Starting backend with IPC inference test..."
echo ""
echo "📤 Sending inference request:"
echo '{"type":"infer","id":"test-1","code":"def hello_world","language":"python","strategy":"TASK_PREFIX"}'
echo ""

# Send inference request via stdin
echo '{"type":"infer","id":"test-1","code":"def hello_world","language":"python","strategy":"TASK_PREFIX"}' | \
  java -Xmx4G -cp "build/classes/java/main:$(./gradlew printClasspath -q)" \
  dev.neuralforge.NeuralForgeBackendApplication 2>&1 | \
  grep -v "WARNING: Closing" | \
  grep -v "Starting NeuralForgeBackendApplication" | \
  grep -E '(\[IPC\]|{)'

echo ""
echo "✅ Test complete!"
