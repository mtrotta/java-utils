package org.matteo.utils.concurrency;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.*;

class AsyncTest {

    private boolean completed;

    @Test
    void basic() throws Exception {
        assertFalse(completed);
        Future<?> future = Async.run(() -> completed = true);
        future.get();
        assertTrue(completed);
    }
}