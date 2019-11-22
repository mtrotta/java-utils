package org.matteo.utils.clean;

public class EraseException extends Exception {

    public EraseException(String message) {
        super(message);
    }

    public EraseException(String message, Throwable cause) {
        super(message, cause);
    }

    public EraseException(Throwable cause) {
        super(cause);
    }

}
