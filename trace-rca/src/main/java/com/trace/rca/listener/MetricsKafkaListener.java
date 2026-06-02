package com.trace.rca.listener;

import com.trace.common.model.MetricPoint;
import com.trace.common.util.SpanJsonCodec;
import com.trace.rca.detector.AnomalyDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MetricsKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(MetricsKafkaListener.class);

    private final AnomalyDetector anomalyDetector;

    public MetricsKafkaListener(AnomalyDetector anomalyDetector) {
        this.anomalyDetector = anomalyDetector;
    }

    @KafkaListener(topics = "trace-metrics", groupId = "rca-group")
    public void onMetric(String message) {
        try {
            MetricPoint point = SpanJsonCodec.decodeMetricPoint(message);
            anomalyDetector.evaluate(point);
        } catch (Exception e) {
            logger.error("Failed to process metric message", e);
        }
    }
}
