package com.trace.agent.interceptor;

import com.trace.common.context.TraceContext;
import com.trace.common.model.Span;
import com.trace.common.util.IdGenerator;
import com.trace.reporter.SpanReporter;
import net.bytebuddy.asm.Advice;

public class DubboInterceptor {

    private static final int IDX_START_TIME = 0;
    private static final int IDX_TRACE_ID = 1;
    private static final int IDX_SPAN_ID = 2;
    private static final int IDX_PARENT_SPAN_ID = 3;
    private static final int IDX_IS_CONSUMER = 4;
    private static final int IDX_ORIG_TRACE_ID = 5;
    private static final int IDX_ORIG_SPAN_ID = 6;
    private static final int IDX_ORIG_PARENT_SPAN_ID = 7;

    @Advice.OnMethodEnter
    public static Object[] onEnter(@Advice.Argument(0) Object invoker,
                                   @Advice.Argument(1) Object invocation) {
        String origTraceId = TraceContext.getTraceId();
        String origSpanId = TraceContext.getSpanId();
        String origParentSpanId = TraceContext.getParentSpanId();

        boolean isConsumer = determineConsumerSide(invoker, invocation);

        String traceId;
        String spanId = IdGenerator.generateSpanId();
        String parentSpanId;

        if (isConsumer) {
            traceId = origTraceId != null ? origTraceId : IdGenerator.generateTraceId();
            parentSpanId = origSpanId;

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
                    : (origTraceId != null ? origTraceId : IdGenerator.generateTraceId());
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
                origTraceId,
                origSpanId,
                origParentSpanId
        };
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Argument(0) Object invoker,
                              @Advice.Argument(1) Object invocation,
                              @Advice.Enter Object[] ctx,
                              @Advice.Thrown Throwable thrown,
                              @Advice.Return Object result) {
        long startTime = (Long) ctx[IDX_START_TIME];
        String traceId = (String) ctx[IDX_TRACE_ID];
        String spanId = (String) ctx[IDX_SPAN_ID];
        String parentSpanId = (String) ctx[IDX_PARENT_SPAN_ID];
        boolean isConsumer = (Boolean) ctx[IDX_IS_CONSUMER];
        String origTraceId = (String) ctx[IDX_ORIG_TRACE_ID];
        String origSpanId = (String) ctx[IDX_ORIG_SPAN_ID];
        String origParentSpanId = (String) ctx[IDX_ORIG_PARENT_SPAN_ID];

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
            TraceContext.setTraceId(origTraceId);
            TraceContext.setSpanId(origSpanId);
            TraceContext.setParentSpanId(origParentSpanId);
        }
    }

    private static boolean determineConsumerSide(Object invoker, Object invocation) {
        // 1st priority: check URL "side" parameter (Dubbo framework always sets this)
        try {
            java.lang.reflect.Method getUrl = invoker.getClass().getMethod("getUrl");
            Object url = getUrl.invoke(invoker);
            if (url != null) {
                java.lang.reflect.Method getParameter = url.getClass()
                        .getMethod("getParameter", String.class);
                String side = (String) getParameter.invoke(url, "side");
                if ("consumer".equals(side)) {
                    return true;
                }
                if ("provider".equals(side)) {
                    return false;
                }
            }
        } catch (Exception ignored) {
        }

        // 2nd priority: check if incoming trace headers exist in attachments
        // If trace headers are already present, this is the provider receiving a call
        String incomingTraceId = getAttachment(invocation, "X-Trace-Id");
        if (incomingTraceId != null && !incomingTraceId.isEmpty()) {
            return false;
        }

        // 3rd priority: check RpcContext.getContext().isConsumerSide()
        try {
            Class<?> rpcContextClass = null;
            try {
                rpcContextClass = Class.forName("org.apache.dubbo.rpc.RpcContext");
            } catch (ClassNotFoundException e) {
                rpcContextClass = Class.forName("com.alibaba.dubbo.rpc.RpcContext");
            }
            java.lang.reflect.Method getContext = rpcContextClass.getMethod("getContext");
            Object rpcContext = getContext.invoke(null);
            java.lang.reflect.Method isConsumerSide = rpcContext.getClass().getMethod("isConsumerSide");
            Boolean result = (Boolean) isConsumerSide.invoke(rpcContext);
            if (result != null) {
                return result;
            }
        } catch (Exception ignored) {
        }

        // Default: assume consumer if we have existing local context (we initiated the call)
        return TraceContext.getSpanId() != null;
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
