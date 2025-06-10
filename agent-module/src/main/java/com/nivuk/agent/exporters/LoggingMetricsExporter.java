package com.nivuk.agent.exporters;

import com.nivuk.agent.model.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class LoggingMetricsExporter implements MetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingMetricsExporter.class);
    private final MetricJsonFormatter formatter;

    public LoggingMetricsExporter() {
        this(new MetricJsonFormatter(false)); // Use compact format
    }

    public LoggingMetricsExporter(MetricJsonFormatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void export(List<Metric> metrics) {
        if (!metrics.isEmpty()) {
            logger.info("Collected metrics: {}", formatter.format(metrics));
        }
    }
}
