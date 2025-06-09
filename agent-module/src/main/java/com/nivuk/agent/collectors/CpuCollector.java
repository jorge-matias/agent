package com.nivuk.agent.collectors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.nivuk.agent.model.Metric;

public class CpuCollector implements Collector {
    private static final String PROC_STAT = "/proc/stat";
    private static final int MEASUREMENT_INTERVAL_MS = 100;

    @Override
    public List<Metric> collect() {
        try {
            CpuStats first = readCpuStats();
            Thread.sleep(MEASUREMENT_INTERVAL_MS);
            CpuStats second = readCpuStats();

            double cpuUsage = calculateCpuUsage(first, second);
            return List.of(new Metric("cpu", cpuUsage, "%"));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to collect CPU metrics", e);
        }
    }

    private CpuStats readCpuStats() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(PROC_STAT))) {
            String line = reader.readLine(); // First line contains CPU stats
            String[] fields = line.split("\\s+");

            // Fields: cpu user nice system idle iowait irq softirq steal guest guest_nice
            return new CpuStats(
                Long.parseLong(fields[1]), // user
                Long.parseLong(fields[2]), // nice
                Long.parseLong(fields[3]), // system
                Long.parseLong(fields[4]), // idle
                Long.parseLong(fields[5]), // iowait
                Long.parseLong(fields[6]), // irq
                Long.parseLong(fields[7]), // softirq
                Long.parseLong(fields[8])  // steal
            );
        }
    }

    private double calculateCpuUsage(CpuStats first, CpuStats second) {
        long totalFirst = first.total();
        long totalSecond = second.total();
        long totalDiff = totalSecond - totalFirst;

        if (totalDiff == 0) {
            return 0.0;
        }

        long idleDiff = second.idle - first.idle;
        return 100.0 * (1.0 - (double) idleDiff / totalDiff);
    }

    private record CpuStats(
        long user,
        long nice,
        long system,
        long idle,
        long iowait,
        long irq,
        long softirq,
        long steal
    ) {
        public long total() {
            return user + nice + system + idle + iowait + irq + softirq + steal;
        }
    }
}
