package com.nivuk.agent.collectors;

import com.nivuk.agent.model.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CpuCollectorTest {

    @Mock
    private SystemInfoProvider systemInfo;

    private CpuCollector cpuCollector;

    @BeforeEach
    void setUp() {
        cpuCollector = new CpuCollector(systemInfo);
    }

    @Test
    void shouldReturnValidCpuMetric() {
        // Given
        when(systemInfo.getCpuLoad()).thenReturn(0.75); // 75% CPU load

        // When
        List<Metric> metrics = cpuCollector.collect();

        // Then
        assertNotNull(metrics);
        assertEquals(1, metrics.size());

        Metric cpuMetric = metrics.get(0);
        assertEquals("cpu", cpuMetric.name());
        assertEquals(75.0, cpuMetric.value(), 0.1);
        assertEquals("p", cpuMetric.unit());
    }

    @Test
    void shouldHandleNaN() {
        // Given
        when(systemInfo.getCpuLoad()).thenReturn(Double.NaN);

        // When
        List<Metric> metrics = cpuCollector.collect();

        // Then
        assertNotNull(metrics);
        assertEquals(1, metrics.size());

        Metric cpuMetric = metrics.get(0);
        assertEquals(0.0, cpuMetric.value(), 0.1);
    }

    @Test
    void shouldClampValuesBetweenZeroAndHundred() {
        // Given
        when(systemInfo.getCpuLoad()).thenReturn(1.5); // 150% should be clamped to 100%

        // When
        List<Metric> metrics = cpuCollector.collect();

        // Then
        assertNotNull(metrics);
        assertEquals(1, metrics.size());

        Metric cpuMetric = metrics.get(0);
        assertEquals(100.0, cpuMetric.value(), 0.1);
    }
}
