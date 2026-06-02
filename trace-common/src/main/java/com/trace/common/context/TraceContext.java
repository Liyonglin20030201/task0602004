package com.trace.common.context;

public final class TraceContext {

    private static final ThreadLocal<String> TRACE_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> SPAN_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> PARENT_SPAN_ID = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> SERVICE_NAME = new InheritableThreadLocal<>();

    private TraceContext() {
    }

    public static String getTraceId() {
        return TRACE_ID.get();
    }

    public static void setTraceId(String traceId) {
        TRACE_ID.set(traceId);
    }

    public static String getSpanId() {
        return SPAN_ID.get();
    }

    public static void setSpanId(String spanId) {
        SPAN_ID.set(spanId);
    }

    public static String getParentSpanId() {
        return PARENT_SPAN_ID.get();
    }

    public static void setParentSpanId(String parentSpanId) {
        PARENT_SPAN_ID.set(parentSpanId);
    }

    public static String getServiceName() {
        return SERVICE_NAME.get();
    }

    public static void setServiceName(String serviceName) {
        SERVICE_NAME.set(serviceName);
    }

    public static void clear() {
        TRACE_ID.remove();
        SPAN_ID.remove();
        PARENT_SPAN_ID.remove();
    }
}
