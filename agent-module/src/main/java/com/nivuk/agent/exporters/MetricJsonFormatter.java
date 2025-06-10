package com.nivuk.agent.exporters;

import com.nivuk.agent.model.Metric;
import java.util.*;
import java.util.stream.Collectors;

public class MetricJsonFormatter {
    private final boolean prettyPrint;

    public MetricJsonFormatter() {
        this(false);
    }

    public MetricJsonFormatter(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
    }

    public String format(List<Metric> metrics) {
        if (metrics.isEmpty()) {
            return "{}";
        }

        return prettyPrint ? formatPretty(metrics) : formatCompact(metrics);
    }

    private String formatPretty(List<Metric> metrics) {
        String host = metrics.get(0).host();
        Map<String, List<Metric>> metricsByName = metrics.stream()
            .collect(Collectors.groupingBy(Metric::name));

        StringJoiner metricsJson = new StringJoiner(",\n  ");
        for (Map.Entry<String, List<Metric>> entry : metricsByName.entrySet()) {
            String metricName = entry.getKey();
            StringJoiner valuesJson = new StringJoiner(",\n    ");

            for (Metric metric : entry.getValue()) {
                String value = formatValue(metric.value());
                valuesJson.add(String.format("{\"t\":%d,\"v\":%s,\"u\":\"%s\"}",
                    metric.timestamp(), value, metric.unit()));
            }
            metricsJson.add(String.format("    \"%s\": [\n    %s\n    ]", metricName, valuesJson));
        }

        return String.format("{\n  \"host\": \"%s\",\n  \"metrics\": {\n%s\n  }\n}", host, metricsJson);
    }

    private String formatCompact(List<Metric> metrics) {
        String host = metrics.get(0).host();
        Map<String, List<Metric>> metricsByName = metrics.stream()
            .collect(Collectors.groupingBy(Metric::name));

        StringJoiner metricsJson = new StringJoiner(",");
        for (Map.Entry<String, List<Metric>> entry : metricsByName.entrySet()) {
            String metricName = entry.getKey();
            StringJoiner valuesJson = new StringJoiner(",");

            for (Metric metric : entry.getValue()) {
                String value = formatValue(metric.value());
                valuesJson.add(String.format("{\"t\":%d,\"v\":%s,\"u\":\"%s\"}",
                    metric.timestamp(), value, metric.unit()));
            }
            metricsJson.add(String.format("\"%s\":[%s]", metricName, valuesJson));
        }

        return String.format("{\"host\":\"%s\",\"metrics\":{%s}}", host, metricsJson);
    }

    private String formatValue(double value) {
        String formatted = String.format(Locale.ENGLISH, "%.1f", value);
        return formatted.endsWith(".0") ? formatted.substring(0, formatted.length() - 2) : formatted;
    }
}
