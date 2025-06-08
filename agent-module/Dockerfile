FROM ghcr.io/graalvm/graalvm-ce:latest AS builder
WORKDIR /app
COPY target/agent-module-1.0-SNAPSHOT-jar-with-dependencies.jar agent.jar
COPY src/main/resources/agent-config.yml ./
RUN gu install native-image
RUN mkdir -p /app/resources && cp agent-config.yml /app/resources/
RUN native-image --no-fallback -jar agent.jar \
    -H:IncludeResources=agent-config.yml \
    --initialize-at-build-time=org.slf4j.LoggerFactory,ch.qos.logback,org.slf4j.impl.StaticLoggerBinder

FROM ubuntu:22.04
RUN apt-get update && apt-get install -y \
    sysstat \
    htop \
    procps \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/agent ./
COPY --from=builder /app/resources/agent-config.yml ./resources/
ENV SERVER_URL=http://server-module:8080/metrics

# Script to collect metrics while running the agent
COPY <<EOF /app/run-with-metrics.sh
#!/bin/bash

# Create metrics directory
mkdir -p /app/metrics

# Function to cleanup background processes
cleanup() {
    echo "Cleaning up..."
    if [ ! -z "\${AGENT_PID}" ]; then
        kill -TERM "\$AGENT_PID" 2>/dev/null
    fi
    if [ ! -z "\${PIDSTAT_PID}" ]; then
        kill -TERM "\$PIDSTAT_PID" 2>/dev/null
    fi
    exit 0
}

# Set up signal handling
trap cleanup SIGTERM SIGINT

# Start collecting CPU and memory metrics in background
pidstat 1 > /app/metrics/cpu_stats.log &
PIDSTAT_PID=\$!

# Run the agent
./agent &
AGENT_PID=\$!

# Collect memory stats every second
while kill -0 \$AGENT_PID 2>/dev/null; do
    ps -o pid,ppid,rss,vsize,pcpu,pmem,cmd -p \$AGENT_PID >> /app/metrics/memory_stats.log 2>/dev/null || break
    sleep 1
done

# Call cleanup on exit
cleanup
EOF

RUN chmod +x /app/run-with-metrics.sh
CMD ["/app/run-with-metrics.sh"]
