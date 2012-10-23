package org.forgerock.openidm.scheduler.impl;

import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.objset.ObjectSetException;

public class BadRequestException extends ObjectSetException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public BadRequestException() {
        super(JsonResourceException.BAD_REQUEST, (String)null);
    }

    /**
     * Constructs a new exception with the specified detail message.
     */
    public BadRequestException(String message) {
        super(JsonResourceException.BAD_REQUEST, message);
    }

    /**
     * Constructs a new exception with the specified cause.
     */
    public BadRequestException(Throwable cause) {
        super(JsonResourceException.BAD_REQUEST, cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public BadRequestException(String message, Throwable cause) {
        super(JsonResourceException.BAD_REQUEST, message, cause);
    }
}
