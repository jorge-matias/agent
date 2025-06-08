package com.nivuk.agent.collectors;

import com.nivuk.agent.model.Metric;

public interface Collector {
    Metric collect();
}
