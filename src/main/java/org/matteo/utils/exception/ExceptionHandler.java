package org.matteo.utils.exception;

import org.matteo.utils.concurrency.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 09/06/12
 */
public class ExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

    private Exception exception;
    private boolean error;
    private Future halt;

    private final Collection<ExceptionListener> listeners = new ArrayList<>();
    private final Map<Object, ShutdownAction> shutdownActions = new LinkedHashMap<>();

    public synchronized void handle(Exception e) {
        if (!error) {
            error = true;
            this.exception = e;
            logger.error("An error occurred, shutting down NOW", e);
            final Collection<ShutdownAction> actions = new ArrayList<>(shutdownActions.values());
            halt = Async.run(() -> {
                for (ExceptionListener listener : listeners) {
                    listener.onException(e);
                }
                for (ShutdownAction shutdownAction : actions) {
                    shutdownAction.shutdownNow();
                }
            }, "ExceptionHandler");
        }
    }

    public synchronized void register(Object object, ShutdownAction action) {
        shutdownActions.put(object, action);
    }

    public synchronized void register(ShutdownAction shutdownAction) {
        shutdownActions.put(shutdownAction, shutdownAction);
    }

    public synchronized void register(ExceptionListener listener) {
        listeners.add(listener);
    }

    public synchronized void remove(Object object) {
        shutdownActions.remove(object);
    }

    public Exception getException() {
        return exception;
    }

    public boolean isExhausted() {
        return error;
    }

    public void waitForShutdown() throws Exception {
        if (halt != null) {
            halt.get();
        }
    }
}
