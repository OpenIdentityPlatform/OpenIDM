package org.forgerock.openidm.sync.impl;

/**
 * An exception that is thrown during synchronization operations.
 *
 * @author Paul C. Bryan
 */
public class SynchronizationException extends Exception {

    /** Serializable class a version number. */
    static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public SynchronizationException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     */
    public SynchronizationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified cause.
     */
    public SynchronizationException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public SynchronizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
