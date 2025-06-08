package com.example.agent;

import okhttp3.*;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.util.Timer;
import java.util.TimerTask;

public class AgentMain {
    public static void main(String[] args) {
        String serverUrl = System.getenv().getOrDefault("SERVER_URL", "http://server-module:8080/metrics");

        OkHttpClient client = new OkHttpClient();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                double cpuLoad = osBean.getSystemLoadAverage();
                long usedMemory = memBean.getHeapMemoryUsage().getUsed();

                String json = String.format("{\"cpu\": %.2f, \"memory\": %d}", cpuLoad, usedMemory);

                Request request = new Request.Builder()
                        .url(serverUrl)
                        .post(RequestBody.create(MediaType.get("application/json"), json))
                        .build();

                try {
                    Response response = client.newCall(request).execute();
                    response.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 0, 5000); // Run every 5 seconds
    }
}
