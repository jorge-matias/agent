package com.nivuk.agent;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class MemoryCollector implements Collector {
    private final MemoryMXBean memBean;

    public MemoryCollector() {
        this.memBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public String collect() {
        double value = memBean.getHeapMemoryUsage().getUsed();
        return String.format("\"memory\": %.0f", value);
    }
}
