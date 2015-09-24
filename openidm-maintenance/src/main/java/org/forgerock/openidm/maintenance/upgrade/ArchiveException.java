package org.forgerock.openidm.maintenance.upgrade;

/**
 */
public class ArchiveException extends UpdateException {
    private static final long serialVersionUID = 1L;

    ArchiveException(String message) {
        super(message);
    }
    ArchiveException(String message, Exception cause) {
        super(message, cause);
    }
}
