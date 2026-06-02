package com.trace.flink.metrics.serializer;

import com.trace.common.model.MetricPoint;
import com.trace.common.util.SpanJsonCodec;
import org.apache.flink.api.common.serialization.SerializationSchema;

public class MetricPointSerializationSchema implements SerializationSchema<MetricPoint> {

    @Override
    public byte[] serialize(MetricPoint element) {
        return SpanJsonCodec.encodeMetricPoint(element).getBytes();
    }
}
