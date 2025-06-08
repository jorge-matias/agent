package com.nivuk.agent.exporters;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

        Map<String, Double> metricsMap = new HashMap<>();
        metrics.forEach(metric -> metricsMap.put(metric.name(), metric.value()));
        sendMetricsToServer(metricsMap);
    }

    private void sendMetricsToServer(Map<String, Double> metrics) {
        String json = metricsToJson(metrics);
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

    @NotNull
    private static String metricsToJson(Map<String, Double> metrics) {
        StringJoiner joiner = new StringJoiner(",");
        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            String format = String.format("\n                \"%s\": %s", entry.getKey(), entry.getValue());
            joiner.add(format);
        }
        return String.format("""
            {
              "timestamp": %d,
              "host": "%s",
              "metrics": {%s
              }
            }""",
            Metric.getCurrentTimestamp(),
            Metric.getHostname(),
                joiner.toString()
        );
    }
}
