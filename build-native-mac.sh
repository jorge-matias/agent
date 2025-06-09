#!/bin/bash
set -e

echo "Building native macOS agent..."

# Set up GraalVM environment
GRAALVM_HOME="/Library/Java/JavaVirtualMachines/graalvm-ce-java17-22.3.1/Contents/Home"
if [ ! -d "$GRAALVM_HOME" ]; then
    echo "GraalVM not found. Installing..."
    brew install --cask graalvm/tap/graalvm-ce-java17
fi

# Add GraalVM to PATH
export PATH="$GRAALVM_HOME/bin:$PATH"
export JAVA_HOME="$GRAALVM_HOME"

# Install native-image if not present
if ! command -v native-image &> /dev/null; then
    echo "Installing native-image component..."
    $GRAALVM_HOME/bin/gu install native-image
fi

# Build the project with GraalVM
cd agent-module
$GRAALVM_HOME/bin/java -version
mvn clean package
$GRAALVM_HOME/bin/native-image \
    --no-fallback \
    -H:+ReportExceptionStackTraces \
    --initialize-at-build-time=org.slf4j.LoggerFactory,ch.qos.logback,org.slf4j.impl.StaticLoggerBinder \
    -H:IncludeResources=agent-config.yml \
    -jar target/agent-module-1.0-SNAPSHOT-jar-with-dependencies.jar \
    target/agent-mac

# Copy the binary to the root directory
cp target/agent-mac ../
cd ..
chmod +x agent-mac

echo "Build complete! The native macOS binary is available as './agent-mac'"

