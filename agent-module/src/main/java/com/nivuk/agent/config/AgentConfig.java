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
    private final int collectionIntervalSeconds;
    private final List<ComponentConfig> exporters;
    private final List<ComponentConfig> collectors;

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

    private AgentConfig(int collectionIntervalSeconds, List<ComponentConfig> exporters,
                       List<ComponentConfig> collectors) {
        this.collectionIntervalSeconds = collectionIntervalSeconds;
        this.exporters = exporters;
        this.collectors = collectors;
    }

    public int getCollectionIntervalSeconds() {
        return collectionIntervalSeconds;
    }

    public static AgentConfig load() {
        Map<String, Object> config = loadYamlConfig();
        String intervalStr = getConfigValue(config, "collectionIntervalSeconds",
            "AGENT_COLLECTION_INTERVAL", "60");

        return new AgentConfig(
            Integer.parseInt(intervalStr),
            loadComponents(config, "exporters"),
            loadComponents(config, "collectors")
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

    private static String getConfigValue(Map<String, Object> yamlConfig, String yamlPath,
                                      String envVar, String defaultValue) {
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            logger.debug("Using environment variable {} value: {}", envVar, envValue);
            return envValue;
        }

        Object yamlValue = yamlConfig.get(yamlPath);
        if (yamlValue != null) {
            logger.debug("Using YAML config value for {}: {}", yamlPath, yamlValue);
            return String.valueOf(yamlValue);
        }

        logger.debug("Using default value for {}: {}", yamlPath, defaultValue);
        return defaultValue;
    }
}
