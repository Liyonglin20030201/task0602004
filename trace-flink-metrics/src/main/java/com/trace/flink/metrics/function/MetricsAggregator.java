package com.trace.flink.metrics.function;

import com.trace.common.model.Span;
import com.trace.flink.metrics.model.MetricsAccumulator;
import org.apache.flink.api.common.functions.AggregateFunction;

public class MetricsAggregator implements AggregateFunction<Span, MetricsAccumulator, MetricsAccumulator> {

    @Override
    public MetricsAccumulator createAccumulator() {
        return new MetricsAccumulator();
    }

    @Override
    public MetricsAccumulator add(Span span, MetricsAccumulator acc) {
        acc.addSpan(span.getDuration(), isError(span), span.getServiceName(), span.getOperationName());
        return acc;
    }

    @Override
    public MetricsAccumulator getResult(MetricsAccumulator acc) {
        return acc;
    }

    @Override
    public MetricsAccumulator merge(MetricsAccumulator a, MetricsAccumulator b) {
        a.merge(b);
        return a;
    }

    private boolean isError(Span span) {
        return span.getStatus() >= 400 || span.getStatus() == 1;
    }
}
