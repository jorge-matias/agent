package com.nivuk.agent.collectors;

import com.nivuk.agent.model.Metric;
import java.util.List;
import java.util.Arrays;

public class MemoryCollector implements Collector {
    private static final double BYTES_TO_MB = 1024.0 * 1024.0;

    @Override
    public List<Metric> collect() {
        Runtime runtime = Runtime.getRuntime();
        double totalMemoryMB = runtime.totalMemory() / BYTES_TO_MB;
        double freeMemoryMB = runtime.freeMemory() / BYTES_TO_MB;

        // Create separate metrics for free and total memory
        return Arrays.asList(
            new Metric("memory_free", freeMemoryMB, "MB"),
            new Metric("memory_total", totalMemoryMB, "MB")
        );
    }
}
