package com.nivuk.agent.collectors;

import com.sun.management.OperatingSystemMXBean;
import java.lang.management.ManagementFactory;

public class DefaultSystemInfoProvider implements SystemInfoProvider {
    private final OperatingSystemMXBean osBean;

    public DefaultSystemInfoProvider() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public double getCpuLoad() {
        return osBean.getCpuLoad();
    }
}
