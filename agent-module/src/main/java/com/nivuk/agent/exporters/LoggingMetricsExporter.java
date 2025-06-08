package com.nivuk.agent.exporters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nivuk.agent.model.Metric;

public class LoggingMetricsExporter implements MetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingMetricsExporter.class);

    @Override
    public void export(Metric metric) {
        logger.info("Metric: {}={} {}", metric.name(), metric.value(), metric.unit());
    }
}
