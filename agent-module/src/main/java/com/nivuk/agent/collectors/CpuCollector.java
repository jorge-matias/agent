package com.nivuk.agent.collectors;

import com.nivuk.agent.model.Metric;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

public class CpuCollector implements Collector {
    private final OperatingSystemMXBean osBean;

    public CpuCollector() {
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public Metric collect() {
        double value = osBean.getSystemLoadAverage();
        return new Metric("cpu", value, "load");
    }
}
