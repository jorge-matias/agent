package com.nivuk.server;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(MetricsController.class)
class MetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsStorage storage;

    @Test
    void shouldHandleMetricsInTimeSeriesFormat() throws Exception {
        String requestBody = """
            {
                "points": [
                    {
                        "t": 1686394800000,
                        "h": "test-host",
                        "n": "cpu",
                        "v": 75.5,
                        "u": "%"
                    },
                    {
                        "t": 1686394800000,
                        "h": "test-host",
                        "n": "memory",
                        "v": 1024,
                        "u": "MB"
                    }
                ]
            }""";

        mockMvc.perform(post("/metrics")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk());

        verify(storage, times(2)).addMetric(any());
    }

    @Test
    void shouldQueryMetricsWithFilters() throws Exception {
        List<MetricsPayload.MetricPoint> mockPoints = new ArrayList<>();
        MetricsPayload.MetricPoint point = new MetricsPayload.MetricPoint();
        point.setTimestamp(1686394800000L);
        point.setHostName("test-host");
        point.setMetricName("cpu");
        point.setValue(75.5);
        point.setUnit("%");
        mockPoints.add(point);

        when(storage.queryMetrics(anyString(), anyString(), anyLong(), anyLong()))
            .thenReturn(mockPoints);

        mockMvc.perform(get("/metrics")
                .param("host", "test-host")
                .param("metric", "cpu")
                .param("from", "1686394800000")
                .param("to", "1686394801000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].t").value(1686394800000L))
                .andExpect(jsonPath("$[0].h").value("test-host"))
                .andExpect(jsonPath("$[0].n").value("cpu"))
                .andExpect(jsonPath("$[0].v").value(75.5))
                .andExpect(jsonPath("$[0].u").value("%"));
    }

    @Test
    void shouldReturnAvailableHosts() throws Exception {
        Set<String> mockHosts = Set.of("host1", "host2");
        when(storage.getHosts()).thenReturn(mockHosts);

        mockMvc.perform(get("/metrics/hosts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("host1"))
                .andExpect(jsonPath("$[1]").value("host2"));
    }

    @Test
    void shouldReturnAvailableMetricNames() throws Exception {
        Set<String> mockMetrics = Set.of("cpu", "memory");
        when(storage.getMetricNames()).thenReturn(mockMetrics);

        mockMvc.perform(get("/metrics/names"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("cpu"))
                .andExpect(jsonPath("$[1]").value("memory"));
    }
}
