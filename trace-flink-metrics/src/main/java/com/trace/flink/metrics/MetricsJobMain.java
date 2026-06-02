package com.trace.flink.metrics;

import com.trace.common.constants.KafkaTopics;
import com.trace.common.model.Span;
import com.trace.flink.metrics.deserializer.SpanDeserializationSchema;
import com.trace.flink.metrics.function.MetricsAggregator;
import com.trace.flink.metrics.function.MetricsWindowFunction;
import com.trace.flink.metrics.function.SpanKeySelector;
import com.trace.flink.metrics.model.MetricsAccumulator;
import com.trace.flink.metrics.serializer.MetricPointSerializationSchema;
import com.trace.common.model.MetricPoint;
import org.apache.flink.api.common.eventtime.SerializableTimestampAssigner;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.time.Duration;

public class MetricsJobMain {

    public static void main(String[] args) throws Exception {
        String kafkaBrokers = getParam(args, "kafka.brokers", "localhost:9092");

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.enableCheckpointing(60000);

        KafkaSource<Span> source = KafkaSource.<Span>builder()
                .setBootstrapServers(kafkaBrokers)
                .setTopics(KafkaTopics.TRACE_SPANS)
                .setGroupId("flink-metrics-group")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setDeserializer(new SpanDeserializationSchema())
                .build();

        WatermarkStrategy<Span> watermarkStrategy = WatermarkStrategy
                .<Span>forBoundedOutOfOrderness(Duration.ofSeconds(5))
                .withTimestampAssigner(
                        (SerializableTimestampAssigner<Span>) (span, ts) -> span.getStartTime());

        DataStream<Span> spanStream = env.fromSource(source, watermarkStrategy, "kafka-span-source");

        DataStream<MetricPoint> metricsStream = spanStream
                .filter(span -> "SERVER".equals(span.getSpanKind()))
                .keyBy(new SpanKeySelector())
                .window(SlidingEventTimeWindows.of(Time.seconds(60), Time.seconds(10)))
                .aggregate(new MetricsAggregator(), new MetricsWindowFunction());

        KafkaSink<MetricPoint> sink = KafkaSink.<MetricPoint>builder()
                .setBootstrapServers(kafkaBrokers)
                .setRecordSerializer(
                        KafkaRecordSerializationSchema.builder()
                                .setTopic(KafkaTopics.TRACE_METRICS)
                                .setValueSerializationSchema(new MetricPointSerializationSchema())
                                .build())
                .build();

        metricsStream.sinkTo(sink);

        env.execute("Trace Metrics Job");
    }

    private static String getParam(String[] args, String key, String defaultValue) {
        for (String arg : args) {
            if (arg.startsWith("--" + key + "=")) {
                return arg.substring(("--" + key + "=").length());
            }
        }
        return defaultValue;
    }
}
