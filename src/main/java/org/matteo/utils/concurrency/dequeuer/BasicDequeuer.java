package org.matteo.utils.concurrency.dequeuer;

import org.matteo.utils.concurrency.Async;
import org.matteo.utils.concurrency.exception.ExceptionHandler;
import org.matteo.utils.concurrency.NamedThreadFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 13/07/12
 */
public class BasicDequeuer<T> implements Dequeuer<T> {

    static final TimeUnit UNIT = TimeUnit.NANOSECONDS;
    static final long CLOCK = TimeUnit.SECONDS.toNanos(1);

    BlockingQueue<T> queue;

    private String name;
    private ExecutorService service;
    final Phaser phaser = new Phaser();

    final List<Processor<T>> processors = new ArrayList<>();

    private final Collection<T> unprocessed = new ArrayList<>();

    volatile boolean shutdown;
    private volatile boolean terminated;

    CompleteAction<T> completeAction;

    ExceptionHandler exceptionHandler = new ExceptionHandler();

    public BasicDequeuer(Processor<T> processor) {
        this(processor, true, Runtime.getRuntime().availableProcessors());
    }

    public BasicDequeuer(Processor<T> processor, int threads) {
        this(processor, true, threads);
    }

    public BasicDequeuer(Processor<T> processor, boolean synchronous, int threads) {
        this(synchronous);
        processors.add(processor);
        for (int i = 0; i < threads; i++) {
            Worker worker = createWorker(processor);
            startWorker(worker);
        }
    }

    public BasicDequeuer(Collection<? extends Processor<T>> processors, boolean synchronous) {
        this(synchronous);
        for (Processor<T> processor : processors) {
            this.processors.add(processor);
            Worker worker = createWorker(processor);
            startWorker(worker);
        }
    }

    BasicDequeuer(boolean synchronous) {
        this.name = "BasicDequeuer";
        service = Executors.newCachedThreadPool(new NamedThreadFactory(name));
        queue = getQueue(synchronous);
        exceptionHandler.register(this);
        phaser.register();
    }

    private BlockingQueue<T> getQueue(boolean synchronous) {
        return synchronous ? new SynchronousQueue<>() : new LinkedBlockingQueue<>();
    }

    private Worker createWorker(Processor<T> processor) {
        return new Worker(processor);
    }

    void startWorker(Worker worker) {
        service.submit(worker);
    }

    @Override
    public void shutdown() {
        shutdown = true;
    }

    @Override
    public synchronized void shutdownNow() {
        try {
            shutdown = true;
            queue.drainTo(unprocessed);
            service.shutdownNow();
        } finally {
            terminate();
        }
    }

    @Override
    public boolean awaitTermination(long time, TimeUnit unit) throws Exception {
        boolean elapsed = false;
        if (!terminated) {
            try {
                shutdown();
                Async.run(() -> {
                    phaser.arriveAndAwaitAdvance();
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

    @Override
    public Collection<T> getUnprocessed() {
        return unprocessed;
    }

    @Override
    public boolean isTerminated() {
        return terminated;
    }

    @Override
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
            exceptionHandler.remove(this);
        }
    }

    void setCompleteAction(CompleteAction<T> completeAction) {
        this.completeAction = completeAction;
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    void setExceptionHandler(ExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
        exceptionHandler.register(this);
    }

    interface CompleteAction<T> {
        void onComplete(T t) throws Exception;
    }

    protected class Worker implements Runnable {

        final Processor<T> processor;

        Worker(Processor<T> processor) {
            this.processor = processor;
        }

        @Override
        public void run() {
            try {
                phaser.register();
                synchronized (this) {
                    boolean working = true;
                    while (working) {
                        T t = queue.poll(CLOCK, UNIT);
                        if (t != null) {
                            processor.process(t);
                            if (completeAction != null) {
                                completeAction.onComplete(t);
                            }
                        } else if (shutdown) {
                            working = false;
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
    }
}
