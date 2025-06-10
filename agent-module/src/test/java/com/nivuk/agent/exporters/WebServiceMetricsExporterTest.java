package com.nivuk.agent.exporters;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nivuk.agent.model.Metric;

import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class WebServiceMetricsExporterTest {
    private TestHttpClient httpClient;
    private WebServiceMetricsExporter exporter;
    private final String serverUrl = "http://localhost:8080/metrics";

    @BeforeEach
    void setUp() {
        httpClient = new TestHttpClient();
        exporter = new WebServiceMetricsExporter(httpClient, serverUrl);
    }

    @Test
    void shouldSerializeMetricsInTimeSeriesFormat() {
        // Given
        long timestamp = 1686394800000L;
        List<Metric> metrics = List.of(
            new Metric("cpu", 75.5, "%", "test-host", timestamp),
            new Metric("memory", 1024.0, "MB", "test-host", timestamp)
        );

        Response response = new Response.Builder()
            .request(new Request.Builder().url(serverUrl).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create("", MediaType.get("application/json")))
            .build();
        httpClient.setNextResponse(response);

        // When
        exporter.export(metrics);

        // Then
        List<TestHttpClient.RequestWrapper> requests = httpClient.getRequests();
        assertEquals(1, requests.size());
        TestHttpClient.RequestWrapper request = requests.get(0);

        assertEquals(serverUrl, request.url().toString());
        assertEquals("POST", request.method());
        assertEquals(MediaType.get("application/json; charset=utf-8"), request.body().contentType());

        String json = request.toString();

        assertTrue(json.contains("\"points\":["), "Should contain points array");
        assertTrue(json.contains(String.format("\"t\":%d", timestamp)), "Should contain timestamp");
        assertTrue(json.contains("\"h\":\"test-host\""), "Should contain host");
        assertTrue(json.contains("\"n\":\"cpu\""), "Should contain CPU metric name");
        assertTrue(json.contains("\"v\":75.5"), "Should contain CPU value");
        assertTrue(json.contains("\"u\":\"%\""), "Should contain CPU unit");
        assertTrue(json.contains("\"n\":\"memory\""), "Should contain memory metric name");
        assertTrue(json.contains("\"v\":1024"), "Should contain memory value");
        assertTrue(json.contains("\"u\":\"MB\""), "Should contain memory unit");
    }

    @Test
    void shouldHandleConsecutiveMetricsCorrectly() {
        // Given
        long timestamp1 = 1686394800000L;
        long timestamp2 = 1686394801000L;
        List<Metric> batch1 = List.of(
            new Metric("cpu", 75.5, "%", "test-host", timestamp1)
        );
        List<Metric> batch2 = List.of(
            new Metric("cpu", 80.0, "%", "test-host", timestamp2)
        );

        Response response = new Response.Builder()
            .request(new Request.Builder().url(serverUrl).build())
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create("", MediaType.get("application/json")))
            .build();
        httpClient.setNextResponse(response);

        // When (using non-batching mode for testing)
        exporter.export(batch1);
        exporter.export(batch2);

        // Then
        List<TestHttpClient.RequestWrapper> requests = httpClient.getRequests();
        assertEquals(2, requests.size());

        String json1 = requests.get(0).toString();
        assertTrue(json1.contains(String.format("\"t\":%d", timestamp1)), "JSON should contain first timestamp");

        String json2 = requests.get(1).toString();
        assertTrue(json2.contains(String.format("\"t\":%d", timestamp2)), "JSON should contain second timestamp");
    }

    @Test
    void shouldHandleEmptyMetricsList() {
        // Given
        List<Metric> metrics = List.of();

        // When & Then
        exporter.export(metrics);
        assertTrue(httpClient.getRequests().isEmpty());
    }

    @Test
    void shouldHandleFailedRequest() {
        // Given
        List<Metric> metrics = List.of(new Metric("cpu", 75.5, "p"));
        Response response = new Response.Builder()
            .request(new Request.Builder().url(serverUrl).build())
            .protocol(Protocol.HTTP_1_1)
            .code(500)
            .message("Server Error")
            .body(ResponseBody.create("Server Error", MediaType.get("text/plain")))
            .build();
        httpClient.setNextResponse(response);

        // When & Then (should not throw exception)
        exporter.export(metrics);
        assertEquals(1, httpClient.getRequests().size());
    }

    @Test
    void shouldHandleIOException() {
        // Given
        List<Metric> metrics = List.of(new Metric("cpu", 75.5, "p"));
        httpClient.setNextError(new IOException("Network error"));

        // When & Then (should not throw exception)
        exporter.export(metrics);
        assertEquals(1, httpClient.getRequests().size());
    }
}
