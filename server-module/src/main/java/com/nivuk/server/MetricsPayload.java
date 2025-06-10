package com.nivuk.server;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class MetricsPayload {
    private List<MetricPoint> points;

    public static class MetricPoint {
        @JsonProperty("t")
        private long timestamp;
        @JsonProperty("h")
        private String hostName;
        @JsonProperty("n")
        private String metricName;
        @JsonProperty("v")
        private double value;
        @JsonProperty("u")
        private String unit;

        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public String getHostName() { return hostName; }
        public void setHostName(String hostName) { this.hostName = hostName; }
        public String getMetricName() { return metricName; }
        public void setMetricName(String metricName) { this.metricName = metricName; }
        public double getValue() { return value; }
        public void setValue(double value) { this.value = value; }
        public String getUnit() { return unit; }
        public void setUnit(String unit) { this.unit = unit; }

        @Override
        public String toString() {
            return String.format("%s=%s %s @ %d", metricName, value, unit, timestamp);
        }
    }

    public List<MetricPoint> getPoints() { return points; }
    public void setPoints(List<MetricPoint> points) { this.points = points; }
}
