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

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.JsonResourceException;
import org.forgerock.openidm.objset.JsonResourceObjectSet;
import org.forgerock.openidm.objset.ObjectSetException;

/**
 * An exception that is thrown to indicate that an executed script encountered an exception.
 *
 * @author Paul C. Bryan
 */
public class ScriptThrownException extends ScriptException {

    /** Serializable class a version number. */
    static final long serialVersionUID = 1L;

    /** Value that was thrown by the script. */
    private Object value;

    /**
     * Constructs a new exception with the specified value and {@code null} as its detail
     * message.
     */
    public ScriptThrownException(Object value) {
        this.value = value;
    }

    /**
     * Constructs a new exception with the specified value and detail message.
     */
    public ScriptThrownException(Object value, String message) {
        super(message);
        this.value = value;
    }
    
    /**
     * Constructs a new exception with the specified value and cause.
     */
    public ScriptThrownException(Object value, Throwable cause) {
        super(cause);
        this.value = value;
    }

    /**
     * Constructs a new exception with the specified value, detail message and cause.
     */
    public ScriptThrownException(Object value, String message, Throwable cause) {
        super(message, cause);
        this.value = value;
    }

    /**
     * Returns the value that was thrown from the script.
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * Converts the script exception to an appropriate json resource exception.
     * 
     * The exception message is set to, in order of precedence
     * 1. Specific message set in the thrown script exception
     * 2. Default exception supplied to this method, or if null
     * 3. value toString of this exception 
     * @param defaultMsg a default message to use if no explicit message is set, or null if 
     * value toString shoudl be used instead
     * @return the appropriate JsonResourceException
     */
    @Override
    public JsonResourceException toJsonResourceException(int defaultCode, String defaultMsg) {
        if (value != null && value instanceof Map) {
            // Convention on structuring well known exceptions with value that contains
            // openidmCode : Integer matching JsonResourceException codes 
            //               (required for it to be considered a known exception definition)
            // message : String - optional exception message, set to value.toString if not present
            // detail : Map<String, Object> - optional structure with exception detail
            // cause : Throwable - optional cause to chain
            JsonValue val = new JsonValue(value);
            Integer openidmCode = val.get("openidmCode").asInteger();
            if (openidmCode != null) {
                String message = val.get("message").asString();
                if (message == null) {
                    if (defaultMsg != null) {
                        message = defaultMsg;
                    } else {
                        message = (value == null ? null : value.toString());
                    }
                }
                Map<String, Object> failureDetail = (Map<String, Object>) val.get("detail").asMap();
                Throwable throwable = (Throwable) val.get("cause").getObject();
                if (throwable == null) {
                    throwable = this;
                }
                return new ObjectSetException(openidmCode.intValue(), message, failureDetail, throwable);
            }
        }
        if (defaultMsg != null) {
            return new JsonResourceException(defaultCode, defaultMsg, this);
        } else if (value == null) {
            return new JsonResourceException(defaultCode, this);
        } else {
            return new JsonResourceException(defaultCode, value.toString(), this);
        }
    }
}
