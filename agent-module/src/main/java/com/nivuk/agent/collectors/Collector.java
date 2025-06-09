package com.nivuk.agent.collectors;

import com.nivuk.agent.model.Metric;
import java.util.List;

public interface Collector {
    List<Metric> collect();
}
