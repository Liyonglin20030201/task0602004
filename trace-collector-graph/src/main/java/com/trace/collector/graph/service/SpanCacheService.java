package com.trace.collector.graph.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SpanCacheService {

    private final Cache<String, String> spanIdToService;

    public SpanCacheService() {
        this.spanIdToService = Caffeine.newBuilder()
                .maximumSize(100000)
                .expireAfterWrite(60, TimeUnit.SECONDS)
                .build();
    }

    public void cacheSpan(String spanId, String serviceName) {
        if (spanId != null && serviceName != null) {
            spanIdToService.put(spanId, serviceName);
        }
    }

    public String getServiceName(String spanId) {
        if (spanId == null) {
            return null;
        }
        return spanIdToService.getIfPresent(spanId);
    }
}
