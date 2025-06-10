package com.nivuk.server;

import java.time.Duration;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
public class MetricsController {
    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    private final MetricsStorage storage;

    public MetricsController(MetricsStorage storage) {
        this.storage = storage;
    }

    @PostMapping("/metrics")
    public void receiveMetrics(@RequestBody MetricsPayload payload) {
        if (payload.getPoints() == null || payload.getPoints().isEmpty()) {
            return;
        }

        logger.info("Received {} metric points", payload.getPoints().size());

        for (MetricsPayload.MetricPoint point : payload.getPoints()) {
            storage.addMetric(point);
            logger.debug("Processed metric - Host: {}, Name: {}, Time: {}, Value: {} {}",
                point.getHostName(), point.getMetricName(),
                new Date(point.getTimestamp()), point.getValue(), point.getUnit());
        }
    }

    @GetMapping("/metrics")
    public List<MetricsPayload.MetricPoint> getMetrics(
            @RequestParam(required = false) String host,
            @RequestParam(required = false) String metric,
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to) {

        long fromTime = from != null ? from : System.currentTimeMillis() - Duration.ofHours(1).toMillis();
        long toTime = to != null ? to : System.currentTimeMillis();

        return storage.queryMetrics(host, metric, fromTime, toTime);
    }

    @GetMapping("/metrics/hosts")
    public Set<String> getHosts() {
        return storage.getHosts();
    }

    @GetMapping("/metrics/names")
    public Set<String> getMetricNames() {
        return storage.getMetricNames();
    }
}
