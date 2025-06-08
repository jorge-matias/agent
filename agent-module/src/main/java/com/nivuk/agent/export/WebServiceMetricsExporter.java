package com.nivuk.agent.export;

import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Map;

public class WebServiceMetricsExporter implements MetricsExporter {
    private static final Logger logger = LoggerFactory.getLogger(WebServiceMetricsExporter.class);
    private final OkHttpClient client;
    private final String serverUrl;

    public WebServiceMetricsExporter(OkHttpClient client, String serverUrl) {
        this.client = client;
        this.serverUrl = serverUrl;
    }

    @Override
    public void export(Map<String, Double> metrics) {
        String json = formatJson(metrics);
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

    private String formatJson(Map<String, Double> metrics) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Double> entry : metrics.entrySet()) {
            if (!first) {
                json.append(", ");
            }
            json.append(String.format("\"%s\": %s", entry.getKey(), entry.getValue()));
            first = false;
        }
        return json.append("}").toString();
    }
}
