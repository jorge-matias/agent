package com.nivuk.agent.exporters;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nivuk.agent.model.Metric;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebServiceMetricsExporter implements MetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(WebServiceMetricsExporter.class);
    private final HttpClient client;
    private final String serverUrl;

    public WebServiceMetricsExporter(OkHttpClient okHttpClient, String serverUrl) {
        this(new OkHttpClientWrapper(okHttpClient), serverUrl);
    }

    // For testing
    WebServiceMetricsExporter(HttpClient client, String serverUrl) {
        this.client = client;
        this.serverUrl = serverUrl;
    }

    @Override
    public void export(List<Metric> metrics) {
        if (metrics.isEmpty()) {
            logger.warn("No metrics to export");
            return;
        }

        // All metrics in the list should have the same timestamp and host
        Metric firstMetric = metrics.get(0);
        String json = metricsToJson(metrics, firstMetric.timestamp(), firstMetric.host());
        sendMetricsToServer(json);
    }

    private void sendMetricsToServer(String json) {
        logger.debug("Sending metrics to server: {}", json);
        Request request = new Request.Builder()
                .url(serverUrl)
                .header("Content-Type", "application/json")
                .post(RequestBody.create(json, MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = "";
                if (response.body() != null) {
                    errorBody = response.body().string();
                }
                logger.error("Failed to send metrics to server. Status: {}, Body: {}",
                    response.code(), errorBody);
            }
        } catch (IOException e) {
            logger.error("Failed to send metrics to server: {}", e.getMessage(), e);
        }
    }

    @NotNull
    private static String metricsToJson(List<Metric> metrics, long timestamp, String host) {
        Map<String, List<Metric>> metricsByName = metrics.stream()
            .collect(Collectors.groupingBy(Metric::name));

        StringJoiner metricsJson = new StringJoiner(",");
        for (Map.Entry<String, List<Metric>> entry : metricsByName.entrySet()) {
            String metricName = entry.getKey();
            StringJoiner valuesJson = new StringJoiner(",");

            for (Metric metric : entry.getValue()) {
                // Use English locale to ensure dot as decimal separator
                String value = String.format(Locale.ENGLISH, "%.1f", metric.value());
                // Remove .0 suffix for whole numbers
                if (value.endsWith(".0")) {
                    value = value.substring(0, value.length() - 2);
                }
                valuesJson.add(String.format("{\"t\":%d,\"v\":%s,\"u\":\"%s\"}",
                    metric.timestamp(), value, getMetricUnit(metric.name())));
            }
            metricsJson.add(String.format("\"%s\":[%s]", metricName, valuesJson));
        }

        return String.format("{\"host\":\"%s\",\"metrics\":{%s}}", host, metricsJson);
    }

    private static String getMetricUnit(String metricName) {
        return switch (metricName) {
            case "cpu" -> "%";
            case "memory" -> "MB";
            default -> "";
        };
    }
}
