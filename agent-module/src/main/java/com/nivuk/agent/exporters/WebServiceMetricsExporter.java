package com.nivuk.agent.exporters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nivuk.agent.model.Metric;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class WebServiceMetricsExporter implements MetricsExporter, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(WebServiceMetricsExporter.class);
    private final HttpClient client;
    private final String serverUrl;
    private final Map<String, Map<String, List<Metric>>> batchedMetrics = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long BATCH_INTERVAL_SECONDS = 10;
    private final boolean batchingEnabled;
    private final MetricJsonFormatter formatter;

    public WebServiceMetricsExporter(OkHttpClient okHttpClient, String serverUrl) {
        this(new OkHttpClientWrapper(okHttpClient), serverUrl, true, new MetricJsonFormatter());
    }

    WebServiceMetricsExporter(HttpClient client, String serverUrl) {
        this(client, serverUrl, false, new MetricJsonFormatter()); // For testing, disable batching
    }

    private WebServiceMetricsExporter(HttpClient client, String serverUrl, boolean batchingEnabled, MetricJsonFormatter formatter) {
        this.client = client;
        this.serverUrl = serverUrl;
        this.batchingEnabled = batchingEnabled;
        this.formatter = formatter;
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
            sendMetricsToServer(formatter.format(metrics));
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
            List<Metric> metricsList = metrics.values().stream()
                .flatMap(List::stream)
                .toList();
            sendMetricsToServer(formatter.format(metricsList));
        });
        batchedMetrics.clear();
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
