package com.nivuk.agent.collectors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.nivuk.agent.model.Metric;

public class CpuCollector implements Collector {

    @Override
    public List<Metric> collect() {
        try {
            String loadAvg = readLoadAverage();
            double value = Double.parseDouble(loadAvg);
            return List.of(new Metric("cpu", value, "%"));
        } catch (IOException e) {
            throw new RuntimeException("Failed to collect CPU metrics", e);
        }
    }

    private String readLoadAverage() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader("/proc/loadavg"))) {
            return reader.readLine().split(" ")[0];
        }
    }
}
