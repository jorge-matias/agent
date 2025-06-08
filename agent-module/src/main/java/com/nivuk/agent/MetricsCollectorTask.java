package com.nivuk.agent;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.TimerTask;

public class MetricsCollectorTask extends TimerTask {
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollectorTask.class);
    private final OkHttpClient client;
    private final String serverUrl;
    private final List<Collector> collectors;

    public MetricsCollectorTask(OkHttpClient client, String serverUrl, List<Collector> collectors) {
        this.client = client;
        this.serverUrl = serverUrl;
        this.collectors = collectors;
    }

    @Override
    public void run() {
        String json = "{" + collectors.stream()
            .map(collector -> {
                MetricValue metric = collector.collect();
                return String.format("\"%s\": %s", metric.name(), metric.value());
            })
            .collect(Collectors.joining(", ")) + "}";

        Request request = new Request.Builder()
                .url(serverUrl)
                .post(RequestBody.create(MediaType.get("application/json"), json))
                .build();

        try {
            Response response = client.newCall(request).execute();
            response.close();
        } catch (IOException e) {
            logger.warn("Failed to send metrics to server: {}", e.getMessage(), e);
        }
    }
}
