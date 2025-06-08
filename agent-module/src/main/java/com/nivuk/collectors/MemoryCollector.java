package com.nivuk.collectors;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class MemoryCollector implements Collector {
    private final MemoryMXBean memBean;

    public MemoryCollector() {
        this.memBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public MetricValue collect() {
        double value = memBean.getHeapMemoryUsage().getUsed();
        return new MetricValue("memory", value, "bytes");
    }
}
