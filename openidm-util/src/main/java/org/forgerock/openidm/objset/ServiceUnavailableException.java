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

package org.forgerock.openidm.objset;

/**
 * An exception that is thrown during an operation on an object set when the server
 * is temporarily unable to handle the request.
 *
 * @author Paul C. Bryan
 */
public class ServiceUnavailableException extends ObjectSetException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public ServiceUnavailableException() {
    }
    
    /**
     * Constructs a new exception with the specified detail message.
     */
    public ServiceUnavailableException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new exception with the specified cause.
     */
    public ServiceUnavailableException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
