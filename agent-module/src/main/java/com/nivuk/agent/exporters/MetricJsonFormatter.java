package com.nivuk.agent.exporters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nivuk.agent.model.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

public class MetricJsonFormatter {
    private static final Logger logger = LoggerFactory.getLogger(MetricJsonFormatter.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static class TimeSeriesMetrics {
        @JsonProperty("points")
        private final List<MetricPoint> points;

        public TimeSeriesMetrics(List<MetricPoint> points) {
            this.points = points;
        }

        public List<MetricPoint> getPoints() {
            return points;
        }
    }

    public static class MetricPoint {
        @JsonProperty("t")
        private final long timestamp;
        @JsonProperty("h")
        private final String hostName;
        @JsonProperty("n")
        private final String metricName;
        @JsonProperty("v")
        private final BigDecimal value;
        @JsonProperty("u")
        private final String unit;

        public MetricPoint(long timestamp, String hostName, String metricName, double value, String unit) {
            this.timestamp = timestamp;
            this.hostName = hostName;
            this.metricName = metricName;
            this.value = BigDecimal.valueOf(value).stripTrailingZeros();
            this.unit = unit;
        }

        public long getTimestamp() { return timestamp; }
        public String getHostName() { return hostName; }
        public String getMetricName() { return metricName; }
        public BigDecimal getValue() { return value; }
        public String getUnit() { return unit; }
    }

    private final boolean prettyPrint;

    public MetricJsonFormatter() {
        this(false);
    }

    public MetricJsonFormatter(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public String format(List<Metric> metrics) {
        if (metrics.isEmpty()) {
            return "{}";
        }

        List<MetricPoint> points = new ArrayList<>(metrics.size());
        for (Metric metric : metrics) {
            points.add(new MetricPoint(
                metric.timestamp(),
                metric.host(),
                metric.name(),
                metric.value(),
                metric.unit()
            ));
        }

        TimeSeriesMetrics timeSeriesMetrics = new TimeSeriesMetrics(points);
        try {
            if (prettyPrint) {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(timeSeriesMetrics);
            }
            return objectMapper.writeValueAsString(timeSeriesMetrics);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize metrics to JSON: {}", e.getMessage());
            return "{}";
        }
    }
}
