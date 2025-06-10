package com.nivuk.agent.collectors;

import com.nivuk.agent.model.Metric;
import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.util.List;

public class CpuCollector implements Collector {
    private final OperatingSystemMXBean osBean;

    public CpuCollector() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public List<Metric> collect() {
        CpuMeasurement measurement = measureCpuLoad();
        return List.of(new Metric("cpu", measurement.cpuLoad, "p"));
    }

    private record CpuMeasurement(double cpuLoad, long timestamp) {}

    private CpuMeasurement measureCpuLoad() {
        long currentTime = System.currentTimeMillis();
        double cpuLoad = osBean.getCpuLoad();

        // Convert to percentage and handle edge cases
        double cpuPercentage = Math.max(0, Math.min(100, cpuLoad * 100));
        cpuPercentage = Double.isNaN(cpuPercentage) ? 0.0 : cpuPercentage;

        return new CpuMeasurement(cpuPercentage, currentTime);
    }
}
