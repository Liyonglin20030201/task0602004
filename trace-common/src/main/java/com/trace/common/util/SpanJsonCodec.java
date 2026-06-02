package com.trace.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trace.common.model.MetricPoint;
import com.trace.common.model.Span;

import java.io.IOException;

public final class SpanJsonCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private SpanJsonCodec() {
    }

    public static String encode(Span span) {
        try {
            return MAPPER.writeValueAsString(span);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize span", e);
        }
    }

    public static Span decode(String json) {
        try {
            return MAPPER.readValue(json, Span.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize span", e);
        }
    }

    public static String encodeMetricPoint(MetricPoint point) {
        try {
            return MAPPER.writeValueAsString(point);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize metric point", e);
        }
    }

    public static MetricPoint decodeMetricPoint(String json) {
        try {
            return MAPPER.readValue(json, MetricPoint.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize metric point", e);
        }
    }
}
