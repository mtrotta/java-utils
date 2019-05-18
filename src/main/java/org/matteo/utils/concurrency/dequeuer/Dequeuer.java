package org.matteo.utils.concurrency.dequeuer;

import org.matteo.utils.concurrency.exception.ExceptionHandler;
import org.matteo.utils.concurrency.exception.ShutdownAction;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public interface Dequeuer<T> extends ShutdownAction {

    void enqueue(T t) throws RejectedObjectException, InterruptedException;

    void shutdown();

    boolean awaitTermination(long time, TimeUnit unit) throws Exception;

    Collection<T> getUnprocessed();

    boolean isTerminated();

    ExceptionHandler getExceptionHandler();

}
