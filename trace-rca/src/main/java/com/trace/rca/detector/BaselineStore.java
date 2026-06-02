package com.trace.rca.detector;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class BaselineStore {

    private final Map<String, ServiceBaseline> baselines = new ConcurrentHashMap<>();

    public void update(String serviceKey, double errorRate, double p99Latency, double alpha) {
        baselines.compute(serviceKey, (key, existing) -> {
            if (existing == null) {
                return new ServiceBaseline(errorRate, p99Latency);
            }
            existing.updateEma(errorRate, p99Latency, alpha);
            return existing;
        });
    }

    public ServiceBaseline getBaseline(String serviceKey) {
        return baselines.get(serviceKey);
    }

    public double getLatestErrorRate(String serviceName) {
        ServiceBaseline baseline = baselines.get(serviceName);
        return baseline != null ? baseline.getLatestErrorRate() : 0.0;
    }

    public double getLatestP99(String serviceName) {
        ServiceBaseline baseline = baselines.get(serviceName);
        return baseline != null ? baseline.getLatestP99() : 0.0;
    }

    public static class ServiceBaseline {
        private double emaErrorRate;
        private double emaP99Latency;
        private double latestErrorRate;
        private double latestP99;

        public ServiceBaseline(double errorRate, double p99Latency) {
            this.emaErrorRate = errorRate;
            this.emaP99Latency = p99Latency;
            this.latestErrorRate = errorRate;
            this.latestP99 = p99Latency;
        }

        public void updateEma(double errorRate, double p99Latency, double alpha) {
            this.emaErrorRate = alpha * errorRate + (1 - alpha) * this.emaErrorRate;
            this.emaP99Latency = alpha * p99Latency + (1 - alpha) * this.emaP99Latency;
            this.latestErrorRate = errorRate;
            this.latestP99 = p99Latency;
        }

        public double getEmaErrorRate() {
            return emaErrorRate;
        }

        public double getEmaP99Latency() {
            return emaP99Latency;
        }

        public double getLatestErrorRate() {
            return latestErrorRate;
        }

        public double getLatestP99() {
            return latestP99;
        }
    }
}
