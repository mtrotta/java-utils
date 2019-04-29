package org.matteo.utils.concurrency.dequeuer;

import org.matteo.utils.concurrency.Async;
import org.matteo.utils.exception.ExceptionHandler;
import org.matteo.utils.exception.ShutdownAction;
import org.matteo.utils.util.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 13/07/12
 */
public class Dequeuer<T> implements ShutdownAction {

    private static final transient Logger LOGGER = LoggerFactory.getLogger(Dequeuer.class);

    private BlockingQueue<T> queue;

    private String name;
    private ExecutorService service;
    private final Phaser phaser = new Phaser();

    private final List<Worker> workers = new ArrayList<>();
    private final List<Processor<T>> processors = new ArrayList<>();

    private Supplier<? extends Processor<T>> supplier;

    private final Collection<T> unprocessed = new ArrayList<>();

    private volatile boolean shutdown;
    private volatile boolean running;
    private volatile boolean terminated;

    private ExceptionHandler exceptionHandler = new ExceptionHandler();

    private boolean balance;
    private int minWorkers;
    private int maxWorkers;
    private AtomicInteger numWorkers;
    private ScheduledExecutorService scheduledExecutorService;
    private final TreeMap<Integer, Long> reference = new TreeMap<>();

    private Profile profile = Profile.MEDIUM;

    private static final int DEFAULT_MIN = 1;
    private static final int DEFAULT_MAX = Runtime.getRuntime().availableProcessors();

    private static final TimeUnit UNIT = TimeUnit.NANOSECONDS;
    private static final long CLOCK = TimeUnit.SECONDS.toNanos(1);
    private volatile double averageWorkTime = CLOCK;

    private final Runnable analyser = new Runnable() {
        @Override
        public void run() {
            try {
                if (running) {
                    switch (getWorkerStatus()) {
                        case INCREASE:
                            increaseWorkers();
                            break;
                        case DECREASE:
                        case IDLE:
                            decreaseWorkers();
                    }
                }
            } catch (Exception e) {
                exceptionHandler.handle(e);
            }
        }
    };

    public Dequeuer(Processor<T> processor, boolean synchronous, int threads) {
        init(synchronous);
        processors.add(processor);
        for (int i = 0; i < threads; i++) {
            Worker worker = addWorker(processor);
            startWorker(worker);
        }
    }

    public Dequeuer(Collection<? extends Processor<T>> processors, boolean synchronous) {
        init(synchronous);
        for (Processor<T> processor : processors) {
            this.processors.add(processor);
            Worker worker = addWorker(processor);
            startWorker(worker);
        }
    }

    public Dequeuer(Processor<T> processor, boolean synchronous) {
        this(processor, synchronous, DEFAULT_MIN, DEFAULT_MAX);
    }

    public Dequeuer(final Processor<T> processor, boolean synchronous, int min, int max) {
        this(processor, synchronous, min, max, min);
    }

    public Dequeuer(final Processor<T> processor, boolean synchronous, int min, int max, int initial) {
        init(synchronous);
        processors.add(processor);
        for (int i = 0; i < max; i++) {
            addWorker(processor);
        }
        startBalance(min, max, initial);
    }

    public Dequeuer(final Supplier<? extends Processor<T>> supplier, boolean synchronous) {
        this(supplier, synchronous, DEFAULT_MIN, DEFAULT_MAX);
    }

    public Dequeuer(final Supplier<? extends Processor<T>> supplier, boolean synchronous, int max) {
        this(supplier, synchronous, DEFAULT_MIN, max, DEFAULT_MIN);
    }

    public Dequeuer(final Supplier<? extends Processor<T>> supplier, boolean synchronous, int min, int max) {
        this(supplier, synchronous, min, max, min);
    }

    public Dequeuer(final Supplier<? extends Processor<T>> supplier, boolean synchronous, int min, int max, int initial) {
        this.supplier = supplier;
        init(synchronous);
        for (int i = 0; i < initial; i++) {
            addProcessor();
        }
        startBalance(min, max, initial);
    }

    public Dequeuer(Collection<? extends Processor<T>> processors, boolean synchronous, int min) {
        this(processors, synchronous, min, min);
    }

    public Dequeuer(Collection<? extends Processor<T>> processors, boolean synchronous, int min, int initial) {
        init(synchronous);
        for (Processor<T> processor : processors) {
            this.processors.add(processor);
            addWorker(processor);
        }
        startBalance(min, processors.size(), initial);
    }

