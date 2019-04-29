package org.matteo.utils.concurrency.dequeuer;

/**
 * Created with IntelliJ IDEA.
 * User: Matteo Trotta
 * Date: 28/02/13
 */
public class RejectedObjectException extends Exception {

    public RejectedObjectException(String message) {
        super(message);
    }

}
