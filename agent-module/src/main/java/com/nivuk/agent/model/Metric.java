package com.nivuk.agent.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;

public record Metric(String name, double value, String unit) {
    private static final Logger logger = LoggerFactory.getLogger(Metric.class);
    private static final String hostname = determineHostname();

    private static String determineHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.warn("Could not determine hostname, using 'unknown'", e);
            return "unknown";
        }
    }

    public static String getHostname() {
        return hostname;
    }

    public static long getCurrentTimestamp() {
        return System.currentTimeMillis() / 1000;
    }
}
