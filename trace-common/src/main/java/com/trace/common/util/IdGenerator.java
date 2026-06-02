package com.trace.common.util;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class IdGenerator {

    private IdGenerator() {
    }

    public static String generateTraceId() {
        UUID uuid = UUID.randomUUID();
        return String.format("%016x%016x", uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());
    }

    public static String generateSpanId() {
        long id = ThreadLocalRandom.current().nextLong();
        return String.format("%016x", id);
    }
}
