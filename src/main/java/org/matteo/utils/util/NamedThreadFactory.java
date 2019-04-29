package org.matteo.utils.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThreadFactory implements ThreadFactory {

    private final String name;
    private final AtomicInteger counter = new AtomicInteger();

    public NamedThreadFactory(String name) {
        this.name = name;
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        return new Thread(runnable, String.format("%s-%d", name, counter.incrementAndGet()));
    }
}
