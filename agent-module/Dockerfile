# GraalVM native image builder
FROM ghcr.io/graalvm/graalvm-ce:latest AS builder
WORKDIR /app
COPY target/agent-module-1.0-SNAPSHOT-jar-with-dependencies.jar agent.jar
COPY src/main/resources/agent-config.yml ./

# Install native-image and build the executable
RUN gu install native-image && \
    mkdir -p /app/resources && \
    cp agent-config.yml /app/resources/ && \
    native-image --no-fallback -jar agent.jar \
    -H:IncludeResources=agent-config.yml \
    --initialize-at-build-time=org.slf4j.LoggerFactory,ch.qos.logback,org.slf4j.impl.StaticLoggerBinder \
    -J-Xmx4G \
    -J-Xms4G \
    -H:+ReportExceptionStackTraces

# Runtime image
FROM ubuntu:22.04
RUN apt-get update && apt-get install -y \
    sysstat \
    htop \
    procps \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/agent ./
COPY --from=builder /app/resources/agent-config.yml ./resources/
COPY src/main/resources/run-with-metrics.sh /app/

RUN chmod +x /app/run-with-metrics.sh
ENV SERVER_URL=http://server-module:8080/metrics
CMD ["/app/run-with-metrics.sh"]
