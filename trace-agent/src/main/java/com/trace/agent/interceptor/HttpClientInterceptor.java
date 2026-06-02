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

    private static final int IDX_START_TIME = 0;
    private static final int IDX_SPAN_TRACE_ID = 1;
    private static final int IDX_SPAN_ID = 2;
    private static final int IDX_ORIG_TRACE_ID = 3;
    private static final int IDX_ORIG_SPAN_ID = 4;
    private static final int IDX_ORIG_PARENT_SPAN_ID = 5;

    @Advice.OnMethodEnter
    public static Object[] onEnter(@Advice.Argument(1) HttpRequest request) {
        String origTraceId = TraceContext.getTraceId();
        String origSpanId = TraceContext.getSpanId();
        String origParentSpanId = TraceContext.getParentSpanId();

        String traceId = origTraceId != null ? origTraceId : IdGenerator.generateTraceId();
        String newSpanId = IdGenerator.generateSpanId();

        request.setHeader(TraceHeaders.TRACE_ID, traceId);
        request.setHeader(TraceHeaders.SPAN_ID, newSpanId);
        if (origSpanId != null) {
            request.setHeader(TraceHeaders.PARENT_SPAN_ID, origSpanId);
        }

        TraceContext.setTraceId(traceId);
        TraceContext.setSpanId(newSpanId);
        TraceContext.setParentSpanId(origSpanId);

        return new Object[]{
                System.currentTimeMillis(),
                traceId,
                newSpanId,
                origTraceId,
                origSpanId,
                origParentSpanId
        };
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Argument(1) HttpRequest request,
                              @Advice.Enter Object[] ctx,
                              @Advice.Thrown Throwable thrown,
                              @Advice.Return Object response) {
        long startTime = (Long) ctx[IDX_START_TIME];
        String traceId = (String) ctx[IDX_SPAN_TRACE_ID];
        String spanId = (String) ctx[IDX_SPAN_ID];
        String origTraceId = (String) ctx[IDX_ORIG_TRACE_ID];
        String origSpanId = (String) ctx[IDX_ORIG_SPAN_ID];
        String origParentSpanId = (String) ctx[IDX_ORIG_PARENT_SPAN_ID];

        try {
            long duration = System.currentTimeMillis() - startTime;

            Span span = new Span();
            span.setTraceId(traceId);
            span.setSpanId(spanId);
            span.setParentSpanId(origSpanId);
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
            TraceContext.setTraceId(origTraceId);
            TraceContext.setSpanId(origSpanId);
            TraceContext.setParentSpanId(origParentSpanId);
        }
    }
}
