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
