package com.nivuk.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import com.nivuk.agent.collectors.Collector;
import com.nivuk.agent.exporters.MetricsExporter;
import com.nivuk.agent.model.Metric;

public class MetricsCollectionTask extends TimerTask {
    private final List<Collector> collectors;
    private final List<MetricsExporter> exporters;

    public MetricsCollectionTask(List<Collector> collectors, List<MetricsExporter> exporters) {
        this.collectors = collectors;
        this.exporters = exporters;
    }

    public MetricsCollectionTask(List<Collector> collectors, MetricsExporter exporter) {
        this.collectors = collectors;
        this.exporters = List.of(exporter);
    }

    @Override
    public void run() {
        List<Metric> metrics = new ArrayList<>();
        for (Collector collector : collectors) {
            metrics.addAll(collector.collect());
        }

        for (MetricsExporter exporter : exporters) {
            exporter.export(metrics);
        }
    }
}
