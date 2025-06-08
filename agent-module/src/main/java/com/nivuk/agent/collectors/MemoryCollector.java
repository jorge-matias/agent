package com.nivuk.agent.collectors;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

import com.nivuk.agent.model.Metric;

public class MemoryCollector implements Collector {
    private final MemoryMXBean memBean;

    public MemoryCollector() {
        this.memBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public Metric collect() {
        double value = memBean.getHeapMemoryUsage().getUsed();
        return new Metric("memory", value, "bytes");
    }
}
