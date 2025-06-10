package com.nivuk.server;

import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@Component
public class MetricsStorage {
    // Host -> MetricName -> TimeSeries
    private final Map<String, Map<String, ConcurrentSkipListMap<Long, MetricsPayload.MetricPoint>>> storage = new ConcurrentHashMap<>();
    private final Set<String> hosts = ConcurrentHashMap.newKeySet();
    private final Set<String> metricNames = ConcurrentHashMap.newKeySet();

    public void addMetric(MetricsPayload.MetricPoint point) {
        hosts.add(point.getHostName());
        metricNames.add(point.getMetricName());

        storage.computeIfAbsent(point.getHostName(), k -> new ConcurrentHashMap<>())
               .computeIfAbsent(point.getMetricName(), k -> new ConcurrentSkipListMap<>())
               .put(point.getTimestamp(), point);
    }

    public List<MetricsPayload.MetricPoint> queryMetrics(String host, String metricName, long fromTime, long toTime) {
        if (host != null && metricName != null) {
            // Query specific host and metric
            return storage.getOrDefault(host, Collections.emptyMap())
                    .getOrDefault(metricName, new ConcurrentSkipListMap<>())
                    .subMap(fromTime, toTime)
                    .values()
                    .stream()
                    .toList();
        } else if (host != null) {
            // Query all metrics for a host
            return storage.getOrDefault(host, Collections.emptyMap())
                    .values()
                    .stream()
                    .map(series -> series.subMap(fromTime, toTime))
                    .flatMap(map -> map.values().stream())
                    .toList();
        } else if (metricName != null) {
            // Query a specific metric across all hosts
            return storage.values()
                    .stream()
                    .map(hostMetrics -> hostMetrics.getOrDefault(metricName, new ConcurrentSkipListMap<>()))
                    .map(series -> series.subMap(fromTime, toTime))
                    .flatMap(map -> map.values().stream())
                    .toList();
        } else {
            // Query all metrics from all hosts
            return storage.values()
                    .stream()
                    .flatMap(hostMetrics -> hostMetrics.values().stream())
                    .map(series -> series.subMap(fromTime, toTime))
                    .flatMap(map -> map.values().stream())
                    .toList();
        }
    }

    public Set<String> getHosts() {
        return Collections.unmodifiableSet(hosts);
    }

    public Set<String> getMetricNames() {
        return Collections.unmodifiableSet(metricNames);
    }
}
