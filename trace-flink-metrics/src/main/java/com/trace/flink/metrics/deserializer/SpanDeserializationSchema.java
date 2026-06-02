package com.trace.flink.metrics.deserializer;

import com.trace.common.model.Span;
import com.trace.common.util.SpanJsonCodec;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;

import java.io.IOException;

public class SpanDeserializationSchema implements DeserializationSchema<Span> {

    @Override
    public Span deserialize(byte[] message) throws IOException {
        String json = new String(message, "UTF-8");
        return SpanJsonCodec.decode(json);
    }

    @Override
    public boolean isEndOfStream(Span nextElement) {
        return false;
    }

    @Override
    public TypeInformation<Span> getProducedType() {
        return TypeInformation.of(Span.class);
    }
}
