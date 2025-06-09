#!/bin/bash

echo "Comparing native and JVM agent performance..."
echo "----------------------------------------"

# Check requirements
if ! command -v ps &> /dev/null; then
    echo "ps command not found"
    exit 1
fi

# Function to get memory usage in MB
get_memory() {
    ps -o rss= -p $1 | awk '{print $1/1024}'
}

# Function to get CPU usage
get_cpu() {
    ps -p $1 -o %cpu= | awk '{print $1}'
}

# Function to monitor process
monitor_process() {
    local pid=$1
    local type=$2
    local start_time=$(date +%s)

    echo "Monitoring $type (PID: $pid)"
    echo "Time(s) CPU(%) Memory(MB)"
    echo "-------------------------"

    for i in {1..10}; do
        local cpu=$(get_cpu $pid)
        local memory=$(get_memory $pid)
        local current_time=$(($(date +%s) - start_time))
        printf "%7d %6.1f %9.1f\n" $current_time $cpu $memory
        sleep 1
    done
}

# Run and monitor native version
echo "Starting native agent..."
./agent-mac &
NATIVE_PID=$!
sleep 2  # Wait for startup
monitor_process $NATIVE_PID "Native"
kill $NATIVE_PID

echo
echo "Waiting 5 seconds before starting JVM version..."
sleep 5

# Run and monitor JVM version
echo "Starting JVM agent..."
java -Xms64m -Xmx128m -jar agent-module/target/agent-module-1.0-SNAPSHOT-jar-with-dependencies.jar &
JVM_PID=$!
sleep 2  # Wait for startup
monitor_process $JVM_PID "JVM"
kill $JVM_PID

echo
echo "Comparison complete!"
echo "See README.md for more details about the tradeoffs between native and JVM modes."
