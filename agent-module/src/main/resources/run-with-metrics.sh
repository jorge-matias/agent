#!/bin/bash

# Create metrics directory
mkdir -p /app/metrics

# Function to cleanup background processes
cleanup() {
    echo "Cleaning up..."
    if [ ! -z "${AGENT_PID}" ]; then
        kill -TERM "$AGENT_PID" 2>/dev/null
    fi
    if [ ! -z "${PIDSTAT_PID}" ]; then
        kill -TERM "$PIDSTAT_PID" 2>/dev/null
    fi
    exit 0
}

# Set up signal handling
trap cleanup SIGTERM SIGINT

# Start collecting CPU and memory metrics in background
pidstat 1 > /app/metrics/cpu_stats.log &
PIDSTAT_PID=$!

# Run the agent
./agent &
AGENT_PID=$!

# Collect memory stats every second
while kill -0 $AGENT_PID 2>/dev/null; do
    ps -o pid,ppid,rss,vsize,pcpu,pmem,cmd -p $AGENT_PID >> /app/metrics/memory_stats.log 2>/dev/null || break
    sleep 1
done

# Call cleanup on exit
cleanup
