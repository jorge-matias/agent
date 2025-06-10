package com.nivuk.agent;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.nivuk.agent.collectors.Collector;
import com.nivuk.agent.collectors.CpuCollector;
import com.nivuk.agent.collectors.MemoryCollector;
import com.nivuk.agent.exporters.MetricsExporter;
import com.nivuk.agent.exporters.WebServiceMetricsExporter;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MetricsIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void shouldCollectAndExportMetrics() throws Exception {
        // Configure WireMock
        wireMock.stubFor(post(urlEqualTo("/metrics"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)));

        // Create collectors and exporter
        List<Collector> collectors = List.of(
                new MemoryCollector(),
                new CpuCollector()
        );

        String serverUrl = wireMock.baseUrl() + "/metrics";
        MetricsExporter exporter = new WebServiceMetricsExporter(new OkHttpClient(), serverUrl);

        // Run a collection cycle
        MetricsCollectionTask task = new MetricsCollectionTask(collectors, exporter);
        task.run();

        // Allow some time for the request to be processed
        TimeUnit.MILLISECONDS.sleep(500);

        // Verify the metrics were sent correctly
        wireMock.verify(postRequestedFor(urlEqualTo("/metrics"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.t", matching("\\d+"))) // timestamp
                .withRequestBody(matchingJsonPath("$.h", matching(".+"))) // hostname
                .withRequestBody(matchingJsonPath("$.m.cpu", matching("\\d+(\\.\\d+)?"))) // CPU percentage
                .withRequestBody(matchingJsonPath("$.m.mem_free", matching("\\d+(\\.\\d+)?"))) // Free memory
                .withRequestBody(matchingJsonPath("$.m.mem_total", matching("\\d+(\\.\\d+)?")))); // Total memory

        // Verify we got exactly one request
        assertEquals(1, wireMock.getAllServeEvents().size());
    }

    @Test
    void shouldHandleServerError() throws Exception {
        // Configure WireMock to return a 500 error
        wireMock.stubFor(post(urlEqualTo("/metrics"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Create collectors and exporter
        List<Collector> collectors = List.of(
                new MemoryCollector(),
                new CpuCollector()
        );

        String serverUrl = wireMock.baseUrl() + "/metrics";
        MetricsExporter exporter = new WebServiceMetricsExporter(new OkHttpClient(), serverUrl);

        // Run a collection cycle
        MetricsCollectionTask task = new MetricsCollectionTask(collectors, exporter);
        task.run();

        // Allow some time for the request to be processed
        TimeUnit.MILLISECONDS.sleep(500);

        // Verify we got exactly one request (even though it failed)
        assertEquals(1, wireMock.getAllServeEvents().size());
    }

    @Test
    void shouldHandleServerTimeout() throws Exception {
        // Configure WireMock to delay response
        wireMock.stubFor(post(urlEqualTo("/metrics"))
                .willReturn(aResponse()
                        .withFixedDelay(5000) // 5 second delay
                        .withStatus(200)));

        // Create collectors and exporter with a short timeout
        List<Collector> collectors = List.of(
                new MemoryCollector(),
                new CpuCollector()
        );

        OkHttpClient client = new OkHttpClient.Builder()
                .callTimeout(1, TimeUnit.SECONDS)
                .build();

        String serverUrl = wireMock.baseUrl() + "/metrics";
        MetricsExporter exporter = new WebServiceMetricsExporter(client, serverUrl);

        // Run a collection cycle
        MetricsCollectionTask task = new MetricsCollectionTask(collectors, exporter);
        task.run();

        // Allow some time for the request to be processed
        TimeUnit.MILLISECONDS.sleep(500);

        // Verify we got exactly one request attempt
        assertEquals(1, wireMock.getAllServeEvents().size());
    }
}
