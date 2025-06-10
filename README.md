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

The agent sends metrics to the server using a time-series optimized format:

```json
{
    "points": [
        {
            "t": 1686394800000,  // timestamp in milliseconds
            "h": "hostname",     // host identifier
            "n": "cpu",         // metric name
            "v": 45.2,          // value
            "u": "%"            // unit
        },
        {
            "t": 1686394800000,
            "h": "hostname",
            "n": "memory",
            "v": 1024,
            "u": "MB"
        }
    ]
}
```

Each data point contains:
- `t`: Unix timestamp in milliseconds
- `h`: Host identifier
- `n`: Metric name
- `v`: Metric value
- `u`: Unit of measurement

This format is optimized for:
- Time-series databases like Prometheus or InfluxDB
- Direct visualization in Grafana
- Easy querying and aggregation by time, host, or metric type
- Efficient streaming and batch processing

## Server API Endpoints

- `POST /metrics`: Submit new metrics in time-series format
- `GET /metrics`: Query metrics with optional filters:
  - `host`: Filter by host
  - `metric`: Filter by metric name
  - `from`: Start timestamp (defaults to 1 hour ago)
  - `to`: End timestamp (defaults to now)
- `GET /metrics/hosts`: Get list of available hosts
- `GET /metrics/names`: Get list of available metric types

## Storage and Integration

The server stores metrics in a time-series optimized structure that:
- Uses skip lists for efficient time-range queries
- Maintains separate time series per host and metric type
- Supports real-time aggregation and downsampling
- Is ready for integration with time-series databases

### Grafana Integration

The metrics format is designed for easy visualization in Grafana:
1. Configure a JSON datasource pointing to the server
2. Use metric names as series
3. Host and unit are available as labels
4. Time range queries are natively supported

## Directory Structure

- `agent-module/`: Agent implementation and configuration
  - `src/`: Source code and resources
  - `Dockerfile`: Native image container build
  - `Dockerfile.jvm`: JVM-based container build
- `server-module/`: Metrics server implementation
- `metrics/`: Directory mounted in the agent container for metric logs
- `run.sh`: Unified build and run script
