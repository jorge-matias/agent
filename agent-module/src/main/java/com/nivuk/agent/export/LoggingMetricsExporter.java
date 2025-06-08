package com.nivuk.agent.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class LoggingMetricsExporter implements MetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingMetricsExporter.class);

    @Override
    public void export(Map<String, Double> metrics) {
        logger.info("Current metrics: {}", metrics);
    }
}
