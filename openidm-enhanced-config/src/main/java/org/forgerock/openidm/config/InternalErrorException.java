/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openidm.config;

/**
 * An exception that is throw for internal (possibly transient) errors in 
 * retrieving configuration
 * 
 * @author aegloff
 */
public class InternalErrorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public InternalErrorException() {
    }
    
    /**
     * Constructs a new exception with the specified detail message.
     */
    public InternalErrorException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new exception with the specified cause.
     */
    public InternalErrorException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public InternalErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
