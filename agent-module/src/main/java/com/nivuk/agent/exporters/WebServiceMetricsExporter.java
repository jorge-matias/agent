package com.nivuk.agent.exporters;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.nivuk.agent.model.Metric;
import okhttp3.*;

public class WebServiceMetricsExporter implements MetricsExporter, AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(WebServiceMetricsExporter.class);
    private final HttpClient client;
    private final String serverUrl;
    private final Map<String, List<Metric>> batchedMetrics = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final long BATCH_INTERVAL_SECONDS = 10;
    private final boolean batchingEnabled;
    private final MetricJsonFormatter formatter;

    public WebServiceMetricsExporter(OkHttpClient okHttpClient, String serverUrl) {
        this(new OkHttpClientWrapper(okHttpClient), serverUrl, true);
    }

    WebServiceMetricsExporter(HttpClient client, String serverUrl) {
        this(client, serverUrl, false);
    }

    private WebServiceMetricsExporter(HttpClient client, String serverUrl, boolean batchingEnabled) {
        this.client = client;
        this.serverUrl = serverUrl;
        this.batchingEnabled = batchingEnabled;
        this.formatter = new MetricJsonFormatter();
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
            sendMetricsToServer(metrics);
            return;
        }

        // Production batching mode
        metrics.forEach(metric ->
            batchedMetrics.computeIfAbsent(metric.host(), k -> Collections.synchronizedList(new ArrayList<>()))
                .add(metric)
        );
    }

    private void flushMetrics() {
        if (batchedMetrics.isEmpty()) {
            return;
        }

        List<Metric> allMetrics = batchedMetrics.values().stream()
            .flatMap(List::stream)
            .toList();

        sendMetricsToServer(allMetrics);
        batchedMetrics.clear();
    }

    private void sendMetricsToServer(List<Metric> metrics) {
        String json = formatter.format(metrics);
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
            logger.error("Network error while sending metrics to server: {}", e.getMessage(), e);
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
