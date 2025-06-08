package com.nivuk.server;

import java.util.Map;

import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class MetricsController {
    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);

    public static class MetricsPayload {
        private long timestamp;
        private String host;
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
        logger.info("Received metrics from host '{}' at timestamp {}: {}",
            payload.getHost(),
            payload.getTimestamp(),
            payload.getMetrics());
    }
}
