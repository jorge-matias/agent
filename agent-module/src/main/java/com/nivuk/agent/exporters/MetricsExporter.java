package com.nivuk.agent.exporters;

import com.nivuk.agent.model.Metric;
import java.util.List;

public interface MetricsExporter {
    void export(List<Metric> metrics);
}
