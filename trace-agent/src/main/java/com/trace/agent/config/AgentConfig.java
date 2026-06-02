package com.trace.agent.config;

import java.util.HashMap;
import java.util.Map;

public class AgentConfig {

    private String serviceName = "unknown-service";
    private String kafkaBrokers = "localhost:9092";
    private int queueCapacity = 10000;
    private int batchSize = 100;
    private long flushIntervalMs = 1000;

    public static AgentConfig parse(String arguments) {
        AgentConfig config = new AgentConfig();
        if (arguments == null || arguments.trim().isEmpty()) {
            return config;
        }

        Map<String, String> params = new HashMap<>();
        for (String pair : arguments.split(",")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(kv[0].trim(), kv[1].trim());
            }
        }

        if (params.containsKey("serviceName")) {
            config.serviceName = params.get("serviceName");
        }
        if (params.containsKey("kafkaBrokers")) {
            config.kafkaBrokers = params.get("kafkaBrokers");
        }
        if (params.containsKey("queueCapacity")) {
            config.queueCapacity = Integer.parseInt(params.get("queueCapacity"));
        }
        if (params.containsKey("batchSize")) {
            config.batchSize = Integer.parseInt(params.get("batchSize"));
        }
        if (params.containsKey("flushIntervalMs")) {
            config.flushIntervalMs = Long.parseLong(params.get("flushIntervalMs"));
        }

        return config;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getKafkaBrokers() {
        return kafkaBrokers;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public long getFlushIntervalMs() {
        return flushIntervalMs;
    }
}
