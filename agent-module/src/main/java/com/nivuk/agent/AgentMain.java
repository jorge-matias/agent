package com.nivuk.agent;

import java.util.Timer;
import com.nivuk.agent.config.AgentConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentMain {
    private static final Logger logger = LoggerFactory.getLogger(AgentMain.class);

    public static void main(String[] args) {
        AgentConfig config = AgentConfig.load();
        logger.info("Starting agent with collection interval={}",
            config.getCollectionIntervalSeconds());

        MetricsCollectionTask job = new MetricsCollectionTask(
            config.createCollectors(),
            config.createExporters()
        );

        new Timer().schedule(job, 0, config.getCollectionIntervalSeconds() * 1000L);
    }
}
