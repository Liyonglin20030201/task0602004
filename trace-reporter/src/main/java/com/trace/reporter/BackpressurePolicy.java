package com.trace.reporter;

import com.trace.common.model.Span;

import java.util.concurrent.ArrayBlockingQueue;

public final class BackpressurePolicy {

    public enum Action {
        DROP,
        BLOCK_BRIEFLY
    }

    private final Action action;
    private final long blockTimeoutMs;

    public BackpressurePolicy(Action action, long blockTimeoutMs) {
        this.action = action;
        this.blockTimeoutMs = blockTimeoutMs;
    }

    public static BackpressurePolicy dropOnFull() {
        return new BackpressurePolicy(Action.DROP, 0);
    }

    public static BackpressurePolicy blockBriefly(long timeoutMs) {
        return new BackpressurePolicy(Action.BLOCK_BRIEFLY, timeoutMs);
    }

    public boolean offer(ArrayBlockingQueue<Span> queue, Span span) {
        if (action == Action.DROP) {
            return queue.offer(span);
        } else {
            try {
                return queue.offer(span, blockTimeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
}
