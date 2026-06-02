package com.trace.reporter;

import com.trace.common.model.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

public class SpanReporter {

    private static final Logger logger = LoggerFactory.getLogger(SpanReporter.class);

    private static volatile SpanReporter instance;

    private final ArrayBlockingQueue<Span> queue;
    private final SpanBatchSender batchSender;
    private final AtomicLong droppedCount = new AtomicLong(0);

    private SpanReporter(String kafkaBrokers, int queueCapacity, int batchSize, long flushIntervalMs) {
        this.queue = new ArrayBlockingQueue<>(queueCapacity);
        this.batchSender = new SpanBatchSender(queue, kafkaBrokers, batchSize, flushIntervalMs);
        this.batchSender.start();
    }

    public static void initialize(String kafkaBrokers, int queueCapacity, int batchSize, long flushIntervalMs) {
        if (instance == null) {
            synchronized (SpanReporter.class) {
                if (instance == null) {
                    instance = new SpanReporter(kafkaBrokers, queueCapacity, batchSize, flushIntervalMs);
                    logger.info("SpanReporter initialized: brokers={}, queue={}, batch={}, flush={}ms",
                            kafkaBrokers, queueCapacity, batchSize, flushIntervalMs);
                }
            }
        }
    }

    public static void initialize(String kafkaBrokers) {
        initialize(kafkaBrokers, 10000, 100, 1000);
    }

    public static SpanReporter getInstance() {
        return instance;
    }

    public void report(Span span) {
        if (!queue.offer(span)) {
            long count = droppedCount.incrementAndGet();
            if (count % 1000 == 1) {
                logger.warn("Span queue full, dropped {} spans total", count);
            }
        }
    }

    public long getDroppedCount() {
        return droppedCount.get();
    }

    public int getQueueSize() {
        return queue.size();
    }

    public void shutdown() {
        batchSender.shutdown();
    }
}
