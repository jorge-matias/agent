package com.nivuk.agent.config;

import com.nivuk.agent.collectors.*;
import com.nivuk.agent.exporters.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

public class AgentConfig {
    private static final Logger logger = LoggerFactory.getLogger(AgentConfig.class);
    private final Map<String, String> exporterProperties;
    private final Map<String, Boolean> collectorFlags;
    private final Map<String, Boolean> exporterFlags;
    private final int collectorIntervalSeconds;

    private AgentConfig(Map<String, Boolean> collectorFlags,
                       Map<String, Boolean> exporterFlags,
                       Map<String, String> exporterProperties,
                       int collectorIntervalSeconds) {
        this.collectorFlags = collectorFlags;
        this.exporterFlags = exporterFlags;
        this.exporterProperties = exporterProperties;
        this.collectorIntervalSeconds = collectorIntervalSeconds;
    }

    public int getCollectionIntervalSeconds() {
        return collectorIntervalSeconds;
    }

    @SuppressWarnings("unchecked")
    public static AgentConfig load() {
        Map<String, Object> config = loadYamlConfig();

        // Get interval seconds
        int intervalSeconds = (Integer) config.getOrDefault("intervalSeconds", 60);

        // Override with environment variable if present
        String envInterval = System.getenv("AGENT_COLLECTION_INTERVAL");
        if (envInterval != null && !envInterval.isEmpty()) {
            intervalSeconds = Integer.parseInt(envInterval);
        }

        // Load collector flags
        Map<String, Boolean> collectorFlags = new HashMap<>();
        Map<String, Object> collectors = (Map<String, Object>) config.getOrDefault("collectors", Map.of());
        collectorFlags.put("cpu", (Boolean) collectors.getOrDefault("cpu", false));
        collectorFlags.put("memory", (Boolean) collectors.getOrDefault("memory", false));

        // Load exporter flags and properties
        Map<String, Boolean> exporterFlags = new HashMap<>();
        Map<String, String> exporterProperties = new HashMap<>();
        Map<String, Object> exporters = (Map<String, Object>) config.getOrDefault("exporters", Map.of());

        // Handle logging exporter config
        if (exporters.containsKey("logging")) {
            Map<String, Object> logging = (Map<String, Object>) exporters.get("logging");
            exporterFlags.put("logging", (Boolean) logging.getOrDefault("enabled", false));
            if (logging.containsKey("bufferSeconds")) {
                exporterProperties.put("logging.bufferSeconds", String.valueOf(logging.get("bufferSeconds")));
            }
        } else {
            exporterFlags.put("logging", false);
        }

        // Handle webservice exporter config
        if (exporters.containsKey("webservice")) {
            Map<String, Object> webservice = (Map<String, Object>) exporters.get("webservice");
            exporterFlags.put("webservice", (Boolean) webservice.getOrDefault("enabled", false));
            if (webservice.containsKey("serverUrl")) {
                exporterProperties.put("serverUrl", (String) webservice.get("serverUrl"));
            }
            if (webservice.containsKey("bufferSeconds")) {
                exporterProperties.put("webservice.bufferSeconds", String.valueOf(webservice.get("bufferSeconds")));
            }
        } else {
            exporterFlags.put("webservice", false);
        }

        return new AgentConfig(collectorFlags, exporterFlags, exporterProperties, intervalSeconds);
    }

    private static Map<String, Object> loadYamlConfig() {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("agent-config.yml")) {
            if (input == null) {
                logger.warn("No agent-config.yml found, using defaults");
                return Map.of();
            }
            return new Yaml().load(input);
        } catch (Exception e) {
            logger.error("Error loading configuration", e);
            return Map.of();
        }
    }

    public List<Collector> createCollectors() {
        List<Collector> result = new ArrayList<>();

        if (collectorFlags.getOrDefault("cpu", false)) {
            result.add(new CpuCollector());
        }

        if (collectorFlags.getOrDefault("memory", false)) {
            result.add(new MemoryCollector());
        }

        return result;
    }

    public List<MetricsExporter> createExporters() {
        List<MetricsExporter> result = new ArrayList<>();

        if (exporterFlags.getOrDefault("logging", false)) {
            MetricsExporter exporter = new LoggingMetricsExporter();
            int bufferSeconds = Integer.parseInt(exporterProperties.getOrDefault("logging.bufferSeconds", "0"));
            if (bufferSeconds > 0) {
                exporter = new BufferedMetricsExporter(exporter, bufferSeconds);
            }
            result.add(exporter);
        }

        if (exporterFlags.getOrDefault("webservice", false)) {
            String serverUrl = exporterProperties.getOrDefault("serverUrl", "http://localhost:8080/metrics");
            MetricsExporter exporter = new WebServiceMetricsExporter(new okhttp3.OkHttpClient(), serverUrl);
            int bufferSeconds = Integer.parseInt(exporterProperties.getOrDefault("webservice.bufferSeconds", "0"));
            if (bufferSeconds > 0) {
                exporter = new BufferedMetricsExporter(exporter, bufferSeconds);
            }
            result.add(exporter);
        }

        return result;
    }
}
