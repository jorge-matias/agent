package com.nivuk.agent;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class CpuCollector implements Collector {
    private final OperatingSystemMXBean osBean;

    public CpuCollector() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public MetricValue collect() {
        double value = osBean.getSystemLoadAverage();
        return new MetricValue("cpu", value, "load");
    }
}
