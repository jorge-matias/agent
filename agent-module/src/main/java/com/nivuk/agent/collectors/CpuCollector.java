package com.nivuk.agent.collectors;

import com.nivuk.agent.model.Metric;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.util.List;

public class CpuCollector implements Collector {
    private final OperatingSystemMXBean osBean;
    private static final int MEASUREMENT_INTERVAL_MS = 100;

    public CpuCollector() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public List<Metric> collect() {
        try {
            double cpuLoad = measureCpuLoad();
            return List.of(new Metric("cpu", cpuLoad, "%"));
        } catch (InterruptedException e) {
            throw new RuntimeException("Failed to collect CPU metrics", e);
        }
    }

    private double measureCpuLoad() throws InterruptedException {
        double first = osBean.getCpuLoad();
        if (first < 0) { // First reading might be negative
            Thread.sleep(MEASUREMENT_INTERVAL_MS);
            first = osBean.getCpuLoad();
        }

        Thread.sleep(MEASUREMENT_INTERVAL_MS);
        double second = osBean.getCpuLoad();

        // Convert to percentage and ensure it's within bounds
        double cpuPercentage = Math.max(0, Math.min(100, second * 100));
        return Double.isNaN(cpuPercentage) ? 0.0 : cpuPercentage;
    }
}
