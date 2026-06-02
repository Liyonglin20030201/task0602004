package com.trace.flink.metrics.model;

import com.tdunning.math.stats.TDigest;

import java.io.Serializable;

public class MetricsAccumulator implements Serializable {

    private static final long serialVersionUID = 1L;

    private long totalCount;
    private long errorCount;
    private String serviceName;
    private String operationName;
    private transient TDigest digest;
    private double[] samples;
    private int sampleIndex;

    public MetricsAccumulator() {
        this.totalCount = 0;
        this.errorCount = 0;
        this.samples = new double[10000];
        this.sampleIndex = 0;
    }

    public void addSpan(long durationMs, boolean isError, String serviceName, String operationName) {
        this.totalCount++;
        if (isError) {
            this.errorCount++;
        }
        if (this.serviceName == null) {
            this.serviceName = serviceName;
        }
        if (this.operationName == null) {
            this.operationName = operationName;
        }
        if (sampleIndex < samples.length) {
            samples[sampleIndex++] = durationMs;
        }
    }

    public void merge(MetricsAccumulator other) {
        this.totalCount += other.totalCount;
        this.errorCount += other.errorCount;
        for (int i = 0; i < other.sampleIndex && this.sampleIndex < this.samples.length; i++) {
            this.samples[this.sampleIndex++] = other.samples[i];
        }
    }

    public double getSuccessRate() {
        if (totalCount == 0) return 1.0;
        return 1.0 - ((double) errorCount / totalCount);
    }

    public double getP50() {
        return getPercentile(0.50);
    }

    public double getP99() {
        return getPercentile(0.99);
    }

    private double getPercentile(double quantile) {
        if (sampleIndex == 0) return 0.0;
        TDigest td = TDigest.createMergingDigest(100);
        for (int i = 0; i < sampleIndex; i++) {
            td.add(samples[i]);
        }
        return td.quantile(quantile);
    }

    public long getTotalCount() {
        return totalCount;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getOperationName() {
        return operationName;
    }
}
