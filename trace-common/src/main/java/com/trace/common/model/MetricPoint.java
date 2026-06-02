package com.trace.common.model;

public class MetricPoint {

    private String serviceName;
    private String endpoint;
    private long windowStart;
    private long windowEnd;
    private double successRate;
    private double p50Latency;
    private double p99Latency;
    private long totalCount;

    public MetricPoint() {
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public long getWindowStart() {
        return windowStart;
    }

    public void setWindowStart(long windowStart) {
        this.windowStart = windowStart;
    }

    public long getWindowEnd() {
        return windowEnd;
    }

    public void setWindowEnd(long windowEnd) {
        this.windowEnd = windowEnd;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public double getP50Latency() {
        return p50Latency;
    }

    public void setP50Latency(double p50Latency) {
        this.p50Latency = p50Latency;
    }

    public double getP99Latency() {
        return p99Latency;
    }

    public void setP99Latency(double p99Latency) {
        this.p99Latency = p99Latency;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    public String toString() {
        return "MetricPoint{" +
                "serviceName='" + serviceName + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", successRate=" + successRate +
                ", p50=" + p50Latency +
                ", p99=" + p99Latency +
                ", count=" + totalCount +
                '}';
    }
}
