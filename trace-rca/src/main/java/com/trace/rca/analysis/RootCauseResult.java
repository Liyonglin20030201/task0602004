package com.trace.rca.analysis;

import java.util.List;

public class RootCauseResult {

    private String rootCauseService;
    private List<String> dependencyPath;
    private double anomalyScore;
    private double errorRate;
    private double p99Latency;
    private long timestamp;

    public RootCauseResult() {
        this.timestamp = System.currentTimeMillis();
    }

    public String getRootCauseService() {
        return rootCauseService;
    }

    public void setRootCauseService(String rootCauseService) {
        this.rootCauseService = rootCauseService;
    }

    public List<String> getDependencyPath() {
        return dependencyPath;
    }

    public void setDependencyPath(List<String> dependencyPath) {
        this.dependencyPath = dependencyPath;
    }

    public double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public double getErrorRate() {
        return errorRate;
    }

    public void setErrorRate(double errorRate) {
        this.errorRate = errorRate;
    }

    public double getP99Latency() {
        return p99Latency;
    }

    public void setP99Latency(double p99Latency) {
        this.p99Latency = p99Latency;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "RootCauseResult{" +
                "rootCause='" + rootCauseService + '\'' +
                ", path=" + dependencyPath +
                ", errorRate=" + errorRate +
                ", p99=" + p99Latency +
                '}';
    }
}
