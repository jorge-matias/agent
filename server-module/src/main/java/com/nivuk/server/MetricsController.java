package com.nivuk.server;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
public class MetricsController {

    @PostMapping("/metrics")
    public void receiveMetrics(@RequestBody Map<String, Object> metrics) {
        System.out.println("Received metrics: " + metrics);
    }
}
