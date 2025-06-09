# Metrics System

A monitoring system consisting of an agent that collects system metrics and a server that receives and processes them.

## Components

- **Agent Module**: Collects CPU and memory metrics using a native executable built with GraalVM
- **Server Module**: Spring Boot application that receives and processes metrics

## Build & Run

### Option 1: Using Local Maven Installation

```bash
./run-with-local-maven.sh
```

This script will:

- Build both modules using your local Maven installation
- Start the containers using docker compose

### Option 2: Using Docker Only

```bash
./run-with-docker-build.sh
```

This script will:

- Build the Java artifacts using Maven in Docker
- Build and start all containers using docker compose

### Option 3: Native macOS Binary

```bash
# Build the native binary
./build-native-mac.sh

# Run the native agent
./agent-mac
```

This approach:

- Builds a native executable using GraalVM
- Creates a platform-specific binary optimized for macOS
- Provides faster startup and lower memory footprint
- Doesn't require a JVM to run

### Option 4: JVM Mode

```bash
# Run the agent using JVM
./run-agent-jvm.sh
```

This approach:

- Runs the agent in traditional JVM mode
- Configures specific memory settings (64MB initial, 128MB max heap)
- Requires a JVM but offers more runtime flexibility

### Memory and Performance Comparison

To compare the performance between native and JVM modes:
```bash
./compare-performance.sh
```

This script will:
- Run both native and JVM versions sequentially
- Monitor CPU and memory usage for 10 seconds each
- Display real-time comparison metrics
- Show startup time, memory footprint, and CPU usage differences

Native vs JVM mode characteristics:

- **Native Mode**
  - Memory usage: ~19-22MB
  - CPU usage: 0-1.1%
  - Startup time: < 200ms
  - No JVM required
  - Better for resource-constrained environments

- **JVM Mode**
  - Memory usage: ~91-96MB
  - CPU usage: 0-0.8%
  - Startup time: ~400ms
  - Runtime optimizations available
  - Platform independent

Actual comparison measurements:
```
Time(s) CPU(%) Memory(MB)
-------------------------
Native:
      1    0.0      19.0
      5    0.0      19.1
     10    0.8      22.4

JVM:
      1    0.2      91.0
      5    0.0      91.0
     10    0.8      95.7
```

Key Findings:
- Native binary uses ~75% less memory
- CPU usage is comparable between both versions
- Native version starts up ~2x faster
- Both versions show stable memory usage over time

## Configuration

### Agent Configuration (agent-config.yml)

```yaml
# Collection interval in seconds
intervalSeconds: 1

# Enable/disable collectors
collectors:
  cpu: true
  memory: true

# Configure exporters
exporters:
  # Console logging exporter
  logging:
    enabled: true
  
  # HTTP exporter with buffering
  webservice:
    enabled: true
    bufferSeconds: 10
    serverUrl: http://server-module:8080/metrics
```

### Environment Variables

- `AGENT_COLLECTION_INTERVAL`: Override the collection interval from the config file
- `SERVER_URL`: Override the metrics server URL (default: http://server-module:8080/metrics)

## Metrics

The agent collects and exports the following metrics:

- **CPU Load**: Current system CPU load percentage
- **Memory**: 
  - Free memory in MB
  - Total memory in MB

## Directory Structure

- `agent-module/`: Agent implementation and configuration
- `server-module/`: Metrics server implementation
- `metrics/`: Directory mounted in the agent container for metric logs
