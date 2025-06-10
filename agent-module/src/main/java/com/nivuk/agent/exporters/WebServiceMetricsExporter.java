package com.nivuk.agent.exporters;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nivuk.agent.model.Metric;
import okhttp3.*;

public class WebServiceMetricsExporter implements MetricsExporter, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(WebServiceMetricsExporter.class);
    private final HttpClient client;
    private final String serverUrl;
    private final Map<String, Map<String, List<Metric>>> batchedMetrics = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long BATCH_INTERVAL_SECONDS = 10;
    private final boolean batchingEnabled;

    public WebServiceMetricsExporter(OkHttpClient okHttpClient, String serverUrl) {
        this(new OkHttpClientWrapper(okHttpClient), serverUrl, true);
    }

    WebServiceMetricsExporter(HttpClient client, String serverUrl) {
        this(client, serverUrl, false); // For testing, disable batching
    }

    private WebServiceMetricsExporter(HttpClient client, String serverUrl, boolean batchingEnabled) {
        this.client = client;
        this.serverUrl = serverUrl;
        this.batchingEnabled = batchingEnabled;
        if (batchingEnabled) {
            scheduler.scheduleAtFixedRate(this::flushMetrics, BATCH_INTERVAL_SECONDS, BATCH_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    @Override
    public void export(List<Metric> metrics) {
        if (metrics.isEmpty()) {
            return;
        }

        if (!batchingEnabled) {
            // For testing: send immediately
            String host = metrics.get(0).host();
            Map<String, List<Metric>> metricsByName = new HashMap<>();
            metrics.forEach(metric ->
                metricsByName.computeIfAbsent(metric.name(), k -> new ArrayList<>()).add(metric)
            );
            sendMetricsToServer(createBatchJson(host, metricsByName));
            return;
        }

        // Production batching mode
        String host = metrics.get(0).host();
        metrics.forEach(metric -> {
            batchedMetrics.computeIfAbsent(host, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(metric.name(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(metric);
        });
    }

    private void flushMetrics() {
        if (batchedMetrics.isEmpty()) {
            return;
        }

        batchedMetrics.forEach((host, metrics) -> {
            String json = createBatchJson(host, metrics);
            sendMetricsToServer(json);
        });
        batchedMetrics.clear();
    }

    @NotNull
    private static String createBatchJson(String host, Map<String, List<Metric>> metricsByName) {
        StringJoiner metricsJson = new StringJoiner(",");

        for (Map.Entry<String, List<Metric>> entry : metricsByName.entrySet()) {
            String metricName = entry.getKey();
            StringJoiner valuesJson = new StringJoiner(",");

            for (Metric metric : entry.getValue()) {
                String value = String.format(Locale.ENGLISH, "%.1f", metric.value());
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
            case "mem_total", "mem_free" -> "MB";
            default -> "";
        };
    }

    private void sendMetricsToServer(String json) {
        logger.debug("Sending batched metrics to server: {}", json);
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

    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
