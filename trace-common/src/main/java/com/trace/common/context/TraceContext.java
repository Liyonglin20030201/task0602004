package com.trace.common.context;

public final class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> SPAN_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> PARENT_SPAN_ID = new InheritableThreadLocal<>();
    private static volatile String serviceName = "unknown-service";

    private TraceContext() {
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static void setTraceId(String traceId) {
        if (traceId == null) {
            TRACE_ID.remove();
        } else {
            TRACE_ID.set(traceId);
        }
    }

    public static String getSpanId() {
        return SPAN_ID.get();
    }

    public static void setSpanId(String spanId) {
        if (spanId == null) {
            SPAN_ID.remove();
        } else {
            SPAN_ID.set(spanId);
        }
    }

    public static String getParentSpanId() {
        return PARENT_SPAN_ID.get();
    }

    public static void setParentSpanId(String parentSpanId) {
        if (parentSpanId == null) {
            PARENT_SPAN_ID.remove();
        } else {
            PARENT_SPAN_ID.set(parentSpanId);
        }
    }

    public static String getServiceName() {
        return serviceName;
    }

    public static void setServiceName(String name) {
        serviceName = name;
    }

    public static void clear() {
        TRACE_ID.remove();
        SPAN_ID.remove();
        PARENT_SPAN_ID.remove();
    }

    public static ContextSnapshot snapshot() {
        return new ContextSnapshot(TRACE_ID.get(), SPAN_ID.get(), PARENT_SPAN_ID.get());
    }

    public static void restore(ContextSnapshot snapshot) {
        if (snapshot == null) {
            clear();
            return;
        }
        if (snapshot.traceId != null) {
            TRACE_ID.set(snapshot.traceId);
        } else {
            TRACE_ID.remove();
        }
        if (snapshot.spanId != null) {
            SPAN_ID.set(snapshot.spanId);
        } else {
            SPAN_ID.remove();
        }
        if (snapshot.parentSpanId != null) {
            PARENT_SPAN_ID.set(snapshot.parentSpanId);
        } else {
            PARENT_SPAN_ID.remove();
        }
    }

    public static class ContextSnapshot {
        private final String traceId;
        private final String spanId;
        private final String parentSpanId;

        private ContextSnapshot(String traceId, String spanId, String parentSpanId) {
            this.traceId = traceId;
            this.spanId = spanId;
            this.parentSpanId = parentSpanId;
        }
    }
}
