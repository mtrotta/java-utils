package org.matteo.utils.concurrency.dequeuer;

import org.junit.jupiter.api.Test;
import org.matteo.utils.exception.ExceptionHandler;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 13/01/13
 */
class ChainedDequeuerTest {

    private class FakeProcessor implements Processor<String> {
        volatile int ctr = 0;

        @Override
        public void process(String s) throws Exception {
            Thread.sleep(1);
            synchronized (this) {
                ctr++;
            }
        }

        @Override
        public void terminate() {

        }
    }

    @Test
    void testQueue() throws Exception {
        for (int n = 0; n < 3; n++) {
            System.out.println("Iteration " + n);
            int threads = 3;

            FakeProcessor processor1 = new FakeProcessor();
            FakeProcessor processor2 = new FakeProcessor();

            final ChainedDequeuer<String> worker1 = new ChainedDequeuer<>(processor1, threads);
            final ChainedDequeuer<String> worker2 = new ChainedDequeuer<>(processor2, threads);

            final ChainedDequeuer<String> root = worker1.append(worker2).getRoot();

            final int num = 10;
            for (int i = 0; i < num; i++) {
                root.enqueue(String.valueOf(i));
            }
            System.out.println("Queue full");

            root.awaitTermination(1, TimeUnit.HOURS);

            assertEquals(num, processor1.ctr);
            assertEquals(num, processor2.ctr);
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

        @Override
        public void terminate() {
        }
    }

    @Test
    void testChainedQueueBadProcessor() throws Exception {
        ConditionalBadProcessor processorSuccess = new ConditionalBadProcessor(false);
        final ChainedDequeuer<String> dequeuer1 = new ChainedDequeuer<>(processorSuccess, 1);

        ConditionalBadProcessor processorFail = new ConditionalBadProcessor(true);
        final ChainedDequeuer<String> dequeuer2 = new ChainedDequeuer<>(processorFail, 1);

        final ChainedDequeuer<String> root = dequeuer1.append(dequeuer2).getRoot();

        final int num = 15;
        try {
            for (int i = 0; i < num; i++) {
                root.enqueue(String.valueOf(i));
            }
        } catch (RejectedObjectException ignore) {
        }
        try {
            root.awaitTermination(1, TimeUnit.HOURS);
        } catch (Exception e) {
            assertSame(SIMULATED_EXCEPTION, e);
        }
        assertTrue(processorSuccess.ctr.get() > 0);
        assertEquals(1, processorFail.ctr.get());
        assertTrue(root.isTerminated());
        assertTrue(root.isAborted());
        assertTrue(dequeuer1.isAborted());
        assertTrue(dequeuer2.isAborted());
        assertTrue(dequeuer1.isTerminated());
        assertTrue(dequeuer2.isTerminated());
        assertTrue(dequeuer1.isAborted());
        assertTrue(dequeuer2.isAborted());
        assertTrue(dequeuer1.isAborted());
        assertSame(SIMULATED_EXCEPTION, dequeuer2.getExceptionHandler().getException());
    }

    @Test
    void testChainedQueueBadProcessorWithShutdownAction() throws Exception {
        final AtomicInteger ctr = new AtomicInteger();
        Processor<String> processor = new Processor<String>() {
            @Override
            public void process(String s) {
                ctr.incrementAndGet();
                throw SIMULATED_EXCEPTION;
            }

            @Override
            public void terminate() {
            }
        };
        final Dequeuer<String> dequeuer = new Dequeuer<>(processor, true, 1);
        ExceptionHandler exceptionHandler = dequeuer.getExceptionHandler();
        exceptionHandler.register(() -> sentinel = true);
        dequeuer.enqueue("A");
        try {
            dequeuer.awaitTermination(1, TimeUnit.HOURS);
        } catch (Exception e) {
            assertSame(SIMULATED_EXCEPTION, e);
        }
        assertEquals(1, ctr.get());
        assertEquals(0, dequeuer.getUnprocessed().size());
        assertTrue(dequeuer.isTerminated());
        assertTrue(dequeuer.isAborted());
        assertTrue(dequeuer.isAborted());
        assertSame(SIMULATED_EXCEPTION, exceptionHandler.getException());
        assertTrue(sentinel);
    }

}
