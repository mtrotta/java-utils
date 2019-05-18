package org.matteo.utils.concurrency.dequeuer;

import org.junit.jupiter.api.Test;
import org.matteo.utils.concurrency.exception.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 02/07/12
 */
class BasicDequeuerTest {

    public static class StringProcessor implements Processor<String> {

        private final AtomicInteger ctr = new AtomicInteger();

        @Override
        public void process(String s) {
            try {
                synchronized (this) {
                    Thread.sleep(1);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            ctr.incrementAndGet();
        }
    }

    @Test
    void testQueue() throws Exception {
        int threads = 10;
        StringProcessor processor = new StringProcessor();
        final Dequeuer<String> dequeuer = new BasicDequeuer<>(processor, false, threads);
        final int num = 1 << 10;
        for (int i = 0; i < num; i++) {
            dequeuer.enqueue(String.valueOf(i));
        }
        assertTrue(dequeuer.awaitTermination(1, TimeUnit.HOURS));
        assertEquals(num, processor.ctr.get());
        assertTrue(dequeuer.isTerminated());
    }

    @Test
    void testQueueTimeout() throws Exception {
        int threads = 10;
        StringProcessor processor = new StringProcessor();
        final Dequeuer<String> dequeuer = new BasicDequeuer<>(processor, false, threads);
        final int num = 1 << 12;
        for (int i = 0; i < num; i++) {
            dequeuer.enqueue(String.valueOf(i));
        }
        assertFalse(dequeuer.awaitTermination(1, TimeUnit.SECONDS));
        assertTrue(num != processor.ctr.get());
        assertTrue(dequeuer.isTerminated());
    }

    @Test
    void testQueueMultiProcessor() throws Exception {
        int threads = 10;
        List<StringProcessor> processors = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            processors.add(new StringProcessor());
        }
        final Dequeuer<String> dequeuer = new BasicDequeuer<>(processors, true);
        final int num = 1 << 14;
        for (int i = 0; i < num; i++) {
            dequeuer.enqueue(String.valueOf(i));
        }
        System.out.println("Queue full");
        dequeuer.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(dequeuer.isTerminated());
    }

    private static final RuntimeException SIMULATED_EXCEPTION = new RuntimeException("Simulated exception");

    private boolean sentinel;

    @Test
    void testQueueBadProcessor() throws Exception {
        final AtomicInteger ctr = new AtomicInteger();
        Processor<String> processor = s -> {
            ctr.incrementAndGet();
            throw SIMULATED_EXCEPTION;
        };
        final Dequeuer<String> dequeuer = new BasicDequeuer<>(processor, true, 1);
        final int num = 15;
        try {
            for (int i = 0; i < num; i++) {
                dequeuer.enqueue(String.valueOf(i));
            }
            fail();
        } catch (RejectedObjectException ignore) {
        }
        try {
            dequeuer.awaitTermination(1, TimeUnit.HOURS);
        } catch (Exception e) {
            assertSame(SIMULATED_EXCEPTION, e);
        }
        assertEquals(1, ctr.get());
        assertTrue(dequeuer.isTerminated());
    }

    @Test
    void testQueueBadProcessorWithShutdownAction() throws Exception {
        final AtomicInteger ctr = new AtomicInteger();
        Processor<String> processor = s -> {
            ctr.incrementAndGet();
            Thread.sleep(1000);
            throw SIMULATED_EXCEPTION;
        };
        final Dequeuer<String> dequeuer = new BasicDequeuer<>(processor, true, 1);
        ExceptionHandler exceptionHandler = dequeuer.getExceptionHandler();
        exceptionHandler.register(() -> sentinel = true);
        exceptionHandler.register(Exception::printStackTrace);
        dequeuer.enqueue("A");
        try {
            dequeuer.awaitTermination(1, TimeUnit.HOURS);
        } catch (Exception e) {
            assertSame(SIMULATED_EXCEPTION, e);
        }
        assertEquals(1, ctr.get());
        assertEquals(0, dequeuer.getUnprocessed().size());
        assertTrue(dequeuer.isTerminated());
        assertSame(SIMULATED_EXCEPTION, dequeuer.getExceptionHandler().getException());
        assertTrue(sentinel);
    }

    @Test
    void testQueueBadProcessorWithShutdownActionTerminate() {
        final AtomicInteger ctr = new AtomicInteger();
        Processor<String> processor = s -> {
            ctr.incrementAndGet();
            throw SIMULATED_EXCEPTION;
        };
        final Dequeuer<String> dequeuer = new BasicDequeuer<>(processor, true, 1);
        ExceptionHandler exceptionHandler = dequeuer.getExceptionHandler();
        exceptionHandler.register(() -> sentinel = true);
        exceptionHandler.register(Exception::printStackTrace);
        try {
            dequeuer.enqueue("A");
            dequeuer.enqueue("B");
            dequeuer.enqueue("C");
        } catch (RejectedObjectException | InterruptedException ignore) {
        }
        try {
            dequeuer.awaitTermination(1, TimeUnit.HOURS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        assertTrue(dequeuer.isTerminated());
        assertSame(SIMULATED_EXCEPTION, dequeuer.getExceptionHandler().getException());
        assertTrue(sentinel);
    }

}
