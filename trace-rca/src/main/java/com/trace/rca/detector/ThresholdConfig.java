package com.trace.rca.detector;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ThresholdConfig {

    @Value("${rca.error-rate-threshold:0.05}")
    private double errorRateThreshold;

    @Value("${rca.p99-latency-threshold-ms:500}")
    private double p99LatencyThresholdMs;

    @Value("${rca.latency-spike-multiplier:3.0}")
    private double latencySpikeMultiplier;

    @Value("${rca.baseline-window-count:30}")
    private int baselineWindowCount;

    @Value("${rca.ema-alpha:0.1}")
    private double emaAlpha;

    public double getErrorRateThreshold() {
        return errorRateThreshold;
    }

    public double getP99LatencyThresholdMs() {
        return p99LatencyThresholdMs;
    }

    public double getLatencySpikeMultiplier() {
        return latencySpikeMultiplier;
    }

    public int getBaselineWindowCount() {
        return baselineWindowCount;
    }

    public double getEmaAlpha() {
        return emaAlpha;
    }
}
