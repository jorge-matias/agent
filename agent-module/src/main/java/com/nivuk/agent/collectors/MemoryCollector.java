package com.nivuk.agent.collectors;

import com.nivuk.agent.model.Metric;
import java.util.List;
import java.util.Arrays;

public class MemoryCollector implements Collector {
    private static final double BYTES_TO_MB = 1024.0 * 1024.0;

    private record MemoryMeasurement(double freeMemoryMB, double totalMemoryMB, long timestamp) {}

    @Override
    public List<Metric> collect() {
        MemoryMeasurement measurement = measureMemory();
        return Arrays.asList(
            new Metric("mem_free", measurement.freeMemoryMB, "m"),
            new Metric("mem_total", measurement.totalMemoryMB, "m")
        );
    }

    private MemoryMeasurement measureMemory() {
        Runtime runtime = Runtime.getRuntime();
        long currentTime = System.currentTimeMillis();
        double totalMemoryMB = runtime.totalMemory() / BYTES_TO_MB;
        double freeMemoryMB = runtime.freeMemory() / BYTES_TO_MB;
        return new MemoryMeasurement(freeMemoryMB, totalMemoryMB, currentTime);
    }
}
