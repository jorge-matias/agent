package com.nivuk.agent.config;

import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class AgentConfig {
    private static final Logger logger = LoggerFactory.getLogger(AgentConfig.class);

    private final String serverUrl;
    private final int collectionIntervalSeconds;
    private final boolean enableLoggingExporter;
    private final boolean enableWebServiceExporter;

    private AgentConfig(String serverUrl, int collectionIntervalSeconds,
                       boolean enableLoggingExporter, boolean enableWebServiceExporter) {
        this.serverUrl = serverUrl;
        this.collectionIntervalSeconds = collectionIntervalSeconds;
        this.enableLoggingExporter = enableLoggingExporter;
        this.enableWebServiceExporter = enableWebServiceExporter;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public int getCollectionIntervalSeconds() {
        return collectionIntervalSeconds;
    }

    public boolean isLoggingExporterEnabled() {
        return enableLoggingExporter;
    }

    public boolean isWebServiceExporterEnabled() {
        return enableWebServiceExporter;
    }

    public static AgentConfig load() {
        Map<String, Object> config = loadYamlConfig();
        return new AgentConfig(
            getConfigValue(config, "serverUrl", "AGENT_SERVER_URL", "http://localhost:8080/metrics"),
            Integer.parseInt(getConfigValue(config, "collectionIntervalSeconds", "AGENT_COLLECTION_INTERVAL", "60")),
            Boolean.parseBoolean(getConfigValue(config, "enableLoggingExporter", "AGENT_ENABLE_LOGGING", "true")),
            Boolean.parseBoolean(getConfigValue(config, "enableWebServiceExporter", "AGENT_ENABLE_WEBSERVICE", "true"))
        );
    }

    private static Map<String, Object> loadYamlConfig() {
        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream("agent-config.yml")) {
            if (input == null) {
                logger.warn("No agent-config.yml found, using defaults and environment variables");
                return Map.of();
            }
            Yaml yaml = new Yaml();
            return yaml.load(input);
        } catch (Exception e) {
            logger.error("Error loading configuration", e);
            return Map.of();
        }
    }

    private static String getConfigValue(Map<String, Object> yamlConfig, String yamlPath, String envVar, String defaultValue) {
        // First try environment variable
        String envValue = System.getenv(envVar);
        if (envValue != null && !envValue.isEmpty()) {
            logger.debug("Using environment variable {} value: {}", envVar, envValue);
            return envValue;
        }

        // Then try YAML config
        Object yamlValue = yamlConfig.get(yamlPath);
        if (yamlValue != null) {
            logger.debug("Using YAML config value for {}: {}", yamlPath, yamlValue);
            return String.valueOf(yamlValue);
        }

        // Finally use default
        logger.debug("Using default value for {}: {}", yamlPath, defaultValue);
        return defaultValue;
    }
}
