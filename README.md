# Distributed Trace System

轻量级分布式链路追踪系统，基于 Java Agent + Byte Buddy 无侵入埋点。

## 模块说明

| 模块 | 说明 |
|------|------|
| `trace-common` | 共享数据模型（Span/MetricPoint）、工具类、常量 |
| `trace-reporter` | Kafka 异步批量上报器 |
| `trace-agent` | Java Agent 探针，拦截 Servlet/Dubbo/HttpClient |
| `trace-collector-graph` | 消费 Kafka span 数据，构建 Neo4j 服务依赖图 |
| `trace-flink-metrics` | Flink 实时计算：成功率、P50/P99 延迟 |
| `trace-rca` | 根因分析：异常检测 + 依赖图 DFS 定位问题源头 |

## 技术栈

- Java 8 + Maven
- Byte Buddy 1.14.12（字节码增强）
- Apache Kafka 3.6.1（数据管道）
- Neo4j 5.x（图数据库）
- Apache Flink 1.17.2（流计算）
- Spring Boot 2.7.18（服务框架）

## 快速开始

### 编译

```bash
mvn clean package -DskipTests
```

### 使用 Agent

```bash
java -javaagent:trace-agent/target/trace-agent-1.0.0-SNAPSHOT.jar=serviceName=order-service,kafkaBrokers=localhost:9092 -jar your-app.jar
```

### 启动 Collector（需要 Neo4j + Kafka）

```bash
java -jar trace-collector-graph/target/trace-collector-graph-1.0.0-SNAPSHOT.jar
```

### 提交 Flink Job

```bash
flink run trace-flink-metrics/target/trace-flink-metrics-1.0.0-SNAPSHOT.jar --kafka.brokers localhost:9092
```

### 启动根因分析服务

```bash
java -jar trace-rca/target/trace-rca-1.0.0-SNAPSHOT.jar
```

## 架构流程

```
[Service A] --agent--> Kafka("trace-spans") --+--> [Collector] --> Neo4j (依赖图)
[Service B] --agent-->                        |
[Service C] --agent-->                        +--> [Flink Job] --> Kafka("trace-metrics")
                                                                         |
                                                                         v
                                                               [RCA Service] --> 告警
```

## Agent 参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `serviceName` | unknown-service | 当前服务名称 |
| `kafkaBrokers` | localhost:9092 | Kafka 集群地址 |
| `queueCapacity` | 10000 | 内部队列容量 |
| `batchSize` | 100 | 批量发送大小 |
| `flushIntervalMs` | 1000 | 强制刷新间隔(ms) |
