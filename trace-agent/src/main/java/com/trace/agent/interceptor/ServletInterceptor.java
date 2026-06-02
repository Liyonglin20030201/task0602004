package com.trace.agent.interceptor;

import com.trace.common.context.TraceContext;
import com.trace.common.constants.TraceHeaders;
import com.trace.common.model.Span;
import com.trace.common.util.IdGenerator;
import com.trace.reporter.SpanReporter;
import net.bytebuddy.asm.Advice;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletInterceptor {

    @Advice.OnMethodEnter
    public static long onEnter(@Advice.Argument(0) HttpServletRequest request) {
        String traceId = request.getHeader(TraceHeaders.TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = IdGenerator.generateTraceId();
        }

        String parentSpanId = request.getHeader(TraceHeaders.SPAN_ID);
        String spanId = IdGenerator.generateSpanId();

        TraceContext.setTraceId(traceId);
        TraceContext.setSpanId(spanId);
        TraceContext.setParentSpanId(parentSpanId);

        return System.currentTimeMillis();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Argument(0) HttpServletRequest request,
                              @Advice.Argument(1) HttpServletResponse response,
                              @Advice.Enter long startTime,
                              @Advice.Thrown Throwable thrown) {
        try {
            long duration = System.currentTimeMillis() - startTime;

            Span span = new Span();
            span.setTraceId(TraceContext.getTraceId());
            span.setSpanId(TraceContext.getSpanId());
            span.setParentSpanId(TraceContext.getParentSpanId());
            span.setServiceName(TraceContext.getServiceName());
            span.setOperationName(request.getMethod() + " " + request.getRequestURI());
            span.setStartTime(startTime);
            span.setDuration(duration);
            span.setSpanKind("SERVER");

            if (thrown != null) {
                span.setStatus(500);
                span.addTag("error", thrown.getClass().getName());
                span.addTag("error.message", thrown.getMessage() != null ? thrown.getMessage() : "");
            } else {
                span.setStatus(response.getStatus());
            }

            span.addTag("http.method", request.getMethod());
            span.addTag("http.url", request.getRequestURI());
            span.addTag("http.status_code", String.valueOf(span.getStatus()));

            SpanReporter reporter = SpanReporter.getInstance();
            if (reporter != null) {
                reporter.report(span);
            }
        } finally {
            TraceContext.clear();
        }
    }
}
