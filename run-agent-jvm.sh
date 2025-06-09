#!/bin/bash

echo "Running agent in JVM mode..."

# Set up environment
JAVA_OPTS="-Xms64m -Xmx128m"
JAR_PATH="agent-module/target/agent-module-1.0-SNAPSHOT-jar-with-dependencies.jar"

# Check if JAR exists
if [ ! -f "$JAR_PATH" ]; then
    echo "Error: Agent JAR not found at $JAR_PATH"
    echo "Please run build-native-mac.sh first to build the project"
    exit 1
fi

# Print memory settings
echo "Java memory settings:"
echo "Initial heap: 64MB"
echo "Maximum heap: 128MB"
echo

# Run the agent
echo "Starting agent..."
java $JAVA_OPTS -jar $JAR_PATH
