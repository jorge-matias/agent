package com.nivuk.agent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;

import com.nivuk.agent.collectors.CpuCollector;
import com.nivuk.agent.collectors.MemoryCollector;
import com.nivuk.agent.config.AgentConfig;
import com.nivuk.agent.exporters.LoggingMetricsExporter;
import com.nivuk.agent.exporters.MetricsExporter;
import com.nivuk.agent.exporters.WebServiceMetricsExporter;

import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentMain {
    private static final Logger logger = LoggerFactory.getLogger(AgentMain.class);

    public static void main(String[] args) {
        AgentConfig config = AgentConfig.load();
        logger.info("Starting agent with configuration: collection interval={}s, server URL={}",
            config.getCollectionIntervalSeconds(), config.getServerUrl());

        List<MetricsExporter> exporters = new ArrayList<>();
        OkHttpClient client = new OkHttpClient();

        if (config.isLoggingExporterEnabled()) {
            exporters.add(new LoggingMetricsExporter());
        }
        if (config.isWebServiceExporterEnabled()) {
            exporters.add(new WebServiceMetricsExporter(client, config.getServerUrl()));
        }

        MetricsCollectionJob job = new MetricsCollectionJob(
            Arrays.asList(new CpuCollector(), new MemoryCollector()),
            exporters
        );

        new Timer().schedule(job, 0, config.getCollectionIntervalSeconds() * 1000L);
    }
}
