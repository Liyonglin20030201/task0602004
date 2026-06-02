package com.trace.rca.analysis;

import com.trace.rca.detector.AnomalyDetector;
import com.trace.rca.detector.BaselineStore;
import com.trace.rca.graph.DependencyGraphClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class RootCauseAnalyzer {

    private static final Logger logger = LoggerFactory.getLogger(RootCauseAnalyzer.class);

    private final DependencyGraphClient graphClient;
    private final BaselineStore baselineStore;
    private final AnomalyDetector anomalyDetector;

    public RootCauseAnalyzer(DependencyGraphClient graphClient, BaselineStore baselineStore,
                             @Lazy AnomalyDetector anomalyDetector) {
        this.graphClient = graphClient;
        this.baselineStore = baselineStore;
        this.anomalyDetector = anomalyDetector;
    }

    public RootCauseResult analyze(String anomalousService) {
        logger.info("Starting root cause analysis from service: {}", anomalousService);

        Set<String> visited = new HashSet<>();
        List<String> path = new ArrayList<>();
        path.add(anomalousService);

        Candidate result = findRootCause(anomalousService, visited, path, 0);

        if (result != null) {
            RootCauseResult rcaResult = new RootCauseResult();
            rcaResult.setRootCauseService(result.service);
            rcaResult.setDependencyPath(result.path);
            rcaResult.setErrorRate(baselineStore.getLatestErrorRate(result.service));
            rcaResult.setP99Latency(baselineStore.getLatestP99(result.service));
            logger.info("Root cause identified: {}", rcaResult);
            return rcaResult;
        }

        RootCauseResult selfResult = new RootCauseResult();
        selfResult.setRootCauseService(anomalousService);
        selfResult.setDependencyPath(path);
        selfResult.setErrorRate(baselineStore.getLatestErrorRate(anomalousService));
        selfResult.setP99Latency(baselineStore.getLatestP99(anomalousService));
        return selfResult;
    }

    private Candidate findRootCause(String service, Set<String> visited, List<String> currentPath, int depth) {
        visited.add(service);

        List<String> downstreams = graphClient.getDownstreamServices(service);
        Candidate deepest = null;

        for (String downstream : downstreams) {
            if (visited.contains(downstream)) {
                continue;
            }

            if (anomalyDetector.isAnomalous(downstream)) {
                List<String> newPath = new ArrayList<>(currentPath);
                newPath.add(downstream);

                Candidate candidate = findRootCause(downstream, visited, newPath, depth + 1);
                if (candidate != null) {
                    if (deepest == null || candidate.depth > deepest.depth) {
                        deepest = candidate;
                    }
                } else {
                    Candidate leaf = new Candidate(downstream, newPath, depth + 1);
                    if (deepest == null || leaf.depth > deepest.depth) {
                        deepest = leaf;
                    }
                }
            }
        }

        return deepest;
    }

    private static class Candidate {
        final String service;
        final List<String> path;
        final int depth;

        Candidate(String service, List<String> path, int depth) {
            this.service = service;
            this.path = path;
            this.depth = depth;
        }
    }
}
