package com.nivuk.agent.exporters;

import com.nivuk.agent.model.Metric;

public interface MetricsExporter {
    void export(Metric metric);
}
