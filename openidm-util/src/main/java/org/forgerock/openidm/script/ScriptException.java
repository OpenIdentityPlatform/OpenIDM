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

package org.forgerock.openidm.script;

import java.util.Map;

import org.forgerock.json.resource.JsonResourceException;

/**
 * An exception that is thrown during script operations.
 *
 * @author Paul C. Bryan
 */
public class ScriptException extends Exception {

    /** Serializable class a version number. */
    static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public ScriptException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     */
    public ScriptException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new exception with the specified cause.
     */
    public ScriptException(Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     */
    public ScriptException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Converts the script exception to an appropriate json resource exception.
     * 
     * The exception message is set to the defaultMsg by this exception, 
     * subclasses may override and provide a prescedence on where the message comes from
     *
     * @param defaultMsg a default message to use 
     * @return the appropriate JsonResourceException
     */
    public JsonResourceException toJsonResourceException(String defaultMsg) {
        return toJsonResourceException(JsonResourceException.INTERNAL_ERROR, defaultMsg);
//        return new JsonResourceException(JsonResourceException.INTERNAL_ERROR, defaultMsg, this);
    }
    
    /**
     * Converts the script exception to an appropriate json resource exception.
     * 
     * The exception message is set to the defaultMsg by this exception, 
     * and the error code to the defaultCode
     * subclasses may override and provide a prescedence on where the 
     * used code and message comes from
     *
     * @param defaultCode a default json resource reason error code
     * @param defaultMsg a default message to use 
     * @return the appropriate JsonResourceException
     */
    public JsonResourceException toJsonResourceException(int defaultCode, String defaultMsg) {
        return new JsonResourceException(defaultCode, defaultMsg, this);
    }

}
