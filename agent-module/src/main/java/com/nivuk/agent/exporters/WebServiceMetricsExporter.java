package com.nivuk.agent.exporters;

import java.io.IOException;
import java.util.List;
import java.util.StringJoiner;

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
        Request request = new Request.Builder()
                .url(serverUrl)
                .post(RequestBody.create(json, MediaType.get("application/json")))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                logger.error("Failed to send metrics to server. Status: {}, Body: {}",
                        response.code(), response.body() != null ? response.body().string() : "empty");
            }
        } catch (IOException e) {
            logger.error("Failed to send metrics to server: {}", e.getMessage(), e);
        }
    }

    @NotNull
    private static String metricsToJson(List<Metric> metrics, long timestamp, String host) {
        StringJoiner joiner = new StringJoiner(",");
        for (Metric metric : metrics) {
            // Use compact format with minimal decimal places
            String value = String.format("%.1f", metric.value());
            // Remove .0 suffix for whole numbers
            if (value.endsWith(".0")) {
                value = value.substring(0, value.length() - 2);
            }
            joiner.add(String.format("\"%s\":%s", metric.name(), value));
        }
        return String.format("{\"t\":%d,\"h\":\"%s\",\"m\":{%s}}", timestamp, host, joiner);
    }
}
