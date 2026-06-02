package com.trace.agent.interceptor;

import com.trace.common.context.TraceContext;
import com.trace.common.model.Span;
import com.trace.common.util.IdGenerator;
import com.trace.reporter.SpanReporter;
import net.bytebuddy.asm.Advice;

public class DubboInterceptor {

    @Advice.OnMethodEnter
    public static long onEnter(@Advice.Argument(1) Object invocation) {
        String traceId = TraceContext.getTraceId();
        if (traceId == null) {
            traceId = IdGenerator.generateTraceId();
            TraceContext.setTraceId(traceId);
        }

        String parentSpanId = TraceContext.getSpanId();
        String spanId = IdGenerator.generateSpanId();
        TraceContext.setSpanId(spanId);
        TraceContext.setParentSpanId(parentSpanId);

        try {
            java.lang.reflect.Method getAttachment = invocation.getClass().getMethod("getAttachment", String.class);
            java.lang.reflect.Method setAttachment = invocation.getClass().getMethod("setAttachment", String.class, String.class);

            String incomingTraceId = (String) getAttachment.invoke(invocation, "X-Trace-Id");
            if (incomingTraceId != null && !incomingTraceId.isEmpty()) {
                TraceContext.setTraceId(incomingTraceId);
            } else {
                setAttachment.invoke(invocation, "X-Trace-Id", TraceContext.getTraceId());
            }
            setAttachment.invoke(invocation, "X-Span-Id", spanId);
        } catch (Exception ignored) {
        }

        return System.currentTimeMillis();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Argument(0) Object invoker,
                              @Advice.Argument(1) Object invocation,
                              @Advice.Enter long startTime,
                              @Advice.Thrown Throwable thrown,
                              @Advice.Return Object result) {
        try {
            long duration = System.currentTimeMillis() - startTime;

            Span span = new Span();
            span.setTraceId(TraceContext.getTraceId());
            span.setSpanId(TraceContext.getSpanId());
            span.setParentSpanId(TraceContext.getParentSpanId());
            span.setServiceName(TraceContext.getServiceName());
            span.setStartTime(startTime);
            span.setDuration(duration);
            span.setSpanKind("SERVER");

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
            TraceContext.clear();
        }
    }
}
