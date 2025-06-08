package com.nivuk.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import com.nivuk.agent.collectors.Collector;
import com.nivuk.agent.exporters.MetricsExporter;
import com.nivuk.agent.model.Metric;

public class MetricsCollectionJob extends TimerTask {
    private final List<Collector> collectors;
    private final List<MetricsExporter> exporters;

    public MetricsCollectionJob(List<Collector> collectors, List<MetricsExporter> exporters) {
        this.collectors = collectors;
        this.exporters = exporters;
    }

    @Override
    public void run() {
        List<Metric> metrics = new ArrayList<>();
        for (Collector collector : collectors) {
            Metric metric = collector.collect();
            metrics.add(metric);
        }

        for (MetricsExporter exporter : exporters) {
            exporter.export(metrics);
        }
    }
}
