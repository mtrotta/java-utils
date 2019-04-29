package org.matteo.utils.concurrency.dequeuer;

import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 02/07/12
 */
public class ChainedDequeuer<T> extends Dequeuer<T> {

    private ChainedDequeuer<T> previous;
    private ChainedDequeuer<T> next;

    public ChainedDequeuer(Processor<T> processor, int threads) {
        this(processor, true, threads);
    }

    public ChainedDequeuer(Processor<T> processor, boolean synchronous, int threads) {
        super(processor, synchronous, threads);
    }

    public ChainedDequeuer<T> append(ChainedDequeuer<T> next) {
        this.next = next;
        next.previous = this;
        next.setExceptionHandler(getExceptionHandler());
        return next;
    }

    public ChainedDequeuer<T> getRoot() {
        return previous != null ? previous.getRoot() : this;
    }

    @Override
    public synchronized void shutdownNow() {
        getRoot().shutdownNowChain();
    }

    private synchronized void shutdownNowChain() {
        super.shutdownNow();
        if (next != null) {
            next.shutdownNowChain();
        }
    }

    public boolean awaitTermination(long time, TimeUnit unit) throws Exception {
        return getRoot().awaitChainTermination(time, unit);
    }

    private boolean awaitChainTermination(long time, TimeUnit unit) throws Exception {
        boolean elapsed = super.awaitTermination(time, unit);
        if (next != null) {
            elapsed |= next.awaitChainTermination(time, unit);
        }
        return elapsed;
    }

    @Override
    public boolean isAborted() {
        return getRoot().isChainAborted();
    }

    private boolean isChainAborted() {
        return super.isAborted() || next.isChainAborted();
    }

    protected Worker createWorker(Processor<T> processor) {
        return new ChainWorker(processor);
    }

    protected class ChainWorker extends Worker {

        public ChainWorker(Processor<T> processor) {
            super(processor);
        }

        @Override
        protected void work(T t) throws Exception {
            super.work(t);
            if (next != null) {
                next.enqueue(t);
            }
        }
    }

}
