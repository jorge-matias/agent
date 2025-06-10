package com.nivuk.agent.exporters;

import com.nivuk.agent.model.Metric;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MetricJsonFormatterTest {
    private final MetricJsonFormatter prettyFormatter = new MetricJsonFormatter(true);
    private final MetricJsonFormatter compactFormatter = new MetricJsonFormatter(false);

    @Test
    void shouldFormatEmptyMetricsList() {
        assertEquals("{}", prettyFormatter.format(List.of()));
        assertEquals("{}", compactFormatter.format(List.of()));
    }

    @Test
    void shouldFormatSingleMetric() {
        long timestamp = 1686394800000L;
        List<Metric> metrics = List.of(
            new Metric("cpu", 75.5, "%", "test-host", timestamp)
        );

        // Test compact formatting
        String expectedCompact = String.format(
            "{\"host\":\"test-host\",\"metrics\":{\"cpu\":[{\"t\":%d,\"v\":75.5,\"u\":\"%%\"}]}}",
            timestamp);
        assertEquals(expectedCompact, compactFormatter.format(metrics));

        // Test pretty formatting (ignoring whitespace)
        String prettyJson = prettyFormatter.format(metrics);
        assertEquals(expectedCompact.replaceAll("\\s+", ""), prettyJson.replaceAll("\\s+", ""));
    }

    @Test
    void shouldFormatMultipleMetricsOfSameType() {
        long timestamp1 = 1686394800000L;
        long timestamp2 = 1686394801000L;
        List<Metric> metrics = List.of(
            new Metric("cpu", 75.5, "%", "test-host", timestamp1),
            new Metric("cpu", 80.0, "%", "test-host", timestamp2)
        );

        String json = compactFormatter.format(metrics);

        assertTrue(json.contains("\"host\":\"test-host\""));
        assertTrue(json.contains(String.format("{\"t\":%d,\"v\":75.5,\"u\":\"%%\"}", timestamp1)));
        assertTrue(json.contains(String.format("{\"t\":%d,\"v\":80,\"u\":\"%%\"}", timestamp2)));
    }

    @Test
    void shouldFormatDifferentMetricTypes() {
        long timestamp = 1686394800000L;
        List<Metric> metrics = List.of(
            new Metric("cpu", 75.5, "%", "test-host", timestamp),
            new Metric("mem_free", 1024.0, "MB", "test-host", timestamp),
            new Metric("mem_total", 2048.0, "MB", "test-host", timestamp)
        );

        String json = compactFormatter.format(metrics);

        // Verify the structure without depending on the order
        assertTrue(json.contains("\"host\":\"test-host\""));
        assertTrue(json.contains(String.format("\"cpu\":[{\"t\":%d,\"v\":75.5,\"u\":\"%%\"}]", timestamp)));
        assertTrue(json.contains(String.format("\"mem_free\":[{\"t\":%d,\"v\":1024,\"u\":\"MB\"}]", timestamp)));
        assertTrue(json.contains(String.format("\"mem_total\":[{\"t\":%d,\"v\":2048,\"u\":\"MB\"}]", timestamp)));
    }

    @Test
    void shouldFormatValuesWithoutTrailingZeros() {
        long timestamp = 1686394800000L;
        List<Metric> metrics = List.of(
            new Metric("cpu", 75.0, "%", "test-host", timestamp)
        );

        String json = compactFormatter.format(metrics);
        assertTrue(json.contains("\"v\":75,"));  // Should not contain .0
    }
}
