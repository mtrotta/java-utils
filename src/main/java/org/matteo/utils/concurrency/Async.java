package org.matteo.utils.concurrency;

import org.matteo.utils.concurrency.exception.ExceptionHandler;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Async {

    private Async() {
    }

    public static Future run(Runnable runnable) {
        return run(runnable, null, UUID.randomUUID().toString());
    }

    public static Future run(Runnable runnable, String name) {
        return run(runnable, null, name);
    }

    public static Future run(Runnable runnable, ExceptionHandler exceptionHandler, String name) {
        final ExecutorService service = Executors.newSingleThreadExecutor(new NamedThreadFactory(name));
        if (exceptionHandler != null) {
            exceptionHandler.register(service, service::shutdownNow);
        }
        try {
            return service.submit(runnable);
        } finally {
            service.shutdown();
            if (exceptionHandler != null) {
                exceptionHandler.remove(service);
            }
        }
    }

}
