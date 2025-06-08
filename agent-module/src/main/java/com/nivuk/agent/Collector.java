package com.nivuk.agent;

public interface Collector {
    MetricValue collect();
}

record MetricValue(String name, double value, String unit) {}
