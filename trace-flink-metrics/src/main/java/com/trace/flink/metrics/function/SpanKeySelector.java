package com.trace.flink.metrics.function;

import com.trace.common.model.Span;
import org.apache.flink.api.java.functions.KeySelector;

public class SpanKeySelector implements KeySelector<Span, String> {

    @Override
    public String getKey(Span span) {
        return span.getServiceName() + ":" + span.getOperationName();
    }
}
