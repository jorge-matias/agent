package com.nivuk.agent.exporters;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nivuk.agent.model.Metric;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static java.util.stream.Collectors.joining;

public class WebServiceMetricsExporter implements MetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(WebServiceMetricsExporter.class);
    private final OkHttpClient client;
    private final String serverUrl;

    public WebServiceMetricsExporter(OkHttpClient client, String serverUrl) {
        this.client = client;
        this.serverUrl = serverUrl;
    }

    @Override
    public void export(List<Metric> metrics) {
        if (metrics.isEmpty()) {
            logger.warn("No metrics to export");
            return;
        }

        Map<String, Double> metricsBuffer = new HashMap<>();
        metrics.forEach(metric -> metricsBuffer.put(metric.name(), metric.value()));
        sendMetricsToServer(metricsBuffer);
    }

    private void sendMetricsToServer(Map<String, Double> metrics) {
        String json = "{" +
            metrics.entrySet().stream()
                .map(entry -> String.format("\"%s\": %s", entry.getKey(), entry.getValue()))
                .collect(joining(", ")) +
            "}";

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(RequestBody.create(json, MediaType.get("application/json")))
                .build();

        try {
            Response response = client.newCall(request).execute();
            response.close();
        } catch (IOException e) {
            logger.error("Failed to send metrics to server: {}", e.getMessage(), e);
        }
    }
}
