package com.nivuk.agent.exporters;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nivuk.agent.model.Metric;

public class LoggingMetricsExporter implements MetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingMetricsExporter.class);

    @Override
    public void export(List<Metric> metrics) {
        if (metrics.isEmpty()) {
            logger.warn("No metrics to export");
            return;
        }

        for (Metric metric : metrics) {
            logger.info("Metric: name={}, value={}{}, host={}, timestamp={}",
                metric.name(),
                metric.value(),
                metric.unit().isEmpty() ? "" : " " + metric.unit(),
                metric.host(),
                metric.timestamp()
            );
        }
    }
}
