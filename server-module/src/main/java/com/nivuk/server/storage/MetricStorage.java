package com.nivuk.server.storage;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class MetricStorage {
    // Host -> MetricName -> SortedMap<Timestamp, MetricValue>
    private final Map<String, Map<String, ConcurrentSkipListMap<Long, MetricValue>>> storage;
    private final Duration retentionPeriod;
    private final Duration aggregationInterval;

    public MetricStorage() {
        this(Duration.ofHours(24), Duration.ofMinutes(5));
    }

    public MetricStorage(Duration retentionPeriod, Duration aggregationInterval) {
        this.storage = new ConcurrentHashMap<>();
        this.retentionPeriod = retentionPeriod;
        this.aggregationInterval = aggregationInterval;
    }

    public static class MetricValue {
        private final double value;
        private final String unit;

        public MetricValue(double value, String unit) {
            this.value = value;
            this.unit = unit;
        }

        public double getValue() { return value; }
        public String getUnit() { return unit; }
    }

    public void addMetrics(String host, String metricName, long timestamp, double value, String unit) {
        Map<String, ConcurrentSkipListMap<Long, MetricValue>> hostMetrics =
            storage.computeIfAbsent(host, h -> new ConcurrentHashMap<>());

        ConcurrentSkipListMap<Long, MetricValue> timeseriesData =
            hostMetrics.computeIfAbsent(metricName, m -> new ConcurrentSkipListMap<>());

        timeseriesData.put(timestamp, new MetricValue(value, unit));

        // Cleanup old data and aggregate if needed
        cleanupAndAggregate(timeseriesData);
    }

    private void cleanupAndAggregate(ConcurrentSkipListMap<Long, MetricValue> timeseries) {
        long now = System.currentTimeMillis();
        long retentionThreshold = now - retentionPeriod.toMillis();

        // Remove old data
        timeseries.headMap(retentionThreshold).clear();

        // Aggregate data older than aggregationInterval into 5-minute buckets
        long aggregationThreshold = now - aggregationInterval.toMillis();
        Map<Long, List<MetricValue>> aggregationBuckets = timeseries.headMap(aggregationThreshold)
            .entrySet()
            .stream()
            .collect(Collectors.groupingBy(
                e -> e.getKey() - (e.getKey() % aggregationInterval.toMillis()),
                Collectors.mapping(Map.Entry::getValue, Collectors.toList())
            ));

        // Calculate averages for each bucket
        aggregationBuckets.forEach((bucketTime, values) -> {
            double avgValue = values.stream()
                .mapToDouble(MetricValue::getValue)
                .average()
                .orElse(0.0);
            String unit = values.get(0).getUnit();

            // Replace individual values with aggregated value
            values.forEach(v -> timeseries.remove(bucketTime));
            timeseries.put(bucketTime, new MetricValue(avgValue, unit));
        });
    }

    public Map<Long, MetricValue> getMetrics(String host, String metricName, long fromTimestamp, long toTimestamp) {
        return storage
            .getOrDefault(host, Collections.emptyMap())
            .getOrDefault(metricName, new ConcurrentSkipListMap<>())
            .subMap(fromTimestamp, toTimestamp);
    }

    public Map<String, Map<Long, MetricValue>> getMetricsByHost(String host, long fromTimestamp, long toTimestamp) {
        return storage
            .getOrDefault(host, Collections.emptyMap())
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> new TreeMap<>(e.getValue().subMap(fromTimestamp, toTimestamp))
            ));
    }

    public Map<String, Map<String, Map<Long, MetricValue>>> getAggregatedView(
            Function<List<MetricValue>, MetricValue> aggregator,
            Duration bucketSize
    ) {
        Map<String, Map<String, Map<Long, MetricValue>>> result = new HashMap<>();

        storage.forEach((host, metrics) -> {
            Map<String, Map<Long, MetricValue>> hostResult = new HashMap<>();

            metrics.forEach((metricName, timeseries) -> {
                Map<Long, List<MetricValue>> buckets = new HashMap<>();

                timeseries.forEach((timestamp, value) -> {
                    long bucket = timestamp - (timestamp % bucketSize.toMillis());
                    buckets.computeIfAbsent(bucket, k -> new ArrayList<>()).add(value);
                });

                Map<Long, MetricValue> aggregated = buckets.entrySet().stream()
                    .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> aggregator.apply(e.getValue())
                    ));

                hostResult.put(metricName, aggregated);
            });

            result.put(host, hostResult);
        });

        return result;
    }
}
