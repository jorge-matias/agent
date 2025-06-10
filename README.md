# Metrics System

A monitoring system consisting of an agent that collects system metrics and a server that receives and processes them.

## Components

- **Agent Module**: Collects CPU and memory metrics using either native GraalVM binary or JVM
- **Server Module**: Spring Boot application that receives and processes metrics

## Build & Run

The system can be built and run in multiple ways using the unified `run.sh` script:

```bash
./run.sh [build-type] [run-type]
```

### Build Options

- `--build-local-jvm`: Build using local Maven installation
- `--build-local-native`: Build native macOS binary using local GraalVM
- `--build-docker`: Build using Maven in Docker

### Run Options

- `--run-local-native`: Run native macOS agent with Docker server
- `--run-local-jvm`: Run JVM agent with Docker server
- `--run-docker-jvm`: Run both server and JVM agent in Docker
- `--run-docker-graal`: Run both server and native agent in Docker

### Examples

```bash
# Local development with Docker server
./run.sh --build-local-jvm --run-local-jvm       # Build & run with local JVM
./run.sh --build-local-native --run-local-native # Build & run native macOS binary

# Full Docker environment
./run.sh --build-docker --run-docker-jvm         # Build & run everything in Docker (JVM version)
./run.sh --build-docker --run-docker-graal       # Build & run everything in Docker (Native version)
```

### Performance Characteristics

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
- `SERVER_URL`: Override the metrics server URL

## Data Format

The agent sends metrics to the server using a JSON format that groups metrics by name. Each metric includes its timestamp, value, and unit:

```json
{
    "host": "hostname",
    "metrics": {
        "cpu": [
            {
                "t": 1686394800000,
                "v": 45.2,
                "u": "%"
            },
            {
                "t": 1686394801000,
                "v": 46.1,
                "u": "%"
            }
        ],
        "memory": [
            {
                "t": 1686394800000,
                "v": 1024,
                "u": "MB"
            },
            {
                "t": 1686394801000,
                "v": 1028,
                "u": "MB"
            }
        ]
    }
}
```

The metrics are:
- Grouped by host and metric name
- Include timestamp (t), value (v), and unit (u) for each measurement
- Sent in batches to optimize network usage

Available metrics and their units:
- CPU Load: Percentage (%)
- Memory: Megabytes (MB)

## Server API Endpoints

- `POST /metrics`: Submit new metrics
- `GET /metrics/{host}/{metric}`: Get specific metric for a host
- `GET /metrics/{host}`: Get all metrics for a host
- `GET /metrics/aggregated?bucketSize=5m`: Get aggregated metrics (supported bucket sizes: s,m,h,d)

## Directory Structure

- `agent-module/`: Agent implementation and configuration
  - `src/`: Source code and resources
  - `Dockerfile`: Native image container build
  - `Dockerfile.jvm`: JVM-based container build
- `server-module/`: Metrics server implementation
- `metrics/`: Directory mounted in the agent container for metric logs
- `run.sh`: Unified build and run script