    private void init(boolean synchronous) {
        this.name = "Dequeuer";
        service = Executors.newCachedThreadPool(new NamedThreadFactory(name));
        queue = getQueue(synchronous);
        exceptionHandler.register(this);
        running = true;
        phaser.register();
    }

    private BlockingQueue<T> getQueue(boolean synchronous) {
        return synchronous ? new SynchronousQueue<>() : new LinkedBlockingQueue<>();
    }

    private void addProcessor() {
        Processor<T> processor = supplier.get();
        processors.add(processor);
        addWorker(processor);
    }

    private Worker addWorker(Processor<T> processor) {
        Worker worker = createWorker(processor);
        workers.add(worker);
        return worker;
    }

    protected Worker createWorker(Processor<T> processor) {
        return new Worker(processor);
    }

    private void startWorker(Worker worker) {
        service.submit(worker);
    }

    private void stopBalance() {
        running = false;
        if (scheduledExecutorService != null) {
            scheduledExecutorService.shutdownNow();
        }
    }

    public void shutdown() {
        shutdown = true;
    }

    @Override
    public synchronized void shutdownNow() {
        try {
            shutdown = true;
            queue.drainTo(unprocessed);
            stopBalance();
            service.shutdownNow();
        } finally {
            terminate();
        }
    }

    public boolean awaitTermination(long time, TimeUnit unit) throws Exception {
        boolean elapsed = false;
        if (!terminated) {
            try {
                shutdown();
                Async.run(() -> {
                    phaser.arriveAndAwaitAdvance();
                    stopBalance();
                    service.shutdown();
                }, exceptionHandler, name);
                elapsed = service.awaitTermination(time, unit);
                if (exceptionHandler.isExhausted()) {
                    exceptionHandler.waitForShutdown();
                    throw exceptionHandler.getException();
                }
            } finally {
                terminate();
            }
        }
        return elapsed;
    }

