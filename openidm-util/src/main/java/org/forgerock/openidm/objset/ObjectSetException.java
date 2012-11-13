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

// JSON Resource
import java.util.Map;

import org.forgerock.json.resource.JsonResourceException;

/**
 * An exception that is thrown during an operation on an object set.
 *
 * @author Paul C. Bryan
 */
public class ObjectSetException extends JsonResourceException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public ObjectSetException() {
        super(JsonResourceException.INTERNAL_ERROR, (String)null);
    }
    
    /**
     * Constructs a new exception with the specified detail message.
     */
    public ObjectSetException(String message) {
        super(JsonResourceException.INTERNAL_ERROR, message);
    }
    
    /**
     * Constructs a new exception with the specified cause.
     */
    public ObjectSetException(Throwable cause) {
        super(JsonResourceException.INTERNAL_ERROR, cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public ObjectSetException(String message, Throwable cause) {
        super(JsonResourceException.INTERNAL_ERROR, message, cause);
    }

    /**
     * Constructs a new exception with the specified code
     */
    public ObjectSetException(int code) {
        super(code);
    }

    /**
     * Constructs a new exception with the specified code and detail message
     */
    public ObjectSetException(int code, String message) {
        super(code, message);
    }

    /**
     * Constructs a new exception with the specified code and cause
     */
    public ObjectSetException(int code, Throwable cause) {
        super(code, cause);
    }

    /**
     * Constructs a new exception with the specified code, detail message
     * and cause
     */
    public ObjectSetException(int code, String message, Throwable cause) {
        super(code, message, cause);
    }

    /**
     * Constructs a new exception with the specified code, detail message
     * and failure detail.
     */
    public ObjectSetException(int code, String message, Map<String, Object> failureDetail) {
        this(code, message);
        setDetail(failureDetail);
    }

    /**
     * Constructs a new exception with the specified code, detail message, 
     * failure detail and cause.
     */
    public ObjectSetException(int code, String message, Map<String, Object> failureDetail, Throwable cause) {
        this(code, message, cause);
        setDetail(failureDetail);
    }

}
