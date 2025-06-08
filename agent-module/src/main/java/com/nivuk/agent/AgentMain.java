package com.nivuk.agent;

import com.nivuk.agent.export.LoggingMetricsExporter;
import com.nivuk.agent.export.WebServiceMetricsExporter;
import com.nivuk.collectors.CpuCollector;
import com.nivuk.collectors.MemoryCollector;
import okhttp3.OkHttpClient;
import java.util.Arrays;
import java.util.Timer;

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
