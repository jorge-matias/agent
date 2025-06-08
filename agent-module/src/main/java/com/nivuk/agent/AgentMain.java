package com.nivuk.agent;

import java.util.Arrays;
import java.util.Timer;

import com.nivuk.agent.collectors.CpuCollector;
import com.nivuk.agent.collectors.MemoryCollector;
import com.nivuk.agent.exporters.LoggingMetricsExporter;
import com.nivuk.agent.exporters.WebServiceMetricsExporter;

import okhttp3.OkHttpClient;

public class AgentMain {
    public static void main(String[] args) {
        String serverUrl = System.getenv().getOrDefault("SERVER_URL", "http://server-module:8080/metrics");
        OkHttpClient client = new OkHttpClient();

        MetricsCollectionJob job = new MetricsCollectionJob(
            Arrays.asList(new CpuCollector(), new MemoryCollector()),
            Arrays.asList(
                new WebServiceMetricsExporter(client, serverUrl),
                new LoggingMetricsExporter()
            )
        );

        new Timer().schedule(job, 0, 1000);
    }
}
