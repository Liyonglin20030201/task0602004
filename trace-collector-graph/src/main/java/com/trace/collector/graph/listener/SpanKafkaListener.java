package com.trace.collector.graph.listener;

import com.trace.collector.graph.service.DependencyGraphService;
import com.trace.common.model.Span;
import com.trace.common.util.SpanJsonCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SpanKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(SpanKafkaListener.class);

    private final DependencyGraphService graphService;

    public SpanKafkaListener(DependencyGraphService graphService) {
        this.graphService = graphService;
    }

    @KafkaListener(topics = "trace-spans", groupId = "graph-collector-group")
    public void onSpanBatch(List<String> messages) {
        for (String message : messages) {
            try {
                Span span = SpanJsonCodec.decode(message);
                graphService.processSpan(span);
            } catch (Exception e) {
                logger.error("Failed to process span message", e);
            }
        }
    }
}
