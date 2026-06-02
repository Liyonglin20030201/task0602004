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
    public static long onEnter(@Advice.Argument(1) HttpRequest request) {
        String traceId = TraceContext.getTraceId();
        if (traceId == null) {
            traceId = IdGenerator.generateTraceId();
            TraceContext.setTraceId(traceId);
        }

        String currentSpanId = TraceContext.getSpanId();
        String newSpanId = IdGenerator.generateSpanId();

        request.setHeader(TraceHeaders.TRACE_ID, traceId);
        request.setHeader(TraceHeaders.SPAN_ID, newSpanId);
        if (currentSpanId != null) {
            request.setHeader(TraceHeaders.PARENT_SPAN_ID, currentSpanId);
        }

        TraceContext.setParentSpanId(currentSpanId);
        TraceContext.setSpanId(newSpanId);

        return System.currentTimeMillis();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(@Advice.Argument(1) HttpRequest request,
                              @Advice.Enter long startTime,
                              @Advice.Thrown Throwable thrown,
                              @Advice.Return Object response) {
        try {
            long duration = System.currentTimeMillis() - startTime;

            Span span = new Span();
            span.setTraceId(TraceContext.getTraceId());
            span.setSpanId(TraceContext.getSpanId());
            span.setParentSpanId(TraceContext.getParentSpanId());
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
            String parentSpanId = TraceContext.getParentSpanId();
            TraceContext.setSpanId(parentSpanId);
        }
    }
}
