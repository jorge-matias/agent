package com.nivuk.agent.exporters;

import com.nivuk.agent.model.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LoggingMetricsExporter implements MetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingMetricsExporter.class);

    @Override
    public void export(List<Metric> metrics) {
        if (metrics.isEmpty()) {
            logger.warn("No metrics to export");
            return;
        }

        Map<String, Double> metricsMap = new HashMap<>();
        metrics.forEach(metric -> metricsMap.put(metric.name(), metric.value()));

        String json = String.format("""
            {
              "timestamp": %d,
              "host": "%s",
              "metrics": {%s
              }
            }""",
            Metric.getCurrentTimestamp(),
            Metric.getHostname(),
            metricsMap.entrySet().stream()
                .map(entry -> String.format("\n                \"%s\": %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(","))
        );

        logger.info("Metrics: {}", json);
    }
}
