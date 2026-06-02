package com.trace.collector.graph.service;

import com.trace.collector.graph.repository.Neo4jGraphRepository;
import com.trace.common.model.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class DependencyGraphService {

    private static final Logger logger = LoggerFactory.getLogger(DependencyGraphService.class);

    private final SpanCacheService spanCache;
    private final Neo4jGraphRepository graphRepository;

    public DependencyGraphService(SpanCacheService spanCache, Neo4jGraphRepository graphRepository) {
        this.spanCache = spanCache;
        this.graphRepository = graphRepository;
    }

    public void processSpan(Span span) {
        spanCache.cacheSpan(span.getSpanId(), span.getServiceName());
        graphRepository.upsertService(span.getServiceName(), span.getStartTime());

        if (span.getParentSpanId() != null && !span.getParentSpanId().isEmpty()) {
            String parentService = spanCache.getServiceName(span.getParentSpanId());
            if (parentService != null && !parentService.equals(span.getServiceName())) {
                boolean isError = isErrorStatus(span);
                graphRepository.upsertDependency(
                        parentService,
                        span.getServiceName(),
                        span.getOperationName(),
                        span.getDuration(),
                        isError,
                        span.getStartTime()
                );
                logger.debug("Recorded dependency: {} -> {} ({})",
                        parentService, span.getServiceName(), span.getOperationName());
            }
        }
    }

    private boolean isErrorStatus(Span span) {
        if ("SERVER".equals(span.getSpanKind()) || "CLIENT".equals(span.getSpanKind())) {
            return span.getStatus() >= 400 || span.getStatus() == 1;
        }
        return span.getStatus() == 1;
    }
}
