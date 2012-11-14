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
 * An exception that is thrown during a operation on an object set when the specified object
 * version does not match the version provided.
 *
 * @author Paul C. Bryan
 */
public class PreconditionFailedException extends ObjectSetException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public PreconditionFailedException() {
        super(JsonResourceException.VERSION_MISMATCH);
    }
    
    /**
     * Constructs a new exception with the specified detail message.
     */
    public PreconditionFailedException(String message) {
        super(JsonResourceException.VERSION_MISMATCH, message);
    }
    
    /**
     * Constructs a new exception with the specified cause.
     */
    public PreconditionFailedException(Throwable cause) {
        super(JsonResourceException.VERSION_MISMATCH, cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public PreconditionFailedException(String message, Throwable cause) {
        super(JsonResourceException.VERSION_MISMATCH, message, cause);
    }

    /**
     * @inheritDoc
     */
    public PreconditionFailedException(String message, Map<String, Object> failureDetail) {
        super(JsonResourceException.VERSION_MISMATCH, message, failureDetail);
    }

    /**
     * @inheritDoc
     */
    public PreconditionFailedException(String message, Map<String, Object> failureDetail, Throwable cause) {
        super(JsonResourceException.VERSION_MISMATCH, message, failureDetail, cause);
    }
}
