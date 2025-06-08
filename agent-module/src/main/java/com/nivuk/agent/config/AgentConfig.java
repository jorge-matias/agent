package com.nivuk.agent.config;

import com.nivuk.agent.collectors.Collector;
import com.nivuk.agent.exporters.MetricsExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.*;

public class AgentConfig {
    private static final Logger logger = LoggerFactory.getLogger(AgentConfig.class);
    private final List<ComponentConfig> exporters;
    private final List<ComponentConfig> collectors;
    private final int collectorIntervalSeconds;

    public static class ComponentConfig {
        private final String className;
        private final Map<String, String> properties;

        public ComponentConfig(String className, Map<String, String> properties) {
            this.className = className;
            this.properties = properties != null ? properties : Map.of();
        }

        public String getClassName() {
            return className;
        }

        public Map<String, String> getProperties() {
            return properties;
        }
    }

    private AgentConfig(List<ComponentConfig> exporters, List<ComponentConfig> collectors,
                       int collectorIntervalSeconds) {
        this.exporters = exporters;
        this.collectors = collectors;
        this.collectorIntervalSeconds = collectorIntervalSeconds;
    }

    public int getCollectionIntervalSeconds() {
        return collectorIntervalSeconds;
    }

    @SuppressWarnings("unchecked")
    public static AgentConfig load() {
        Map<String, Object> config = loadYamlConfig();
        List<Map<String, Object>> collectorsConfig = (List<Map<String, Object>>)
            config.getOrDefault("collectors", List.of());

        // Find intervalSeconds in collectors configuration
        int intervalSeconds = 60; // default value
        List<ComponentConfig> collectors = new ArrayList<>();

        for (Map<String, Object> collectorConfig : collectorsConfig) {
            if (collectorConfig.containsKey("intervalSeconds")) {
                intervalSeconds = Integer.parseInt(String.valueOf(collectorConfig.get("intervalSeconds")));
                continue;
            }

            String className = (String) collectorConfig.get("class");
            if (className != null) {
                collectors.add(new ComponentConfig(className, Map.of()));
            }
        }

        // Override with environment variable if present
        String envInterval = System.getenv("AGENT_COLLECTION_INTERVAL");
        if (envInterval != null && !envInterval.isEmpty()) {
            intervalSeconds = Integer.parseInt(envInterval);
        }

        return new AgentConfig(
            loadComponents(config, "exporters"),
            collectors,
            intervalSeconds
        );
    }

    @SuppressWarnings("unchecked")
    private static List<ComponentConfig> loadComponents(Map<String, Object> config, String key) {
        List<ComponentConfig> components = new ArrayList<>();
        List<Map<String, Object>> componentConfigs = (List<Map<String, Object>>) config.getOrDefault(key, List.of());

        for (Map<String, Object> componentConfig : componentConfigs) {
            String className = (String) componentConfig.get("class");
            Map<String, String> properties = new HashMap<>();

            Map<String, Object> configProperties = (Map<String, Object>) componentConfig.getOrDefault("properties", Map.of());
            configProperties.forEach((k, v) -> properties.put(k, String.valueOf(v)));

            components.add(new ComponentConfig(className, properties));
        }

        return components;
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
        for (ComponentConfig config : collectors) {
            try {
                Class<?> clazz = Class.forName(config.getClassName());
                if (Collector.class.isAssignableFrom(clazz)) {
                    result.add((Collector) clazz.getDeclaredConstructor().newInstance());
                } else {
                    logger.error("Class {} is not a Collector", config.getClassName());
                }
            } catch (Exception e) {
                logger.error("Failed to create collector {}", config.getClassName(), e);
            }
        }
        return result;
    }

    public List<MetricsExporter> createExporters() {
        List<MetricsExporter> result = new ArrayList<>();
        for (ComponentConfig config : exporters) {
            try {
                Class<?> clazz = Class.forName(config.getClassName());
                if (MetricsExporter.class.isAssignableFrom(clazz)) {
                    if (config.getClassName().contains("WebServiceMetricsExporter")) {
                        String serverUrl = config.getProperties().getOrDefault("serverUrl",
                            "http://localhost:8080/metrics");
                        result.add((MetricsExporter) clazz.getDeclaredConstructor(okhttp3.OkHttpClient.class,
                            String.class).newInstance(new okhttp3.OkHttpClient(), serverUrl));
                    } else {
                        result.add((MetricsExporter) clazz.getDeclaredConstructor().newInstance());
                    }
                } else {
                    logger.error("Class {} is not a MetricsExporter", config.getClassName());
                }
            } catch (Exception e) {
                logger.error("Failed to create exporter {}", config.getClassName(), e);
            }
        }
        return result;
    }
}
