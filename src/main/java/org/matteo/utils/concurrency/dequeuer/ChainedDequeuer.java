package org.matteo.utils.concurrency.dequeuer;

import org.matteo.utils.concurrency.exception.ExceptionHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 02/07/12
 */
public class ChainedDequeuer<T> implements Dequeuer<T> {

    private final LinkedList<Dequeuer<T>> chain = new LinkedList<>();

    private final ExceptionHandler exceptionHandler = new ExceptionHandler();

    public ChainedDequeuer(Collection<BasicDequeuer<T>> dequeuers) {
        if (dequeuers.isEmpty()) {
            throw new IllegalArgumentException("An empty chained dequeuer doesn't make sense");
        }
        Iterator<BasicDequeuer<T>> iterator = dequeuers.iterator();
        BasicDequeuer<T> previous = null;
        while (iterator.hasNext()) {
            BasicDequeuer<T> next = iterator.next();
            next.setExceptionHandler(exceptionHandler);
            chain.add(next);
            if (previous != null) {
                previous.setCompleteAction(next::enqueue);
            }
            previous = next;
        }
    }

    @Override
    public synchronized void shutdownNow() {
        for (Dequeuer<T> dequeuer : chain) {
            dequeuer.shutdownNow();
        }
    }

    @Override
    public void shutdown() {
        for (Dequeuer<T> dequeuer : chain) {
            dequeuer.shutdown();
        }
    }

    @Override
    public boolean awaitTermination(long time, TimeUnit unit) throws Exception {
        boolean elapsed = false;
        for (Dequeuer<T> dequeuer : chain) {
            elapsed |= dequeuer.awaitTermination(time, unit);
        }
        return elapsed;
    }

    @Override
    public Collection<T> getUnprocessed() {
        final Collection<T> unprocessed = new ArrayList<>();
        chain.forEach(c -> unprocessed.addAll(c.getUnprocessed()));
        return unprocessed;
    }

    @Override
    public boolean isTerminated() {
        boolean terminated = true;
        for (Dequeuer<T> dequeuer : chain) {
            terminated &= dequeuer.isTerminated();
        }
        return terminated;
    }

    @Override
    public boolean isAborted() {
        boolean aborted = false;
        for (Dequeuer<T> dequeuer : chain) {
            aborted |= dequeuer.isAborted();
        }
        return aborted;
    }

    @Override
    public void enqueue(T t) throws RejectedObjectException, InterruptedException {
        chain.getFirst().enqueue(t);
    }

    @Override
    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

}
