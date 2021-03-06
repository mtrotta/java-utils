package org.matteo.utils.concurrency.dequeuer;

import org.junit.jupiter.api.Test;
import org.matteo.utils.concurrency.exception.ExceptionHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 13/01/13
 */
class ChainedDequeuerTest {

    @Test
    void testEmpty() {
        assertThrows(IllegalArgumentException.class, () -> new ChainedDequeuer<>(new ArrayList<>()));
    }

    @Test
    void testSingle() throws Exception {
        Dequeuer<String> dequeuer = new ChainedDequeuer<>(Collections.singletonList(
                new BasicDequeuer<>(new FakeProcessor())));
        dequeuer.enqueue("1");
        dequeuer.shutdown();
        dequeuer.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(dequeuer.isTerminated());
    }

    @Test
    void testQueue() throws Exception {
        for (int n = 0; n < 3; n++) {
            System.out.println("Iteration " + n);
            int threads = 3;

            FakeProcessor processor1 = new FakeProcessor();
            FakeProcessor processor2 = new FakeProcessor();

            final BasicDequeuer<String> dequeuer1 = new BasicDequeuer<>(processor1, threads);
            final BasicDequeuer<String> dequeuer2 = new BasicDequeuer<>(processor2, threads);

            final Dequeuer<String> chainedDequeuer = new ChainedDequeuer<>(Arrays.asList(dequeuer1, dequeuer2));

            final int num = 10;
            for (int i = 0; i < num; i++) {
                chainedDequeuer.enqueue(String.valueOf(i));
            }
            System.out.println("Queue full");

            chainedDequeuer.awaitTermination(1, TimeUnit.HOURS);

            assertTrue(chainedDequeuer.isTerminated());

            assertEquals(num, processor1.ctr.get());
            assertEquals(num, processor2.ctr.get());
        }
    }

    private final static RuntimeException SIMULATED_EXCEPTION = new RuntimeException("Simulated exception");

    private boolean sentinel;

    private static class ConditionalBadProcessor implements Processor<String> {

        final boolean goBad;
        final AtomicInteger ctr = new AtomicInteger();

        ConditionalBadProcessor(boolean goBad) {
            this.goBad = goBad;
        }

        @Override
        public void process(String s) {
            ctr.incrementAndGet();
            if (goBad) {
                throw SIMULATED_EXCEPTION;
            }
        }
    }

    @Test
    void testChainedQueueBadProcessor() throws Exception {
        ConditionalBadProcessor processorSuccess = new ConditionalBadProcessor(false);
        final BasicDequeuer<String> dequeuer1 = new BasicDequeuer<>(processorSuccess, 1);

        ConditionalBadProcessor processorFail = new ConditionalBadProcessor(true);
        final BasicDequeuer<String> dequeuer2 = new BasicDequeuer<>(processorFail, 1);

        final ChainedDequeuer<String> chainedDequeuer = new ChainedDequeuer<>(Arrays.asList(dequeuer1, dequeuer2));

        ExceptionHandler exceptionHandler = chainedDequeuer.getExceptionHandler();
        exceptionHandler.register(() -> sentinel = true);

        final int num = 15;
        try {
            for (int i = 0; i < num; i++) {
                chainedDequeuer.enqueue(String.valueOf(i));
            }
        } catch (RejectedObjectException ignore) {
        }
        try {
            chainedDequeuer.awaitTermination(1, TimeUnit.HOURS);
        } catch (Exception e) {
            assertSame(SIMULATED_EXCEPTION, e);
        }
        assertTrue(processorSuccess.ctr.get() > 0);
        assertEquals(1, processorFail.ctr.get());
        assertTrue(chainedDequeuer.isTerminated());
        assertTrue(dequeuer1.isTerminated());
        assertTrue(dequeuer2.isTerminated());
        assertSame(SIMULATED_EXCEPTION, dequeuer2.getExceptionHandler().getException());
        assertTrue(sentinel);
    }

    private class FakeProcessor implements Processor<String> {
        AtomicInteger ctr = new AtomicInteger();

        @Override
        public void process(String s) throws Exception {
            Thread.sleep(1);
            ctr.incrementAndGet();
        }
    }

}
