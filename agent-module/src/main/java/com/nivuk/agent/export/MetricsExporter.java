package com.nivuk.agent.export;

import java.util.Map;

public interface MetricsExporter {
    void export(Map<String, Double> metrics);
}
