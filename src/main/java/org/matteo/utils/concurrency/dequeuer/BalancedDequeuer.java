package org.matteo.utils.concurrency.dequeuer;

import org.matteo.utils.concurrency.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 13/07/12
 */
public class BalancedDequeuer<T> extends BasicDequeuer<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalancedDequeuer.class);

    private final List<BalancedWorker> workers = new ArrayList<>();

    private int minWorkers;
    private int maxWorkers;
    private AtomicInteger numWorkers;
    private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("DequeuerBalancer"));
    private final TreeMap<Integer, Long> reference = new TreeMap<>();

    private Profile profile = Profile.MEDIUM;

    private static final int DEFAULT_MIN = 1;
    private static final int DEFAULT_MAX = Runtime.getRuntime().availableProcessors();

    private volatile double averageWorkTime = CLOCK;

    private final Runnable analyser = () -> {
        try {
            switch (getWorkerStatus()) {
                case INCREASE:
                    increaseWorkers();
                    break;
                case DECREASE:
                case IDLE:
                    decreaseWorkers();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            exceptionHandler.handle(e);
        }
    };

    public BalancedDequeuer(Processor<T> processor) {
        this(processor, true, DEFAULT_MIN, DEFAULT_MAX);
    }

    public BalancedDequeuer(Processor<T> processor, boolean synchronous) {
        this(processor, synchronous, DEFAULT_MIN, DEFAULT_MAX);
    }

    public BalancedDequeuer(final Processor<T> processor, boolean synchronous, int min, int max) {
        this(processor, synchronous, min, max, min);
    }

    public BalancedDequeuer(final Processor<T> processor, boolean synchronous, int min, int max, int initial) {
        super(synchronous);
        processors.add(processor);
        for (int i = 0; i < max; i++) {
            addWorker(processor);
        }
        startBalance(min, max, initial);
    }

    public BalancedDequeuer(Collection<? extends Processor<T>> processors, boolean synchronous, int min) {
        this(processors, synchronous, min, min);
    }

    public BalancedDequeuer(Collection<? extends Processor<T>> processors, boolean synchronous, int min, int initial) {
        super(synchronous);
        for (Processor<T> processor : processors) {
            this.processors.add(processor);
            addWorker(processor);
        }
        startBalance(min, processors.size(), initial);
    }

    private void addWorker(Processor<T> processor) {
        BalancedWorker worker = new BalancedWorker(processor);
        workers.add(worker);
    }

    @Override
    protected synchronized void terminate() {
        super.terminate();
        scheduledExecutorService.shutdownNow();
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
        scheduledExecutorService.schedule(analyser, profile.period * CLOCK, UNIT);
    }

    private synchronized void increaseWorkers() {
        try {
            final int num = numWorkers.get();
            if (num < maxWorkers) {
                BalancedWorker worker = workers.get(numWorkers.getAndIncrement());
                startWorker(worker);
            }
        } catch (Exception e) {
            exceptionHandler.handle(e);
        }
    }

    private synchronized void decreaseWorkers() {
        if (numWorkers.get() > minWorkers) {
            BalancedWorker worker = workers.get(numWorkers.decrementAndGet());
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
        for (BalancedWorker worker : workers) {
            if (worker.isObservable()) {
                results.add(worker.getProcessed());
            }
        }
        return results;
    }

    private Integer compare(Long current, Long lower, Long higher) {
        if (higher != null && lower != null) {
            if (higher > lower) {
                return isWorth(higher, current) ? 1 : 0;
            } else {
                return lower >= current ? -1 : 0;
            }
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

    protected class BalancedWorker extends Worker {

        private final AtomicLong ctr = new AtomicLong();

        private volatile boolean working;
        private volatile boolean observable;

        private BalancedWorker(Processor<T> processor) {
            super(processor);
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
                            processor.process(t);
                            long end = System.nanoTime();
                            feed(1);
                            long time = end - begin;
                            averageWorkTime = averageWorkTime * profile.low + time * profile.high;
                            if (completeAction != null) {
                                completeAction.onComplete(t);
                            }
                        } else if (shutdown) {
                            working = false;
                        } else {
                            feed(0);
                        }
                    }
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
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

        boolean isObservable() {
            return observable;
        }

        long getProcessed() {
            long processed = ctr.get();
            ctr.set(0);
            observable = false;
            return processed;
        }

        void shutdown() {
            working = false;
        }

    }
}
