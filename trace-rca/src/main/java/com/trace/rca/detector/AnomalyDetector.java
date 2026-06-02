package com.trace.rca.detector;

import com.trace.common.model.MetricPoint;
import com.trace.rca.analysis.RootCauseAnalyzer;
import com.trace.rca.analysis.RootCauseResult;
import com.trace.rca.alert.AlertPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnomalyDetector {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetector.class);
    private static final long DEBOUNCE_INTERVAL_MS = 5 * 60 * 1000;

    private final ThresholdConfig config;
    private final BaselineStore baselineStore;
    private final RootCauseAnalyzer rootCauseAnalyzer;
    private final AlertPublisher alertPublisher;
    private final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();

    public AnomalyDetector(ThresholdConfig config, BaselineStore baselineStore,
                           RootCauseAnalyzer rootCauseAnalyzer, AlertPublisher alertPublisher) {
        this.config = config;
        this.baselineStore = baselineStore;
        this.rootCauseAnalyzer = rootCauseAnalyzer;
        this.alertPublisher = alertPublisher;
    }

    public void evaluate(MetricPoint point) {
        String serviceKey = point.getServiceName() + ":" + point.getEndpoint();
        double errorRate = 1.0 - point.getSuccessRate();

        baselineStore.update(serviceKey, errorRate, point.getP99Latency(), config.getEmaAlpha());

        double anomalyScore = computeAnomalyScore(serviceKey, errorRate, point.getP99Latency());

        if (anomalyScore > 0) {
            logger.warn("Anomaly detected: service={}, endpoint={}, score={}, errorRate={}, p99={}",
                    point.getServiceName(), point.getEndpoint(), anomalyScore, errorRate, point.getP99Latency());

            if (shouldAlert(point.getServiceName())) {
                RootCauseResult result = rootCauseAnalyzer.analyze(point.getServiceName());
                if (result != null) {
                    alertPublisher.publish(result);
                    lastAlertTime.put(result.getRootCauseService(), System.currentTimeMillis());
                }
            }
        }
    }

    private double computeAnomalyScore(String serviceKey, double errorRate, double p99Latency) {
        double errorScore = 0.0;
        double latencyScore = 0.0;

        if (errorRate > config.getErrorRateThreshold()) {
            errorScore = Math.min(1.0, errorRate / config.getErrorRateThreshold() - 1.0);
        }

        if (p99Latency > config.getP99LatencyThresholdMs()) {
            latencyScore = Math.min(1.0, p99Latency / config.getP99LatencyThresholdMs() - 1.0);
        }

        BaselineStore.ServiceBaseline baseline = baselineStore.getBaseline(serviceKey);
        if (baseline != null && baseline.getEmaP99Latency() > 0) {
            double spikeRatio = p99Latency / baseline.getEmaP99Latency();
            if (spikeRatio > config.getLatencySpikeMultiplier()) {
                latencyScore = Math.max(latencyScore,
                        Math.min(1.0, (spikeRatio - config.getLatencySpikeMultiplier()) / config.getLatencySpikeMultiplier()));
            }
        }

        return Math.max(errorScore, latencyScore);
    }

    private boolean shouldAlert(String serviceName) {
        Long lastTime = lastAlertTime.get(serviceName);
        if (lastTime == null) {
            return true;
        }
        return System.currentTimeMillis() - lastTime > DEBOUNCE_INTERVAL_MS;
    }

    public boolean isAnomalous(String serviceName) {
        double errorRate = baselineStore.getLatestErrorRate(serviceName);
        double p99 = baselineStore.getLatestP99(serviceName);
        return errorRate > config.getErrorRateThreshold() || p99 > config.getP99LatencyThresholdMs();
    }
}
