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

        String json = compactFormatter.format(metrics);
        String expected = String.format(
            "{\"points\":[{\"t\":%d,\"h\":\"test-host\",\"n\":\"cpu\",\"v\":75.5,\"u\":\"%%\"}]}",
            timestamp);

        assertEquals(expected, json);
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

        assertTrue(json.contains("\"points\":["), "Should have points array");
        assertTrue(json.contains(String.format("\"t\":%d", timestamp1)), "Should contain first timestamp");
        assertTrue(json.contains(String.format("\"t\":%d", timestamp2)), "Should contain second timestamp");
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

        assertTrue(json.contains("\"points\":["), "Should have points array");
        assertTrue(json.contains("\"n\":\"cpu\""), "Should contain CPU metric name");
        assertTrue(json.contains("\"n\":\"mem_free\""), "Should contain mem_free metric name");
        assertTrue(json.contains("\"n\":\"mem_total\""), "Should contain mem_total metric name");
        assertTrue(json.contains("\"u\":\"%\""), "Should contain percentage unit");
        assertTrue(json.contains("\"u\":\"MB\""), "Should contain MB unit");
    }

    @Test
    void shouldFormatValuesWithoutTrailingZeros() {
        long timestamp = 1686394800000L;
        List<Metric> metrics = List.of(
            new Metric("cpu", 75.0, "%", "test-host", timestamp)
        );

        String json = compactFormatter.format(metrics);
        assertTrue(json.contains("\"v\":75"), "Should remove trailing zeros");
        assertFalse(json.contains("\"v\":75.0"), "Should not contain trailing zero");
    }
}
