package org.matteo.utils.concurrency.dequeuer;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 02/07/12
 */
class SingleBalancedDequeuerTest {

    public static class StringProcessor implements Processor<String> {

        private final AtomicInteger ctr = new AtomicInteger();

        StringProcessor() {
            System.out.println("Created single processor");
        }

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

        @Override
        public void terminate() {
            System.out.println("Terminated single processor");
        }
    }

    public static class StupidProcessor implements Processor<String> {

        private final AtomicInteger ctr = new AtomicInteger();

        private final String name;

        StupidProcessor(String name) {
            this.name = name;
        }

        @Override
        public void process(String s) {
            try {
                Thread.sleep(ctr.incrementAndGet() % 4000 == 0 ? 10 : 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void terminate() {
            System.out.println("Terminated " + name);
        }
    }

    public static class SmartProcessor implements Processor<Collection<String>> {

        private final String name;

        SmartProcessor(String name) {
            this.name = name;
            System.out.println("Created " + name);
        }

        @Override
        public void process(Collection<String> strings) {
            try {
                for (String s : strings) {
                    Thread.sleep(0);
                }
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void terminate() {
            System.out.println("Terminated " + name);
        }
    }

    @Test
    void testBalanceSingleProcessor() throws Exception {
        long begin = System.currentTimeMillis();
        final Dequeuer<String> dequeuer = new SingleBalancedDequeuer<>(new StringProcessor(), true, 1, 20, 1);
        final int num = 15000;
        for (int i = 0; i < num; i++) {
            dequeuer.enqueue(String.valueOf(i));
        }
        System.out.println("Queue full");
        dequeuer.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(dequeuer.isTerminated());
        long end = System.currentTimeMillis();
        System.out.println(end - begin);
    }

    @Test
    void testBalanceFactory() throws Exception {
        final Dequeuer<String> dequeuer = new SingleBalancedDequeuer<>(StringProcessor::new, false, 1, 20, 20);
        final int num = 50000;
        for (int i = 0; i < num; i++) {
            dequeuer.enqueue(String.valueOf(i));
        }
        System.out.println("Queue full");
        dequeuer.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(dequeuer.isTerminated());
    }

    public static class ThreadUnsafeProcessor implements Processor<String> {

        private final Collection<String> collection = new HashSet<>(1);

        ThreadUnsafeProcessor() {
            System.out.println("Created");
        }

        @Override
        public void process(String string) throws Exception {
            collection.add(string);
            for (String s : collection) {
                Thread.sleep(0);
            }
            collection.remove(string);
        }

        @Override
        public void terminate() {
            System.out.println("Terminated");
        }
    }

    @Test
    void testBalanceFactoryMin() throws Exception {
        final Dequeuer<String> dequeuer = new SingleBalancedDequeuer<>(ThreadUnsafeProcessor::new, false, 1, 5, 1);
        final long num = 1 << 22;
        for (long i = 0; i < num; i++) {
            dequeuer.enqueue(String.valueOf(i));
        }
        System.out.println("-----Before sleep-----");
        Thread.sleep(10000);
        System.out.println("-----After sleep-----");
        for (long i = 0; i < num; i++) {
            dequeuer.enqueue(String.valueOf(i));
        }
        dequeuer.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(dequeuer.isTerminated());
    }

    @Test
    void testBalanceStupid() throws Exception {
        List<StupidProcessor> processors = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            processors.add(new StupidProcessor(Integer.toString(i)));
        }
        final SingleBalancedDequeuer<String> dequeuer = new SingleBalancedDequeuer<>(processors, false, 1, 1);
        dequeuer.setProfile(SingleBalancedDequeuer.Profile.FAST);
        final int num = 1 << 20;
        for (int i = 0; i < num; i++) {
            dequeuer.enqueue(String.valueOf(i));
        }
        dequeuer.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(dequeuer.isTerminated());
    }

    @Test
    void testBalanceSmart() throws Exception {
        List<SmartProcessor> processors = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            processors.add(new SmartProcessor(Integer.toString(i)));
        }
        final SingleBalancedDequeuer<Collection<String>> dequeuer = new SingleBalancedDequeuer<>(processors, true, 1, 1);
        SingleBalancedDequeuer.Profile profile = SingleBalancedDequeuer.Profile.FAST;
        dequeuer.setProfile(profile);
        final int num = 1 << 24;
        List<String> list = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            list.add(Integer.toString(i));
            if (list.size() >= 4000) {
                dequeuer.enqueue(list);
                list = new ArrayList<>();
            }
        }
        dequeuer.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(dequeuer.isTerminated());
        assertEquals(profile, dequeuer.getProfile());
    }

    @Test
    void testBalanceBigDecimal() throws Exception {
        Supplier<Processor<String>> supplier = () -> new Processor<String>() {
            @Override
            public void process(String s) {
                BigDecimal bigDecimal = new BigDecimal(s);
                double d = bigDecimal.doubleValue();
            }

            @Override
            public void terminate() {

            }
        };
        final SingleBalancedDequeuer<String> dequeuer = new SingleBalancedDequeuer<>(supplier, true, 1, 10, 1);
        dequeuer.setProfile(SingleBalancedDequeuer.Profile.SLOW);
        final long num = 1 << 23;
        for (long i = 0; i < num; i++) {
            dequeuer.enqueue(String.valueOf(Math.random()));
        }
        dequeuer.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(dequeuer.isTerminated());
    }

    @Test
    void testBalanceDouble() throws Exception {
        Supplier<Processor<String>> supplier = () -> new Processor<String>() {
            @Override
            public void process(String s) {
                Double d = Double.parseDouble(s);
            }

            @Override
            public void terminate() {
            }
        };
        final Dequeuer<String> dequeuer = new SingleBalancedDequeuer<>(supplier, true, 1, 10, 1);
        final long num = 1 << 23;
        for (long i = 0; i < num; i++) {
            dequeuer.enqueue(String.valueOf(Math.random()));
        }
        dequeuer.awaitTermination(1, TimeUnit.HOURS);
        assertTrue(dequeuer.isTerminated());
    }

    private static final RuntimeException SIMULATED_EXCEPTION = new RuntimeException("Simulated exception");

    @Test
    void testQueueBadProcessor() throws Exception {
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
        final Dequeuer<String> dequeuer = new SingleBalancedDequeuer<>(processor);
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
        assertTrue(dequeuer.isAborted());
    }


}
