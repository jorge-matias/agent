package com.nivuk.agent.exporters;

import java.io.IOException;

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
    public void export(Metric metric) {
        String json = String.format("{\"%s\": %s}", metric.name(), metric.value());

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
