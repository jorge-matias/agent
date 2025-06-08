package com.nivuk.agent;

import com.nivuk.agent.export.MetricsExporter;
import com.nivuk.collectors.Collector;
import com.nivuk.collectors.MetricValue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

public class MetricsCollectionJob extends TimerTask {
    private final List<Collector> collectors;
    private final List<MetricsExporter> exporters;

    public MetricsCollectionJob(List<Collector> collectors, List<MetricsExporter> exporters) {
        this.collectors = collectors;
        this.exporters = exporters;
    }

    @Override
    public void run() {
        Map<String, Double> metrics = new HashMap<>();
        for (Collector collector : collectors) {
            MetricValue metric = collector.collect();
            metrics.put(metric.name(), metric.value());
        }

        for (MetricsExporter exporter : exporters) {
            exporter.export(metrics);
        }
    }
}
