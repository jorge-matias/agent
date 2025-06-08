package com.nivuk.agent.exporters;

import com.nivuk.agent.model.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LoggingMetricsExporter implements MetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingMetricsExporter.class);

    @Override
    public void export(List<Metric> metrics) {
        if (metrics.isEmpty()) {
            logger.warn("No metrics to export");
            return;
        }

        StringBuilder logMessage = new StringBuilder("Metrics: ");
        for (Metric metric : metrics) {
            logMessage.append(metric.name())
                      .append(" = ")
                      .append(metric.value())
                      .append(" ")
                      .append(metric.unit())
                      .append("; ");
        }
        logger.info(logMessage.toString());
    }
}
