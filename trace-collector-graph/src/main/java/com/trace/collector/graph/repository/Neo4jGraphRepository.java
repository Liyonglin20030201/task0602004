package com.trace.collector.graph.repository;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;

@Repository
public class Neo4jGraphRepository {

    private final Driver driver;

    public Neo4jGraphRepository(Driver driver) {
        this.driver = driver;
    }

    public void upsertService(String serviceName, long timestamp) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                tx.run(
                        "MERGE (s:Service {name: $name}) " +
                                "ON CREATE SET s.firstSeen = $ts " +
                                "SET s.lastSeen = $ts",
                        Values.parameters("name", serviceName, "ts", timestamp)
                );
                return null;
            });
        }
    }

    public void upsertDependency(String callerService, String calleeService,
                                 String operationName, long durationMs,
                                 boolean isError, long timestamp) {
        try (Session session = driver.session()) {
            session.writeTransaction(tx -> {
                Map<String, Object> params = new HashMap<>();
                params.put("caller", callerService);
                params.put("callee", calleeService);
                params.put("op", operationName != null ? operationName : "unknown");
                params.put("duration", durationMs);
                params.put("errorCount", isError ? 1 : 0);
                params.put("ts", timestamp);

                tx.run(
                        "MATCH (caller:Service {name: $caller}) " +
                                "MATCH (callee:Service {name: $callee}) " +
                                "MERGE (caller)-[r:CALLS {operationName: $op}]->(callee) " +
                                "ON CREATE SET r.callCount = 1, r.totalLatencyMs = $duration, " +
                                "r.errorCount = $errorCount, r.lastUpdated = $ts " +
                                "ON MATCH SET r.callCount = r.callCount + 1, " +
                                "r.totalLatencyMs = r.totalLatencyMs + $duration, " +
                                "r.errorCount = r.errorCount + $errorCount, " +
                                "r.lastUpdated = $ts",
                        params
                );
                return null;
            });
        }
    }
}
