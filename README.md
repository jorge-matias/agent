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
  - `pom.xml`: Maven configuration for agent module
- `server-module/`: Metrics server implementation
  - `src/`: Source code and resources
  - `Dockerfile`: Container build for the server
  - `pom.xml`: Maven configuration for server module
- `run.sh`: Unified build and run script
- `compare-performance.sh`: Script for performance comparison
- `docker-compose.yml`: Docker Compose configuration
- `pom.xml`: Root Maven configuration file

## Production Readiness Considerations

This agent is designed as a lightweight metrics collector with a demonstration server. For production deployment, consider these agent-focused enhancements:

### Agent Resilience

- **Connectivity Management**:
  - Disk-based persistent buffering for metrics during outages
  - Exponential backoff with jitter for reconnection attempts
  - Configurable retention policies for buffer storage
  - Compression of stored metrics to optimize disk usage

- **Resource Efficiency**:
  - Adaptive collection intervals based on system load
  - Payload compression to reduce bandwidth usage
  - Batching with configurable thresholds to minimize network overhead
  - Low-priority thread scheduling to reduce impact on host system

### Security & Identity

- **Secure Communication**:
  - TLS with certificate pinning for metric transmission
  - API key or mutual TLS for agent authentication
  - Secure credential storage with minimal permissions

- **Data Protection**:
  - Metric obfuscation for sensitive values
  - Local filtering of confidential information
  - Minimal collection scope to comply with privacy regulations

### Extensibility

- **Customizable Processors**:
  - Statistical processors for aggregating metrics (min, max, avg, percentiles)
  - Sampling processors to reduce data volume while preserving patterns
  - Filtering processors to exclude noise or focus on anomalies
  - Composite processors for complex metric transformations

```java
// Example: Configuring a percentile processor
ProcessorConfig processorConfig = new ProcessorConfig();
processorConfig.add(new PercentileProcessor("cpu", List.of(50.0, 90.0, 99.0)));
processorConfig.add(new MinMaxProcessor("memory"));
processorConfig.add(new MovingAverageProcessor("disk_io", 60)); // 60 second window
```

- **Plugin System**:
  - Custom collectors for specialized metrics
  - Alternative exporters for different endpoints
  - Transformation scripts using embedded scripting engine

### Observability

- **Self-Monitoring**:
  - Collection efficiency metrics
  - Buffer utilization statistics
  - Export success/failure rates
  - Runtime performance impact assessment

### Deployment & Updates

- **Installation Options**:
  - Native binaries for target platforms
  - Container images with minimal footprint
  - Scriptable silent installation

- **Lifecycle Management**:
  - Self-update capabilities with rollback
  - Configuration hot-reloading
  - Graceful shutdown with buffer flushing
