package com.nivuk.agent.exporters;

import com.nivuk.agent.model.Metric;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
    void shouldSerializeMetricsCorrectly() throws IOException {
        // Given
        List<Metric> metrics = List.of(
            new Metric("cpu", 75.5, "p"),
            new Metric("mem_free", 1024.0, "m")
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
        List<Request> requests = httpClient.getRequests();
        assertEquals(1, requests.size());
        Request request = requests.get(0);

        assertEquals(serverUrl, request.url().toString());
        assertEquals("POST", request.method());

        MediaType expectedMediaType = MediaType.get("application/json; charset=utf-8");
        assertEquals(expectedMediaType, request.body().contentType());
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
