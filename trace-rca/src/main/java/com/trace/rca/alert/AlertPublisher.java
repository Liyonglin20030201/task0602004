package com.trace.rca.alert;

import com.trace.rca.analysis.RootCauseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AlertPublisher {

    private static final Logger logger = LoggerFactory.getLogger(AlertPublisher.class);
    private static final String ALERT_TOPIC = "trace-alerts";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final KafkaTemplate<String, String> kafkaTemplate;

    public AlertPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(RootCauseResult result) {
        try {
            String json = MAPPER.writeValueAsString(result);
            kafkaTemplate.send(ALERT_TOPIC, result.getRootCauseService(), json);
            logger.warn("[ALERT] Root cause identified: service={}, path={}, errorRate={}, p99={}ms",
                    result.getRootCauseService(),
                    result.getDependencyPath(),
                    result.getErrorRate(),
                    result.getP99Latency());
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize alert", e);
        }
    }
}