    public Collection<T> getUnprocessed() {
        return unprocessed;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public boolean isAborted() {
        return exceptionHandler.isExhausted();
    }

    public void enqueue(T t) throws RejectedObjectException, InterruptedException {
        do {
            if (shutdown || exceptionHandler.isExhausted()) {
                throw new RejectedObjectException("Queue has been shutdown or an exception occurred");
            }
        } while (!queue.offer(t, CLOCK, UNIT));
    }

    private synchronized void terminate() {
        if (!terminated) {
            terminated = true;
            for (Processor<T> processor : processors) {
                try {
                    processor.terminate();
                } catch (Exception e) {
                    exceptionHandler.handle(e);
                }
            }
            exceptionHandler.remove(this);
        }
    }

    private enum WorkerStatus {
        INCREASE,
        STABLE,
        DECREASE,
        IDLE,
        UNAVAILABLE
    }

    public enum Profile {
        FAST(3, 0.9, 0.1, 0.05, true),
        MEDIUM(5, 0.5, 0.5, 0.1, true),
        SLOW(10, 0.1, 0.9, 0.2, false);

        private final int period;
        private final double high;
        private final double low;
        private final double worth;
        private final boolean fluid;

        Profile(int period, double high, double low, double worth, boolean fluid) {
            this.period = period;
            this.high = high;
            this.low = low;
            this.worth = worth;
            this.fluid = fluid;
        }
    }

    private void startBalance(int min, int max, int initial) {
        if (initial < min || initial > max) {
            throw new IllegalArgumentException(String.format("Invalid initial value %d, must be %d <= initial <= %d", initial, min, max));
        }
        setMinThread(min);
        setMaxThread(max);
        numWorkers = new AtomicInteger(initial);
        for (int i = 0; i < initial; i++) {
            startWorker(workers.get(i));
        }
        balance = true;
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DequeuerBalancer"));
        scheduledExecutorService.schedule(analyser, profile.period * CLOCK, UNIT);
    }

    private synchronized void increaseWorkers() {
        try {
            final int num = numWorkers.get();
            if (num < maxWorkers) {
                if (workers.size() <= num) {
                    addProcessor();
                }
                Worker worker = workers.get(numWorkers.getAndIncrement());
                startWorker(worker);
            }
        } catch (Exception e) {
            exceptionHandler.handle(e);
        }
    }

    private synchronized void decreaseWorkers() {
        if (numWorkers.get() > minWorkers) {
            Worker worker = workers.get(numWorkers.decrementAndGet());
            worker.shutdown();
        }
    }

    private synchronized WorkerStatus getWorkerStatus() {
        WorkerStatus status = WorkerStatus.UNAVAILABLE;
        long analysisPeriod = profile.period * CLOCK;
        Collection<Long> results = analyse();
        if (!results.isEmpty()) {
            final int num = numWorkers.get();
            long throughput = getThroughput(results);
            if (throughput == 0) {
                status = WorkerStatus.IDLE;
            } else {
                status = WorkerStatus.STABLE;
                reference.put(num, throughput);
                int higherKey = num + 1;
                int lowerKey = num - 1;
                Long higher = reference.get(higherKey);
                Long lower = reference.get(lowerKey);
                Integer direction = compare(throughput, lower, higher);
                if (num < maxWorkers && (direction == null || direction > 0)) {
                    status = WorkerStatus.INCREASE;
                } else if (num > minWorkers && (direction == null || direction < 0)) {
                    status = WorkerStatus.DECREASE;
                } else if (profile.fluid) {
                    reference.remove(higherKey);
                    reference.remove(lowerKey);
                }
            }
            if (analysisPeriod < averageWorkTime * profile.period) {
                analysisPeriod = (long) (profile.period * averageWorkTime);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Adjusting analysis period to {}", analysisPeriod);
                }
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("Load: %s - Workers: %02d - Current: %d - Reference: %s - Results (%d): %s", status, num, throughput, reference.toString(), results.size(), results.toString()));
            }
        }
        scheduledExecutorService.schedule(analyser, analysisPeriod, UNIT);
        return status;
    }

    private Collection<Long> analyse() {
        Collection<Long> results = new ArrayList<>();
        for (Worker worker : workers) {
            if (worker.isObservable()) {
                results.add(worker.getProcessed());
            }
        }
        return results;
    }

    private Integer compare(Long current, Long lower, Long higher) {
        if (higher != null && lower != null) {
            return higher > lower ? isWorth(higher, current) ? 1 : 0 : lower >= current ? -1 : 0;
        } else if (higher != null) {
            return isWorth(higher, current) ? 1 : -1;
        } else if (lower != null) {
            return lower >= current ? -1 : 1;
        }
        return null;
    }

    private boolean isWorth(double val1, double val2) {
        return (val1 - val2) / val2 > profile.worth;
    }

    private long getThroughput(Collection<Long> data) {
        long total = 0;
        for (Long value : data) {
            total += value;
        }
        return total;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    private void setMinThread(int min) {
        if (min < DEFAULT_MIN) {
            throw new IllegalArgumentException("Invalid minimum " + min + ", must be at least " + DEFAULT_MIN);
        }
        minWorkers = min;
    }

    private void setMaxThread(int max) {
        if (max < minWorkers) {
            throw new IllegalArgumentException("Invalid maximum " + max + ", must be greater than minimum " + minWorkers);
        }
        maxWorkers = max;
    }

    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    protected void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        exceptionHandler.register(this);
    }

    protected class Worker implements Runnable {

        private final Processor<T> processor;
        private final AtomicLong ctr = new AtomicLong();

        private volatile boolean working;

        private volatile boolean observable;

        protected Worker(Processor<T> processor) {
            this.processor = processor;
        }

        @Override
        public void run() {
            try {
                phaser.register();
                synchronized (this) {
                    working = true;
                    while (working) {
                        T t = queue.poll(CLOCK, UNIT);
                        if (t != null) {
                            long begin = System.nanoTime();
                            work(t);
                            long end = System.nanoTime();
                            if (balance) {
                                feed(1);
                                double time = end - begin;
                                averageWorkTime = averageWorkTime * profile.low + time * profile.high;
                            }
                        } else if (shutdown) {
                            working = false;
                        } else if (balance) {
                            feed(0);
                        }
                    }
                }
            } catch (InterruptedException ignore) {
            } catch (Exception unhandled) {
                exceptionHandler.handle(unhandled);
            } finally {
                phaser.arrive();
            }
        }

        private void feed(long delta) {
            ctr.addAndGet(delta);
            observable = true;
        }

        protected void work(T t) throws Exception {
            processor.process(t);
        }

        public boolean isObservable() {
            return observable;
        }

        protected long getProcessed() {
            long processed = ctr.get();
            reset();
            return processed;
        }

        public void reset() {
            ctr.set(0);
            observable = false;
        }

        public void shutdown() {
            working = false;
        }

        public Processor<T> getProcessor() {
            return processor;
        }
    }
}
