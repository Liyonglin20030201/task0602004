package com.trace.agent.interceptor;

import com.trace.common.context.TraceContext;
import com.trace.common.model.Span;
import com.trace.common.util.IdGenerator;
import com.trace.reporter.SpanReporter;
import net.bytebuddy.asm.Advice;

public class DubboInterceptor {

    @Advice.OnMethodEnter
    public static Object[] onEnter(@Advice.Argument(0) Object invoker,
                                   @Advice.Argument(1) Object invocation) {
        String savedTraceId = TraceContext.getTraceId();
        String savedSpanId = TraceContext.getSpanId();
        String savedParentSpanId = TraceContext.getParentSpanId();

        boolean isConsumer = isConsumerSide(invoker);

        String traceId;
        String spanId = IdGenerator.generateSpanId();
        String parentSpanId;

        if (isConsumer) {
            traceId = savedTraceId != null ? savedTraceId : IdGenerator.generateTraceId();
            parentSpanId = savedSpanId;

            try {
                java.lang.reflect.Method setAttachment = invocation.getClass()
                        .getMethod("setAttachment", String.class, String.class);
                setAttachment.invoke(invocation, "X-Trace-Id", traceId);
                setAttachment.invoke(invocation, "X-Span-Id", spanId);
                if (parentSpanId != null) {
                    setAttachment.invoke(invocation, "X-Parent-Span-Id", parentSpanId);
                }
            } catch (Exception ignored) {
            }
        } else {
            String incomingTraceId = getAttachment(invocation, "X-Trace-Id");
            String incomingSpanId = getAttachment(invocation, "X-Span-Id");

            traceId = (incomingTraceId != null && !incomingTraceId.isEmpty())
                    ? incomingTraceId
                    : (savedTraceId != null ? savedTraceId : IdGenerator.generateTraceId());
            parentSpanId = incomingSpanId;
        }

        TraceContext.setTraceId(traceId);
        TraceContext.setSpanId(spanId);
        TraceContext.setParentSpanId(parentSpanId);

        return new Object[]{
                System.currentTimeMillis(),
                traceId,
                spanId,
                parentSpanId,
                isConsumer,
                savedTraceId,
                savedSpanId,
                savedParentSpanId
        };
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Argument(0) Object invoker,
                              @Advice.Argument(1) Object invocation,
                              @Advice.Enter Object[] entered,
                              @Advice.Thrown Throwable thrown,
                              @Advice.Return Object result) {
        long startTime = (Long) entered[0];
        String traceId = (String) entered[1];
        String spanId = (String) entered[2];
        String parentSpanId = (String) entered[3];
        boolean isConsumer = (Boolean) entered[4];
        String origTraceId = (String) entered[5];
        String origSpanId = (String) entered[6];
        String origParentSpanId = (String) entered[7];

        try {
            long duration = System.currentTimeMillis() - startTime;

            Span span = new Span();
            span.setTraceId(traceId);
            span.setSpanId(spanId);
            span.setParentSpanId(parentSpanId);
            span.setServiceName(TraceContext.getServiceName());
            span.setStartTime(startTime);
            span.setDuration(duration);
            span.setSpanKind(isConsumer ? "CLIENT" : "SERVER");

            String methodName = "unknown";
            String interfaceName = "unknown";
            try {
                java.lang.reflect.Method getMethodName = invocation.getClass().getMethod("getMethodName");
                methodName = (String) getMethodName.invoke(invocation);

                java.lang.reflect.Method getInterface = invoker.getClass().getMethod("getInterface");
                Class<?> iface = (Class<?>) getInterface.invoke(invoker);
                interfaceName = iface.getName();
            } catch (Exception ignored) {
            }

            span.setOperationName(interfaceName + "." + methodName);
            span.addTag("rpc.system", "dubbo");
            span.addTag("rpc.method", methodName);
            span.addTag("rpc.service", interfaceName);
            span.addTag("rpc.side", isConsumer ? "consumer" : "provider");

            if (thrown != null) {
                span.setStatus(1);
                span.addTag("error", thrown.getClass().getName());
            } else {
                span.setStatus(0);
                if (result != null) {
                    try {
                        java.lang.reflect.Method hasException = result.getClass().getMethod("hasException");
                        Boolean hasEx = (Boolean) hasException.invoke(result);
                        if (Boolean.TRUE.equals(hasEx)) {
                            span.setStatus(1);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            SpanReporter reporter = SpanReporter.getInstance();
            if (reporter != null) {
                reporter.report(span);
            }
        } finally {
            if (isConsumer) {
                if (origTraceId != null) {
                    TraceContext.setTraceId(origTraceId);
                }
                if (origSpanId != null) {
                    TraceContext.setSpanId(origSpanId);
                } else {
                    TraceContext.setSpanId(null);
                }
                if (origParentSpanId != null) {
                    TraceContext.setParentSpanId(origParentSpanId);
                } else {
                    TraceContext.setParentSpanId(null);
                }
            } else {
                TraceContext.clear();
            }
        }
    }

    private static boolean isConsumerSide(Object invoker) {
        try {
            java.lang.reflect.Method getUrl = invoker.getClass().getMethod("getUrl");
            Object url = getUrl.invoke(invoker);
            if (url != null) {
                java.lang.reflect.Method getParameter = url.getClass()
                        .getMethod("getParameter", String.class);
                String side = (String) getParameter.invoke(url, "side");
                if (side != null) {
                    return "consumer".equals(side);
                }
            }
        } catch (Exception ignored) {
        }
        // fallback: check if invoker URL has remote protocol
        try {
            java.lang.reflect.Method getUrl = invoker.getClass().getMethod("getUrl");
            Object url = getUrl.invoke(invoker);
            if (url != null) {
                java.lang.reflect.Method getProtocol = url.getClass().getMethod("getProtocol");
                String protocol = (String) getProtocol.invoke(url);
                return !"injvm".equals(protocol);
            }
        } catch (Exception ignored) {
        }
        return true;
    }

    private static String getAttachment(Object invocation, String key) {
        try {
            java.lang.reflect.Method getAttachment = invocation.getClass()
                    .getMethod("getAttachment", String.class);
            return (String) getAttachment.invoke(invocation, key);
        } catch (Exception e) {
            return null;
        }
    }
}
