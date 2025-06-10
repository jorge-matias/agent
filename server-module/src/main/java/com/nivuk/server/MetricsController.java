package com.nivuk.server;

import java.time.Duration;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nivuk.server.storage.MetricStorage;
import com.nivuk.server.storage.MetricStorage.MetricValue;

@RestController
public class MetricsController {
    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    private final MetricStorage storage;

    public MetricsController(MetricStorage storage) {
        this.storage = storage;
    }

    public static class MetricsPayload {
        private String host;
        private Map<String, List<MetricPoint>> metrics;

        public static class MetricPoint {
            private long t;
            private double v;
            private String u;

            public long getT() { return t; }
            public void setT(long t) { this.t = t; }
            public double getV() { return v; }
            public void setV(double v) { this.v = v; }
            public String getU() { return u; }
            public void setU(String u) { this.u = u; }
        }

        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public Map<String, List<MetricPoint>> getMetrics() { return metrics; }
        public void setMetrics(Map<String, List<MetricPoint>> metrics) { this.metrics = metrics; }
    }

    @PostMapping("/metrics")
    public void receiveMetrics(@RequestBody MetricsPayload payload) {
        logger.info("Received metrics batch from host: {} with {} metric types",
            payload.getHost(),
            payload.getMetrics().size());

        payload.getMetrics().forEach((metricName, points) -> {
            logger.info("Processing {} {} metrics from host '{}' ({} data points)",
                points.size(),
                metricName,
                payload.getHost(),
                points.size());

            points.forEach(point -> {
                storage.addMetrics(
                    payload.getHost(),
                    metricName,
                    point.getT(),
                    point.getV(),
                    point.getU()
                );
            });
        });

        int totalDataPoints = payload.getMetrics().values().stream()
            .mapToInt(List::size)
            .sum();

        logger.info("Successfully processed batch: {} total data points across {} metric types for host '{}'",
            totalDataPoints,
            payload.getMetrics().size(),
            payload.getHost());
    }

    @GetMapping("/metrics/{host}/{metric}")
    public Map<Long, MetricValue> getMetrics(
            @PathVariable String host,
            @PathVariable String metric,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) {

        long fromTime = from != null ? from : System.currentTimeMillis() - Duration.ofHours(1).toMillis();
        long toTime = to != null ? to : System.currentTimeMillis();

        return storage.getMetrics(host, metric, fromTime, toTime);
    }

    @GetMapping("/metrics/{host}")
    public Map<String, Map<Long, MetricValue>> getHostMetrics(
            @PathVariable String host,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) {

        long fromTime = from != null ? from : System.currentTimeMillis() - Duration.ofHours(1).toMillis();
        long toTime = to != null ? to : System.currentTimeMillis();

        return storage.getMetricsByHost(host, fromTime, toTime);
    }

    @GetMapping("/metrics/aggregated")
    public Map<String, Map<String, Map<Long, MetricValue>>> getAggregatedMetrics(
            @RequestParam(defaultValue = "5m") String bucketSize) {

        Duration interval = parseDuration(bucketSize);
        return storage.getAggregatedView(
            values -> new MetricValue(
                values.stream().mapToDouble(MetricValue::getValue).average().orElse(0.0),
                values.get(0).getUnit()
            ),
            interval
        );
    }

    private Duration parseDuration(String duration) {
        String value = duration.substring(0, duration.length() - 1);
        char unit = duration.charAt(duration.length() - 1);
        long amount = Long.parseLong(value);

        return switch (unit) {
            case 's' -> Duration.ofSeconds(amount);
            case 'm' -> Duration.ofMinutes(amount);
            case 'h' -> Duration.ofHours(amount);
            case 'd' -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("Invalid duration unit: " + unit);
        };
    }
}
