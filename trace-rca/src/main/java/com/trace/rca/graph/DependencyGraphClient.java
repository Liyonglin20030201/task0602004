package com.trace.rca.graph;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DependencyGraphClient {

    private final Driver driver;

    public DependencyGraphClient(Driver driver) {
        this.driver = driver;
    }

    public List<String> getDownstreamServices(String serviceName) {
        List<String> downstreams = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(
                    "MATCH (:Service {name: $name})-[:CALLS]->(downstream:Service) " +
                            "RETURN downstream.name AS name",
                    Values.parameters("name", serviceName)
            );
            while (result.hasNext()) {
                Record record = result.next();
                downstreams.add(record.get("name").asString());
            }
        }
        return downstreams;
    }

    public List<String> getUpstreamServices(String serviceName) {
        List<String> upstreams = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(
                    "MATCH (upstream:Service)-[:CALLS]->(:Service {name: $name}) " +
                            "RETURN upstream.name AS name",
                    Values.parameters("name", serviceName)
            );
            while (result.hasNext()) {
                Record record = result.next();
                upstreams.add(record.get("name").asString());
            }
        }
        return upstreams;
    }

    public List<String> getAllServices() {
        List<String> services = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run("MATCH (s:Service) RETURN s.name AS name");
            while (result.hasNext()) {
                Record record = result.next();
                services.add(record.get("name").asString());
            }
        }
        return services;
    }
}
