package com.nivuk.agent;

import okhttp3.*;
import java.util.Timer;
import java.util.Arrays;

public class AgentMain {
    public static void main(String[] args) {
        String serverUrl = System.getenv().getOrDefault("SERVER_URL", "http://server-module:8080/metrics");
        OkHttpClient client = new OkHttpClient();

        MetricsCollectorTask task = new MetricsCollectorTask(
            client,
            serverUrl,
            Arrays.asList(new CpuCollector(), new MemoryCollector())
        );

        new Timer().schedule(task, 0, 1000);
    }
}
