package com.nivuk.agent.collectors;

import com.nivuk.agent.model.Metric;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MemoryCollectorTest {

    private MemoryCollector memoryCollector;

    @BeforeEach
    void setUp() {
        memoryCollector = new MemoryCollector();
    }

    @Test
    void shouldReturnValidMemoryMetrics() {
        // When
        List<Metric> metrics = memoryCollector.collect();

        // Then
        assertNotNull(metrics);
        assertEquals(2, metrics.size());

        // Verify memory_free metric
        Metric freeMemoryMetric = metrics.get(0);
        assertEquals("mem_free", freeMemoryMetric.name());
        assertTrue(freeMemoryMetric.value() >= 0);
        assertEquals("m", freeMemoryMetric.unit());

        // Verify memory_total metric
        Metric totalMemoryMetric = metrics.get(1);
        assertEquals("mem_total", totalMemoryMetric.name());
        assertTrue(totalMemoryMetric.value() > 0);
        assertEquals("m", totalMemoryMetric.unit());

        // Verify that free memory is less than or equal to total memory
        assertTrue(freeMemoryMetric.value() <= totalMemoryMetric.value(),
                "Free memory should not exceed total memory");
    }

    @Test
    void shouldReportMemoryInMegabytes() {
        // When
        List<Metric> metrics = memoryCollector.collect();

        // Then
        Metric totalMemoryMetric = metrics.get(1);
        double totalMemoryMB = totalMemoryMetric.value();

        // Verify the value is reasonable (greater than 100MB and less than 1TB)
        assertTrue(totalMemoryMB >= 100, "Total memory should be at least 100MB");
        assertTrue(totalMemoryMB <= 1_048_576, "Total memory should not exceed 1TB");
    }
}
