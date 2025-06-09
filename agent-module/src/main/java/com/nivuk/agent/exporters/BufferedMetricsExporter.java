package com.nivuk.agent.exporters;

import com.nivuk.agent.model.Metric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class BufferedMetricsExporter implements MetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(BufferedMetricsExporter.class);
    private final MetricsExporter delegate;
    private final List<Metric> buffer;
    private final ScheduledExecutorService scheduler;
    private final int bufferSeconds;

    public BufferedMetricsExporter(MetricsExporter delegate, int bufferSeconds) {
        this.delegate = delegate;
        this.bufferSeconds = bufferSeconds;
        this.buffer = new ArrayList<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        // Schedule periodic flush
        scheduler.scheduleAtFixedRate(this::flush, bufferSeconds, bufferSeconds, TimeUnit.SECONDS);

        // Add shutdown hook to ensure metrics are flushed on exit
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    @Override
    public void export(List<Metric> metrics) {
        synchronized (buffer) {
            buffer.addAll(metrics);
        }
    }

    private void flush() {
        List<Metric> toExport;
        synchronized (buffer) {
            if (buffer.isEmpty()) {
                return;
            }
            toExport = new ArrayList<>(buffer);
            buffer.clear();
        }

        try {
            // Group metrics by timestamp and host
            Map<String, List<Metric>> groupedMetrics = toExport.stream()
                .collect(Collectors.groupingBy(m -> m.timestamp() + "-" + m.host()));

            // Export each group as a single batch
            for (List<Metric> group : groupedMetrics.values()) {
                delegate.export(group);
            }

            logger.debug("Flushed {} metrics ({} batches) after {} seconds",
                toExport.size(),
                groupedMetrics.size(),
                bufferSeconds);
        } catch (Exception e) {
            logger.error("Error flushing metrics", e);
            // Put metrics back in buffer to try again next time
            synchronized (buffer) {
                buffer.addAll(0, toExport);
            }
        }
    }

    private void shutdown() {
        scheduler.shutdown();
        try {
            // Flush remaining metrics
            flush();
            // Wait for any ongoing flush to complete
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn("Buffer scheduler did not terminate in time");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Interrupted while shutting down buffer scheduler", e);
        }
    }
}
