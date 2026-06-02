package com.trace.reporter;

import com.trace.common.constants.KafkaTopics;
import com.trace.common.model.Span;
import com.trace.common.util.SpanJsonCodec;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SpanBatchSender {

    private static final Logger logger = LoggerFactory.getLogger(SpanBatchSender.class);

    private final ArrayBlockingQueue<Span> queue;
    private final KafkaProducer<String, String> producer;
    private final int batchSize;
    private final long flushIntervalMs;
    private final Thread senderThread;
    private volatile boolean running = true;

    public SpanBatchSender(ArrayBlockingQueue<Span> queue, String kafkaBrokers, int batchSize, long flushIntervalMs) {
        this.queue = queue;
        this.producer = KafkaProducerFactory.create(kafkaBrokers);
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
        this.senderThread = new Thread(this::sendLoop, "trace-span-sender");
        this.senderThread.setDaemon(true);
    }

    public void start() {
        senderThread.start();
    }

    private void sendLoop() {
        List<Span> batch = new ArrayList<>(batchSize);
        long lastFlushTime = System.currentTimeMillis();

        while (running) {
            try {
                Span span = queue.poll(100, TimeUnit.MILLISECONDS);
                if (span != null) {
                    batch.add(span);
                }

                long now = System.currentTimeMillis();
                boolean shouldFlush = batch.size() >= batchSize
                        || (now - lastFlushTime >= flushIntervalMs && !batch.isEmpty());

                if (shouldFlush) {
                    flush(batch);
                    batch.clear();
                    lastFlushTime = now;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        if (!batch.isEmpty()) {
            flush(batch);
        }
        producer.close();
    }

    private void flush(List<Span> batch) {
        for (Span span : batch) {
            try {
                String json = SpanJsonCodec.encode(span);
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        KafkaTopics.TRACE_SPANS, span.getTraceId(), json);
                producer.send(record, (metadata, exception) -> {
                    if (exception != null) {
                        logger.error("Failed to send span to Kafka", exception);
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to encode span", e);
            }
        }
        producer.flush();
    }

    public void shutdown() {
        running = false;
        try {
            senderThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
