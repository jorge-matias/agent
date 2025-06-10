package com.nivuk.server;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonProperty;

@RestController
public class MetricsController {
    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);

    // Store metrics by timestamp-host combination
    private final Map<String, Map<String, Double>> metricsStore = new ConcurrentHashMap<>();

    public static class MetricsPayload {
        @JsonProperty("t")
        private long timestamp;
        @JsonProperty("h")
        private String host;
        @JsonProperty("m")
        private Map<String, Double> metrics;

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public Map<String, Double> getMetrics() { return metrics; }
        public void setMetrics(Map<String, Double> metrics) { this.metrics = metrics; }
    }

    @PostMapping("/metrics")
    public void receiveMetrics(@RequestBody MetricsPayload payload) {
        String key = payload.getTimestamp() + "-" + payload.getHost();

        // Get or create metrics map for this timestamp-host combination
        Map<String, Double> existingMetrics = metricsStore.computeIfAbsent(key, k -> new ConcurrentHashMap<>());

        // Merge new metrics, overwriting existing values (taking latest value like the agent)
        existingMetrics.putAll(payload.getMetrics());

        logger.info("Processed metrics from host '{}' at timestamp {}: {}",
            payload.getHost(),
            payload.getTimestamp(),
            existingMetrics);
    }

    @GetMapping("/metrics")
    public Map<String, Map<String, Double>> getMetrics() {
        return metricsStore;
    }

    @GetMapping("/metrics/{timestampHost}")
    public Map<String, Double> getMetricsByTimestampHost(@PathVariable String timestampHost) {
        return metricsStore.getOrDefault(timestampHost, Collections.emptyMap());
    }
}
