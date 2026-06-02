package com.trace.agent.interceptor;

import com.trace.common.constants.TraceHeaders;
import com.trace.common.context.TraceContext;
import com.trace.common.model.Span;
import com.trace.common.util.IdGenerator;
import com.trace.reporter.SpanReporter;
import net.bytebuddy.asm.Advice;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public class HttpClientInterceptor {

    @Advice.OnMethodEnter
    public static Object[] onEnter(@Advice.Argument(1) HttpRequest request) {
        String savedTraceId = TraceContext.getTraceId();
        String savedSpanId = TraceContext.getSpanId();
        String savedParentSpanId = TraceContext.getParentSpanId();

        String traceId = savedTraceId;
        if (traceId == null) {
            traceId = IdGenerator.generateTraceId();
        }

        String newSpanId = IdGenerator.generateSpanId();

        request.setHeader(TraceHeaders.TRACE_ID, traceId);
        request.setHeader(TraceHeaders.SPAN_ID, newSpanId);
        if (savedSpanId != null) {
            request.setHeader(TraceHeaders.PARENT_SPAN_ID, savedSpanId);
        }

        TraceContext.setTraceId(traceId);
        TraceContext.setSpanId(newSpanId);
        TraceContext.setParentSpanId(savedSpanId);

        return new Object[]{
                System.currentTimeMillis(),
                traceId,
                newSpanId,
                savedSpanId,
                savedTraceId,
                savedSpanId,
                savedParentSpanId
        };
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Argument(1) HttpRequest request,
                              @Advice.Enter Object[] entered,
                              @Advice.Thrown Throwable thrown,
                              @Advice.Return Object response) {
        long startTime = (Long) entered[0];
        String traceId = (String) entered[1];
        String spanId = (String) entered[2];
        String parentSpanId = (String) entered[3];
        String origTraceId = (String) entered[4];
        String origSpanId = (String) entered[5];
        String origParentSpanId = (String) entered[6];

        try {
            long duration = System.currentTimeMillis() - startTime;

            Span span = new Span();
            span.setTraceId(traceId);
            span.setSpanId(spanId);
            span.setParentSpanId(parentSpanId);
            span.setServiceName(TraceContext.getServiceName());
            span.setStartTime(startTime);
            span.setDuration(duration);
            span.setSpanKind("CLIENT");

            String uri = request.getRequestLine().getUri();
            String method = request.getRequestLine().getMethod();
            span.setOperationName(method + " " + uri);
            span.addTag("http.method", method);
            span.addTag("http.url", uri);

            if (thrown != null) {
                span.setStatus(500);
                span.addTag("error", thrown.getClass().getName());
            } else if (response instanceof HttpResponse) {
                int statusCode = ((HttpResponse) response).getStatusLine().getStatusCode();
                span.setStatus(statusCode);
                span.addTag("http.status_code", String.valueOf(statusCode));
            }

            SpanReporter reporter = SpanReporter.getInstance();
            if (reporter != null) {
                reporter.report(span);
            }
        } finally {
            if (origTraceId != null) {
                TraceContext.setTraceId(origTraceId);
            } else {
                TraceContext.setTraceId(null);
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
        }
    }
}
